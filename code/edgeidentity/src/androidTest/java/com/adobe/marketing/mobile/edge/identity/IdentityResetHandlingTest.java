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

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.edge.identity.util.MonitorExtension;
import com.adobe.marketing.mobile.edge.identity.util.TestPersistenceHelper;
import com.adobe.marketing.mobile.util.JSONUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

public class IdentityResetHandlingTest {

	@Rule
	public TestRule rule = new SetupCoreRule();

	// --------------------------------------------------------------------------------------------
	// Setup
	// --------------------------------------------------------------------------------------------

	@Before
	public void setup() throws Exception {
		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Identity.EXTENSION), null);
	}

	// --------------------------------------------------------------------------------------------
	// Reset Handling
	// --------------------------------------------------------------------------------------------

	@Test
	public void testReset_ClearsAllIDAndDispatchesResetComplete() throws Exception {
		// setup
		IdentityMap map = new IdentityMap();
		map.addItem(new IdentityItem("primary@email.com"), "Email");
		map.addItem(new IdentityItem("secondary@email.com"), "Email");
		map.addItem(new IdentityItem("zzzyyyxxx"), "UserId");
		map.addItem(new IdentityItem("John Doe"), "UserName");
		Identity.updateIdentities(map);
		String beforeResetECID = getExperienceCloudIdSync();

		// test
		MobileCore.resetIdentities();

		// verify
		String newECID = getExperienceCloudIdSync();
		assertNotNull(newECID);
		assertNotEquals(beforeResetECID, newECID);

		// verify edge reset complete event dispatched
		List<Event> resetCompleteEvent = getDispatchedEventsWith(EventType.EDGE_IDENTITY, EventSource.RESET_COMPLETE);
		assertEquals(1, resetCompleteEvent.size());

		// verify shared state is updated
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertEquals(3, xdmSharedState.size()); // 3 for ECID still exists
		assertEquals(newECID, xdmSharedState.get("identityMap.ECID[0].id"));

		// verify persistence is updated
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityConstants.DataStoreKey.DATASTORE_NAME,
			IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES
		);
		Map<String, String> persistedMap = flattenMap(JSONUtils.toMap(new JSONObject(persistedJson)));
		assertEquals(3, persistedMap.size()); // 3 for ECID
		assertEquals(newECID, persistedMap.get("identityMap.ECID[0].id"));
	}
}
