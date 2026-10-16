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
		assertEquals(attributes, deduplicator.filter(attributes, true, null));
		assertEquals(attributes, deduplicator.filter(attributes, true, null));
		assertTrue(persisted.isEmpty());
		deduplicator.commit(attributes);
		assertTrue(deduplicator.filter(attributes, true, null).isEmpty());
		assertEquals(attributes, new DeviceAttributeDeduplicator(store).readSynced());
	}

	@Test
	public void nestedAttributesAreDeduplicatedIndependently() {
		deduplicator.commit(Map.of("custom", Map.of("a", "old", "b", "same")));
		assertEquals(
			Map.of("custom", Map.of("a", "new")),
			deduplicator.filter(Map.of("custom", Map.of("a", "new", "b", "same")), true, null)
		);
	}

	@Test
	public void filteredPayloadDoesNotRetainMutableValues() {
		final List<Object> values = new ArrayList<>(List.of(new HashMap<>(Map.of("value", "original"))));
		final Map<String, Object> attributes = Map.of("custom", values);
		final Map<String, Object> payload = deduplicator.filter(attributes, true, null);
		values.clear();
		assertEquals(Map.of("custom", List.of(Map.of("value", "original"))), payload);
		deduplicator.commit(payload);
		assertTrue(deduplicator.filter(payload, true, null).isEmpty());
		assertEquals(attributes, deduplicator.filter(attributes, true, null));
	}

	@Test
	public void commitDoesNotRetainMutableValues() {
		final List<Object> values = new ArrayList<>(List.of("original"));
		deduplicator.commit(Map.of("custom", values));
		values.clear();
		assertEquals(Map.of("custom", List.of("original")), deduplicator.readSynced());
	}

	@Test
	public void ownerMatchesAreNotReturnedAndAreCommitted() {
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
		assertEquals(
			Map.of("tokens", Map.of("pushNotification", "push")),
			deduplicator.filter(attributes, true, owners)
		);
		assertEquals(
			Map.of("timezone", "Europe/London"),
			persisted.get(IdentityConstants.DeviceAttributes.SYNCED_VALUES)
		);
		assertEquals(Map.of("tokens", Map.of("pushNotification", "push")), deduplicator.filter(attributes, true, null));
	}

	@Test
	public void disabledDedupIgnoresSyncedAndOwnerValues() {
		final Map<String, Object> attributes = Map.of("timezone", "Europe/London");
		deduplicator.commit(attributes);
		assertEquals(attributes, deduplicator.filter(attributes, false, attributes));
		assertEquals(attributes, deduplicator.readSynced());
	}

	@Test
	public void emptyPushTokenIsPreservedAndComparedAsAString() {
		final Map<String, Object> attributes = Map.of("tokens", Map.of("pushNotification", ""));
		assertEquals(
			attributes,
			deduplicator.filter(attributes, true, Map.of("tokens", Map.of("pushNotification", "previous")))
		);
		assertEquals(attributes, deduplicator.filter(attributes, true, null));
		deduplicator.commit(attributes);
		assertTrue(deduplicator.filter(attributes, true, null).isEmpty());
	}

	@Test
	public void nullIsDroppedButEmptyContainersAreValues() {
		final Map<String, Object> attributes = new HashMap<>();
		attributes.put("null", null);
		attributes.put("array", List.of());
		attributes.put("object", Map.of());
		final Map<String, Object> expected = Map.of("array", List.of(), "object", Map.of());
		final Map<String, Object> changed = deduplicator.filter(attributes, false, null);
		assertEquals(expected, changed);
		deduplicator.commit(changed);
		assertEquals(expected, deduplicator.readSynced());
		assertTrue(deduplicator.filter(attributes, true, null).isEmpty());
	}

	@Test
	public void invalidJSONLeavesAreDroppedWithoutWritingStorage() {
		final Map<String, Object> nested = new HashMap<>();
		nested.put("valid", "value");
		nested.put("null", null);
		nested.put("invalid", new Object());
		assertEquals(
			Map.of("nested", Map.of("valid", "value")),
			deduplicator.filter(Map.of("nested", nested, "nan", Double.NaN), false, null)
		);
		assertTrue(persisted.isEmpty());
	}

	@Test
	public void arraysAreComparedAsSets() {
		deduplicator.commit(Map.of("array", List.of("first", "second")));
		assertTrue(deduplicator.filter(Map.of("array", List.of("second", "first")), true, null).isEmpty());
		assertTrue(deduplicator.filter(Map.of("array", new String[] { "second", "first" }), true, null).isEmpty());
		assertEquals(
			Map.of("array", List.of("first")),
			deduplicator.filter(Map.of("array", List.of("first")), true, null)
		);
		assertEquals(
			Map.of("array", Arrays.asList("first", null)),
			deduplicator.filter(Map.of("array", Arrays.asList("first", null)), true, null)
		);
	}

	@Test
	public void commitDeepMergesMapsAndReplacesOtherValues() {
		deduplicator.commit(Map.of("prefs", Map.of("lang", "en", "theme", "dark"), "tags", List.of("a")));
		deduplicator.commit(Map.of("prefs", Map.of("lang", "fr")));
		assertEquals(
			Map.of("prefs", Map.of("lang", "fr", "theme", "dark"), "tags", List.of("a")),
			deduplicator.readSynced()
		);
		deduplicator.commit(Map.of("prefs", Map.of(), "tags", Map.of("x", 1)));
		assertEquals(Map.of("prefs", Map.of(), "tags", Map.of("x", 1)), deduplicator.readSynced());
		deduplicator.commit(Map.of("prefs", "off"));
		assertEquals(
			Map.of("prefs", "off", "tags", Map.of("x", 1)),
			new DeviceAttributeDeduplicator(store).readSynced()
		);
	}

	@Test
	public void booleansRemainDistinctFromNumbersAndNumbersSurviveSnapshots() {
		deduplicator.commit(Map.of("value", true));
		assertEquals(Map.of("value", 1), deduplicator.filter(Map.of("value", 1), true, null));
		deduplicator.commit(deduplicator.filter(Map.of("value", 1.0), true, null));
		assertTrue(deduplicator.filter(Map.of("value", 1.0), true, null).isEmpty());
		assertTrue(new DeviceAttributeDeduplicator(store).filter(Map.of("value", 1.0), true, null).isEmpty());
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
}
