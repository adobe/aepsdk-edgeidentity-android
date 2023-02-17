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
import static com.adobe.marketing.mobile.edge.identity.util.TestHelper.getXDMSharedStateFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.adobe.marketing.mobile.edge.identity.util.MonitorExtension;
import com.adobe.marketing.mobile.edge.identity.util.TestHelper;
import com.adobe.marketing.mobile.edge.identity.util.TestPersistenceHelper;
import com.adobe.marketing.mobile.util.JSONUtils;
import java.util.Arrays;
import java.util.Map;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

public class IdentityBootUpTest {

	@Rule
	public TestRule rule = new TestHelper.SetupCoreRule();

	// --------------------------------------------------------------------------------------------
	// OnBootUp
	// --------------------------------------------------------------------------------------------

	@Test
	public void testOnBootUp_LoadsAllIdentitiesFromPreference() throws Exception {
		// test
		setEdgeIdentityPersistence(
			createXDMIdentityMap(
				new TestItem("ECID", "primaryECID"),
				new TestItem("ECID", "secondaryECID"),
				new TestItem("Email", "example@email.com"),
				new TestItem("UserId", "JohnDoe")
			)
		);

		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Identity.EXTENSION), null);

		// verify xdm shared state
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertEquals(12, xdmSharedState.size()); // 3 for ECID and 3 for secondaryECID + 6
		assertEquals("primaryECID", xdmSharedState.get("identityMap.ECID[0].id"));
		assertEquals("secondaryECID", xdmSharedState.get("identityMap.ECID[1].id"));
		assertEquals("example@email.com", xdmSharedState.get("identityMap.Email[0].id"));
		assertEquals("JohnDoe", xdmSharedState.get("identityMap.UserId[0].id"));

		//verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);
		Map<String, String> persistedMap = flattenMap(JSONUtils.toMap(new JSONObject(persistedJson)));
		assertEquals(12, persistedMap.size()); // 3 for ECID and 3 for secondaryECID + 6
	}

	@Test
	public void testOnBootUp_LoadsAllIdentitiesFromPreference_ignoresEmptyItems() throws Exception {
		// test
		setEdgeIdentityPersistence(
			createXDMIdentityMap(
				new TestItem("ECID", "primaryECID"),
				new TestItem("UserId", "JohnDoe"),
				new TestItem("UserId", ""), // empty Item id
				new TestItem("UserId", null) // null Item id
			)
		);

		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Identity.EXTENSION), null);

		// verify xdm shared state
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertEquals(6, xdmSharedState.size()); // 3 for ECID and 3 UserId JohnDoe
		assertEquals("primaryECID", xdmSharedState.get("identityMap.ECID[0].id"));
		assertEquals("JohnDoe", xdmSharedState.get("identityMap.UserId[0].id"));
		assertNull(xdmSharedState.get("identityMap.UserId[1].id"));
	}
	// --------------------------------------------------------------------------------------------
	// All the other bootUp tests with to ECID is coded in IdentityECIDHandling
	// --------------------------------------------------------------------------------------------

}
