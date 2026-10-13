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
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public class DeviceAttributeDeduplicatorTests {

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
	public void testCanonicalization_rejectsUnsupportedValuesAndPreservesNull() {
		final Map<String, Object> attributes = new HashMap<>();
		attributes.put(null, "invalid-key");
		assertNull(DeviceAttributeDeduplicator.canonicalize(attributes));
		assertNull(DeviceAttributeDeduplicator.canonicalize(new Object()));
		assertNull(DeviceAttributeDeduplicator.canonicalize(Double.NaN));
		assertEquals("null", DeviceAttributeDeduplicator.canonicalize(null));
	}
}
