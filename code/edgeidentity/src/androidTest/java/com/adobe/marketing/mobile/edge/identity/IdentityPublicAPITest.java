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

import static com.adobe.marketing.mobile.edge.identity.util.IdentityFunctionalTestUtil.createIdentityMap;
import static com.adobe.marketing.mobile.edge.identity.util.IdentityFunctionalTestUtil.getExperienceCloudIdSync;
import static com.adobe.marketing.mobile.edge.identity.util.IdentityFunctionalTestUtil.getIdentitiesSync;
import static com.adobe.marketing.mobile.edge.identity.util.IdentityFunctionalTestUtil.getUrlVariablesSync;
import static com.adobe.marketing.mobile.edge.identity.util.IdentityFunctionalTestUtil.setupConfiguration;
import static com.adobe.marketing.mobile.util.NodeConfig.Scope.Subtree;
import static com.adobe.marketing.mobile.util.TestHelper.SetupCoreRule;
import static com.adobe.marketing.mobile.util.TestHelper.getDispatchedEventsWith;
import static com.adobe.marketing.mobile.util.TestHelper.getSharedStateFor;
import static com.adobe.marketing.mobile.util.TestHelper.getXDMSharedStateFor;
import static com.adobe.marketing.mobile.util.TestHelper.registerExtensions;
import static com.adobe.marketing.mobile.util.TestHelper.resetTestExpectations;
import static com.adobe.marketing.mobile.util.TestHelper.waitForThreads;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.edge.identity.util.IdentityTestConstants;
import com.adobe.marketing.mobile.util.ElementCount;
import com.adobe.marketing.mobile.util.JSONAsserts;
import com.adobe.marketing.mobile.util.MonitorExtension;
import com.adobe.marketing.mobile.util.TestPersistenceHelper;
import com.adobe.marketing.mobile.util.ValueNotEqual;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class IdentityPublicAPITest {

	@Rule
	public TestRule rule = new SetupCoreRule();

	// --------------------------------------------------------------------------------------------
	// Tests for GetExtensionVersion API
	// --------------------------------------------------------------------------------------------

	@Test
	public void testGetExtensionVersionAPI() {
		assertEquals(IdentityConstants.EXTENSION_VERSION, Identity.extensionVersion());
	}

	// --------------------------------------------------------------------------------------------
	// Tests for Register extension API
	// --------------------------------------------------------------------------------------------
	@Test
	public void testRegisterExtension_withClass() throws InterruptedException {
		// test
		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Identity.EXTENSION), null);

		// verify that the extension is registered with the correct version details
		Map<String, Object> sharedStateMap = getSharedStateFor(IdentityTestConstants.SharedStateName.EVENT_HUB, 5000);

		String expected =
			"{ " +
			"  \"extensions\": { " +
			"    \"com.adobe.edge.identity\": { " +
			"      \"version\": \"" +
			IdentityConstants.EXTENSION_VERSION +
			"\" " +
			"    } " +
			"  } " +
			"}";

		JSONAsserts.assertExactMatch(expected, sharedStateMap);
	}

	// --------------------------------------------------------------------------------------------
	// Tests for UpdateIdentities API
	// --------------------------------------------------------------------------------------------

	@Test
	public void testUpdateIdentitiesAPI() throws Exception {
		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Identity.EXTENSION), null);

		// test
		Identity.updateIdentities(
			createIdentityMap("Email", "example@email.com", AuthenticatedState.AUTHENTICATED, true)
		);
		waitForThreads(2000);

		// verify xdm shared state
		Map<String, Object> xdmSharedState = getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000);

		String expected =
			"{" +
			"  \"identityMap\": {" +
			"    \"ECID\": [" +
			"      {" +
			"        \"authenticatedState\": \"ambiguous\"" +
			"      }" +
			"    ]," +
			"    \"Email\": [" +
			"      {" +
			"        \"id\": \"example@email.com\"," +
			"        \"primary\": true" +
			"      }" +
			"    ]" +
			"  }" +
			"}";

		JSONAsserts.assertExactMatch(
			expected,
			xdmSharedState,
			new ElementCount(6, Subtree) // 3 for ECID and 3 for Email
		);

		// verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);

		JSONAsserts.assertExactMatch(
			expected,
			persistedJson,
			new ElementCount(6, Subtree) // 3 for ECID and 3 for Email
		);
	}

	@Test
	public void testUpdateAPI_nullData() throws Exception {
		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Identity.EXTENSION), null);
		resetTestExpectations();
		// test
		Identity.updateIdentities(null);
		waitForThreads(2000);

		// verify no shares state change event dispatched
		List<Event> dispatchedEvents = getDispatchedEventsWith(EventType.HUB, EventSource.SHARED_STATE);
		assertEquals(0, dispatchedEvents.size());

		// verify xdm shared state is not disturbed
		Map<String, Object> xdmSharedState = getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000);

		String expected =
			"{" +
			"  \"identityMap\": {" +
			"    \"ECID\": [" +
			"    {" +
			"        \"id\": \"STRING_TYPE\"" +
			"      }" +
			"    ]" +
			"  }" +
			"}";

		JSONAsserts.assertTypeMatch(
			expected,
			xdmSharedState,
			new ElementCount(3, Subtree) // 3 for ECID still exists
		);
	}

	@Test
	public void testUpdateAPI_emptyData() throws Exception {
		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Identity.EXTENSION), null);
		resetTestExpectations();

		// test
		Identity.updateIdentities(new IdentityMap());
		waitForThreads(2000);

		// verify no shares state change event dispatched
		List<Event> dispatchedEvents = getDispatchedEventsWith(EventType.HUB, EventSource.SHARED_STATE);
		assertEquals(0, dispatchedEvents.size());

		// verify xdm shared state is not disturbed
		Map<String, Object> xdmSharedState = getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000);

		String expected =
			"{" +
			"  \"identityMap\": {" +
			"    \"ECID\": [" +
			"    {" +
			"        \"id\": \"STRING_TYPE\"" +
			"      }" +
			"    ]" +
			"  }" +
			"}";

		JSONAsserts.assertTypeMatch(
			expected,
			xdmSharedState,
			new ElementCount(3, Subtree) // 3 for ECID still exists
		);
	}

	@Test
	public void testUpdateAPI_shouldReplaceExistingIdentities() throws Exception {
		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Identity.EXTENSION), null);

		// test
		Identity.updateIdentities(createIdentityMap("Email", "example@email.com"));
		Identity.updateIdentities(
			createIdentityMap("Email", "example@email.com", AuthenticatedState.AUTHENTICATED, true)
		);
		Identity.updateIdentities(
			createIdentityMap("Email", "example@email.com", AuthenticatedState.LOGGED_OUT, false)
		);
		waitForThreads(2000);

		Map<String, Object> xdmSharedState = getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000);

		// verify the final xdm shared state
		String expected =
			"{" +
			"  \"identityMap\": {" +
			"    \"Email\": [" +
			"      {" +
			"        \"authenticatedState\": \"loggedOut\"," +
			"        \"id\": \"example@email.com\"," +
			"        \"primary\": false" +
			"      }" +
			"    ]" +
			"  }" +
			"}";

		JSONAsserts.assertExactMatch(
			expected,
			xdmSharedState,
			new ElementCount(6, Subtree) // 3 for ECID and 3 for Email
		);

		// verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);

		JSONAsserts.assertExactMatch(
			expected,
			persistedJson,
			new ElementCount(6, Subtree) // 3 for ECID and 3 for Email
		);
	}

	@Test
	public void testUpdateAPI_withReservedNamespaces() throws Exception {
		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Identity.EXTENSION), null);

		// test
		Identity.updateIdentities(createIdentityMap("ECID", "newECID"));
		Identity.updateIdentities(createIdentityMap("GAID", "<gaid>"));
		Identity.updateIdentities(createIdentityMap("IDFA", "<idfa>"));
		Identity.updateIdentities(createIdentityMap("IDFa", "<newIdfa>"));
		Identity.updateIdentities(createIdentityMap("gaid", "<newgaid>"));
		Identity.updateIdentities(createIdentityMap("ecid", "<newecid>"));
		Identity.updateIdentities(createIdentityMap("idfa", "<newidfa>"));
		waitForThreads(2000);

		// verify xdm shared state does not get updated
		Map<String, Object> xdmSharedState = getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000);

		String expected =
			"{" +
			"  \"identityMap\": {" +
			"    \"ECID\": [" +
			"      {" +
			"        \"id\": \"newECID\"" +
			"      }" +
			"    ]" +
			"  }" +
			"}";

		JSONAsserts.assertExactMatch(
			expected,
			xdmSharedState,
			new ElementCount(3, Subtree), // 3 for ECID
			new ValueNotEqual("identityMap.ECID[0].id") // ECID doesn't get replaced by API
		);

		// verify persisted data doesn't change
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);

		JSONAsserts.assertExactMatch(
			expected,
			persistedJson,
			new ElementCount(3, Subtree), // 3 for ECID
			new ValueNotEqual("identityMap.ECID[0].id") // ECID doesn't get replaced by API
		);
	}

	@Test
	public void testUpdateAPI_multipleNamespaceMap() throws Exception {
		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Identity.EXTENSION), null);

		// test
		IdentityMap map = new IdentityMap();
		map.addItem(new IdentityItem("primary@email.com"), "Email");
		map.addItem(new IdentityItem("secondary@email.com"), "Email");
		map.addItem(new IdentityItem("zzzyyyxxx"), "UserId");
		map.addItem(new IdentityItem("John Doe"), "UserName");
		Identity.updateIdentities(map);
		waitForThreads(2000);

		// verify xdm shared state

		Map<String, Object> xdmSharedState = getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000);

		String expected =
			"{" +
			"  \"identityMap\": {" +
			"    \"Email\": [" +
			"      {" +
			"        \"id\": \"primary@email.com\"" +
			"      }," +
			"      {" +
			"        \"id\": \"secondary@email.com\"" +
			"      }" +
			"    ]," +
			"    \"UserId\": [" +
			"      {" +
			"        \"id\": \"zzzyyyxxx\"" +
			"      }" +
			"    ]," +
			"    \"UserName\": [" +
			"      {" +
			"        \"id\": \"John Doe\"" +
			"      }" +
			"    ]" +
			"  }" +
			"}";

		JSONAsserts.assertExactMatch(
			expected,
			xdmSharedState,
			new ElementCount(15, Subtree) // 3 for ECID + 12 for new identities
		);

		// verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);

		JSONAsserts.assertExactMatch(
			expected,
			persistedJson,
			new ElementCount(15, Subtree) // 3 for ECID + 12 for new identities
		);
	}

	@Test
	public void testUpdateAPI_caseSensitiveNamespacesForCustomIdentifiers() throws Exception {
		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Identity.EXTENSION), null);

		// test
		IdentityMap map = new IdentityMap();
		map.addItem(new IdentityItem("primary@email.com"), "Email");
		map.addItem(new IdentityItem("secondary@email.com"), "email");
		Identity.updateIdentities(map);
		waitForThreads(2000);

		// verify xdm shared state
		Map<String, Object> xdmSharedState = getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000);

		String expected =
			"{" +
			"  \"identityMap\": {" +
			"    \"Email\": [" +
			"      {" +
			"        \"id\": \"primary@email.com\"" +
			"      }" +
			"    ]," +
			"    \"email\": [" +
			"      {" +
			"        \"id\": \"secondary@email.com\"" +
			"      }" +
			"    ]" +
			"  }" +
			"}";

		JSONAsserts.assertExactMatch(
			expected,
			xdmSharedState,
			new ElementCount(9, Subtree) // 3 for ECID + 6 for new identities
		);

		// verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);

		JSONAsserts.assertExactMatch(
			expected,
			persistedJson,
			new ElementCount(9, Subtree) // 3 for ECID + 6 for new identities
		);
	}

	// --------------------------------------------------------------------------------------------
	// Tests for getExperienceCloudId API
	// --------------------------------------------------------------------------------------------

	@Test
	public void testGetECID() {
		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Identity.EXTENSION), null);

		// test
		String ecid = getExperienceCloudIdSync();

		// returns an ecid string
		assertNotNull(ecid); // verify that ecid is always generated and returned
	}

	@Test
	public void testGetExperienceCloudId_nullCallback() {
		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Identity.EXTENSION), null);

		// test
		try {
			Identity.getExperienceCloudId(null); // should not crash
		} catch (Exception e) {
			fail();
		}
	}

	// --------------------------------------------------------------------------------------------
	// Tests for getUrlVariables API
	// --------------------------------------------------------------------------------------------

	@Test
	public void testGetUrlVariables() throws Exception {
		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Identity.EXTENSION), null);

		// test
		setupConfiguration();
		String urlVariables = getUrlVariablesSync();

		assertNotNull(urlVariables);
	}

	@Test
	public void testGetUrlVariables_nullCallback() {
		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Identity.EXTENSION), null);

		// test
		try {
			Identity.getUrlVariables(null); // should not crash
		} catch (Exception e) {
			fail();
		}
	}

	// --------------------------------------------------------------------------------------------
	// Tests for getIdentities API
	// --------------------------------------------------------------------------------------------

	@Test
	public void testGetIdentities() {
		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Identity.EXTENSION), null);

		// setup
		// update Identities through API
		IdentityMap map = new IdentityMap();
		map.addItem(new IdentityItem("primary@email.com"), "Email");
		map.addItem(new IdentityItem("secondary@email.com"), "Email");
		map.addItem(new IdentityItem("zzzyyyxxx"), "UserId");
		map.addItem(new IdentityItem("John Doe"), "UserName");
		Identity.updateIdentities(map);

		// test
		Map<String, Object> getIdentitiesResponse = getIdentitiesSync();
		waitForThreads(2000);

		// verify
		IdentityMap responseMap = (IdentityMap) getIdentitiesResponse.get(
			IdentityTestConstants.GetIdentitiesHelper.VALUE
		);
		assertEquals(4, responseMap.getNamespaces().size());
		assertEquals(2, responseMap.getIdentityItemsForNamespace("Email").size());
		assertEquals(1, responseMap.getIdentityItemsForNamespace("UserId").size());
		assertEquals(1, responseMap.getIdentityItemsForNamespace("UserName").size());
		assertEquals(1, responseMap.getIdentityItemsForNamespace("ECID").size());
	}

	@Test
	public void testGetIdentities_itemWithEmptyId_notAddedToMap() {
		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Identity.EXTENSION), null);

		// setup
		// update Identities through API
		IdentityMap map = new IdentityMap();
		map.addItem(new IdentityItem("primary@email.com"), "Email");
		map.addItem(new IdentityItem("secondary@email.com"), "Email");
		map.addItem(new IdentityItem("zzzyyyxxx"), "UserId");
		map.addItem(new IdentityItem(""), "UserId");
		map.addItem(new IdentityItem("John Doe"), "UserName");
		map.addItem(new IdentityItem(""), "EmptyNamespace");
		Identity.updateIdentities(map);

		// test
		Map<String, Object> getIdentitiesResponse = getIdentitiesSync();
		waitForThreads(2000);

		// verify
		IdentityMap responseMap = (IdentityMap) getIdentitiesResponse.get(
			IdentityTestConstants.GetIdentitiesHelper.VALUE
		);

		assertEquals(4, responseMap.getNamespaces().size());
		assertEquals(2, responseMap.getIdentityItemsForNamespace("Email").size());
		assertEquals(1, responseMap.getIdentityItemsForNamespace("UserId").size());
		assertEquals(1, responseMap.getIdentityItemsForNamespace("UserName").size());
		assertEquals(1, responseMap.getIdentityItemsForNamespace("ECID").size());
	}

	@Test
	public void testGetIdentities_nullCallback() {
		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Identity.EXTENSION), null);

		// test
		try {
			Identity.getIdentities(null); // should not crash
		} catch (Exception e) {
			fail();
		}
	}

	// --------------------------------------------------------------------------------------------
	// Tests for RemoveIdentity API
	// --------------------------------------------------------------------------------------------

	@Test
	public void testRemoveIdentity() throws Exception {
		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Identity.EXTENSION), null);

		// setup
		// update Identities through API
		IdentityMap map = new IdentityMap();
		map.addItem(new IdentityItem("primary@email.com"), "Email");
		map.addItem(new IdentityItem("secondary@email.com"), "Email");
		Identity.updateIdentities(map);

		// test
		Identity.removeIdentity(new IdentityItem("primary@email.com"), "Email");
		waitForThreads(2000);

		// verify xdm shared state
		Map<String, Object> xdmSharedState = getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000);

		String expected =
			"{" +
			"  \"identityMap\": {" +
			"    \"Email\": [" +
			"      {" +
			"        \"id\": \"secondary@email.com\"" +
			"      }" +
			"    ]" +
			"  }" +
			"}";

		JSONAsserts.assertTypeMatch(
			expected,
			xdmSharedState,
			new ElementCount(6, Subtree) // 3 for ECID + 3 for Email secondary
		);

		// test again
		Identity.removeIdentity(new IdentityItem("secondary@email.com"), "Email");
		waitForThreads(2000);

		// verify xdm shared state
		xdmSharedState = getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000);

		// 3 for ECID
		JSONAsserts.assertExactMatch("{}", xdmSharedState, new ElementCount(3, Subtree));

		// verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);

		// 3 for ECID
		JSONAsserts.assertExactMatch("{}", persistedJson, new ElementCount(3, Subtree));
	}

	@Test
	public void testRemoveIdentity_nonExistentNamespace() throws Exception {
		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Identity.EXTENSION), null);

		// test
		Identity.removeIdentity(new IdentityItem("primary@email.com"), "Email");
		waitForThreads(2000);

		// verify item is not removed
		// verify xdm shared state
		Map<String, Object> xdmSharedState = getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000);
		// 3 for ECID
		JSONAsserts.assertExactMatch("{}", xdmSharedState, new ElementCount(3, Subtree));

		// verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);

		// 3 for ECID
		JSONAsserts.assertExactMatch("{}", persistedJson, new ElementCount(3, Subtree));
	}

	@Test
	public void testRemoveIdentity_nameSpaceCaseSensitive() throws Exception {
		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Identity.EXTENSION), null);

		// setup
		// update Identities through API
		Identity.updateIdentities(createIdentityMap("Email", "example@email.com"));

		// test
		Identity.removeIdentity(new IdentityItem("example@email.com"), "email");
		waitForThreads(2000);

		// verify item is not removed
		// verify xdm shared state
		Map<String, Object> xdmSharedState = getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000);

		// 3 for ECID + 3 for Email
		JSONAsserts.assertExactMatch("{}", xdmSharedState, new ElementCount(6, Subtree));

		// verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);

		// 3 for ECID + 3 for Email
		JSONAsserts.assertExactMatch("{}", persistedJson, new ElementCount(6, Subtree));
	}

	@Test
	public void testRemoveIdentity_nonExistentItem() throws Exception {
		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Identity.EXTENSION), null);

		// setup
		// update Identities through API
		Identity.updateIdentities(createIdentityMap("Email", "example@email.com"));

		// test
		Identity.removeIdentity(new IdentityItem("secondary@email.com"), "Email");
		waitForThreads(2000);

		// verify item is not removed
		// verify xdm shared state
		Map<String, Object> xdmSharedState = getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000);

		// 3 for ECID + 3 for Email
		JSONAsserts.assertExactMatch("{}", xdmSharedState, new ElementCount(6, Subtree));

		// verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);

		// 3 for ECID + 3 for Email
		JSONAsserts.assertExactMatch("{}", persistedJson, new ElementCount(6, Subtree));
	}

	@Test
	public void testRemoveIdentity_doesNotRemoveECID() throws Exception {
		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Identity.EXTENSION), null);

		// test
		String currentECID = getExperienceCloudIdSync();

		// attempt to remove ECID
		Identity.removeIdentity(new IdentityItem(currentECID), "ECID");
		waitForThreads(2000);

		// ECID is a reserved namespace and should not be removed
		// verify xdm shared state
		Map<String, Object> xdmSharedState = getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000);

		// 3 for ECID that still exists
		JSONAsserts.assertExactMatch("{}", xdmSharedState, new ElementCount(3, Subtree));

		// verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);

		// 3 for ECID that still exists
		JSONAsserts.assertExactMatch("{}", persistedJson, new ElementCount(3, Subtree));
	}
}
