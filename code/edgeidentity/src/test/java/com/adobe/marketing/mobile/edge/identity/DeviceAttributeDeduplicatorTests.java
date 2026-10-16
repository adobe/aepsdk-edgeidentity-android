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
		assertEquals(attributes, deduplicator.filter(attributes, true, null, null));
		assertEquals(attributes, deduplicator.filter(attributes, true, null, null));
		assertTrue(persisted.isEmpty());
		deduplicator.commit(attributes);
		assertTrue(deduplicator.filter(attributes, true, null, null).isEmpty());
		assertEquals(attributes, new DeviceAttributeDeduplicator(store).readSynced());
	}

	@Test
	public void nestedAttributesAreDeduplicatedIndependently() {
		deduplicator.commit(Map.of("custom", Map.of("a", "old", "b", "same")));
		assertEquals(
			Map.of("custom", Map.of("a", "new")),
			deduplicator.filter(Map.of("custom", Map.of("a", "new", "b", "same")), true, null, null)
		);
	}

	@Test
	public void filteredPayloadDoesNotRetainMutableValues() {
		final List<Object> values = new ArrayList<>(List.of(new HashMap<>(Map.of("value", "original"))));
		final Map<String, Object> attributes = Map.of("custom", values);
		final Map<String, Object> payload = deduplicator.filter(attributes, true, null, null);
		values.clear();
		assertEquals(Map.of("custom", List.of(Map.of("value", "original"))), payload);
		deduplicator.commit(payload);
		assertTrue(deduplicator.filter(payload, true, null, null).isEmpty());
		assertEquals(attributes, deduplicator.filter(attributes, true, null, null));
	}

	@Test
	public void commitDoesNotRetainMutableValues() {
		final List<Object> values = new ArrayList<>(List.of("original"));
		deduplicator.commit(Map.of("custom", values));
		values.clear();
		assertEquals(Map.of("custom", List.of("original")), deduplicator.readSynced());
	}

	@Test
	public void ownerMatchesDoNotCommitAndDisabledDedupIgnoresAllStores() {
		final Map<String, Object> attributes = Map.of(
			"timezone",
			"Europe/London",
			"tokens",
			Map.of("pushNotification", "push")
		);
		final Map<String, Object> messagingState = Map.of("pushidentifier", "push");
		assertTrue(deduplicator.filter(attributes, true, messagingState, "Europe/London").isEmpty());
		assertTrue(persisted.isEmpty());
		assertEquals(attributes, deduplicator.filter(attributes, true, null, null));
		deduplicator.commit(attributes);
		assertEquals(attributes, deduplicator.filter(attributes, false, messagingState, "Europe/London"));
	}

	@Test
	public void emptyPushTokenIsPreservedAndComparedAsAString() {
		final Map<String, Object> attributes = Map.of("tokens", Map.of("pushNotification", ""));
		assertEquals(attributes, deduplicator.filter(attributes, true, Map.of("pushidentifier", "previous"), null));
		assertTrue(deduplicator.filter(attributes, true, Map.of("pushidentifier", ""), null).isEmpty());
		assertEquals(attributes, deduplicator.filter(attributes, true, null, null));
		deduplicator.commit(attributes);
		assertTrue(deduplicator.filter(attributes, true, null, null).isEmpty());
	}

	@Test
	public void nullIsDroppedButEmptyContainersAreValuesAndAppIsNotAnAttribute() {
		final Map<String, Object> attributes = new HashMap<>();
		attributes.put("null", null);
		attributes.put("array", List.of());
		attributes.put("object", Map.of());
		attributes.put("app", Map.of("id", "caller"));
		final Map<String, Object> expected = Map.of("array", List.of(), "object", Map.of());
		assertEquals(expected, deduplicator.filter(attributes, false, null, null));
		deduplicator.commit(attributes);
		assertEquals(expected, deduplicator.readSynced());
	}

	@Test
	public void invalidJSONLeavesAreDroppedWithoutWritingStorage() {
		final Map<String, Object> nested = new HashMap<>();
		nested.put("valid", "value");
		nested.put("null", null);
		nested.put("invalid", new Object());
		assertEquals(
			Map.of("nested", Map.of("valid", "value")),
			deduplicator.filter(Map.of("nested", nested, "nan", Double.NaN), false, null, null)
		);
		assertTrue(persisted.isEmpty());
	}

	@Test
	public void arraysAreAtomicAndOrderChangesAreSent() {
		deduplicator.commit(Map.of("array", List.of("first", "second")));
		assertEquals(
			Map.of("array", List.of("second", "first")),
			deduplicator.filter(Map.of("array", List.of("second", "first")), true, null, null)
		);
		assertEquals(
			Map.of("array", Arrays.asList("first", null)),
			deduplicator.filter(Map.of("array", Arrays.asList("first", null)), true, null, null)
		);
	}

	@Test
	public void booleansRemainDistinctFromNumbersAndNumbersSurviveSnapshots() {
		deduplicator.commit(Map.of("value", true));
		assertEquals(Map.of("value", 1), deduplicator.filter(Map.of("value", 1), true, null, null));
		deduplicator.commit(deduplicator.filter(Map.of("value", 1.0), true, null, null));
		assertTrue(deduplicator.filter(Map.of("value", 1.0), true, null, null).isEmpty());
		assertTrue(new DeviceAttributeDeduplicator(store).filter(Map.of("value", 1.0), true, null, null).isEmpty());
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
