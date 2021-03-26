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

import com.adobe.marketing.mobile.TestHelper;
import com.adobe.marketing.mobile.TestPersistenceHelper;

import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.util.Map;

import static com.adobe.marketing.mobile.TestHelper.getXDMSharedStateFor;
import static com.adobe.marketing.mobile.edge.identity.IdentityFunctionalTestUtil.*;
import static com.adobe.marketing.mobile.edge.identity.IdentityTestUtil.createXDMIdentityMap;
import static com.adobe.marketing.mobile.edge.identity.IdentityTestUtil.flattenMap;
import static org.junit.Assert.assertEquals;

public class IdentityBootUpTest {

    @Rule
    public RuleChain rule = RuleChain.outerRule(new TestHelper.SetupCoreRule())
            .around(new TestHelper.RegisterMonitorExtensionRule());

    // --------------------------------------------------------------------------------------------
    // OnBootUp
    // --------------------------------------------------------------------------------------------

    @Test
    public void testOnBootUp_LoadsAllIdentitiesFromPreference() throws Exception {
        // test
        setEdgeIdentityPersistence(createXDMIdentityMap(
                new IdentityTestUtil.TestItem("ECID", "primaryECID"),
                new IdentityTestUtil.TestItem("ECID", "secondaryECID"),
                new IdentityTestUtil.TestItem("Email", "example@email.com"),
                new IdentityTestUtil.TestItem("UserId", "JohnDoe")
        ));
        registerEdgeIdentityExtension();

        // verify xdm shared state
        Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
        assertEquals(12, xdmSharedState.size());  // 3 for ECID and 3 for secondaryECID + 6
        assertEquals("primaryECID", xdmSharedState.get("identityMap.ECID[0].id"));
        assertEquals("secondaryECID", xdmSharedState.get("identityMap.ECID[1].id"));
        assertEquals("example@email.com", xdmSharedState.get("identityMap.Email[0].id"));
        assertEquals("JohnDoe", xdmSharedState.get("identityMap.UserId[0].id"));


        //verify persisted data
        final String persistedJson = TestPersistenceHelper.readPersistedData(IdentityConstants.DataStoreKey.DATASTORE_NAME, IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES);
        Map<String, String> persistedMap = flattenMap(IdentityTestUtil.toMap(new JSONObject(persistedJson)));
        assertEquals(12, persistedMap.size());  // 3 for ECID and 3 for secondaryECID + 6
    }

    // --------------------------------------------------------------------------------------------
    // All the other bootUp tests with to ECID is coded in IdentityECIDHandling
    // --------------------------------------------------------------------------------------------

}
