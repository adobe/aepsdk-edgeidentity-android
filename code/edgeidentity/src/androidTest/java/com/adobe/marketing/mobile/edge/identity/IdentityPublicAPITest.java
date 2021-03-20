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

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.TestHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.adobe.marketing.mobile.TestHelper.getSharedStateFor;
import static com.adobe.marketing.mobile.TestHelper.resetTestExpectations;
import static com.adobe.marketing.mobile.edge.identity.IdentityTestUtil.*;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class IdentityPublicAPITest {

    @Rule
    public RuleChain rule = RuleChain.outerRule(new TestHelper.SetupCoreRule())
            .around(new TestHelper.RegisterMonitorExtensionRule());


    // --------------------------------------------------------------------------------------------
    // Setup
    // --------------------------------------------------------------------------------------------

    @Before
    public void setup() throws Exception {
        Identity.registerExtension();

        final CountDownLatch latch = new CountDownLatch(1);
        MobileCore.start(new AdobeCallback() {
            @Override
            public void call(Object o) {
                latch.countDown();
            }
        });

        latch.await();
        resetTestExpectations();
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
        Map<String, String> sharedStateMap = flattenMap(getSharedStateFor(IdentityTestConstants.SharedStateName.EVENT_HUB, 1000));
        assertEquals(IdentityConstants.EXTENSION_VERSION, sharedStateMap.get("extensions.com.adobe.edge.identity.version"));
    }
}
