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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends device attributes to Edge after the rules engine has filtered them.
 *
 * <ol>
 *     <li>{@link #handleDeviceAttributes(Event)} sends the public API data to the rules engine.</li>
 *     <li>Each forward-operational-data consequence comes back through {@link #handleDeviceAttributeRuleResponse(Event)}.</li>
 *     <li>Consequences wait in a FIFO queue until the active Edge request finishes. Values already synced are
 *     dropped and only changed values are sent to Edge.</li>
 *     <li>When Edge responds ({@link #handleDeviceAttributeResult(Event)}), the active request is released. On
 *     success the sent values are merged into the synced values and published in {@code profileAttributes}.
 *     A {@code Device Attributes Sync Result} event reports the outcome.</li>
 * </ol>
 */
final class DeviceAttributeSync {

	private static final String LOG_SOURCE = "DeviceAttributeSync";

	private final ExtensionApi api;
	private final ProfileAttributeStore profileAttributeStore;
	private final DeviceAttributeDeduplicator deduplicator;
	private final Deque<Event> pendingRequests = new ArrayDeque<>();
	private Event activeRequest;

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
		this.deduplicator = new DeviceAttributeDeduplicator(deviceAttributeStore);
	}

	/** Forwards public API data to the rules engine, which decides which attributes may be sent. */
	void handleDeviceAttributes(final Event event) {
		final Map<String, Object> data = event.getEventData();
		if (data == null || data.isEmpty()) {
			Log.warning(IdentityConstants.LOG_TAG, LOG_SOURCE, "Ignoring empty device attribute request.");
			return;
		}
		Log.debug(
			IdentityConstants.LOG_TAG,
			LOG_SOURCE,
			"Forwarding device attributes to the rules engine. Nothing is sent to Edge unless a forward operational data rule matches."
		);
		api.dispatch(
			new Event.Builder(
				IdentityConstants.EventNames.DEVICE_ATTRIBUTES_TO_RULES_ENGINE,
				IdentityConstants.DeviceAttributes.OPERATIONAL_DATA_TYPE,
				EventSource.REQUEST_CONTENT
			)
				.setEventData(data)
				.chainToParentEvent(event)
				.build()
		);
	}

	/** Queues a rule consequence until the active Edge request has completed. */
	void handleDeviceAttributeRuleResponse(final Event event) {
		if (event.getEventData() == null || event.getEventData().isEmpty()) {
			Log.debug(IdentityConstants.LOG_TAG, LOG_SOURCE, "Rule response contains no device attributes.");
			return;
		}
		pendingRequests.addLast(event);
		processQueue();
	}

	/** Sends queued requests until one is waiting for an Edge response or the queue is empty. */
	private void processQueue() {
		while (activeRequest == null && !pendingRequests.isEmpty()) {
			final Event request = buildEdgeRequest(pendingRequests.removeFirst());
			if (request != null) {
				activeRequest = request;
				Log.debug(
					IdentityConstants.LOG_TAG,
					LOG_SOURCE,
					"Sending device attributes to Edge (request '%s').",
					request.getUniqueIdentifier()
				);
				api.dispatch(request);
			}
		}
	}

	/** Returns the Edge request for the changed values in {@code ruleResponse}, or null if there is nothing to send. */
	private Event buildEdgeRequest(final Event ruleResponse) {
		final Map<String, Object> attributes = new HashMap<>(ruleResponse.getEventData());
		attributes.remove(IdentityConstants.DeviceAttributes.APP);
		final boolean dedup = DataReader.optBoolean(
			configuration(),
			IdentityConstants.DeviceAttributes.OPTIMIZE_OPERATIONAL_PROFILE_DATA_SYNC,
			true
		);
		final Map<String, Object> owners = dedup && isCollectConsentYes() ? ownerValues() : null;
		final DeviceAttributeDeduplicator.FilterResult result = deduplicator.filter(attributes, dedup, owners);
		if (!result.ownerMatches.isEmpty()) {
			deduplicator.commit(result.ownerMatches);
			Log.debug(IdentityConstants.LOG_TAG, LOG_SOURCE, "Marked device attributes held by their owner as synced.");
			publishProfileAttributes(ruleResponse);
		}
		final Map<String, Object> changed = result.changed;
		if (changed.isEmpty()) {
			Log.debug(
				IdentityConstants.LOG_TAG,
				LOG_SOURCE,
				"Device attributes unchanged, not sending to Edge (rule event '%s').",
				ruleResponse.getUniqueIdentifier()
			);
			dispatchSyncResult(
				IdentityConstants.DeviceAttributes.STATUS_SUCCESS,
				IdentityConstants.DeviceAttributes.REASON_UNCHANGED,
				attributes,
				ruleResponse
			);
			return null;
		}

		final Application application = MobileCore.getApplication();
		final String appId = application != null ? application.getPackageName() : null;
		if (appId == null || appId.isEmpty()) {
			Log.warning(
				IdentityConstants.LOG_TAG,
				LOG_SOURCE,
				"Cannot send device attributes without an app identifier. Dropping rule event '%s'.",
				ruleResponse.getUniqueIdentifier()
			);
			dispatchSyncResult(
				IdentityConstants.DeviceAttributes.STATUS_FAILED,
				IdentityConstants.DeviceAttributes.REASON_MISSING_APP_IDENTIFIER,
				changed,
				ruleResponse
			);
			return null;
		}
		final Map<String, Object> app = new HashMap<>();
		app.put(IdentityConstants.DeviceAttributes.APP_ID, appId);
		app.put(IdentityConstants.DeviceAttributes.APP_PLATFORM, IdentityConstants.DeviceAttributes.PLATFORM_FCM);
		changed.put(IdentityConstants.DeviceAttributes.APP, app);
		return new Event.Builder(
			IdentityConstants.EventNames.DEVICE_ATTRIBUTES_EDGE_REQUEST,
			IdentityConstants.DeviceAttributes.BYPASS_TYPE,
			EventSource.REQUEST_CONTENT
		)
			.setEventData(changed)
			.chainToParentEvent(ruleResponse)
			.build();
	}

	/** Handles the Edge response for the active request, then sends the next queued request. */
	void handleDeviceAttributeResult(final Event event) {
		if (!isResponseToActiveRequest(event)) {
			Log.debug(
				IdentityConstants.LOG_TAG,
				LOG_SOURCE,
				"Ignoring device attribute response '%s', it does not match the active request.",
				event.getUniqueIdentifier()
			);
			return;
		}
		final String requestId = activeRequest.getUniqueIdentifier();
		final Map<String, Object> attributes = new HashMap<>(activeRequest.getEventData());
		attributes.remove(IdentityConstants.DeviceAttributes.APP);
		activeRequest = null;

		final Map<String, Object> responseData = event.getEventData();
		final String status = DataReader.optString(responseData, IdentityConstants.DeviceAttributes.STATUS, null);
		if (IdentityConstants.DeviceAttributes.STATUS_SUCCESS.equals(status)) {
			deduplicator.commit(attributes);
			final String timezone = DataReader.optString(attributes, IdentityConstants.DeviceAttributes.TIMEZONE, null);
			if (timezone != null) {
				profileAttributeStore.setString(IdentityConstants.DeviceAttributes.TIME_ZONE, timezone);
			}
			publishProfileAttributes(event);
			Log.debug(IdentityConstants.LOG_TAG, LOG_SOURCE, "Device attributes synced (request '%s').", requestId);
			dispatchSyncResult(IdentityConstants.DeviceAttributes.STATUS_SUCCESS, "", attributes, event);
		} else {
			final String reason = DataReader.optString(
				responseData,
				IdentityConstants.DeviceAttributes.REASON,
				IdentityConstants.DeviceAttributes.UNKNOWN_REASON
			);
			Log.warning(
				IdentityConstants.LOG_TAG,
				LOG_SOURCE,
				"Device attribute sync failed (request '%s') with status '%s' and reason '%s'. Values remain unsynced.",
				requestId,
				status != null ? status : "missing",
				reason
			);
			dispatchSyncResult(IdentityConstants.DeviceAttributes.STATUS_FAILED, reason, attributes, event);
		}
		processQueue();
	}

	/** Dispatches the Device Attributes Sync Result event, chained to {@code parent}. */
	private void dispatchSyncResult(
		final String status,
		final String reason,
		final Map<String, Object> attributes,
		final Event parent
	) {
		final Map<String, Object> result = new HashMap<>();
		result.put(IdentityConstants.DeviceAttributes.STATUS, status);
		result.put(IdentityConstants.DeviceAttributes.REASON, reason);
		result.put(IdentityConstants.DeviceAttributes.ATTRIBUTES, attributes);
		api.dispatch(
			new Event.Builder(
				IdentityConstants.EventNames.DEVICE_ATTRIBUTES_SYNC_RESULT,
				EventType.EDGE_IDENTITY,
				EventSource.RESPONSE_CONTENT
			)
				.setEventData(result)
				.chainToParentEvent(parent)
				.build()
		);
	}

	/** A response matches when it is chained to the active request or names it in {@code requestEventId}. */
	private boolean isResponseToActiveRequest(final Event event) {
		if (activeRequest == null) return false;
		final String requestId = activeRequest.getUniqueIdentifier();
		return (
			requestId.equals(event.getParentID()) ||
			requestId.equals(
				DataReader.optString(event.getEventData(), IdentityConstants.DeviceAttributes.REQUEST_EVENT_ID, null)
			)
		);
	}

	/**
	 * Returns the values owned by other sources, in the same shape as the device attributes: the timezone set
	 * through the profile attributes API and the push token in the Messaging shared state.
	 */
	private Map<String, Object> ownerValues() {
		final Map<String, Object> owners = new HashMap<>();
		final String timezone = profileAttributeStore.getString(IdentityConstants.DeviceAttributes.TIME_ZONE);
		if (timezone != null) owners.put(IdentityConstants.DeviceAttributes.TIMEZONE, timezone);

		final SharedStateResult messaging = api.getSharedState(
			IdentityConstants.SharedState.Messaging.NAME,
			null,
			false,
			SharedStateResolution.LAST_SET
		);
		if (messaging != null && messaging.getStatus() == SharedStateStatus.SET) {
			final String pushToken = DataReader.optString(
				messaging.getValue(),
				IdentityConstants.SharedState.Messaging.PUSH_IDENTIFIER,
				null
			);
			if (pushToken != null) {
				owners.put(
					IdentityConstants.DeviceAttributes.TOKENS,
					Collections.singletonMap(IdentityConstants.DeviceAttributes.PUSH_NOTIFICATION, pushToken)
				);
			}
		}
		return owners;
	}

	/**
	 * Returns true when collect consent is "y". When the Consent extension is not registered, consent is treated
	 * as "y", matching the Edge extension.
	 */
	private boolean isCollectConsentYes() {
		final SharedStateResult hub = api.getSharedState(
			IdentityConstants.SharedState.Hub.NAME,
			null,
			false,
			SharedStateResolution.LAST_SET
		);
		if (hub == null || hub.getStatus() != SharedStateStatus.SET) return false;
		final Map<String, Object> extensions = DataReader.optTypedMap(
			Object.class,
			hub.getValue(),
			IdentityConstants.SharedState.Hub.EXTENSIONS,
			null
		);
		final Map<String, Object> consentExtension = DataReader.optTypedMap(
			Object.class,
			extensions,
			IdentityConstants.SharedState.Consent.NAME,
			null
		);
		if (consentExtension == null || consentExtension.isEmpty()) return true;

		final SharedStateResult consentResult = api.getXDMSharedState(
			IdentityConstants.SharedState.Consent.NAME,
			null,
			false,
			SharedStateResolution.ANY
		);
		if (consentResult == null || consentResult.getStatus() != SharedStateStatus.SET) return false;
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

	/**
	 * Reports the active and queued requests as failed, clears them and all synced values, then republishes the
	 * profile attributes. The profile attribute store itself is cleared by
	 * {@link IdentityState#clearProfileAttributes}.
	 */
	void reset(final Event event) {
		final List<Event> dropped = new ArrayList<>();
		if (activeRequest != null) dropped.add(activeRequest);
		dropped.addAll(pendingRequests);
		for (final Event request : dropped) {
			final Map<String, Object> attributes = new HashMap<>(request.getEventData());
			attributes.remove(IdentityConstants.DeviceAttributes.APP);
			dispatchSyncResult(
				IdentityConstants.DeviceAttributes.STATUS_FAILED,
				IdentityConstants.DeviceAttributes.REASON_IDENTITY_RESET,
				attributes,
				request
			);
		}
		pendingRequests.clear();
		activeRequest = null;
		deduplicator.clear();
		publishProfileAttributes(event);
		Log.debug(
			IdentityConstants.LOG_TAG,
			LOG_SOURCE,
			"Cleared device attribute requests and synced values after identity reset."
		);
	}

	/** Publishes the synced device attributes merged with the consent-path timezone in the standard shared state. */
	void publishProfileAttributes(final Event event) {
		final Map<String, Object> attributes = new LinkedHashMap<>(deduplicator.readSynced());
		final String timezone = profileAttributeStore.getString(IdentityConstants.DeviceAttributes.TIME_ZONE);
		if (timezone != null) attributes.put(IdentityConstants.DeviceAttributes.TIMEZONE, timezone);
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
