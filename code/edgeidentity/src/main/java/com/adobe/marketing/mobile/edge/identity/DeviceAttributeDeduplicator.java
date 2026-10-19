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

import static com.adobe.marketing.mobile.edge.identity.IdentityConstants.DeviceAttributes.PUSH_NOTIFICATION;
import static com.adobe.marketing.mobile.edge.identity.IdentityConstants.DeviceAttributes.TIMEZONE;
import static com.adobe.marketing.mobile.edge.identity.IdentityConstants.DeviceAttributes.TOKENS;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores the confirmed timezone and push token strings and filters out those that do not need to be sent again.
 * Values at any other path, and deduplicated values of an unexpected type, are always sent and never stored.
 */
final class DeviceAttributeDeduplicator {

	private final ProfileAttributeStore store;

	DeviceAttributeDeduplicator(final ProfileAttributeStore store) {
		this.store = store;
	}

	/** The result of {@link #filter}: the values to send to Edge and the values their owner already holds. */
	static final class FilterResult {

		final Map<String, Object> changed;
		final Map<String, Object> ownerMatches;

		FilterResult(final Map<String, Object> changed, final Map<String, Object> ownerMatches) {
			this.changed = changed;
			this.ownerMatches = ownerMatches;
		}
	}

	/**
	 * Removes the strings that match the confirmed value or, when owners are given, the owner value.
	 * Owner matches are returned separately and are not committed here; the caller commits them.
	 *
	 * @param attributes the attributes to filter
	 * @param owners the values their owners hold, in the same shape as {@code attributes}; may be null
	 * @return the values to send and the owner matches; each is empty when there are none
	 */
	FilterResult filter(final Map<String, Object> attributes, final Map<String, Object> owners) {
		final Map<String, Object> changed = new LinkedHashMap<>();
		final Map<String, Object> ownerMatches = new LinkedHashMap<>();
		if (attributes == null) return new FilterResult(changed, ownerMatches);
		changed.putAll(attributes);
		filterString(
			TIMEZONE,
			attributes,
			changed,
			ownerMatches,
			store.getString(TIMEZONE),
			owners != null ? owners.get(TIMEZONE) : null
		);

		final Map<?, ?> tokens = asMap(attributes.get(TOKENS));
		if (tokens == null) return new FilterResult(changed, ownerMatches);
		final Map<?, ?> ownerTokens = owners != null ? asMap(owners.get(TOKENS)) : null;
		final Map<Object, Object> changedTokens = new LinkedHashMap<>(tokens);
		final Map<String, Object> matchedTokens = new LinkedHashMap<>();
		filterString(
			PUSH_NOTIFICATION,
			tokens,
			changedTokens,
			matchedTokens,
			store.getString(PUSH_NOTIFICATION),
			ownerTokens != null ? ownerTokens.get(PUSH_NOTIFICATION) : null
		);
		if (changedTokens.size() != tokens.size()) {
			if (changedTokens.isEmpty()) {
				changed.remove(TOKENS);
			} else {
				changed.put(TOKENS, changedTokens);
			}
		}
		if (!matchedTokens.isEmpty()) ownerMatches.put(TOKENS, matchedTokens);
		return new FilterResult(changed, ownerMatches);
	}

	/**
	 * Stores the timezone and push token strings in {@code attributes}, replacing the confirmed value of each.
	 * Other values are ignored.
	 *
	 * @param attributes the attributes confirmed by Edge, or matched with their owner
	 */
	void commit(final Map<String, Object> attributes) {
		if (attributes == null) return;
		final Object timezone = attributes.get(TIMEZONE);
		if (timezone instanceof String) store.setString(TIMEZONE, (String) timezone);
		final Map<?, ?> tokens = asMap(attributes.get(TOKENS));
		final Object pushToken = tokens != null ? tokens.get(PUSH_NOTIFICATION) : null;
		if (pushToken instanceof String) store.setString(PUSH_NOTIFICATION, (String) pushToken);
	}

	/** Returns the confirmed strings in the same nested shape as the device attributes. */
	Map<String, Object> readSynced() {
		final Map<String, Object> synced = new LinkedHashMap<>();
		final String timezone = store.getString(TIMEZONE);
		if (timezone != null) synced.put(TIMEZONE, timezone);
		final String pushToken = store.getString(PUSH_NOTIFICATION);
		if (pushToken != null) {
			final Map<String, Object> tokens = new LinkedHashMap<>();
			tokens.put(PUSH_NOTIFICATION, pushToken);
			synced.put(TOKENS, tokens);
		}
		return synced;
	}

	/** Removes all confirmed device attribute strings. */
	void clear() {
		store.remove(TIMEZONE);
		store.remove(PUSH_NOTIFICATION);
	}

	private static Map<?, ?> asMap(final Object value) {
		return value instanceof Map<?, ?> ? (Map<?, ?>) value : null;
	}

	private static void filterString(
		final String key,
		final Map<?, ?> source,
		final Map<?, ?> changed,
		final Map<String, Object> ownerMatches,
		final String synced,
		final Object owner
	) {
		final Object value = source.get(key);
		if (!(value instanceof String)) return;
		if (value.equals(synced)) {
			changed.remove(key);
		} else if (value.equals(owner)) {
			changed.remove(key);
			ownerMatches.put(key, value);
		}
	}
}
