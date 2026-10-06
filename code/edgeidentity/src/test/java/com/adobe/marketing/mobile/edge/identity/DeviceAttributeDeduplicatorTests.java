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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import org.mockito.Mockito;

public class DeviceAttributeDeduplicatorTests {

	@Test
	public void testFilter_deduplicatesNestedMapsIndependentOfKeyOrder() {
		final ProfileAttributeStore store = Mockito.mock(ProfileAttributeStore.class);
		final String syncedValues = "{\"attribute\":\"{\\\"a\\\":1,\\\"b\\\":2}\"}";
		when(store.getString(IdentityConstants.DeviceAttributes.SYNCED_VALUES)).thenReturn(null, syncedValues);
		final DeviceAttributeDeduplicator deduplicator = new DeviceAttributeDeduplicator(store);

		final Map<String, Object> firstNestedValue = new LinkedHashMap<>();
		firstNestedValue.put("b", 2);
		firstNestedValue.put("a", 1);
		final Map<String, Object> firstAttributes = Map.of("attribute", firstNestedValue);

		assertEquals(firstAttributes, deduplicator.filter(firstAttributes, true));
		assertTrue(deduplicator.filter(Map.of("attribute", Map.of("a", 1, "b", 2)), true).isEmpty());
		verify(store, times(1)).setString(IdentityConstants.DeviceAttributes.SYNCED_VALUES, syncedValues);
	}

	@Test
	public void testFilter_dropsNonJsonAttributesAndNullKeys() {
		final ProfileAttributeStore store = Mockito.mock(ProfileAttributeStore.class);
		when(store.getString(IdentityConstants.DeviceAttributes.SYNCED_VALUES)).thenReturn(null);
		final DeviceAttributeDeduplicator deduplicator = new DeviceAttributeDeduplicator(store);
		final Map<String, Object> attributes = new HashMap<>();
		attributes.put(null, "invalid-key");
		attributes.put("invalid", new Object());
		attributes.put("valid", true);

		assertEquals(Map.of("valid", true), deduplicator.filter(attributes, false));
		verify(store).setString(IdentityConstants.DeviceAttributes.SYNCED_VALUES, "{\"valid\":\"true\"}");
	}
}
