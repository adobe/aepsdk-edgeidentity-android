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

import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.NamedCollection;
import com.adobe.marketing.mobile.util.JSONUtils;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Thin, null-safe wrapper around an attribute {@link NamedCollection}. Profile handlers and
 * device-attribute synchronization use separate collections with the same persistence helpers.
 *
 * <p>All operations no-op (and log a warning) when the underlying collection is unavailable.
 */
class ProfileAttributeStore {

	private static final String LOG_SOURCE = "ProfileAttributeStore";

	private final NamedCollection collection;

	ProfileAttributeStore(final NamedCollection collection) {
		this.collection = collection;
	}

	/**
	 * Returns the value stored under {@code key}, or {@code null} if absent or the underlying
	 * collection is unavailable.
	 *
	 * @param key the persistence key
	 * @return the stored value, or {@code null}
	 */
	String getString(final String key) {
		if (collection == null) {
			Log.warning(
				IdentityConstants.LOG_TAG,
				LOG_SOURCE,
				"Attribute named collection is null. Unable to read key '" + key + "'."
			);
			return null;
		}
		return collection.getString(key, null);
	}

	Map<String, Object> getMap(final String key) {
		final String value = getString(key);
		if (value == null) return null;
		try {
			return JSONUtils.toMap(new JSONObject(value));
		} catch (JSONException exception) {
			Log.warning(IdentityConstants.LOG_TAG, LOG_SOURCE, "Unable to read stored map for key '" + key + "'.");
			return null;
		}
	}

	void setMap(final String key, final Map<String, Object> value) {
		if (value == null) {
			remove(key);
			return;
		}
		try {
			final JSONObject json = new JSONObject();
			for (final Map.Entry<String, Object> entry : value.entrySet()) {
				json.put(entry.getKey(), toJsonValue(entry.getValue()));
			}
			setString(key, json.toString());
		} catch (Exception exception) {
			Log.error(IdentityConstants.LOG_TAG, LOG_SOURCE, "Unable to persist map for key '" + key + "'.");
		}
	}

	private static Object toJsonValue(final Object value) throws JSONException {
		if (value == null) return JSONObject.NULL;
		if (value instanceof Map<?, ?>) {
			final JSONObject json = new JSONObject();
			for (final Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
				if (!(entry.getKey() instanceof String)) {
					throw new JSONException("Map key is not a string");
				}
				json.put((String) entry.getKey(), toJsonValue(entry.getValue()));
			}
			return json;
		}
		if (value instanceof List<?>) {
			final JSONArray json = new JSONArray();
			for (final Object element : (List<?>) value) json.put(toJsonValue(element));
			return json;
		}
		if (value.getClass().isArray()) {
			final JSONArray json = new JSONArray();
			for (int index = 0; index < Array.getLength(value); index++) json.put(toJsonValue(Array.get(value, index)));
			return json;
		}
		return value;
	}

	/**
	 * Persists {@code value} under {@code key}. A {@code null} value removes the key.
	 *
	 * @param key   the persistence key
	 * @param value the value to store, or {@code null} to remove
	 */
	void setString(final String key, final String value) {
		if (collection == null) {
			Log.warning(
				IdentityConstants.LOG_TAG,
				LOG_SOURCE,
				"Attribute named collection is null. Unable to write key '" + key + "'."
			);
			return;
		}
		if (value == null) {
			collection.remove(key);
			return;
		}
		collection.setString(key, value);
	}

	/**
	 * Removes the value stored under {@code key}, if any.
	 *
	 * @param key the persistence key
	 */
	void remove(final String key) {
		if (collection == null) {
			Log.warning(
				IdentityConstants.LOG_TAG,
				LOG_SOURCE,
				"Attribute named collection is null. Unable to remove key '" + key + "'."
			);
			return;
		}
		collection.remove(key);
	}

	/**
	 * Clears every key in the attribute collection. Invoked on reset to force a fresh
	 * sync against the new ECID.
	 */
	void clearAll() {
		if (collection == null) {
			Log.warning(
				IdentityConstants.LOG_TAG,
				LOG_SOURCE,
				"Attribute named collection is null. Unable to clear stored attributes."
			);
			return;
		}
		collection.removeAll();
	}
}
