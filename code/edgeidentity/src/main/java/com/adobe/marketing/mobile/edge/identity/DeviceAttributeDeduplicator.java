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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONObject;

/**
 * Canonical JSON representation used to compare device attribute values for deduplication.
 */
final class DeviceAttributeDeduplicator {

	private DeviceAttributeDeduplicator() {}

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
