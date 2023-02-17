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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.adobe.marketing.mobile.util.JSONUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.junit.Test;

public class IdentityMapTests {

	@Test
	public void test_AddItem() {
		// test
		IdentityMap map = new IdentityMap();
		map.addItem(new IdentityItem("California"), "location");

		// verify
		IdentityTestUtil.flattenMap(map.asXDMMap(false)).get("identityMap.location[0].id");
	}

	@Test
	public void test_addItem_withEmptyID_notAdded() {
		// test
		IdentityMap map = new IdentityMap();
		map.addItem(new IdentityItem(""), "location");
		assertTrue(map.isEmpty());
	}

	@Test
	public void test_AddItem_InvalidInputs() {
		// test
		IdentityMap map = new IdentityMap();
		map.addItem(new IdentityItem("California"), "");
		map.addItem(new IdentityItem("California"), null);
		map.addItem(null, "namespace");

		// verify
		assertTrue(map.isEmpty());
	}

	@Test
	public void test_getIdentityItemsForNamespace() {
		// setup
		IdentityMap sampleUserMap = buildSampleIdentityMap(); // 2 items with namespace "Location", 3 items with namespace "Login"

		// test
		List<IdentityItem> locationItems = sampleUserMap.getIdentityItemsForNamespace("location");

		// verify
		assertEquals(2, locationItems.size());
		assertEquals("280 Highway Lane", locationItems.get(0).getId());
		assertEquals("California", locationItems.get(1).getId());

		// verify the login namespace returns 3 items
		assertEquals(3, sampleUserMap.getIdentityItemsForNamespace("login").size());
	}

	@Test
	public void test_getIdentityItemsForNamespace_InvalidInputs() {
		// setup
		IdentityMap sampleUserMap = buildSampleIdentityMap(); // 2 items with namespace "Location", 3 items with namespace "Login"

		// test 1
		List<IdentityItem> items = sampleUserMap.getIdentityItemsForNamespace("");
		assertEquals(0, items.size());

		// test 2
		items = sampleUserMap.getIdentityItemsForNamespace(null);
		assertEquals(0, items.size());

		// test 3
		items = sampleUserMap.getIdentityItemsForNamespace("unavailable");
		assertEquals(0, items.size());
	}

	@Test
	public void test_RemoveItem() {
		// setup
		IdentityMap sampleUserMap = buildSampleIdentityMap(); // 2 items with namespace "Location", 3 items with namespace "Login"

		// test
		sampleUserMap.removeItem(new IdentityItem("California"), "location");

		// verify
		Map<String, List<IdentityItem>> castedMap = getCastedIdentityMap(sampleUserMap);
		assertEquals(1, castedMap.get("location").size());
		assertEquals(3, castedMap.get("login").size());

		// test 2
		sampleUserMap.removeItem(new IdentityItem("280 Highway Lane"), "location");
		sampleUserMap.removeItem(new IdentityItem("Student"), "login");

		// verify
		Map<String, List<IdentityItem>> castedMap2 = getCastedIdentityMap(sampleUserMap);
		assertNull(castedMap2.get("location"));
		assertEquals(2, castedMap2.get("login").size());
	}

	@Test
	public void test_RemoveItem_InvalidInputs() {
		// setup
		IdentityMap sampleUserMap = buildSampleIdentityMap(); // 2 items with namespace "Location", 3 items with namespace "Login"

		// test
		sampleUserMap.removeItem(new IdentityItem("California"), "");
		sampleUserMap.removeItem(new IdentityItem("California"), null);
		sampleUserMap.removeItem(null, "location");

		// verify the existing identityMap is unchanged
		Map<String, List<IdentityItem>> castedMap = getCastedIdentityMap(sampleUserMap);
		assertEquals(2, castedMap.get("location").size());
		assertEquals(3, castedMap.get("login").size());
	}

	@Test
	public void test_merge() {
		// setup
		IdentityMap sampleUserMap = buildSampleIdentityMap(); // 2 items with namespace "Location", 3 items with namespace "Login"

		// test
		IdentityMap newMap = new IdentityMap();
		newMap.addItem(new IdentityItem("doorNumber:544"), "location");
		sampleUserMap.merge(newMap);

		// verify the existing identityMap is unchanged
		Map<String, List<IdentityItem>> castedMap = getCastedIdentityMap(sampleUserMap);
		assertEquals(3, castedMap.get("location").size());
		assertEquals(3, castedMap.get("login").size());
	}

	@Test
	public void test_merge_sameItem_GetsReplaced() {
		// setup
		IdentityMap baseMap = new IdentityMap();
		baseMap.addItem(new IdentityItem("California", AuthenticatedState.LOGGED_OUT, false), "location");

		// test
		IdentityMap newMap = new IdentityMap();
		newMap.addItem(new IdentityItem("California", AuthenticatedState.AUTHENTICATED, true), "location");
		baseMap.merge(newMap);

		// verify the existing identityMap is unchanged
		Map<String, String> flattenedMap = IdentityTestUtil.flattenMap(baseMap.asXDMMap(false));
		assertEquals(3, flattenedMap.size());
		assertEquals("California", flattenedMap.get("identityMap.location[0].id"));
		assertEquals("authenticated", flattenedMap.get("identityMap.location[0].authenticatedState"));
		assertEquals("true", flattenedMap.get("identityMap.location[0].primary"));
	}

	@Test
	public void test_merge_EmptyIdentityMap() {
		// setup
		IdentityMap sampleUserMap = buildSampleIdentityMap(); // 2 items with namespace "Location", 3 items with namespace "Login"

		// test
		sampleUserMap.merge(new IdentityMap());

		// verify the existing identityMap is unchanged
		Map<String, List<IdentityItem>> castedMap = getCastedIdentityMap(sampleUserMap);
		assertEquals(2, castedMap.get("location").size());
		assertEquals(3, castedMap.get("login").size());
	}

	@Test
	public void test_merge_nullMap() {
		// setup
		IdentityMap sampleUserMap = buildSampleIdentityMap(); // 2 items with namespace "location", 3 items with namespace "login"

		// test
		sampleUserMap.merge(null);

		// verify the existing identityMap is unchanged
		Map<String, List<IdentityItem>> castedMap = getCastedIdentityMap(sampleUserMap);
		assertEquals(2, castedMap.get("location").size());
		assertEquals(3, castedMap.get("login").size());
	}

	@Test
	public void test_removeAllIdentityItemsForNamespace() {
		// setup
		IdentityMap sampleUserMap = buildSampleIdentityMap(); // 2 items with namespace "location", 3 items with namespace "login"

		// test
		sampleUserMap.clearItemsForNamespace("location");

		// verify
		Map<String, List<IdentityItem>> castedMap = getCastedIdentityMap(sampleUserMap);
		assertNull(castedMap.get("location"));
		assertEquals(3, castedMap.get("login").size());
	}

	@Test
	public void test_removeAllIdentityItemsForNamespace_InvalidNamespace() {
		// setup
		IdentityMap sampleUserMap = buildSampleIdentityMap(); // 2 items with namespace "location", 3 items with namespace "login"

		// test
		sampleUserMap.clearItemsForNamespace(null);
		sampleUserMap.clearItemsForNamespace("");

		// verify the existing identityMap is unchanged
		Map<String, List<IdentityItem>> castedMap = getCastedIdentityMap(sampleUserMap);
		assertEquals(2, castedMap.get("location").size());
		assertEquals(3, castedMap.get("login").size());
	}

	@Test
	public void test_removeAllIdentityItemsForNamespace_onEmptyMap() {
		// setup
		IdentityMap emptyMap = new IdentityMap();

		// test
		emptyMap.clearItemsForNamespace("location");
		assertTrue(emptyMap.asXDMMap(false).isEmpty());
	}

	@Test
	public void test_remove() {
		// setup
		IdentityMap sampleUserMap = buildSampleIdentityMap(); // 2 items with namespace "location", 3 items with namespace "login"
		IdentityMap tobeRemovedMap = new IdentityMap();
		tobeRemovedMap.addItem(new IdentityItem("Student"), "login");
		tobeRemovedMap.addItem(new IdentityItem("California"), "location");

		// test
		sampleUserMap.remove(tobeRemovedMap);

		// verify
		Map<String, List<IdentityItem>> castedMap = getCastedIdentityMap(sampleUserMap);
		assertEquals(1, castedMap.get("location").size());
		assertEquals(2, castedMap.get("login").size());
	}

	@Test
	public void test_remove_NullAndEmptyMap() {
		// setup
		IdentityMap sampleUserMap = buildSampleIdentityMap(); // 2 items with namespace "location", 3 items with namespace "login"

		// test
		sampleUserMap.remove(null);
		sampleUserMap.remove(new IdentityMap());

		// verify the existing identityMap is unchanged
		Map<String, List<IdentityItem>> castedMap = getCastedIdentityMap(sampleUserMap);
		assertEquals(2, castedMap.get("location").size());
		assertEquals(3, castedMap.get("login").size());
	}

	@Test
	public void test_remove_nonexistentNamespaceAndItems() {
		// setup
		IdentityMap sampleUserMap = buildSampleIdentityMap(); // 2 items with namespace "location", 3 items with namespace "login"
		IdentityMap tobeRemovedMap = new IdentityMap();
		tobeRemovedMap.addItem(new IdentityItem("California"), "nonexistentNamespace");
		tobeRemovedMap.addItem(new IdentityItem("nonexistentID"), "login");

		// test
		sampleUserMap.remove(tobeRemovedMap);

		// verify the existing identityMap is unchanged
		Map<String, List<IdentityItem>> castedMap = getCastedIdentityMap(sampleUserMap);
		assertEquals(2, castedMap.get("location").size());
		assertEquals(3, castedMap.get("login").size());
	}

	@Test
	public void test_FromData() throws Exception {
		// setup
		final String jsonStr =
			"{\n" +
			"      \"identityMap\": {\n" +
			"        \"ECID\": [\n" +
			"          {\n" +
			"            \"id\":randomECID,\n" +
			"            \"authenticatedState\": \"ambiguous\",\n" +
			"            \"primary\": true\n" +
			"          }\n" +
			"        ],\n" +
			"        \"USERID\": [\n" +
			"          {\n" +
			"            \"id\":someUserID,\n" +
			"            \"authenticatedState\": \"authenticated\",\n" +
			"            \"primary\": false\n" +
			"          }\n" +
			"        ]\n" +
			"      }\n" +
			"}";

		final JSONObject jsonObject = new JSONObject(jsonStr);
		final Map<String, Object> xdmData = JSONUtils.toMap(jsonObject);

		// test
		IdentityMap map = IdentityMap.fromXDMMap(xdmData);

		// verify
		Map<String, String> flattenedMap = IdentityTestUtil.flattenMap(map.asXDMMap(false));
		assertEquals("randomECID", flattenedMap.get("identityMap.ECID[0].id"));
		assertEquals("ambiguous", flattenedMap.get("identityMap.ECID[0].authenticatedState"));
		assertEquals("true", flattenedMap.get("identityMap.ECID[0].primary"));
		assertEquals("someUserID", flattenedMap.get("identityMap.USERID[0].id"));
		assertEquals("authenticated", flattenedMap.get("identityMap.USERID[0].authenticatedState"));
		assertEquals("false", flattenedMap.get("identityMap.USERID[0].primary"));
	}

	@Test
	public void test_FromData_removesItem_emptyId() throws Exception {
		// setup
		final String jsonStr =
			"{\n" +
			"      \"identityMap\": {\n" +
			"        \"ECID\": [\n" +
			"          {\n" +
			"            \"id\":randomECID,\n" +
			"            \"authenticatedState\": \"ambiguous\",\n" +
			"            \"primary\": true\n" +
			"          }\n" +
			"        ],\n" +
			"        \"USERID\": [\n" +
			"          {\n" +
			"            \"id\":\"\",\n" +
			"            \"authenticatedState\": \"authenticated\",\n" +
			"            \"primary\": false\n" +
			"          }\n" +
			"        ]\n" +
			"      }\n" +
			"}";

		final JSONObject jsonObject = new JSONObject(jsonStr);
		final Map<String, Object> xdmData = JSONUtils.toMap(jsonObject);

		// test
		IdentityMap map = IdentityMap.fromXDMMap(xdmData);

		// verify
		Map<String, String> flattenedMap = IdentityTestUtil.flattenMap(map.asXDMMap(false));
		assertEquals("randomECID", flattenedMap.get("identityMap.ECID[0].id"));
		assertEquals("ambiguous", flattenedMap.get("identityMap.ECID[0].authenticatedState"));
		assertEquals("true", flattenedMap.get("identityMap.ECID[0].primary"));
		assertNull(flattenedMap.get("identityMap.USERID[0].id"));
		assertNull(flattenedMap.get("identityMap.USERID[0].authenticatedState"));
		assertNull(flattenedMap.get("identityMap.USERID[0].primary"));
	}

	@Test
	public void test_FromData_removesItem_missingId() throws Exception {
		// setup
		final String jsonStr =
			"{\n" +
			"      \"identityMap\": {\n" +
			"        \"ECID\": [\n" +
			"          {\n" +
			"            \"id\":randomECID,\n" +
			"            \"authenticatedState\": \"ambiguous\",\n" +
			"            \"primary\": true\n" +
			"          }\n" +
			"        ],\n" +
			"        \"USERID\": [\n" +
			"          {\n" +
			"            \"authenticatedState\": \"authenticated\",\n" +
			"            \"primary\": false\n" +
			"          }\n" +
			"        ]\n" +
			"      }\n" +
			"}";

		final JSONObject jsonObject = new JSONObject(jsonStr);
		final Map<String, Object> xdmData = JSONUtils.toMap(jsonObject);

		// test
		IdentityMap map = IdentityMap.fromXDMMap(xdmData);

		// verify
		Map<String, String> flattenedMap = IdentityTestUtil.flattenMap(map.asXDMMap(false));
		assertEquals("randomECID", flattenedMap.get("identityMap.ECID[0].id"));
		assertEquals("ambiguous", flattenedMap.get("identityMap.ECID[0].authenticatedState"));
		assertEquals("true", flattenedMap.get("identityMap.ECID[0].primary"));
		assertNull(flattenedMap.get("identityMap.USERID[0].id"));
		assertNull(flattenedMap.get("identityMap.USERID[0].authenticatedState"));
		assertNull(flattenedMap.get("identityMap.USERID[0].primary"));
	}

	@Test
	public void testFromXDMMap_NullAndEmptyData() {
		assertNull(IdentityMap.fromXDMMap(null));
		assertNull(IdentityMap.fromXDMMap(new HashMap<String, Object>()));
	}

	@Test
	public void testFromXDMMap_InvalidNamespace() throws Exception {
		// setup
		// ECID namespace is map instead of list
		final String invalidJsonStr =
			"{\n" +
			"  \"identityMap\": {\n" +
			"    \"ECID\": {\n" +
			"        \"id\": \"randomECID\",\n" +
			"        \"authenticatedState\": \"ambiguous\",\n" +
			"        \"primary\": true\n" +
			"    }\n" +
			"  }\n" +
			"}";

		final JSONObject jsonObject = new JSONObject(invalidJsonStr);
		final Map<String, Object> xdmData = JSONUtils.toMap(jsonObject);

		// test
		IdentityMap map = IdentityMap.fromXDMMap(xdmData);

		// verify
		assertTrue(map.isEmpty());
	}

	@Test
	public void testFromXDMMap_InvalidItemForNamespace() throws Exception {
		// setup
		// namespace is an array of arrays instead of an array of identity items
		final String invalidJsonStr =
			"{\n" +
			"  \"identityMap\": {\n" +
			"    \"ECID\": [{\n" +
			"        \"id\": \"randomECID\",\n" +
			"        \"authenticatedState\": \"ambiguous\",\n" +
			"        \"primary\": true\n" +
			"    }],\n" +
			"    \"namespace\": [\n" +
			"       [ \"arrayInsteadOfMap\", \"invalid\"]\n" +
			"    ]\n" +
			"  }\n," +
			"}";

		final JSONObject jsonObject = new JSONObject(invalidJsonStr);
		final Map<String, Object> xdmData = JSONUtils.toMap(jsonObject);

		// test
		IdentityMap map = IdentityMap.fromXDMMap(xdmData);

		// verify
		// only ECID namespace is correct, namespace should be dropped due to invalid format
		assertEquals(1, map.getNamespaces().size());
		assertEquals("ECID", map.getNamespaces().get(0));
	}

	@Test
	public void testFromXDMMap_InvalidIdentityMap() throws Exception {
		// setup
		final String invalidJsonStr = "{\"identityMap\": [\"not a map\"]}";

		final JSONObject jsonObject = new JSONObject(invalidJsonStr);
		final Map<String, Object> xdmData = JSONUtils.toMap(jsonObject);

		// test
		IdentityMap map = IdentityMap.fromXDMMap(xdmData);

		// verify
		assertNull(map);
	}

	@Test
	public void testAsXDMMap_AllowEmptyFalse() {
		IdentityMap map = new IdentityMap();
		Map xdmMap = map.asXDMMap(false);
		assertTrue(xdmMap.isEmpty());
	}

	@Test
	public void testAsXDMMap_AllowEmptyTrue() {
		IdentityMap map = new IdentityMap();
		Map xdmMap = map.asXDMMap(true);

		// verify that the base xdm key identityMap is present
		assertEquals(1, xdmMap.size());
		assertEquals(new HashMap<>(), xdmMap.get(IdentityConstants.XDMKeys.IDENTITY_MAP));
	}

	private Map<String, List<IdentityItem>> getCastedIdentityMap(final IdentityMap map) {
		final Map<String, Object> xdmMap = map.asXDMMap(false);
		return (Map<String, List<IdentityItem>>) xdmMap.get(IdentityConstants.XDMKeys.IDENTITY_MAP);
	}

	private IdentityMap buildSampleIdentityMap() {
		// User Login Identity Items
		IdentityItem email = new IdentityItem("john@doe", AuthenticatedState.AUTHENTICATED, true);
		IdentityItem userName = new IdentityItem("John Doe", AuthenticatedState.AUTHENTICATED, false);
		IdentityItem accountType = new IdentityItem("Student", AuthenticatedState.AUTHENTICATED, false);

		// User Location Address Identity Items
		IdentityItem street = new IdentityItem("280 Highway Lane", AuthenticatedState.AMBIGUOUS, false);
		IdentityItem state = new IdentityItem("California", AuthenticatedState.AMBIGUOUS, false);

		IdentityMap adobeIdentityMap = new IdentityMap();
		adobeIdentityMap.addItem(email, "login");
		adobeIdentityMap.addItem(userName, "login");
		adobeIdentityMap.addItem(accountType, "login");

		adobeIdentityMap.addItem(street, "location");
		adobeIdentityMap.addItem(state, "location");
		return adobeIdentityMap;
	}
}
