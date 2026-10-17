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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class DeviceAttributeDeduplicatorTests {

	private ProfileAttributeStore store;
	private DeviceAttributeDeduplicator deduplicator;
	private Map<String, Map<String, Object>> persisted;

	@Before
	public void setup() {
		store = mock(ProfileAttributeStore.class);
		persisted = new HashMap<>();
		when(store.getMap(anyString())).thenAnswer(call -> persisted.get(call.<String>getArgument(0)));
		doAnswer(call -> persisted.put(call.getArgument(0), call.getArgument(1)))
			.when(store)
			.setMap(anyString(), anyMap());
		doAnswer(call -> persisted.remove(call.<String>getArgument(0))).when(store).remove(anyString());
		deduplicator = new DeviceAttributeDeduplicator(store);
	}

	@Test
	public void filterDoesNotWriteUntilCommit() {
		final Map<String, Object> attributes = Map.of("timezone", "Europe/London");
		assertEquals(attributes, deduplicator.filter(attributes, true, null).changed);
		assertEquals(attributes, deduplicator.filter(attributes, true, null).changed);
		assertTrue(persisted.isEmpty());
		deduplicator.commit(attributes);
		assertTrue(deduplicator.filter(attributes, true, null).changed.isEmpty());
		assertEquals(attributes, new DeviceAttributeDeduplicator(store).readSynced());
	}

	@Test
	public void nestedAttributesAreDeduplicatedIndependently() {
		deduplicator.commit(Map.of("timezone", "same", "tokens", Map.of("pushNotification", "old")));
		assertEquals(
			Map.of("tokens", Map.of("pushNotification", "new")),
			deduplicator.filter(Map.of("timezone", "same", "tokens", Map.of("pushNotification", "new")), true, null)
				.changed
		);
	}

	@Test
	public void filteredPayloadDoesNotRetainMutableValues() {
		final List<Object> values = new ArrayList<>(List.of(new HashMap<>(Map.of("value", "original"))));
		final Map<String, Object> attributes = Map.of("timezone", values);
		final Map<String, Object> payload = deduplicator.filter(attributes, true, null).changed;
		values.clear();
		assertEquals(Map.of("timezone", List.of(Map.of("value", "original"))), payload);
		deduplicator.commit(payload);
		assertTrue(deduplicator.filter(payload, true, null).changed.isEmpty());
		assertEquals(attributes, deduplicator.filter(attributes, true, null).changed);
	}

	@Test
	public void commitDoesNotRetainMutableValues() {
		final List<Object> values = new ArrayList<>(List.of("original"));
		deduplicator.commit(Map.of("timezone", values));
		values.clear();
		assertEquals(Map.of("timezone", List.of("original")), deduplicator.readSynced());
	}

	@Test
	public void ownerMatchesAreReturnedSeparatelyAndNotCommitted() {
		final Map<String, Object> attributes = Map.of(
			"timezone",
			"Europe/London",
			"tokens",
			Map.of("pushNotification", "push")
		);
		final Map<String, Object> owners = Map.of(
			"timezone",
			"Europe/London",
			"tokens",
			Map.of("pushNotification", "other")
		);
		final DeviceAttributeDeduplicator.FilterResult result = deduplicator.filter(attributes, true, owners);
		assertEquals(Map.of("tokens", Map.of("pushNotification", "push")), result.changed);
		assertEquals(Map.of("timezone", "Europe/London"), result.ownerMatches);
		assertTrue(persisted.isEmpty());
		assertEquals(attributes, deduplicator.filter(attributes, true, null).changed);
	}

	@Test
	public void nestedOwnerMatchesKeepTheirPath() {
		final Map<String, Object> attributes = Map.of("tokens", Map.of("pushNotification", "push", "other", "x"));
		final DeviceAttributeDeduplicator.FilterResult result = deduplicator.filter(
			attributes,
			true,
			Map.of("tokens", Map.of("pushNotification", "push"))
		);
		assertEquals(Map.of("tokens", Map.of("other", "x")), result.changed);
		assertEquals(Map.of("tokens", Map.of("pushNotification", "push")), result.ownerMatches);
	}

	@Test
	public void syncedValuesAreNotReportedAsOwnerMatches() {
		final Map<String, Object> attributes = Map.of("timezone", "Europe/London");
		deduplicator.commit(attributes);
		final DeviceAttributeDeduplicator.FilterResult result = deduplicator.filter(attributes, true, attributes);
		assertTrue(result.changed.isEmpty());
		assertTrue(result.ownerMatches.isEmpty());
	}

	@Test
	public void disabledDedupIgnoresSyncedAndOwnerValues() {
		final Map<String, Object> attributes = Map.of("timezone", "Europe/London");
		deduplicator.commit(attributes);
		final DeviceAttributeDeduplicator.FilterResult result = deduplicator.filter(attributes, false, attributes);
		assertEquals(attributes, result.changed);
		assertTrue(result.ownerMatches.isEmpty());
		assertEquals(attributes, deduplicator.readSynced());
	}

	@Test
	public void emptyPushTokenIsPreservedAndComparedAsAString() {
		final Map<String, Object> attributes = Map.of("tokens", Map.of("pushNotification", ""));
		assertEquals(
			attributes,
			deduplicator.filter(attributes, true, Map.of("tokens", Map.of("pushNotification", "previous"))).changed
		);
		assertEquals(attributes, deduplicator.filter(attributes, true, null).changed);
		deduplicator.commit(attributes);
		assertTrue(deduplicator.filter(attributes, true, null).changed.isEmpty());
	}

	@Test
	public void nullIsDroppedButEmptyContainersAreValues() {
		final Map<String, Object> attributes = new HashMap<>();
		attributes.put("null", null);
		attributes.put("timezone", List.of());
		attributes.put("tokens", Map.of("pushNotification", Map.of()));
		final Map<String, Object> expected = Map.of(
			"timezone",
			List.of(),
			"tokens",
			Map.of("pushNotification", Map.of())
		);
		final Map<String, Object> changed = deduplicator.filter(attributes, false, null).changed;
		assertEquals(expected, changed);
		deduplicator.commit(changed);
		assertEquals(expected, deduplicator.readSynced());
		assertTrue(deduplicator.filter(attributes, true, null).changed.isEmpty());
	}

	@Test
	public void invalidJSONLeavesAreDroppedWithoutWritingStorage() {
		final Map<String, Object> nested = new HashMap<>();
		nested.put("valid", "value");
		nested.put("null", null);
		nested.put("invalid", new Object());
		assertEquals(
			Map.of("nested", Map.of("valid", "value")),
			deduplicator.filter(Map.of("nested", nested, "nan", Double.NaN), false, null).changed
		);
		assertTrue(persisted.isEmpty());
	}

	@Test
	public void arraysAreComparedAsSets() {
		deduplicator.commit(Map.of("timezone", List.of("first", "second")));
		assertTrue(deduplicator.filter(Map.of("timezone", List.of("second", "first")), true, null).changed.isEmpty());
		assertTrue(
			deduplicator.filter(Map.of("timezone", new String[] { "second", "first" }), true, null).changed.isEmpty()
		);
		assertEquals(
			Map.of("timezone", List.of("first")),
			deduplicator.filter(Map.of("timezone", List.of("first")), true, null).changed
		);
		assertEquals(
			Map.of("timezone", Arrays.asList("first", null)),
			deduplicator.filter(Map.of("timezone", Arrays.asList("first", null)), true, null).changed
		);
		deduplicator.commit(Map.of("tokens", Map.of("pushNotification", List.of(List.of("x", "y"), List.of("z")))));
		assertTrue(
			deduplicator
				.filter(
					Map.of("tokens", Map.of("pushNotification", List.of(List.of("z", "z"), List.of("y", "x", "x")))),
					true,
					null
				)
				.changed.isEmpty()
		);
	}

	@Test
	public void commitReplacesValueAtEachDeduplicatedPathAndKeepsSiblings() {
		deduplicator.commit(Map.of("timezone", "Asia/Tokyo", "tokens", Map.of("pushNotification", "token-1")));
		deduplicator.commit(Map.of("tokens", Map.of("pushNotification", "token-2")));
		assertEquals(
			Map.of("timezone", "Asia/Tokyo", "tokens", Map.of("pushNotification", "token-2")),
			deduplicator.readSynced()
		);
		deduplicator.commit(Map.of("tokens", Map.of("pushNotification", Map.of("token", "token-3", "meta", "x"))));
		deduplicator.commit(Map.of("tokens", Map.of("pushNotification", Map.of("token", "token-4"))));
		assertEquals(
			Map.of("timezone", "Asia/Tokyo", "tokens", Map.of("pushNotification", Map.of("token", "token-4"))),
			deduplicator.readSynced()
		);
		deduplicator.commit(Map.of("tokens", Map.of("pushNotification", "off")));
		assertEquals(
			Map.of("timezone", "Asia/Tokyo", "tokens", Map.of("pushNotification", "off")),
			new DeviceAttributeDeduplicator(store).readSynced()
		);
	}

	@Test
	public void mapAtDeduplicatedPathIsComparedAndStoredWhole() {
		deduplicator.commit(Map.of("tokens", Map.of("pushNotification", Map.of("token", "a", "meta", "x"))));
		final Map<String, Object> attributes = Map.of("tokens", Map.of("pushNotification", Map.of("token", "b")));
		assertEquals(attributes, deduplicator.filter(attributes, true, null).changed);

		deduplicator.commit(attributes);

		assertEquals(attributes, deduplicator.readSynced());
		assertEquals(attributes, new DeviceAttributeDeduplicator(store).readSynced());
		assertTrue(deduplicator.filter(attributes, true, null).changed.isEmpty());
	}

	@Test
	public void booleansRemainDistinctFromNumbersAndNumbersSurviveSnapshots() {
		deduplicator.commit(Map.of("timezone", true));
		assertEquals(Map.of("timezone", 1), deduplicator.filter(Map.of("timezone", 1), true, null).changed);
		deduplicator.commit(deduplicator.filter(Map.of("timezone", 1.0), true, null).changed);
		assertTrue(deduplicator.filter(Map.of("timezone", 1.0), true, null).changed.isEmpty());
		assertTrue(
			new DeviceAttributeDeduplicator(store).filter(Map.of("timezone", 1.0), true, null).changed.isEmpty()
		);
	}

	@Test
	public void clearRemovesOnlyConfirmedDeviceValues() {
		deduplicator.commit(Map.of("timezone", "Europe/London"));
		deduplicator.clear();
		assertTrue(persisted.isEmpty());
		assertTrue(deduplicator.readSynced().isEmpty());
		verify(store).remove(IdentityConstants.DeviceAttributes.SYNCED_VALUES);
		verify(store, never()).clearAll();
	}

	@Test
	public void testCanonicalization_isIndependentOfMapKeyOrder() {
		final Map<String, Object> firstNestedValue = new LinkedHashMap<>();
		firstNestedValue.put("b", 2);
		firstNestedValue.put("a", 1);
		assertEquals("{\"a\":1,\"b\":2}", DeviceAttributeDeduplicator.canonicalize(firstNestedValue));
		assertEquals(
			DeviceAttributeDeduplicator.canonicalize(firstNestedValue),
			DeviceAttributeDeduplicator.canonicalize(Map.of("a", 1, "b", 2))
		);
	}

	@Test
	public void testCanonicalization_rejectsUnsupportedValuesAndTopLevelNull() {
		final Map<String, Object> attributes = new HashMap<>();
		attributes.put(null, "invalid-key");
		assertNull(DeviceAttributeDeduplicator.canonicalize(attributes));
		assertNull(DeviceAttributeDeduplicator.canonicalize(new Object()));
		assertNull(DeviceAttributeDeduplicator.canonicalize(Double.NaN));
		assertNull(DeviceAttributeDeduplicator.canonicalize(null));
		final Map<String, Object> nestedNull = new HashMap<>();
		nestedNull.put("value", null);
		assertEquals("{\"value\":null}", DeviceAttributeDeduplicator.canonicalize(nestedNull));
	}

	// Shared canonicalize cases: keep in sync with the iOS DeviceAttributeDeduplicator tests.

	@Test
	public void testCanonicalization_sharedEqualCases() {
		final Map<String, Object> reorderedKeys = new LinkedHashMap<>();
		reorderedKeys.put("b", 2);
		reorderedKeys.put("a", 1);
		final Object[][] equalPairs = {
			{ 1, 1.0 },
			{ 0, -0.0 },
			{ 5L, 5.0f },
			{ "a", "a" },
			{ reorderedKeys, Map.of("a", 1, "b", 2) },
			{ List.of("a", "b"), List.of("b", "a") },
			{ List.of("a", "a", "b"), List.of("b", "a") },
			{ List.of(List.of("x", "y"), List.of("z")), List.of(List.of("z"), List.of("y", "x")) },
			{ Map.of("k", List.of(1, List.of(2, 3, 3))), Map.of("k", List.of(List.of(3, 2), 1.0)) },
			{ List.of(Map.of("a", 1), Map.of("b", 2)), List.of(Map.of("b", 2), Map.of("a", 1)) },
		};
		for (final Object[] pair : equalPairs) {
			final String first = DeviceAttributeDeduplicator.canonicalize(pair[0]);
			assertNotNull(first);
			assertEquals(first, DeviceAttributeDeduplicator.canonicalize(pair[1]));
		}
	}

	@Test
	public void testCanonicalization_sharedDistinctCases() {
		final Object[][] distinctPairs = {
			{ true, 1 },
			{ false, 0 },
			{ "1", 1 },
			{ 1.5, 1 },
			{ List.of(), Map.of() },
			{ Arrays.asList((Object) null), List.of() },
			{ Collections.singletonMap("a", null), Map.of() },
			{ List.of(List.of("a", "b")), List.of("a", "b") },
		};
		for (final Object[] pair : distinctPairs) {
			assertNotEquals(
				DeviceAttributeDeduplicator.canonicalize(pair[0]),
				DeviceAttributeDeduplicator.canonicalize(pair[1])
			);
		}
	}

	@Test
	public void testCanonicalization_sharedInvalidCases() {
		final Object[] invalid = {
			null,
			Double.NaN,
			Double.POSITIVE_INFINITY,
			Double.NEGATIVE_INFINITY,
			new Object(),
			Map.of(1, "x"),
			List.of("a", Double.NaN),
			Map.of("a", Double.POSITIVE_INFINITY),
		};
		for (final Object value : invalid) {
			assertNull(DeviceAttributeDeduplicator.canonicalize(value));
		}
	}

	@Test
	public void testCanonicalization_sharedExpectedStrings() {
		assertEquals("\"a\"", DeviceAttributeDeduplicator.canonicalize("a"));
		assertEquals("true", DeviceAttributeDeduplicator.canonicalize(true));
		assertEquals("1", DeviceAttributeDeduplicator.canonicalize(1.0));
		assertEquals("0", DeviceAttributeDeduplicator.canonicalize(-0.0));
		assertEquals("[\"a\",\"b\"]", DeviceAttributeDeduplicator.canonicalize(List.of("b", "a", "a")));
		assertEquals("{\"a\":[1,2],\"b\":null}", DeviceAttributeDeduplicator.canonicalize(mapWithNull()));
		assertEquals("[]", DeviceAttributeDeduplicator.canonicalize(List.of()));
		assertEquals("{}", DeviceAttributeDeduplicator.canonicalize(Map.of()));
	}

	private static Map<String, Object> mapWithNull() {
		final Map<String, Object> map = new HashMap<>();
		map.put("b", null);
		map.put("a", List.of(2, 1, 2));
		return map;
	}

	@Test
	public void deeplyNestedCustomValuesAreSentInFull() {
		final Map<String, Object> attributes = Map.of(
			"maps",
			nestedMaps(40),
			"lists",
			nestedLists(40),
			"timezone",
			"Asia/Tokyo"
		);
		assertEquals(attributes, deduplicator.filter(attributes, true, null).changed);

		deduplicator.commit(attributes);

		assertEquals(Map.of("timezone", "Asia/Tokyo"), deduplicator.readSynced());
		assertEquals(
			Map.of("maps", nestedMaps(40), "lists", nestedLists(40)),
			deduplicator.filter(attributes, true, null).changed
		);
	}

	/** Returns {@code levels} maps nested inside each other, with a string in the innermost one. */
	private static Map<String, Object> nestedMaps(final int levels) {
		Map<String, Object> value = Map.of("leaf", "value");
		for (int level = 1; level < levels; level++) {
			value = Map.of("level", value);
		}
		return value;
	}

	/** Returns {@code levels} lists nested inside each other, with a string in the innermost one. */
	private static List<Object> nestedLists(final int levels) {
		List<Object> value = List.of("value");
		for (int level = 1; level < levels; level++) {
			value = List.of(value);
		}
		return value;
	}

	@Test
	public void customKeysAreAlwaysChangedAndNeverStored() {
		final Map<String, Object> attributes = Map.of("custom", "value", "tokens", Map.of("foo", "bar"));
		assertEquals(attributes, deduplicator.filter(attributes, true, null).changed);
		deduplicator.commit(attributes);
		assertTrue(deduplicator.readSynced().isEmpty());
		assertTrue(persisted.isEmpty());
		assertEquals(attributes, deduplicator.filter(attributes, true, null).changed);
	}

	@Test
	public void customKeysAreNeverOwnerMatched() {
		final Map<String, Object> attributes = Map.of("custom", "value", "timezone", "Asia/Tokyo");
		final DeviceAttributeDeduplicator.FilterResult result = deduplicator.filter(attributes, true, attributes);
		assertEquals(Map.of("custom", "value"), result.changed);
		assertEquals(Map.of("timezone", "Asia/Tokyo"), result.ownerMatches);
	}

	@Test
	public void persistedUnknownValuesArePrunedOnLoad() {
		persisted.put(
			IdentityConstants.DeviceAttributes.SYNCED_VALUES,
			Map.of(
				"timezone",
				"Asia/Tokyo",
				"custom",
				"value",
				"tokens",
				Map.of("pushNotification", "token-1", "foo", "bar")
			)
		);
		final Map<String, Object> expected = Map.of(
			"timezone",
			"Asia/Tokyo",
			"tokens",
			Map.of("pushNotification", "token-1")
		);
		assertEquals(expected, new DeviceAttributeDeduplicator(store).readSynced());
		assertEquals(expected, persisted.get(IdentityConstants.DeviceAttributes.SYNCED_VALUES));
	}
}
