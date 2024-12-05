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

import static com.adobe.marketing.mobile.edge.identity.util.IdentityFunctionalTestUtil.TestItem;
import static com.adobe.marketing.mobile.edge.identity.util.IdentityFunctionalTestUtil.createXDMIdentityMap;
import static com.adobe.marketing.mobile.edge.identity.util.IdentityFunctionalTestUtil.setEdgeIdentityPersistence;
import static com.adobe.marketing.mobile.util.JSONAsserts.assertExactMatch;
import static com.adobe.marketing.mobile.util.NodeConfig.Scope.Subtree;
import static com.adobe.marketing.mobile.util.TestHelper.getXDMSharedStateFor;
import static com.adobe.marketing.mobile.util.TestHelper.registerExtensions;

import com.adobe.marketing.mobile.util.ElementCount;
import com.adobe.marketing.mobile.util.KeyMustBeAbsent;
import com.adobe.marketing.mobile.util.MonitorExtension;
import com.adobe.marketing.mobile.util.TestHelper;
import com.adobe.marketing.mobile.util.TestPersistenceHelper;
import java.util.Arrays;
import java.util.Map;
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
		Map<String, Object> xdmSharedState = getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000);

		String expected =
			"{" +
			"\"identityMap\": {" +
			"    \"ECID\": [" +
			"        {" +
			"            \"id\": \"primaryECID\"" +
			"        }," +
			"        {" +
			"            \"id\": \"secondaryECID\"" +
			"        }" +
			"    ]," +
			"    \"Email\": [" +
			"        {" +
			"            \"id\": \"example@email.com\"" +
			"        }" +
			"    ]," +
			"    \"UserId\": [" +
			"        {" +
			"            \"id\": \"JohnDoe\"" +
			"        }" +
			"    ]" +
			"  }" +
			"}";
		assertExactMatch(
			expected,
			xdmSharedState,
			new ElementCount(12, Subtree) // 3 for ECID and 3 for secondaryECID + 6
		);

		//verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);

		assertExactMatch("{}", persistedJson, new ElementCount(12, Subtree));
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
		Map<String, Object> xdmSharedState = getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000);

		String expected =
			"{" +
			"\"identityMap\": {" +
			"    \"ECID\": [" +
			"        {" +
			"            \"id\": \"primaryECID\"" +
			"        }" +
			"    ]," +
			"    \"UserId\": [" +
			"        {" +
			"            \"id\": \"JohnDoe\"" +
			"        }" +
			"    ]" +
			"  }" +
			"}";
		assertExactMatch(
			expected,
			xdmSharedState,
			new ElementCount(6, Subtree), // 3 for ECID and 3 UserId JohnDoe
			new KeyMustBeAbsent("identityMap.UserId[1].id")
		);
	}
	// --------------------------------------------------------------------------------------------
	// All the other bootUp tests with to ECID is coded in IdentityECIDHandling
	// --------------------------------------------------------------------------------------------
}
