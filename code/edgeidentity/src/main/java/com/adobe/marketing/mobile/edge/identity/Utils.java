/*
  Copyright 2021 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.edge.identity;

import static com.adobe.marketing.mobile.edge.identity.IdentityConstants.LOG_TAG;

import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.util.JSONUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

class Utils {

	private static final long MILLISECONDS_PER_SECOND = 1000L;

	private Utils() {}

	static boolean isNullOrEmpty(final Map<String, Object> map) {
		return map == null || map.isEmpty();
	}

	static boolean isNullOrEmpty(final List<?> list) {
		return list == null || list.isEmpty();
	}

	/**
	 * Adds {@code key}/{@code value} to {@code map} if {@code value} is not null or an
	 * empty collection.
	 *
	 * @param map   collection to put {@code value} mapped to {@code key} if {@code value} is
	 *              non-null and contains at least one entry
	 * @param key   key used to map {@code value} in {@code map}
	 * @param value a Object to add to {@code map} if not null
	 */
	static void putIfNotNull(final Map<String, Object> map, final String key, final Object value) {
		boolean addValues = map != null && key != null && value != null;

		if (addValues) {
			map.put(key, value);
		}
	}

	/**
	 * Creates a deep copy of the provided {@link Map}.
	 *
	 * @param map to be copied
	 * @return {@link Map} containing a deep copy of all the elements in {@code map}
	 */
	static Map<String, Object> deepCopy(final Map<String, Object> map) {
		if (map == null) {
			return null;
		}

		try {
			return JSONUtils.toMap(new JSONObject(map));
		} catch (final JSONException e) {
			MobileCore.log(
				LoggingMode.DEBUG,
				LOG_TAG,
				"Utils(deepCopy) - Unable to deep copy map, json string invalid."
			);
		}

		return null;
	}

	/**
	 * Creates a deep copy of the provided {@code listOfMaps}.
	 *
	 * @param listOfMaps to be copied
	 * @return {@link List} containing a deep copy of all the elements in {@code listOfMaps}
	 * @see #deepCopy(Map)
	 */
	static List<Map<String, Object>> deepCopy(final List<Map<String, Object>> listOfMaps) {
		if (listOfMaps == null) {
			return null;
		}

		List<Map<String, Object>> deepCopy = new ArrayList<>();

		for (Map<String, Object> map : listOfMaps) {
			deepCopy.add(deepCopy(map));
		}

		return deepCopy;
	}

	/**
	 * Gets current unix timestamp in seconds.
	 *
	 * @return {code long} current timestamp
	 */
	static long getUnixTimeInSeconds() {
		return System.currentTimeMillis() / MILLISECONDS_PER_SECOND;
	}
}
