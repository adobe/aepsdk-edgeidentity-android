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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class OperationalDataDeduplicatorTests {

	private ProfileAttributeStore store;
	private OperationalDataDeduplicator deduplicator;
	private Map<String, String> persisted;

	@Before
	public void setup() {
		store = mock(ProfileAttributeStore.class);
		persisted = new HashMap<>();
		when(store.getString(anyString())).thenAnswer(call -> persisted.get(call.<String>getArgument(0)));
		doAnswer(call -> persisted.put(call.getArgument(0), call.getArgument(1)))
			.when(store)
			.setString(anyString(), anyString());
		doAnswer(call -> persisted.remove(call.<String>getArgument(0))).when(store).remove(anyString());
		deduplicator = new OperationalDataDeduplicator(store);
	}

	@Test
	public void filterDoesNotWriteUntilCommit() {
		final Map<String, Object> attributes = attributes("Europe/London", "push");
		assertEquals(attributes, deduplicator.filter(attributes, null).changed);
		assertEquals(attributes, deduplicator.filter(attributes, null).changed);
		assertTrue(persisted.isEmpty());
		deduplicator.commit(attributes);
		assertTrue(deduplicator.filter(attributes, null).changed.isEmpty());
		assertEquals(Map.of("timezone", "Europe/London", "pushNotification", "push"), persisted);
		final OperationalDataDeduplicator restarted = new OperationalDataDeduplicator(store);
		assertEquals(attributes, restarted.readSynced());
		assertTrue(restarted.filter(attributes, null).changed.isEmpty());
	}

	@Test
	public void timezoneAndPushTokenAreDeduplicatedIndependently() {
		deduplicator.commit(attributes("same", "old"));
		assertEquals(
			Map.of("tokens", Map.of("pushNotification", "new")),
			deduplicator.filter(attributes("same", "new"), null).changed
		);
		assertEquals(Map.of("timezone", "new"), deduplicator.filter(attributes("new", "old"), null).changed);
	}

	@Test
	public void filterAndReadSyncedUseCurrentStoreValues() {
		deduplicator.commit(attributes("old", "old"));
		persisted.put("timezone", "external");
		persisted.put("pushNotification", "external-push");
		final Map<String, Object> external = attributes("external", "external-push");
		assertEquals(external, deduplicator.readSynced());
		assertTrue(deduplicator.filter(external, null).changed.isEmpty());
		assertEquals(attributes("old", "old"), deduplicator.filter(attributes("old", "old"), null).changed);
		persisted.remove("timezone");
		persisted.remove("pushNotification");
		assertTrue(deduplicator.readSynced().isEmpty());
		assertEquals(external, deduplicator.filter(external, null).changed);
	}

	@Test
	public void ownerMatchesAreReturnedSeparatelyAndNotCommitted() {
		final Map<String, Object> attributes = attributes("Europe/London", "push");
		final OperationalDataDeduplicator.FilterResult result = deduplicator.filter(
			attributes,
			attributes("Europe/London", "other")
		);
		assertEquals(Map.of("tokens", Map.of("pushNotification", "push")), result.changed);
		assertEquals(Map.of("timezone", "Europe/London"), result.ownerMatches);
		assertTrue(persisted.isEmpty());
		assertEquals(attributes, deduplicator.filter(attributes, null).changed);
		deduplicator.commit(result.ownerMatches);
		assertEquals(Map.of("timezone", "Europe/London"), deduplicator.readSynced());
	}

	@Test
	public void nestedOwnerMatchesKeepTheirPathAndUnknownSiblings() {
		final Map<String, Object> attributes = Map.of("tokens", Map.of("pushNotification", "push", "other", "x"));
		final OperationalDataDeduplicator.FilterResult result = deduplicator.filter(attributes, attributes);
		assertEquals(Map.of("tokens", Map.of("other", "x")), result.changed);
		assertEquals(Map.of("tokens", Map.of("pushNotification", "push")), result.ownerMatches);
		deduplicator.commit(result.ownerMatches);
		assertEquals(result.ownerMatches, deduplicator.readSynced());
	}

	@Test
	public void matchingBothOwnersRemovesTokensWrapper() {
		final Map<String, Object> attributes = attributes("Europe/London", "push");
		final OperationalDataDeduplicator.FilterResult result = deduplicator.filter(attributes, attributes);
		assertTrue(result.changed.isEmpty());
		assertEquals(attributes, result.ownerMatches);
		assertTrue(persisted.isEmpty());
	}

	@Test
	public void confirmedValuesTakePriorityOverOwnerMatches() {
		final Map<String, Object> attributes = attributes("Europe/London", "push");
		deduplicator.commit(attributes);
		final OperationalDataDeduplicator.FilterResult result = deduplicator.filter(attributes, attributes);
		assertTrue(result.changed.isEmpty());
		assertTrue(result.ownerMatches.isEmpty());
	}

	@Test
	public void changedConfirmedValueCanMatchCurrentOwner() {
		deduplicator.commit(attributes("old", "old"));
		final Map<String, Object> attributes = attributes("current", "current");
		final OperationalDataDeduplicator.FilterResult result = deduplicator.filter(attributes, attributes);
		assertTrue(result.changed.isEmpty());
		assertEquals(attributes, result.ownerMatches);
		assertEquals(attributes("old", "old"), deduplicator.readSynced());
	}

	@Test
	public void unexpectedOwnerTypesDoNotMatchStrings() {
		final Map<String, Object> attributes = attributes("1", "true");
		final OperationalDataDeduplicator.FilterResult result = deduplicator.filter(attributes, attributes(1, true));
		assertEquals(attributes, result.changed);
		assertTrue(result.ownerMatches.isEmpty());
	}

	@Test
	public void unexpectedOwnerTokensContainerDoesNotMatch() {
		final Map<String, Object> attributes = attributes("current", "push");
		final OperationalDataDeduplicator.FilterResult result = deduplicator.filter(
			attributes,
			Map.of("tokens", "push")
		);
		assertEquals(attributes, result.changed);
		assertTrue(result.ownerMatches.isEmpty());
	}

	@Test
	public void emptyStringsAreStoredAndCompared() {
		final Map<String, Object> attributes = attributes("", "");
		assertEquals(attributes, deduplicator.filter(attributes, attributes("previous", "previous")).changed);
		deduplicator.commit(attributes);
		assertEquals(Map.of("timezone", "", "pushNotification", ""), persisted);
		assertEquals(attributes, deduplicator.readSynced());
		assertTrue(deduplicator.filter(attributes, null).changed.isEmpty());
		assertTrue(new OperationalDataDeduplicator(store).filter(attributes, null).changed.isEmpty());
	}

	@Test
	public void emptyStringsCanMatchOwners() {
		final Map<String, Object> attributes = attributes("", "");
		final OperationalDataDeduplicator.FilterResult result = deduplicator.filter(attributes, attributes);
		assertTrue(result.changed.isEmpty());
		assertEquals(attributes, result.ownerMatches);
	}

	@Test
	public void originallyEmptyTokensMapIsPreserved() {
		final Map<String, Object> tokens = new LinkedHashMap<>();
		final Map<String, Object> attributes = Map.of("tokens", tokens);
		deduplicator.commit(attributes("stored", "stored"));
		final OperationalDataDeduplicator.FilterResult result = deduplicator.filter(attributes, attributes);
		assertEquals(attributes, result.changed);
		assertSame(tokens, result.changed.get("tokens"));
		assertTrue(result.ownerMatches.isEmpty());
	}

	@Test
	public void matchingPushTokenRemovesOnlySelectedFieldAndKeepsInputUnchanged() {
		final Map<String, Object> sibling = Map.of("nested", List.of("value"));
		final Map<String, Object> tokens = new LinkedHashMap<>();
		tokens.put("pushNotification", "push");
		tokens.put("other", sibling);
		final Map<String, Object> attributes = Map.of("tokens", tokens);
		deduplicator.commit(Map.of("tokens", Map.of("pushNotification", "push")));
		final OperationalDataDeduplicator.FilterResult result = deduplicator.filter(attributes, null);
		assertEquals(Map.of("tokens", Map.of("other", sibling)), result.changed);
		assertEquals("push", tokens.get("pushNotification"));
		assertSame(sibling, ((Map<?, ?>) result.changed.get("tokens")).get("other"));
		assertTrue(result.ownerMatches.isEmpty());
	}

	@Test
	public void invalidNestedKeysArePreservedWhenMatchingPushTokenIsRemoved() {
		final Map<Object, Object> tokens = new LinkedHashMap<>();
		tokens.put("pushNotification", "push");
		tokens.put(1, "non-string-key");
		tokens.put(null, null);
		final Map<String, Object> attributes = Map.of("tokens", tokens);
		deduplicator.commit(attributes);
		final OperationalDataDeduplicator.FilterResult result = deduplicator.filter(attributes, attributes);
		final Map<?, ?> changedTokens = (Map<?, ?>) result.changed.get("tokens");
		assertEquals(2, changedTokens.size());
		assertEquals("non-string-key", changedTokens.get(1));
		assertTrue(changedTokens.containsKey(null));
		assertNull(changedTokens.get(null));
		assertEquals("push", tokens.get("pushNotification"));
		assertEquals(Map.of("pushNotification", "push"), persisted);
		assertTrue(result.ownerMatches.isEmpty());
	}

	@Test
	public void nullAndInvalidJSONValuesPassThroughWithoutValidation() {
		final Object unsupported = new Object();
		final Map<String, Object> nested = new LinkedHashMap<>();
		nested.put("null", null);
		nested.put("unsupported", unsupported);
		nested.put("nan", Double.NaN);
		nested.put("jsonNull", JSONObject.NULL);
		final Map<String, Object> attributes = new LinkedHashMap<>(nested);
		attributes.put("nested", nested);
		final OperationalDataDeduplicator.FilterResult result = deduplicator.filter(attributes, attributes);
		assertEquals(attributes, result.changed);
		assertSame(nested, result.changed.get("nested"));
		assertSame(unsupported, result.changed.get("unsupported"));
		assertTrue(result.changed.containsKey("null"));
		assertTrue(result.ownerMatches.isEmpty());
		deduplicator.commit(result.changed);
		assertTrue(persisted.isEmpty());
	}

	@Test
	public void unexpectedKnownTypesAreNeverComparedOrPersisted() {
		final Object[] unexpected = {
			null,
			JSONObject.NULL,
			1,
			1.0,
			true,
			Double.NaN,
			Double.POSITIVE_INFINITY,
			new Object(),
			List.of(),
			List.of("a", "b"),
			new String[] { "a", "b" },
			Arrays.asList("a", null),
			Map.of(),
			Map.of("nested", "value"),
		};
		for (final Object value : unexpected) {
			final Map<String, Object> attributes = attributes(value, value);
			final OperationalDataDeduplicator.FilterResult result = deduplicator.filter(attributes, attributes);
			assertEquals(attributes, result.changed);
			assertSame(value, result.changed.get("timezone"));
			assertSame(attributes.get("tokens"), result.changed.get("tokens"));
			assertTrue(result.ownerMatches.isEmpty());
			deduplicator.commit(attributes);
			assertTrue(persisted.isEmpty());
			assertTrue(deduplicator.readSynced().isEmpty());
		}
	}

	@Test
	public void unexpectedKnownTypesDoNotOverwriteConfirmedStrings() {
		final Map<String, Object> confirmed = attributes("1", "true");
		deduplicator.commit(confirmed);
		final Map<String, Object> unexpected = attributes(1, true);
		assertEquals(unexpected, deduplicator.filter(unexpected, unexpected).changed);
		deduplicator.commit(unexpected);
		deduplicator.commit(attributes(null, JSONObject.NULL));
		assertEquals(confirmed, deduplicator.readSynced());
		assertTrue(deduplicator.filter(confirmed, null).changed.isEmpty());
	}

	@Test
	public void unexpectedTokensContainerIsForwardedAndIgnoredByCommit() {
		for (final Object value : Arrays.asList(null, "push", List.of("push"), 1, JSONObject.NULL)) {
			final Map<String, Object> attributes = new LinkedHashMap<>();
			attributes.put("tokens", value);
			assertEquals(attributes, deduplicator.filter(attributes, attributes).changed);
			deduplicator.commit(attributes);
			assertTrue(persisted.isEmpty());
		}
	}

	@Test
	public void mixedNestedAttributesOnlyDeduplicateSelectedStrings() {
		final Map<String, Object> custom = Map.of("timezone", "same", "tokens", Map.of("pushNotification", "push"));
		final Map<String, Object> attributes = Map.of(
			"timezone",
			"same",
			"custom",
			custom,
			"tokens",
			Map.of("pushNotification", "push", "nested", custom, "liveActivityStart", List.of("ios-only"))
		);
		deduplicator.commit(attributes);
		final OperationalDataDeduplicator.FilterResult result = deduplicator.filter(attributes, attributes);
		assertEquals(
			Map.of("custom", custom, "tokens", Map.of("nested", custom, "liveActivityStart", List.of("ios-only"))),
			result.changed
		);
		assertSame(custom, result.changed.get("custom"));
		assertTrue(result.ownerMatches.isEmpty());
		assertEquals(attributes("same", "push"), deduplicator.readSynced());
	}

	@Test
	public void unknownValuesRemainTheOriginalReferences() {
		final Map<String, Object> cyclic = new LinkedHashMap<>();
		cyclic.put("self", cyclic);
		final Map<String, Object> attributes = Map.of("custom", cyclic, "tokens", Map.of("other", cyclic));
		final OperationalDataDeduplicator.FilterResult result = deduplicator.filter(attributes, attributes);
		assertSame(cyclic, result.changed.get("custom"));
		assertSame(attributes.get("tokens"), result.changed.get("tokens"));
		assertTrue(result.ownerMatches.isEmpty());
		deduplicator.commit(attributes);
		assertTrue(persisted.isEmpty());
	}

	@Test
	public void commitReplacesOnlyProvidedStringsAndKeepsOtherConfirmedValues() {
		deduplicator.commit(attributes("Asia/Tokyo", "token-1"));
		deduplicator.commit(Map.of("tokens", Map.of("pushNotification", "token-2", "other", "ignored")));
		assertEquals(attributes("Asia/Tokyo", "token-2"), deduplicator.readSynced());
		deduplicator.commit(Map.of("timezone", "Europe/London", "other", "ignored"));
		assertEquals(attributes("Europe/London", "token-2"), new OperationalDataDeduplicator(store).readSynced());
		assertEquals(Map.of("timezone", "Europe/London", "pushNotification", "token-2"), persisted);
	}

	@Test
	public void readSyncedReconstructsOnlyPresentKeys() {
		assertTrue(deduplicator.readSynced().isEmpty());
		persisted.put("pushNotification", "push");
		persisted.put("other", "ignored");
		assertEquals(Map.of("tokens", Map.of("pushNotification", "push")), deduplicator.readSynced());
		persisted.remove("pushNotification");
		persisted.put("timezone", "Asia/Tokyo");
		assertEquals(Map.of("timezone", "Asia/Tokyo"), deduplicator.readSynced());
	}

	@Test
	public void modifyingReadSyncedDoesNotChangePersistence() {
		final Map<String, Object> attributes = attributes("Europe/London", "push");
		deduplicator.commit(attributes);
		final Map<String, Object> snapshot = deduplicator.readSynced();
		((Map<?, ?>) snapshot.get("tokens")).clear();
		snapshot.clear();
		assertEquals(attributes, deduplicator.readSynced());
	}

	@Test
	public void clearRemovesOnlyBothConfirmedDeviceKeys() {
		deduplicator.commit(attributes("Europe/London", "push"));
		persisted.put("other", "keep");
		deduplicator.clear();
		assertEquals(Map.of("other", "keep"), persisted);
		assertTrue(deduplicator.readSynced().isEmpty());
		assertTrue(new OperationalDataDeduplicator(store).readSynced().isEmpty());
		assertEquals(
			attributes("Europe/London", "push"),
			deduplicator.filter(attributes("Europe/London", "push"), null).changed
		);
		verify(store).remove("timezone");
		verify(store).remove("pushNotification");
		verify(store, never()).clearAll();
	}

	@Test
	public void nullAndEmptyAttributeMapsDoNotWrite() {
		assertTrue(deduplicator.filter(null, null).changed.isEmpty());
		final OperationalDataDeduplicator.FilterResult result = deduplicator.filter(Map.of(), attributes("x", "y"));
		assertTrue(result.changed.isEmpty());
		assertTrue(result.ownerMatches.isEmpty());
		deduplicator.commit(null);
		deduplicator.commit(Map.of());
		assertTrue(persisted.isEmpty());
	}

	private static Map<String, Object> attributes(final Object timezone, final Object pushToken) {
		final Map<String, Object> tokens = new LinkedHashMap<>();
		tokens.put("pushNotification", pushToken);
		final Map<String, Object> attributes = new LinkedHashMap<>();
		attributes.put("timezone", timezone);
		attributes.put("tokens", tokens);
		return attributes;
	}
}
