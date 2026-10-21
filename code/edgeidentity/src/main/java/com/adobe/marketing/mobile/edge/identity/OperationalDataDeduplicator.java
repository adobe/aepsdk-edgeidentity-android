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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores the confirmed timezone and push token strings and filters out those that do not need to be sent again.
 * Values at any other path, and a timezone or push token that is not a string, are always sent and never stored.
 */
final class OperationalDataDeduplicator {

	private final ProfileAttributeStore store;

	OperationalDataDeduplicator(final ProfileAttributeStore store) {
		this.store = store;
	}

	/** The result of {@link #filter}: the values to send to Edge and the owner-held values. */
	static final class FilterResult {

		final Map<String, Object> changed;
		final Map<String, Object> ownerMatches;

		FilterResult(final Map<String, Object> changed, final Map<String, Object> ownerMatches) {
			this.changed = changed;
			this.ownerMatches = ownerMatches;
		}
	}

	/**
	 * Removes the timezone and push token strings that match the confirmed value or the owner-held value. The owner
	 * of the timezone is the profile attributes API, and the owner of the push token is Messaging; the values they
	 * hold are owner-held. Owner-held values are returned separately and are not stored here; the caller stores them
	 * as confirmed.
	 *
	 * @param attributes the attributes to filter
	 * @param owners the owner-held values, in the same shape as {@code attributes}; may be null
	 * @return the values to send and the owner-held values; each is empty when there are none
	 */
	FilterResult filter(final Map<String, Object> attributes, final Map<String, Object> owners) {
		final Map<String, Object> changed = new LinkedHashMap<>();
		final Map<String, Object> ownerMatches = new LinkedHashMap<>();
		if (attributes == null) return new FilterResult(changed, ownerMatches);
		changed.putAll(attributes);
		filterString(
			IdentityConstants.OperationalData.TIMEZONE,
			attributes,
			changed,
			ownerMatches,
			store.getString(IdentityConstants.OperationalData.TIMEZONE),
			owners != null ? owners.get(IdentityConstants.OperationalData.TIMEZONE) : null
		);

		final Map<?, ?> tokens = asMap(attributes.get(IdentityConstants.OperationalData.TOKENS));
		if (tokens == null) return new FilterResult(changed, ownerMatches);
		final Map<?, ?> ownerTokens = owners != null
			? asMap(owners.get(IdentityConstants.OperationalData.TOKENS))
			: null;
		final Map<Object, Object> changedTokens = new LinkedHashMap<>(tokens);
		final Map<String, Object> matchedTokens = new LinkedHashMap<>();
		filterString(
			IdentityConstants.OperationalData.PUSH_NOTIFICATION,
			tokens,
			changedTokens,
			matchedTokens,
			store.getString(IdentityConstants.OperationalData.PUSH_NOTIFICATION),
			ownerTokens != null ? ownerTokens.get(IdentityConstants.OperationalData.PUSH_NOTIFICATION) : null
		);
		if (changedTokens.size() != tokens.size()) {
			if (changedTokens.isEmpty()) {
				changed.remove(IdentityConstants.OperationalData.TOKENS);
			} else {
				changed.put(IdentityConstants.OperationalData.TOKENS, changedTokens);
			}
		}
		if (!matchedTokens.isEmpty()) ownerMatches.put(IdentityConstants.OperationalData.TOKENS, matchedTokens);
		return new FilterResult(changed, ownerMatches);
	}

	/**
	 * Stores the timezone and push token strings in {@code attributes}, replacing the confirmed value of each.
	 * Other values are ignored.
	 *
	 * @param attributes the attributes Edge confirmed, or owner-held values
	 */
	void commit(final Map<String, Object> attributes) {
		if (attributes == null) return;
		final Object timezone = attributes.get(IdentityConstants.OperationalData.TIMEZONE);
		if (timezone instanceof String) store.setString(IdentityConstants.OperationalData.TIMEZONE, (String) timezone);
		final Map<?, ?> tokens = asMap(attributes.get(IdentityConstants.OperationalData.TOKENS));
		final Object pushToken = tokens != null
			? tokens.get(IdentityConstants.OperationalData.PUSH_NOTIFICATION)
			: null;
		if (pushToken instanceof String) store.setString(
			IdentityConstants.OperationalData.PUSH_NOTIFICATION,
			(String) pushToken
		);
	}

	/** Returns the confirmed strings in the same nested shape as the operational data. */
	Map<String, Object> readSynced() {
		final Map<String, Object> synced = new LinkedHashMap<>();
		final String timezone = store.getString(IdentityConstants.OperationalData.TIMEZONE);
		if (timezone != null) synced.put(IdentityConstants.OperationalData.TIMEZONE, timezone);
		final String pushToken = store.getString(IdentityConstants.OperationalData.PUSH_NOTIFICATION);
		if (pushToken != null) {
			final Map<String, Object> tokens = new LinkedHashMap<>();
			tokens.put(IdentityConstants.OperationalData.PUSH_NOTIFICATION, pushToken);
			synced.put(IdentityConstants.OperationalData.TOKENS, tokens);
		}
		return synced;
	}

	/** Removes all confirmed operational data strings. */
	void clear() {
		store.remove(IdentityConstants.OperationalData.TIMEZONE);
		store.remove(IdentityConstants.OperationalData.PUSH_NOTIFICATION);
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
