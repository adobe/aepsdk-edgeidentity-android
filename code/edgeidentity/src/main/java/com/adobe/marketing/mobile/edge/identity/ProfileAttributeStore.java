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

/**
 * Thin, null-safe wrapper around the profile-attributes {@link NamedCollection}. Each
 * {@link ProfileAttributeHandler} owns its own persistence key and reads/writes through this
 * store, keeping per-attribute persistence concerns out of {@link IdentityStorageManager}.
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
				"Profile attributes named collection is null. Unable to read key '" + key + "'."
			);
			return null;
		}
		return collection.getString(key, null);
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
				"Profile attributes named collection is null. Unable to write key '" + key + "'."
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
				"Profile attributes named collection is null. Unable to remove key '" + key + "'."
			);
			return;
		}
		collection.remove(key);
	}

	/**
	 * Clears every key in the profile-attributes collection. Invoked on reset to force a fresh
	 * sync against the new ECID.
	 */
	void clearAll() {
		if (collection == null) {
			Log.warning(
				IdentityConstants.LOG_TAG,
				LOG_SOURCE,
				"Profile attributes named collection is null. Unable to clear profile attributes."
			);
			return;
		}
		collection.removeAll();
	}
}
