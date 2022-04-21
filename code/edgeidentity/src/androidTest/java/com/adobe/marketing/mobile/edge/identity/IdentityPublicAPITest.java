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

import static com.adobe.marketing.mobile.TestHelper.*;
import static com.adobe.marketing.mobile.edge.identity.IdentityFunctionalTestUtil.registerEdgeIdentityExtension;
import static com.adobe.marketing.mobile.edge.identity.IdentityFunctionalTestUtil.setupConfiguration;
import static com.adobe.marketing.mobile.edge.identity.IdentityTestUtil.*;
import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.TestHelper;
import com.adobe.marketing.mobile.TestPersistenceHelper;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class IdentityPublicAPITest {

	@Rule
	public RuleChain rule = RuleChain
		.outerRule(new TestHelper.SetupCoreRule())
		.around(new TestHelper.RegisterMonitorExtensionRule());

	// --------------------------------------------------------------------------------------------
	// Setup
	// --------------------------------------------------------------------------------------------

	@Before
	public void setup() throws Exception {
		registerEdgeIdentityExtension();
	}

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
	public void testRegisterExtensionAPI() throws InterruptedException {
		// test
		// Consent.registerExtension() is called in the setup method

		// verify that the extension is registered with the correct version details
		Map<String, String> sharedStateMap = flattenMap(
			getSharedStateFor(IdentityTestConstants.SharedStateName.EVENT_HUB, 1000)
		);
		assertEquals(
			IdentityConstants.EXTENSION_VERSION,
			sharedStateMap.get("extensions.com.adobe.edge.identity.version")
		);
	}

	// --------------------------------------------------------------------------------------------
	// Tests for UpdateIdentities API
	// --------------------------------------------------------------------------------------------

	@Test
	public void testUpdateIdentitiesAPI() throws Exception {
		// test
		Identity.updateIdentities(
			CreateIdentityMap("Email", "example@email.com", AuthenticatedState.AUTHENTICATED, true)
		);
		TestHelper.waitForThreads(2000);

		// verify xdm shared state
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertEquals(6, xdmSharedState.size()); // 3 for ECID and 3 for Email
		assertEquals("example@email.com", xdmSharedState.get("identityMap.Email[0].id"));
		assertEquals("true", xdmSharedState.get("identityMap.Email[0].primary"));
		assertEquals("ambiguous", xdmSharedState.get("identityMap.ECID[0].authenticatedState"));

		// verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);
		Map<String, String> persistedMap = flattenMap(IdentityTestUtil.toMap(new JSONObject(persistedJson)));
		assertEquals(6, persistedMap.size()); // 3 for ECID and 3 for Email
		assertEquals("example@email.com", persistedMap.get("identityMap.Email[0].id"));
		assertEquals("true", persistedMap.get("identityMap.Email[0].primary"));
		assertEquals("ambiguous", persistedMap.get("identityMap.ECID[0].authenticatedState"));
	}

	@Test
	public void testUpdateAPI_nullData() throws Exception {
		// test
		Identity.updateIdentities(null);
		TestHelper.waitForThreads(2000);

		// verify no shares state change event dispatched
		List<Event> dispatchedEvents = getDispatchedEventsWith(
			IdentityConstants.EventType.HUB,
			IdentityConstants.EventSource.SHARED_STATE
		);
		assertEquals(0, dispatchedEvents.size());

		// verify xdm shared state is not disturbed
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertEquals(3, xdmSharedState.size()); // 3 for ECID still exists
		assertNotNull(xdmSharedState.get("identityMap.ECID[0].id"));
	}

	@Test
	public void testUpdateAPI_emptyData() throws Exception {
		// test
		Identity.updateIdentities(new IdentityMap());
		TestHelper.waitForThreads(2000);

		// verify no shares state change event dispatched
		List<Event> dispatchedEvents = getDispatchedEventsWith(
			IdentityConstants.EventType.HUB,
			IdentityConstants.EventSource.SHARED_STATE
		);
		assertEquals(0, dispatchedEvents.size());

		// verify xdm shared state is not disturbed
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertEquals(3, xdmSharedState.size()); // 3 for ECID still exists
		assertNotNull(xdmSharedState.get("identityMap.ECID[0].id"));
	}

	@Test
	public void testUpdateAPI_shouldReplaceExistingIdentities() throws Exception {
		// test
		Identity.updateIdentities(CreateIdentityMap("Email", "example@email.com"));
		Identity.updateIdentities(
			CreateIdentityMap("Email", "example@email.com", AuthenticatedState.AUTHENTICATED, true)
		);
		Identity.updateIdentities(
			CreateIdentityMap("Email", "example@email.com", AuthenticatedState.LOGGED_OUT, false)
		);
		TestHelper.waitForThreads(2000);

		// verify the final xdm shared state
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertEquals(6, xdmSharedState.size()); // 3 for ECID and 3 for Email
		assertEquals("example@email.com", xdmSharedState.get("identityMap.Email[0].id"));
		assertEquals("false", xdmSharedState.get("identityMap.Email[0].primary"));
		assertEquals("loggedOut", xdmSharedState.get("identityMap.Email[0].authenticatedState"));

		// verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);
		Map<String, String> persistedMap = flattenMap(IdentityTestUtil.toMap(new JSONObject(persistedJson)));
		assertEquals(6, persistedMap.size()); // 3 for ECID and 3 for Email
		assertEquals("example@email.com", persistedMap.get("identityMap.Email[0].id"));
		assertEquals("false", persistedMap.get("identityMap.Email[0].primary"));
		assertEquals("loggedOut", persistedMap.get("identityMap.Email[0].authenticatedState"));
	}

	@Test
	public void testUpdateAPI_withReservedNamespaces() throws Exception {
		// test
		Identity.updateIdentities(CreateIdentityMap("ECID", "newECID"));
		Identity.updateIdentities(CreateIdentityMap("GAID", "<gaid>"));
		Identity.updateIdentities(CreateIdentityMap("IDFA", "<idfa>"));
		Identity.updateIdentities(CreateIdentityMap("IDFa", "<newIdfa>"));
		Identity.updateIdentities(CreateIdentityMap("gaid", "<newgaid>"));
		Identity.updateIdentities(CreateIdentityMap("ecid", "<newecid>"));
		Identity.updateIdentities(CreateIdentityMap("idfa", "<newidfa>"));
		TestHelper.waitForThreads(2000);

		// verify xdm shared state does not get updated
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertEquals(3, xdmSharedState.size()); // 3 for ECID
		assertNotEquals("newECID", xdmSharedState.get("identityMap.ECID[0].id")); // ECID doesn't get replaced by API

		// verify persisted data doesn't change
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);
		Map<String, String> persistedMap = flattenMap(IdentityTestUtil.toMap(new JSONObject(persistedJson)));
		assertEquals(3, persistedMap.size()); // 3 for ECID
		assertNotEquals("newECID", persistedMap.get("identityMap.ECID[0].id")); // ECID doesn't get replaced by API
	}

	@Test
	public void testUpdateAPI_multipleNamespaceMap() throws Exception {
		// test
		IdentityMap map = new IdentityMap();
		map.addItem(new IdentityItem("primary@email.com"), "Email");
		map.addItem(new IdentityItem("secondary@email.com"), "Email");
		map.addItem(new IdentityItem("zzzyyyxxx"), "UserId");
		map.addItem(new IdentityItem("John Doe"), "UserName");
		Identity.updateIdentities(map);
		TestHelper.waitForThreads(2000);

		// verify xdm shared state
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertEquals(15, xdmSharedState.size()); // 3 for ECID + 12 for new identities
		assertEquals("primary@email.com", xdmSharedState.get("identityMap.Email[0].id"));
		assertEquals("secondary@email.com", xdmSharedState.get("identityMap.Email[1].id"));
		assertEquals("zzzyyyxxx", xdmSharedState.get("identityMap.UserId[0].id"));
		assertEquals("John Doe", xdmSharedState.get("identityMap.UserName[0].id"));

		// verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);
		Map<String, String> persistedMap = flattenMap(IdentityTestUtil.toMap(new JSONObject(persistedJson)));
		assertEquals(15, persistedMap.size()); // 3 for ECID + 12 for new identities
		assertEquals("primary@email.com", persistedMap.get("identityMap.Email[0].id"));
		assertEquals("secondary@email.com", persistedMap.get("identityMap.Email[1].id"));
		assertEquals("zzzyyyxxx", persistedMap.get("identityMap.UserId[0].id"));
		assertEquals("John Doe", persistedMap.get("identityMap.UserName[0].id"));
	}

	@Test
	public void testUpdateAPI_caseSensitiveNamespacesForCustomIdentifiers() throws Exception {
		// test
		IdentityMap map = new IdentityMap();
		map.addItem(new IdentityItem("primary@email.com"), "Email");
		map.addItem(new IdentityItem("secondary@email.com"), "email");
		Identity.updateIdentities(map);
		TestHelper.waitForThreads(2000);

		// verify xdm shared state
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertEquals(9, xdmSharedState.size()); // 3 for ECID + 6 for new identities
		assertEquals("primary@email.com", xdmSharedState.get("identityMap.Email[0].id"));
		assertEquals("secondary@email.com", xdmSharedState.get("identityMap.email[0].id"));

		// verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);
		Map<String, String> persistedMap = flattenMap(IdentityTestUtil.toMap(new JSONObject(persistedJson)));
		assertEquals(9, persistedMap.size()); // 3 for ECID + 6 for new identities
		assertEquals("primary@email.com", persistedMap.get("identityMap.Email[0].id"));
		assertEquals("secondary@email.com", persistedMap.get("identityMap.email[0].id"));
	}

	// --------------------------------------------------------------------------------------------
	// Tests for getExperienceCloudId API
	// --------------------------------------------------------------------------------------------

	@Test
	public void testGetECID() {
		// test
		String ecid = getExperienceCloudIdSync();

		// returns an ecid string
		assertNotNull(ecid); // verify that ecid is always generated and returned
	}

	@Test
	public void testGetExperienceCloudId_nullCallback() {
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
		// test
		setupConfiguration();
		String urlVariables = getUrlVariablesSync();

		assertNotNull(urlVariables);
	}

	@Test
	public void testGetUrlVariables_nullCallback() {
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
		TestHelper.waitForThreads(2000);

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
		// setup
		// update Identities through API
		IdentityMap map = new IdentityMap();
		map.addItem(new IdentityItem("primary@email.com"), "Email");
		map.addItem(new IdentityItem("secondary@email.com"), "Email");
		Identity.updateIdentities(map);

		// test
		Identity.removeIdentity(new IdentityItem("primary@email.com"), "Email");
		TestHelper.waitForThreads(2000);

		// verify xdm shared state
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertEquals(6, xdmSharedState.size()); // 3 for ECID + 3 for Email secondary
		assertEquals("secondary@email.com", xdmSharedState.get("identityMap.Email[0].id"));

		// test again
		Identity.removeIdentity(new IdentityItem("secondary@email.com"), "Email");
		TestHelper.waitForThreads(2000);

		// verify xdm shared state
		xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertEquals(3, xdmSharedState.size()); // 3 for ECID

		// verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);
		Map<String, String> persistedMap = flattenMap(IdentityTestUtil.toMap(new JSONObject(persistedJson)));
		assertEquals(3, persistedMap.size()); // 3 for ECID
	}

	@Test
	public void testRemoveIdentity_nonExistentNamespace() throws Exception {
		// test
		Identity.removeIdentity(new IdentityItem("primary@email.com"), "Email");
		TestHelper.waitForThreads(2000);

		// verify item is not removed
		// verify xdm shared state
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertEquals(3, xdmSharedState.size()); // 3 for ECID

		// verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);
		Map<String, String> persistedMap = flattenMap(IdentityTestUtil.toMap(new JSONObject(persistedJson)));
		assertEquals(3, persistedMap.size()); // 3 for ECID
	}

	@Test
	public void testRemoveIdentity_nameSpaceCaseSensitive() throws Exception {
		// setup
		// update Identities through API
		Identity.updateIdentities(CreateIdentityMap("Email", "example@email.com"));

		// test
		Identity.removeIdentity(new IdentityItem("example@email.com"), "email");
		TestHelper.waitForThreads(2000);

		// verify item is not removed
		// verify xdm shared state
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertEquals(6, xdmSharedState.size()); // 3 for ECID +  3 for  Email

		// verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);
		Map<String, String> persistedMap = flattenMap(IdentityTestUtil.toMap(new JSONObject(persistedJson)));
		assertEquals(6, persistedMap.size()); // 3 for ECID +  3 for  Email
	}

	@Test
	public void testRemoveIdentity_nonExistentItem() throws Exception {
		// setup
		// update Identities through API
		Identity.updateIdentities(CreateIdentityMap("Email", "example@email.com"));

		// test
		Identity.removeIdentity(new IdentityItem("secondary@email.com"), "Email");
		TestHelper.waitForThreads(2000);

		// verify item is not removed
		// verify xdm shared state
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertEquals(6, xdmSharedState.size()); // 3 for ECID +  3 for  Email

		// verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);
		Map<String, String> persistedMap = flattenMap(IdentityTestUtil.toMap(new JSONObject(persistedJson)));
		assertEquals(6, persistedMap.size()); // 3 for ECID +  3 for  Email
	}

	@Test
	public void testRemoveIdentity_doesNotRemoveECID() throws Exception {
		// test
		String currentECID = getExperienceCloudIdSync();

		// attempt to remove ECID
		Identity.removeIdentity(new IdentityItem(currentECID), "ECID");
		TestHelper.waitForThreads(2000);

		// ECID is a reserved namespace and should not be removed
		// verify xdm shared state
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertEquals(3, xdmSharedState.size()); // 3 for ECID that still exists

		// verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);
		Map<String, String> persistedMap = flattenMap(IdentityTestUtil.toMap(new JSONObject(persistedJson)));
		assertEquals(3, persistedMap.size()); // 3 for ECID that still exists
	}
}
