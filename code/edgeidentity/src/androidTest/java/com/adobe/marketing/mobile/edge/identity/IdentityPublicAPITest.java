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

import static com.adobe.marketing.mobile.edge.identity.util.IdentityFunctionalTestUtil.*;
import static com.adobe.marketing.mobile.edge.identity.util.TestHelper.*;
import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.edge.identity.util.IdentityTestConstants;
import com.adobe.marketing.mobile.edge.identity.util.MonitorExtension;
import com.adobe.marketing.mobile.edge.identity.util.TestPersistenceHelper;
import com.adobe.marketing.mobile.util.JSONUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
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
	public void testRegisterExtensionAPI() throws InterruptedException {
		// test
		//noinspection deprecation
		Identity.registerExtension();

		// now register monitor extension and start the hub
		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION), null);

		// verify that the extension is registered with the correct version details
		Map<String, String> sharedStateMap = flattenMap(
			getSharedStateFor(IdentityTestConstants.SharedStateName.EVENT_HUB, 5000)
		);
		assertEquals(
			IdentityConstants.EXTENSION_VERSION,
			sharedStateMap.get("extensions.com.adobe.edge.identity.version")
		);
	}

	@Test
	public void testRegisterExtension_withClass() throws InterruptedException {
		// test
		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Identity.EXTENSION), null);

		// verify that the extension is registered with the correct version details
		Map<String, String> sharedStateMap = flattenMap(
			getSharedStateFor(IdentityTestConstants.SharedStateName.EVENT_HUB, 5000)
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
		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Identity.EXTENSION), null);

		// test
		Identity.updateIdentities(
			createIdentityMap("Email", "example@email.com", AuthenticatedState.AUTHENTICATED, true)
		);
		waitForThreads(2000);

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
		Map<String, String> persistedMap = flattenMap(JSONUtils.toMap(new JSONObject(persistedJson)));
		assertEquals(6, persistedMap.size()); // 3 for ECID and 3 for Email
		assertEquals("example@email.com", persistedMap.get("identityMap.Email[0].id"));
		assertEquals("true", persistedMap.get("identityMap.Email[0].primary"));
		assertEquals("ambiguous", persistedMap.get("identityMap.ECID[0].authenticatedState"));
	}

	@Test
	public void testUpdateAPI_nullData() throws Exception {
		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Identity.EXTENSION), null);

		// test
		Identity.updateIdentities(null);
		waitForThreads(2000);

		// verify no shares state change event dispatched
		List<Event> dispatchedEvents = getDispatchedEventsWith(EventType.HUB, EventSource.SHARED_STATE);
		assertEquals(0, dispatchedEvents.size());

		// verify xdm shared state is not disturbed
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertEquals(3, xdmSharedState.size()); // 3 for ECID still exists
		assertNotNull(xdmSharedState.get("identityMap.ECID[0].id"));
	}

	@Test
	public void testUpdateAPI_emptyData() throws Exception {
		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Identity.EXTENSION), null);

		// test
		Identity.updateIdentities(new IdentityMap());
		waitForThreads(2000);

		// verify no shares state change event dispatched
		List<Event> dispatchedEvents = getDispatchedEventsWith(EventType.HUB, EventSource.SHARED_STATE);
		assertEquals(0, dispatchedEvents.size());

		// verify xdm shared state is not disturbed
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertEquals(3, xdmSharedState.size()); // 3 for ECID still exists
		assertNotNull(xdmSharedState.get("identityMap.ECID[0].id"));
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
		Map<String, String> persistedMap = flattenMap(JSONUtils.toMap(new JSONObject(persistedJson)));
		assertEquals(6, persistedMap.size()); // 3 for ECID and 3 for Email
		assertEquals("example@email.com", persistedMap.get("identityMap.Email[0].id"));
		assertEquals("false", persistedMap.get("identityMap.Email[0].primary"));
		assertEquals("loggedOut", persistedMap.get("identityMap.Email[0].authenticatedState"));
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
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertEquals(3, xdmSharedState.size()); // 3 for ECID
		assertNotEquals("newECID", xdmSharedState.get("identityMap.ECID[0].id")); // ECID doesn't get replaced by API

		// verify persisted data doesn't change
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);
		Map<String, String> persistedMap = flattenMap(JSONUtils.toMap(new JSONObject(persistedJson)));
		assertEquals(3, persistedMap.size()); // 3 for ECID
		assertNotEquals("newECID", persistedMap.get("identityMap.ECID[0].id")); // ECID doesn't get replaced by API
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
		Map<String, String> persistedMap = flattenMap(JSONUtils.toMap(new JSONObject(persistedJson)));
		assertEquals(15, persistedMap.size()); // 3 for ECID + 12 for new identities
		assertEquals("primary@email.com", persistedMap.get("identityMap.Email[0].id"));
		assertEquals("secondary@email.com", persistedMap.get("identityMap.Email[1].id"));
		assertEquals("zzzyyyxxx", persistedMap.get("identityMap.UserId[0].id"));
		assertEquals("John Doe", persistedMap.get("identityMap.UserName[0].id"));
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
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertEquals(9, xdmSharedState.size()); // 3 for ECID + 6 for new identities
		assertEquals("primary@email.com", xdmSharedState.get("identityMap.Email[0].id"));
		assertEquals("secondary@email.com", xdmSharedState.get("identityMap.email[0].id"));

		// verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);
		Map<String, String> persistedMap = flattenMap(JSONUtils.toMap(new JSONObject(persistedJson)));
		assertEquals(9, persistedMap.size()); // 3 for ECID + 6 for new identities
		assertEquals("primary@email.com", persistedMap.get("identityMap.Email[0].id"));
		assertEquals("secondary@email.com", persistedMap.get("identityMap.email[0].id"));
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
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertEquals(6, xdmSharedState.size()); // 3 for ECID + 3 for Email secondary
		assertEquals("secondary@email.com", xdmSharedState.get("identityMap.Email[0].id"));

		// test again
		Identity.removeIdentity(new IdentityItem("secondary@email.com"), "Email");
		waitForThreads(2000);

		// verify xdm shared state
		xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertEquals(3, xdmSharedState.size()); // 3 for ECID

		// verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);
		Map<String, String> persistedMap = flattenMap(JSONUtils.toMap(new JSONObject(persistedJson)));
		assertEquals(3, persistedMap.size()); // 3 for ECID
	}

	@Test
	public void testRemoveIdentity_nonExistentNamespace() throws Exception {
		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Identity.EXTENSION), null);

		// test
		Identity.removeIdentity(new IdentityItem("primary@email.com"), "Email");
		waitForThreads(2000);

		// verify item is not removed
		// verify xdm shared state
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertEquals(3, xdmSharedState.size()); // 3 for ECID

		// verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);
		Map<String, String> persistedMap = flattenMap(JSONUtils.toMap(new JSONObject(persistedJson)));
		assertEquals(3, persistedMap.size()); // 3 for ECID
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
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertEquals(6, xdmSharedState.size()); // 3 for ECID +  3 for  Email

		// verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);
		Map<String, String> persistedMap = flattenMap(JSONUtils.toMap(new JSONObject(persistedJson)));
		assertEquals(6, persistedMap.size()); // 3 for ECID +  3 for  Email
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
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertEquals(6, xdmSharedState.size()); // 3 for ECID +  3 for  Email

		// verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);
		Map<String, String> persistedMap = flattenMap(JSONUtils.toMap(new JSONObject(persistedJson)));
		assertEquals(6, persistedMap.size()); // 3 for ECID +  3 for  Email
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
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertEquals(3, xdmSharedState.size()); // 3 for ECID that still exists

		// verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);
		Map<String, String> persistedMap = flattenMap(JSONUtils.toMap(new JSONObject(persistedJson)));
		assertEquals(3, persistedMap.size()); // 3 for ECID that still exists
	}
}
