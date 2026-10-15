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

import androidx.annotation.VisibleForTesting;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.JSONUtils;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Compares device attributes with Edge-confirmed and consent-granted owner values.
 * Filtering never writes storage; only the successful Edge payload is committed.
 */
final class DeviceAttributeDeduplicator {

	private static final String LOG_SOURCE = "DeviceAttributeDeduplicator";
	private final ProfileAttributeStore store;
	private final Map<List<String>, Object> synced = new LinkedHashMap<>();

	DeviceAttributeDeduplicator(final ProfileAttributeStore store) {
		this.store = store;
		final Map<String, Object> persisted = store.getMap(IdentityConstants.DeviceAttributes.SYNCED_VALUES);
		if (persisted != null) synced.putAll(paths(persisted));
	}

	Map<String, Object> filter(
		final Map<String, Object> attributes,
		final boolean dedup,
		final Map<String, Object> messagingState,
		final String timezone
	) {
		final Map<List<String>, Object> toSend = new LinkedHashMap<>();
		for (final Map.Entry<List<String>, Object> entry : paths(attributes).entrySet()) {
			final List<String> path = entry.getKey();
			final Object value = entry.getValue();
			final String canonical = canonicalize(value);
			if (canonical == null) {
				Log.warning(
					IdentityConstants.LOG_TAG,
					LOG_SOURCE,
					"Dropping invalid device attribute at path %s.",
					path
				);
				continue;
			}
			if (
				dedup &&
				(
					canonical.equals(canonicalize(synced.get(path))) ||
					matchesOwner(path, value, messagingState, timezone)
				)
			) continue;
			try {
				toSend.put(path, snapshot(canonical));
			} catch (JSONException exception) {
				Log.warning(
					IdentityConstants.LOG_TAG,
					LOG_SOURCE,
					"Unable to snapshot device attribute at path %s.",
					path
				);
			}
		}
		return tree(toSend);
	}

	private static boolean matchesOwner(
		final List<String> path,
		final Object value,
		final Map<String, Object> messagingState,
		final String timezone
	) {
		if (path.equals(List.of(IdentityConstants.DeviceAttributes.TIMEZONE))) return value.equals(timezone);
		if (
			path.equals(
				List.of(IdentityConstants.DeviceAttributes.TOKENS, IdentityConstants.DeviceAttributes.PUSH_NOTIFICATION)
			)
		) {
			return (
				value instanceof String &&
				value.equals(
					DataReader.optString(messagingState, IdentityConstants.SharedState.Messaging.PUSH_IDENTIFIER, null)
				)
			);
		}
		return false;
	}

	void commit(final Map<String, Object> attributes) {
		for (final Map.Entry<List<String>, Object> entry : paths(attributes).entrySet()) {
			final String canonical = canonicalize(entry.getValue());
			if (canonical == null) {
				Log.warning(
					IdentityConstants.LOG_TAG,
					LOG_SOURCE,
					"Cannot confirm invalid device attribute at path %s.",
					entry.getKey()
				);
				continue;
			}
			try {
				final Object value = snapshot(canonical);
				removeRelatedPaths(entry.getKey());
				synced.put(entry.getKey(), value);
			} catch (JSONException exception) {
				Log.warning(
					IdentityConstants.LOG_TAG,
					LOG_SOURCE,
					"Unable to confirm device attribute at path %s.",
					entry.getKey()
				);
			}
		}
		store.setMap(IdentityConstants.DeviceAttributes.SYNCED_VALUES, readSynced());
	}

	Map<String, Object> readSynced() {
		return tree(synced);
	}

	void clear() {
		synced.clear();
		store.remove(IdentityConstants.DeviceAttributes.SYNCED_VALUES);
	}

	private static Object snapshot(final String canonical) throws JSONException {
		return JSONUtils.toMap(new JSONObject("{\"value\":" + canonical + "}")).get("value");
	}

	private void removeRelatedPaths(final List<String> path) {
		synced.keySet().removeIf(existing -> isPrefix(existing, path) || isPrefix(path, existing));
	}

	private static boolean isPrefix(final List<String> prefix, final List<String> path) {
		return prefix.size() <= path.size() && path.subList(0, prefix.size()).equals(prefix);
	}

	private static Map<List<String>, Object> paths(final Map<String, Object> attributes) {
		final Map<List<String>, Object> result = new LinkedHashMap<>();
		if (attributes != null) extract(attributes, Collections.emptyList(), result);
		return result;
	}

	private static void extract(
		final Map<?, ?> attributes,
		final List<String> prefix,
		final Map<List<String>, Object> result
	) {
		for (final Map.Entry<?, ?> entry : attributes.entrySet()) {
			if (!(entry.getKey() instanceof String)) {
				Log.warning(IdentityConstants.LOG_TAG, LOG_SOURCE, "Dropping device attribute with a non-string key.");
				continue;
			}
			final String key = (String) entry.getKey();
			if (prefix.isEmpty() && IdentityConstants.DeviceAttributes.APP.equals(key)) continue;
			final List<String> path = new ArrayList<>(prefix);
			path.add(key);
			final Object value = entry.getValue();
			if (value instanceof Map<?, ?> && !((Map<?, ?>) value).isEmpty()) {
				extract((Map<?, ?>) value, path, result);
			} else {
				result.put(path, value);
			}
		}
	}

	private static Map<String, Object> tree(final Map<List<String>, Object> attributes) {
		final Map<String, Object> result = new LinkedHashMap<>();
		for (final Map.Entry<List<String>, Object> entry : attributes.entrySet()) set(
			entry.getValue(),
			entry.getKey(),
			0,
			result
		);
		return result;
	}

	@SuppressWarnings("unchecked")
	private static void set(
		final Object value,
		final List<String> path,
		final int index,
		final Map<String, Object> attributes
	) {
		final String key = path.get(index);
		if (index == path.size() - 1) {
			attributes.put(key, value);
			return;
		}
		final Map<String, Object> child = attributes.get(key) instanceof Map<?, ?>
			? new LinkedHashMap<>((Map<String, Object>) attributes.get(key))
			: new LinkedHashMap<>();
		set(value, path, index + 1, child);
		attributes.put(key, child);
	}

	@VisibleForTesting
	static String canonicalize(final Object value) {
		return value == null || value == JSONObject.NULL ? null : canonicalValue(value);
	}

	private static String canonicalValue(final Object value) {
		if (value == null || value == JSONObject.NULL) {
			return "null";
		}
		if (value instanceof String) {
			return JSONObject.quote((String) value);
		}
		if (value instanceof Boolean) {
			return value.toString();
		}
		if (value instanceof Number) {
			final double number = ((Number) value).doubleValue();
			if (!Double.isFinite(number)) return null;
			try {
				return number == 0 ? "0" : JSONObject.numberToString((Number) value);
			} catch (JSONException exception) {
				return null;
			}
		}
		if (value instanceof Map<?, ?>) {
			final Map<String, Object> sorted = new TreeMap<>();
			for (final Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
				if (!(entry.getKey() instanceof String)) {
					return null;
				}
				sorted.put((String) entry.getKey(), entry.getValue());
			}
			final List<String> entries = new ArrayList<>();
			for (final Map.Entry<String, Object> entry : sorted.entrySet()) {
				final String canonicalValue = canonicalValue(entry.getValue());
				if (canonicalValue == null) {
					return null;
				}
				entries.add(JSONObject.quote(entry.getKey()) + ":" + canonicalValue);
			}
			return "{" + String.join(",", entries) + "}";
		}
		if (value instanceof List<?>) {
			final List<String> elements = new ArrayList<>();
			for (final Object element : (List<?>) value) {
				final String canonicalElement = canonicalValue(element);
				if (canonicalElement == null) {
					return null;
				}
				elements.add(canonicalElement);
			}
			return "[" + String.join(",", elements) + "]";
		}
		if (value.getClass().isArray()) {
			final List<String> elements = new ArrayList<>();
			for (int index = 0; index < Array.getLength(value); index++) {
				final String canonicalElement = canonicalValue(Array.get(value, index));
				if (canonicalElement == null) {
					return null;
				}
				elements.add(canonicalElement);
			}
			return "[" + String.join(",", elements) + "]";
		}
		return null;
	}
}
