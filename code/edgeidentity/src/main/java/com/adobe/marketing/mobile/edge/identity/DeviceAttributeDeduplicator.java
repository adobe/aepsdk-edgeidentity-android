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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Filters unchanged, JSON-compatible device attributes and persists their canonical values.
 */
final class DeviceAttributeDeduplicator {

	private static final String LOG_SOURCE = "DeviceAttributeDeduplicator";

	private final ProfileAttributeStore store;

	DeviceAttributeDeduplicator(final ProfileAttributeStore store) {
		this.store = store;
	}

	Map<String, Object> filter(final Map<String, Object> attributes, final boolean dedup) {
		final Map<String, String> syncedValues = readSyncedValues();
		final Map<String, Object> changedValues = new TreeMap<>();
		boolean hasChanges = false;

		for (final Map.Entry<String, Object> attribute : attributes.entrySet()) {
			final String key = attribute.getKey();
			if (key == null) {
				Log.warning(IdentityConstants.LOG_TAG, LOG_SOURCE, "Dropping device attribute with a null key.");
				continue;
			}
			final String canonicalValue = canonicalize(attribute.getValue());
			if (canonicalValue == null) {
				Log.warning(
					IdentityConstants.LOG_TAG,
					LOG_SOURCE,
					"Dropping '" + key + "': value is not JSON-representable."
				);
				continue;
			}
			if (dedup && canonicalValue.equals(syncedValues.get(key))) {
				Log.debug(IdentityConstants.LOG_TAG, LOG_SOURCE, "'" + key + "' unchanged since last sync, skipping.");
				continue;
			}
			syncedValues.put(key, canonicalValue);
			changedValues.put(key, attribute.getValue());
			hasChanges = true;
		}

		if (hasChanges) {
			store.setString(IdentityConstants.DeviceAttributes.SYNCED_VALUES, new JSONObject(syncedValues).toString());
		}
		return changedValues;
	}

	void clear() {
		store.remove(IdentityConstants.DeviceAttributes.SYNCED_VALUES);
	}

	private Map<String, String> readSyncedValues() {
		final String serializedValues = store.getString(IdentityConstants.DeviceAttributes.SYNCED_VALUES);
		if (serializedValues == null) {
			return new TreeMap<>();
		}

		try {
			final JSONObject storedValues = new JSONObject(serializedValues);
			final Map<String, String> result = new TreeMap<>();
			final Iterator<String> keys = storedValues.keys();
			while (keys.hasNext()) {
				final String key = keys.next();
				final Object value = storedValues.opt(key);
				if (value instanceof String) {
					result.put(key, (String) value);
				}
			}
			return result;
		} catch (final JSONException exception) {
			Log.warning(
				IdentityConstants.LOG_TAG,
				LOG_SOURCE,
				"Unable to read persisted device attribute values; treating them as unsynced."
			);
			return new TreeMap<>();
		}
	}

	@VisibleForTesting
	static String canonicalize(final Object value) {
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
			return Double.isFinite(number) ? value.toString() : null;
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
				final String canonicalValue = canonicalize(entry.getValue());
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
				final String canonicalElement = canonicalize(element);
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
				final String canonicalElement = canonicalize(Array.get(value, index));
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
