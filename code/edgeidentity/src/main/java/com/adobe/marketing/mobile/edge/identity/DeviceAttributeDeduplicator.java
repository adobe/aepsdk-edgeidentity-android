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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Keeps the device attributes confirmed by Edge as a nested map and filters out values that do not need to be
 * sent again. A value is compared with the confirmed value at the same path and, when owner values are given
 * (collect consent is "y"), with the owner value at that path. Arrays are compared as sets.
 */
final class DeviceAttributeDeduplicator {

	private static final String LOG_SOURCE = "DeviceAttributeDeduplicator";
	private final ProfileAttributeStore store;
	private Map<String, Object> synced;

	DeviceAttributeDeduplicator(final ProfileAttributeStore store) {
		this.store = store;
		final Map<String, Object> persisted = store.getMap(IdentityConstants.DeviceAttributes.SYNCED_VALUES);
		synced = persisted != null ? persisted : new LinkedHashMap<>();
	}

	/** The result of {@link #filter}: the values to send to Edge and the values already held by their owner. */
	static final class FilterResult {

		final Map<String, Object> changed;
		final Map<String, Object> ownerMatches;

		FilterResult(final Map<String, Object> changed, final Map<String, Object> ownerMatches) {
			this.changed = changed;
			this.ownerMatches = ownerMatches;
		}
	}

	/**
	 * Splits the attributes into the values that need to be sent to Edge and the values that match an owner value.
	 * Owner matches are not sent; the caller commits them so later requests can be deduplicated against the
	 * confirmed values alone.
	 *
	 * @param attributes the attributes to filter
	 * @param dedup whether to filter out unchanged values; when false, every valid value is returned as changed
	 * @param owners owner values to compare with, in the same shape as {@code attributes}; may be null
	 * @return the changed values and the owner matches; each is empty when there are none
	 */
	FilterResult filter(final Map<String, Object> attributes, final boolean dedup, final Map<String, Object> owners) {
		final Map<String, Object> changed = new LinkedHashMap<>();
		final Map<String, Object> ownerMatches = new LinkedHashMap<>();
		collect(attributes, dedup ? synced : null, dedup ? owners : null, changed, ownerMatches);
		return new FilterResult(changed, ownerMatches);
	}

	private static void collect(
		final Map<?, ?> attributes,
		final Map<?, ?> synced,
		final Map<?, ?> owners,
		final Map<String, Object> changed,
		final Map<String, Object> ownerMatches
	) {
		if (attributes == null) return;
		for (final Map.Entry<?, ?> entry : attributes.entrySet()) {
			if (!(entry.getKey() instanceof String)) {
				Log.warning(IdentityConstants.LOG_TAG, LOG_SOURCE, "Dropping device attribute with a non-string key.");
				continue;
			}
			final String key = (String) entry.getKey();
			final Object value = entry.getValue();
			if (value instanceof Map<?, ?> && !((Map<?, ?>) value).isEmpty()) {
				final Map<String, Object> changedChild = new LinkedHashMap<>();
				final Map<String, Object> ownerMatchesChild = new LinkedHashMap<>();
				collect((Map<?, ?>) value, child(synced, key), child(owners, key), changedChild, ownerMatchesChild);
				if (!changedChild.isEmpty()) changed.put(key, changedChild);
				if (!ownerMatchesChild.isEmpty()) ownerMatches.put(key, ownerMatchesChild);
				continue;
			}
			final String canonical = canonicalize(value);
			if (canonical == null) {
				Log.warning(IdentityConstants.LOG_TAG, LOG_SOURCE, "Dropping invalid device attribute '%s'.", key);
				continue;
			}
			if (synced != null && canonical.equals(canonicalize(synced.get(key)))) continue;
			if (owners != null && canonical.equals(canonicalize(owners.get(key)))) {
				ownerMatches.put(key, copy(value));
				continue;
			}
			changed.put(key, copy(value));
		}
	}

	/**
	 * Deep-merges {@code attributes} into the confirmed values and persists them. A map merges into an existing
	 * map; any other value, or an empty map, replaces the existing value at that key.
	 *
	 * @param attributes the attributes confirmed by Edge
	 */
	void commit(final Map<String, Object> attributes) {
		synced = merge(synced, attributes);
		store.setMap(IdentityConstants.DeviceAttributes.SYNCED_VALUES, synced);
	}

	Map<String, Object> readSynced() {
		return new LinkedHashMap<>(synced);
	}

	void clear() {
		synced = new LinkedHashMap<>();
		store.remove(IdentityConstants.DeviceAttributes.SYNCED_VALUES);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> merge(final Map<String, Object> base, final Map<String, Object> attributes) {
		final Map<String, Object> result = new LinkedHashMap<>(base);
		for (final Map.Entry<String, Object> entry : attributes.entrySet()) {
			final Object value = entry.getValue();
			final Object existing = result.get(entry.getKey());
			if (value instanceof Map<?, ?> && !((Map<?, ?>) value).isEmpty() && existing instanceof Map<?, ?>) {
				result.put(entry.getKey(), merge((Map<String, Object>) existing, (Map<String, Object>) value));
			} else {
				result.put(entry.getKey(), copy(value));
			}
		}
		return result;
	}

	private static Map<?, ?> child(final Map<?, ?> map, final String key) {
		final Object value = map != null ? map.get(key) : null;
		return value instanceof Map<?, ?> ? (Map<?, ?>) value : null;
	}

	/** Deep copies a valid attribute value so later changes to the caller's collections are not retained. */
	private static Object copy(final Object value) {
		if (value instanceof Map<?, ?>) {
			final Map<String, Object> result = new LinkedHashMap<>();
			for (final Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
				result.put((String) entry.getKey(), copy(entry.getValue()));
			}
			return result;
		}
		final List<?> list = asList(value);
		if (list != null) {
			final List<Object> result = new ArrayList<>();
			for (final Object element : list) result.add(copy(element));
			return result;
		}
		return value;
	}

	private static List<?> asList(final Object value) {
		if (value instanceof List<?>) return (List<?>) value;
		if (value == null || !value.getClass().isArray()) return null;
		final List<Object> list = new ArrayList<>();
		for (int index = 0; index < Array.getLength(value); index++) list.add(Array.get(value, index));
		return list;
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
		final List<?> list = asList(value);
		if (list != null) {
			// Arrays are compared as sets, so element order does not matter.
			final Set<String> elements = new TreeSet<>();
			for (final Object element : list) {
				final String canonicalElement = canonicalValue(element);
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
