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

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.SharedStateResolution;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.SharedStateStatus;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends device attributes to Edge after the rules engine has filtered them.
 *
 * <ol>
 *     <li>{@link #requestRules(Event)} sends the public API data to the rules engine.</li>
 *     <li>Each forward-operational-data consequence comes back through {@link #onRuleResponse(Event)}.</li>
 *     <li>Consequences wait in a FIFO queue until the previous Edge request finishes. Values already
 *     synced are dropped and only changed values are sent to Edge.</li>
 *     <li>When Edge reports success ({@link #onEdgeResult(Event)}), the complete confirmed store is persisted and
 *     published in {@code profileAttributes}, which Messaging reads to keep its token store in sync.</li>
 * </ol>
 */
final class DeviceAttributeSync {

	static final String RULE_RESPONSE = "com.adobe.eventSource.responseOperationalData";
	static final String BYPASS = "com.adobe.eventType.edgeBypassConsent";
	private static final String LOG_SOURCE = "DeviceAttributeSync";
	private static final String APP = "app";
	private static final String PERSISTED_ATTRIBUTES = "deviceAttributes";
	private static final List<String> TIMEZONE = List.of("timezone");

	private final ExtensionApi api;
	private final ProfileAttributeStore profileAttributeStore;
	private final Deque<Event> queue = new ArrayDeque<>();
	private Event inFlightRequest;
	private Map<List<String>, Object> inFlightValues;
	// Values Edge confirmed; persisted and published as profileAttributes.
	private final Map<List<String>, Object> syncedValues = new LinkedHashMap<>();

	DeviceAttributeSync(final ExtensionApi api, final ProfileAttributeStore profileAttributeStore) {
		this.api = api;
		this.profileAttributeStore =
			profileAttributeStore != null ? profileAttributeStore : new ProfileAttributeStore(null);
		final Map<String, Object> persisted = this.profileAttributeStore.getMap(PERSISTED_ATTRIBUTES);
		if (persisted != null) syncedValues.putAll(paths(persisted));
	}

	/** Forwards public API data to the rules engine, which decides which attributes may be sent. */
	void requestRules(final Event event) {
		final Map<String, Object> data = event.getEventData();
		if (paths(data).isEmpty() || DeviceAttributeDeduplicator.canonicalize(data) == null) {
			Log.warning(IdentityConstants.LOG_TAG, LOG_SOURCE, "Ignoring empty or invalid device attribute request.");
			return;
		}
		final Map<String, Object> payload = new LinkedHashMap<>(data);
		if (!payload.containsKey(APP) && MobileCore.getApplication() != null) {
			payload.put(APP, Map.of("id", MobileCore.getApplication().getPackageName(), "platform", "fcm"));
		}
		final Event rulesRequest = new Event.Builder(
			IdentityConstants.EventNames.DEVICE_ATTRIBUTES_TO_RULES_ENGINE,
			IdentityConstants.DeviceAttributes.OPERATIONAL_DATA_TYPE,
			EventSource.REQUEST_CONTENT
		)
			.setEventData(payload)
			.chainToParentEvent(event)
			.build();
		api.dispatch(rulesRequest);
	}

	/** Queues a consequence until the previous Edge request has completed. */
	void onRuleResponse(final Event event) {
		queue.addLast(event);
		processQueue();
	}

	private void processQueue() {
		while (inFlightRequest == null && !queue.isEmpty()) sendChangedValues(queue.removeFirst());
	}

	private void sendChangedValues(final Event event) {
		final boolean consentYes = isConsentYes(event);
		final Map<String, Object> data = event.getEventData();
		final boolean dedup = !Boolean.FALSE.equals(
			configuration(event).get(IdentityConstants.DeviceAttributes.DEDUP_CONFIG_KEY)
		);
		final Map<List<String>, Object> changed = new LinkedHashMap<>();
		for (final Map.Entry<List<String>, Object> entry : paths(data).entrySet()) {
			if (!dedup || !alreadySynced(entry.getKey(), entry.getValue(), consentYes, event)) changed.put(
				entry.getKey(),
				entry.getValue()
			);
		}
		if (changed.isEmpty()) {
			Log.debug(IdentityConstants.LOG_TAG, LOG_SOURCE, "Device attributes unchanged, not sending to Edge.");
			return;
		}

		final Map<String, Object> payload = tree(changed);
		if (data.get(APP) != null) payload.put(APP, data.get(APP));
		final Event edgeRequest = new Event.Builder("Edge bypass request", BYPASS, EventSource.REQUEST_CONTENT)
			.setEventData(payload)
			.chainToParentEvent(event)
			.build();
		inFlightRequest = edgeRequest;
		inFlightValues = changed;
		api.dispatch(edgeRequest);
	}

	/** Stores successful values before processing the next queued consequence. */
	void onEdgeResult(final Event event) {
		if (inFlightRequest == null || !inFlightRequest.getUniqueIdentifier().equals(event.getParentID())) return;
		final boolean success = "success".equals(DataReader.optString(event.getEventData(), "status", null));
		if (success) {
			for (final Map.Entry<List<String>, Object> entry : inFlightValues.entrySet()) commit(
				entry.getKey(),
				entry.getValue()
			);
			profileAttributeStore.setMap(PERSISTED_ATTRIBUTES, tree(syncedValues));
			publishProfileAttributes(event);
		} else {
			Log.warning(
				IdentityConstants.LOG_TAG,
				LOG_SOURCE,
				"Device attribute request failed; processing the next queued consequence."
			);
		}
		inFlightRequest = null;
		inFlightValues = null;
		processQueue();
	}

	private boolean alreadySynced(
		final List<String> path,
		final Object value,
		final boolean consentYes,
		final Event event
	) {
		if (consentYes && TIMEZONE.equals(path)) {
			final String timezone = profileAttributeStore.getString("timeZone");
			if (timezone != null && sameValue(timezone, value)) return true;
		}
		if (consentYes && path.equals(List.of("tokens", "pushNotification"))) {
			final SharedStateResult messagingState = api.getSharedState(
				IdentityConstants.SharedState.Messaging.NAME,
				null,
				false,
				SharedStateResolution.LAST_SET
			);
			final String messagingToken = messagingState != null && messagingState.getStatus() == SharedStateStatus.SET
				? DataReader.optString(
					messagingState.getValue(),
					IdentityConstants.SharedState.Messaging.PUSH_IDENTIFIER,
					null
				)
				: null;
			if (messagingToken != null && sameValue(messagingToken, value)) return true;
		}
		return syncedValues.containsKey(path) && sameValue(syncedValues.get(path), value);
	}

	private void commit(final List<String> path, final Object value) {
		if (TIMEZONE.equals(path)) {
			profileAttributeStore.setString("timeZone", value instanceof String ? (String) value : null);
		}
		syncedValues.put(path, value);
	}

	private boolean isConsentYes(final Event event) {
		final SharedStateResult consentResult = api.getXDMSharedState(
			IdentityConstants.SharedState.Consent.NAME,
			event,
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
			return IdentityConstants.SharedState.Consent.YES.equalsIgnoreCase(value);
		}

		final SharedStateResult hubResult = api.getSharedState(
			IdentityConstants.SharedState.Hub.NAME,
			event,
			false,
			SharedStateResolution.ANY
		);
		if (hubResult == null || hubResult.getStatus() != SharedStateStatus.SET) return false;
		final Map<String, Object> extensions = DataReader.optTypedMap(
			Object.class,
			hubResult.getValue(),
			IdentityConstants.SharedState.Hub.EXTENSIONS,
			null
		);
		final Map<String, Object> consentExtension = DataReader.optTypedMap(
			Object.class,
			extensions,
			IdentityConstants.SharedState.Consent.NAME,
			null
		);
		return consentExtension == null || consentExtension.isEmpty();
	}

	private static boolean sameValue(final Object first, final Object second) {
		final String canonical = DeviceAttributeDeduplicator.canonicalize(first);
		return canonical != null && canonical.equals(DeviceAttributeDeduplicator.canonicalize(second));
	}

	void reset(final Event event) {
		queue.clear();
		inFlightRequest = null;
		inFlightValues = null;
		syncedValues.clear();
		profileAttributeStore.clearAll();
		publishProfileAttributes(event);
	}

	void publishProfileAttributes(final Event event) {
		final Map<String, Object> attributes = tree(syncedValues);
		final String timezone = profileAttributeStore.getString("timeZone");
		if (timezone != null) attributes.put("timezone", timezone);
		api.createSharedState(
			Collections.singletonMap(IdentityConstants.ProfileAttributes.SHARED_STATE_KEY, attributes),
			event
		);
	}

	private Map<String, Object> configuration(final Event event) {
		final SharedStateResult result = api.getSharedState(
			IdentityConstants.SharedState.Configuration.NAME,
			event,
			false,
			SharedStateResolution.LAST_SET
		);
		return result != null && result.getValue() != null ? result.getValue() : Collections.emptyMap();
	}

	/** Flattens event data to leaf paths, keeping reserved token/timezone values whole and skipping app context. */
	private static Map<List<String>, Object> paths(final Map<String, Object> values) {
		final Map<List<String>, Object> result = new LinkedHashMap<>();
		if (values != null) extract(values, Collections.emptyList(), result);
		return result;
	}

	private static void extract(
		final Map<?, ?> values,
		final List<String> prefix,
		final Map<List<String>, Object> result
	) {
		for (final Map.Entry<?, ?> entry : values.entrySet()) {
			if (!(entry.getKey() instanceof String)) continue;
			final String key = (String) entry.getKey();
			if (prefix.isEmpty() && APP.equals(key)) continue;
			final List<String> path = new ArrayList<>(prefix);
			path.add(key);
			final Object value = entry.getValue();
			final boolean nested = value instanceof Map<?, ?> && !((Map<?, ?>) value).isEmpty();
			if (nested && !isReserved(path)) extract((Map<?, ?>) value, path, result); else result.put(path, value);
		}
	}

	private static boolean isReserved(final List<String> path) {
		return TIMEZONE.equals(path) || path.equals(List.of("tokens", "pushNotification"));
	}

	private static Map<String, Object> tree(final Map<List<String>, ?> values) {
		final Map<String, Object> result = new LinkedHashMap<>();
		for (final Map.Entry<List<String>, ?> entry : values.entrySet()) insert(
			result,
			entry.getKey(),
			0,
			entry.getValue()
		);
		return result;
	}

	@SuppressWarnings("unchecked")
	private static void insert(
		final Map<String, Object> values,
		final List<String> path,
		final int index,
		final Object value
	) {
		final String key = path.get(index);
		if (index == path.size() - 1) {
			values.put(key, value);
			return;
		}
		final Object existing = values.get(key);
		final Map<String, Object> child = existing instanceof Map<?, ?>
			? (Map<String, Object>) existing
			: new LinkedHashMap<>();
		insert(child, path, index + 1, value);
		values.put(key, child);
	}
}
