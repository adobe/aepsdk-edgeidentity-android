/*
  Copyright 2026 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.edge.identity;

import android.app.Application;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.SharedStateResolution;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.SharedStateStatus;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.DataReader;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Sends device attributes to Edge after the rules engine has filtered them.
 *
 * <ol>
 *     <li>{@link #handleDeviceAttributes(Event)} sends the public API data to the rules engine.</li>
 *     <li>Each forward-operational-data consequence comes back through {@link #handleDeviceAttributeRuleResponse(Event)}.</li>
 *     <li>Consequences wait in a FIFO queue until the previous Edge request finishes. Values already
 *     synced are dropped and only changed values are sent to Edge.</li>
 *     <li>When Edge reports success ({@link #handleDeviceAttributeResult(Event)}), the complete confirmed store is persisted and
 *     published in {@code profileAttributes}, which Messaging reads to keep its token store in sync.</li>
 * </ol>
 *
 * <p>Confirming a path replaces previously confirmed ancestor and descendant paths, while
 * preserving unrelated values. Failed requests leave the confirmed store unchanged.</p>
 */
final class DeviceAttributeSync {

	static final String RULE_RESPONSE_SOURCE = "com.adobe.eventSource.responseOperationalData";
	static final String BYPASS_TYPE = "com.adobe.eventType.edgeBypassConsent";
	private static final String LOG_SOURCE = "DeviceAttributeSync";
	private static final String APP = "app";

	private final ExtensionApi api;
	private final ProfileAttributeStore profileAttributeStore;
	private final Deque<Event> deviceAttributeQueue = new ArrayDeque<>();
	private Event deviceAttributesForEdge;
	private Map<String, Object> pendingDeviceAttributes;
	private final DeviceAttributeDeduplicator deviceAttributeDeduplicator;

	DeviceAttributeSync(final ExtensionApi api, final ProfileAttributeStore profileAttributeStore) {
		this(
			api,
			profileAttributeStore,
			new ProfileAttributeStore(
				ServiceProvider
					.getInstance()
					.getDataStoreService()
					.getNamedCollection(IdentityConstants.DataStoreKey.DEVICE_ATTRIBUTES_DATASTORE_NAME)
			)
		);
	}

	DeviceAttributeSync(
		final ExtensionApi api,
		final ProfileAttributeStore profileAttributeStore,
		final ProfileAttributeStore deviceAttributeStore
	) {
		this.api = api;
		this.profileAttributeStore =
			profileAttributeStore != null ? profileAttributeStore : new ProfileAttributeStore(null);
		this.deviceAttributeDeduplicator = new DeviceAttributeDeduplicator(deviceAttributeStore);
		// Preserve confirmed values from the earlier shared-store implementation.
		final Map<String, Object> persisted = this.profileAttributeStore.getMap("deviceAttributes");
		if (
			deviceAttributeStore.getMap(IdentityConstants.DeviceAttributes.SYNCED_VALUES) == null && persisted != null
		) {
			deviceAttributeDeduplicator.commit(persisted);
		}
	}

	/** Forwards public API data to the rules engine, which decides which attributes may be sent. */
	void handleDeviceAttributes(final Event event) {
		final Map<String, Object> data = event.getEventData();
		if (data == null || data.isEmpty()) {
			Log.warning(IdentityConstants.LOG_TAG, LOG_SOURCE, "Ignoring empty or invalid device attribute request.");
			return;
		}
		final Event operationalEvent = new Event.Builder(
			IdentityConstants.EventNames.DEVICE_ATTRIBUTES_TO_RULES_ENGINE,
			IdentityConstants.DeviceAttributes.OPERATIONAL_DATA_TYPE,
			EventSource.REQUEST_CONTENT
		)
			.setEventData(data)
			.chainToParentEvent(event)
			.build();
		api.dispatch(operationalEvent);
	}

	/** Queues a consequence until the previous Edge request has completed. */
	void handleDeviceAttributeRuleResponse(final Event event) {
		if (event.getEventData() == null || event.getEventData().isEmpty()) {
			Log.debug(IdentityConstants.LOG_TAG, LOG_SOURCE, "Rule response contains no device attributes.");
			return;
		}
		deviceAttributeQueue.addLast(event);
		sendNextDeviceAttributeRequest();
	}

	private void sendNextDeviceAttributeRequest() {
		while (deviceAttributesForEdge == null && !deviceAttributeQueue.isEmpty()) sendChangedValues(
			deviceAttributeQueue.removeFirst()
		);
	}

	private void sendChangedValues(final Event event) {
		final Map<String, Object> attributes = event.getEventData();
		final boolean dedup = !Boolean.FALSE.equals(
			configuration().get(IdentityConstants.DeviceAttributes.DEDUP_CONFIG_KEY)
		);
		final boolean compareOwners = dedup && isCollectConsentGranted();
		final SharedStateResult messagingResult = compareOwners
			? api.getSharedState(
				IdentityConstants.SharedState.Messaging.NAME,
				null,
				false,
				SharedStateResolution.LAST_SET
			)
			: null;
		final Map<String, Object> messagingState = messagingResult != null &&
			messagingResult.getStatus() == SharedStateStatus.SET
			? messagingResult.getValue()
			: null;
		final String timezone = compareOwners ? profileAttributeStore.getString("timeZone") : null;
		final Map<String, Object> payload = deviceAttributeDeduplicator.filter(
			attributes,
			dedup,
			messagingState,
			timezone
		);
		if (payload.isEmpty()) {
			Log.debug(IdentityConstants.LOG_TAG, LOG_SOURCE, "Device attributes unchanged, not sending to Edge.");
			return;
		}

		final Application application = MobileCore.getApplication();
		final String appId = application != null ? application.getPackageName() : null;
		if (appId == null || appId.isEmpty()) {
			Log.warning(
				IdentityConstants.LOG_TAG,
				LOG_SOURCE,
				"Cannot send device attributes without an app package identifier."
			);
			return;
		}
		payload.put(APP, Map.of("id", appId, "platform", "fcm"));
		final Event edgeEvent = new Event.Builder("Edge bypass request", BYPASS_TYPE, EventSource.REQUEST_CONTENT)
			.setEventData(edgePayload(payload))
			.chainToParentEvent(event)
			.build();
		deviceAttributesForEdge = edgeEvent;
		pendingDeviceAttributes = payload;
		api.dispatch(edgeEvent);
	}

	/**
	 * Edge requires {@code tokens.pushNotification} as {@code {"value": token}}. The SDK keeps the
	 * token as a string internally, so only the outgoing request is wrapped.
	 */
	static Map<String, Object> edgePayload(final Map<String, Object> payload) {
		final Map<String, Object> tokens = DataReader.optTypedMap(
			Object.class,
			payload,
			IdentityConstants.DeviceAttributes.TOKENS,
			null
		);
		if (tokens == null || !(tokens.get(IdentityConstants.DeviceAttributes.PUSH_NOTIFICATION) instanceof String)) {
			return payload;
		}
		final Map<String, Object> edgeTokens = new HashMap<>(tokens);
		edgeTokens.put(
			IdentityConstants.DeviceAttributes.PUSH_NOTIFICATION,
			Collections.singletonMap(
				IdentityConstants.DeviceAttributes.TOKEN_VALUE,
				tokens.get(IdentityConstants.DeviceAttributes.PUSH_NOTIFICATION)
			)
		);
		final Map<String, Object> edgePayload = new HashMap<>(payload);
		edgePayload.put(IdentityConstants.DeviceAttributes.TOKENS, edgeTokens);
		return edgePayload;
	}

	/** Stores successful values before processing the next queued consequence. */
	void handleDeviceAttributeResult(final Event event) {
		if (
			deviceAttributesForEdge == null ||
			!deviceAttributesForEdge.getUniqueIdentifier().equals(event.getParentID())
		) return;
		final String status = DataReader.optString(event.getEventData(), "status", null);
		if (!"success".equals(status) && !"failed".equals(status) && !"dropped".equals(status)) {
			Log.warning(
				IdentityConstants.LOG_TAG,
				LOG_SOURCE,
				"Ignoring device attribute result with an invalid status."
			);
			return;
		}
		if ("success".equals(status)) {
			final Map<String, Object> attributes = pendingDeviceAttributes;
			deviceAttributeDeduplicator.commit(attributes);
			final String timezone = DataReader.optString(attributes, IdentityConstants.DeviceAttributes.TIMEZONE, null);
			if (timezone != null) profileAttributeStore.setString("timeZone", timezone);
			publishProfileAttributes(event);
			final Map<String, Object> tokens = DataReader.optTypedMap(
				Object.class,
				attributes,
				IdentityConstants.DeviceAttributes.TOKENS,
				null
			);
			if (tokens != null && !tokens.isEmpty()) {
				api.dispatch(
					new Event.Builder(
						IdentityConstants.EventNames.DEVICE_ATTRIBUTES_CONFIRMED,
						EventType.MESSAGING,
						EventSource.RESPONSE_CONTENT
					)
						.setEventData(Collections.singletonMap(IdentityConstants.DeviceAttributes.TOKENS, tokens))
						.chainToParentEvent(event)
						.build()
				);
			}
		} else {
			Log.warning(
				IdentityConstants.LOG_TAG,
				LOG_SOURCE,
				"Device attribute sync %s: %s. Values remain unsynced.",
				status,
				DataReader.optString(event.getEventData(), "reason", "unknown")
			);
		}
		deviceAttributesForEdge = null;
		pendingDeviceAttributes = null;
		sendNextDeviceAttributeRequest();
	}

	private boolean isCollectConsentGranted() {
		final SharedStateResult consentResult = api.getXDMSharedState(
			IdentityConstants.SharedState.Consent.NAME,
			null,
			false,
			SharedStateResolution.ANY
		);
		if (consentResult != null && consentResult.getStatus() == SharedStateStatus.SET) {
			final Map<String, Object> consents = DataReader.optTypedMap(
				Object.class,
				consentResult.getValue(),
				IdentityConstants.SharedState.Consent.CONSENTS,
				null
			);
			final Map<String, Object> collect = DataReader.optTypedMap(
				Object.class,
				consents,
				IdentityConstants.SharedState.Consent.COLLECT,
				null
			);
			final String value = DataReader.optString(collect, IdentityConstants.SharedState.Consent.VALUE, null);
			return IdentityConstants.SharedState.Consent.YES.equals(value);
		}

		return false;
	}

	void reset(final Event event) {
		deviceAttributeQueue.clear();
		deviceAttributesForEdge = null;
		pendingDeviceAttributes = null;
		deviceAttributeDeduplicator.clear();
		profileAttributeStore.clearAll();
		publishProfileAttributes(event);
	}

	void publishProfileAttributes(final Event event) {
		final Map<String, Object> attributes = deviceAttributeDeduplicator.readSynced();
		final String timezone = profileAttributeStore.getString("timeZone");
		if (timezone != null) attributes.put("timezone", timezone);
		api.createSharedState(
			Collections.singletonMap(IdentityConstants.ProfileAttributes.SHARED_STATE_KEY, attributes),
			event
		);
	}

	private Map<String, Object> configuration() {
		final SharedStateResult result = api.getSharedState(
			IdentityConstants.SharedState.Configuration.NAME,
			null,
			false,
			SharedStateResolution.LAST_SET
		);
		return result != null && result.getValue() != null ? result.getValue() : Collections.emptyMap();
	}
}
