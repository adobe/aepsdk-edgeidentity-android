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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.adobe.marketing.mobile.util.JSONAsserts;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class IdentityItemTests {

	@Test
	public void testIdentityItem_toObjectMap_full() {
		// setup
		IdentityItem item = new IdentityItem("id", AuthenticatedState.AUTHENTICATED, true);

		// test
		Map<String, Object> data = item.toObjectMap();

		// verify
		String expected =
			"{\n" +
			"  \"id\": \"id\",\n" +
			"  \"authenticatedState\": \"authenticated\",\n" +
			"  \"primary\": true\n" +
			"}";

		JSONAsserts.assertEquals(expected, data);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIdentityItem_ctor1_nullId_throwsIllegalArgumentException() {
		IdentityItem item = new IdentityItem(null, AuthenticatedState.AUTHENTICATED, true);
	}

	@Test
	public void testIdentityItem_ctor1_emptyId() {
		IdentityItem item = new IdentityItem("", AuthenticatedState.AUTHENTICATED, true);

		// For backward compatibility, an IdentityItem can contain empty identifiers.
		String expected =
			"{" + "  \"id\": \"\"," + "  \"authenticatedState\": \"authenticated\"," + "  \"primary\": true" + "}";

		JSONAsserts.assertEquals(expected, item.toObjectMap());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIdentityItem_ctor2_nullId_throwsIllegalArgumentException() {
		IdentityItem item = new IdentityItem((String) null);
	}

	@Test
	public void testIdentityItem_ctor2_emptyId() {
		IdentityItem item = new IdentityItem("");

		// For backward compatibility, an IdentityItem can contain empty identifiers.
		String expected =
			"{" + "  \"id\": \"\"," + "  \"authenticatedState\": \"ambiguous\"," + "  \"primary\": false" + "}";

		JSONAsserts.assertEquals(expected, item.toObjectMap());
	}

	@Test
	public void testIdentityItem_toObjectMap_missingAuthState() {
		// setup
		IdentityItem item = new IdentityItem("id", null, true);

		// test
		Map<String, Object> data = item.toObjectMap();

		// verify
		String expected =
			"{" + "  \"id\": \"id\"," + "  \"authenticatedState\": \"ambiguous\"," + "  \"primary\": true" + "}";

		JSONAsserts.assertEquals(expected, item.toObjectMap());
	}

	@Test
	public void testIdentityItem_fromData_full() {
		// setup
		Map<String, Object> map = new HashMap<>();
		map.put("id", "test-id");
		map.put("authenticatedState", "loggedOut");
		map.put("primary", true);

		// test
		IdentityItem item = IdentityItem.fromData(map);

		// verify
		String expected =
			"{" + "  \"id\": \"test-id\"," + "  \"authenticatedState\": \"loggedOut\"," + "  \"primary\": true" + "}";

		JSONAsserts.assertEquals(expected, item.toObjectMap());
	}

	@Test
	public void testIdentityItem_fromData_missingAuthState() {
		// setup
		Map<String, Object> map = new HashMap<>();
		map.put("id", "test-id");
		map.put("primary", true);

		// test
		IdentityItem item = IdentityItem.fromData(map);

		// verify
		String expected =
			"{" + "  \"id\": \"test-id\"," + "  \"authenticatedState\": \"ambiguous\"," + "  \"primary\": true" + "}";

		JSONAsserts.assertEquals(expected, item.toObjectMap());
	}

	@Test
	public void testIdentityItem_fromData_missingPrimary() {
		// setup
		Map<String, Object> map = new HashMap<>();
		map.put("id", "test-id");
		map.put("authenticatedState", "loggedOut");

		// test
		IdentityItem item = IdentityItem.fromData(map);

		// verify
		String expected =
			"{\n" +
			"  \"id\": \"test-id\"," +
			"  \"authenticatedState\": \"loggedOut\"," +
			"  \"primary\": false" +
			"}";

		JSONAsserts.assertEquals(expected, item.toObjectMap());
	}

	@Test
	public void testIdentityItem_fromData_missingId_returnNull() {
		// setup
		Map<String, Object> map = new HashMap<>();
		map.put("authenticatedState", "loggedOut");
		map.put("primary", false);

		assertNull(IdentityItem.fromData(map));
	}

	@Test
	public void testIdentityItem_fromData_nullId_returnNull() {
		// setup
		Map<String, Object> map = new HashMap<>();
		map.put("id", null);
		map.put("authenticatedState", "loggedOut");
		map.put("primary", false);

		assertNull(IdentityItem.fromData(map));
	}

	@Test
	public void testIdentityItem_fromData_emptyId() {
		// setup
		Map<String, Object> map = new HashMap<>();
		map.put("id", "");
		map.put("authenticatedState", "loggedOut");
		map.put("primary", false);

		IdentityItem item = IdentityItem.fromData(map);

		// For backward compatibility, an IdentityItem can contain empty identifiers.
		String expected =
			"{" + "  \"id\": \"\"," + "  \"authenticatedState\": \"loggedOut\"," + "  \"primary\": false" + "}";

		JSONAsserts.assertEquals(expected, item.toObjectMap());
	}

	@Test
	public void testIdentityItem_isEqualShouldReturnTrue() {
		IdentityItem item1 = new IdentityItem("id", AuthenticatedState.AMBIGUOUS, false);
		IdentityItem item2 = new IdentityItem("id", AuthenticatedState.AUTHENTICATED, true);

		assertTrue(item1.equals(item2));
	}

	@Test
	public void testIdentityItem_isEqualShouldReturnFalse() {
		IdentityItem item1 = new IdentityItem("id", AuthenticatedState.AMBIGUOUS, false);
		IdentityItem item2 = new IdentityItem("id2", AuthenticatedState.AUTHENTICATED, true);

		assertFalse(item1.equals(item2));
	}
}
