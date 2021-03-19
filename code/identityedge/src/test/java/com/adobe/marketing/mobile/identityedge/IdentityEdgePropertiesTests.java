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

package com.adobe.marketing.mobile.identityedge;

import org.junit.Test;

import java.util.Map;

import static com.adobe.marketing.mobile.identityedge.IdentityEdgeTestUtil.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class IdentityEdgePropertiesTests {

    // ======================================================================================================================
    // Tests for method : toXDMData(final boolean allowEmpty)
    // ======================================================================================================================

    @Test
    public void test_toXDMData_Empty() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();

        // test
        Map<String, Object> xdmMap = props.toXDMData(false);

        // verify
        assertNull(xdmMap.get(IdentityEdgeConstants.XDMKeys.IDENTITY_MAP));
    }


    @Test
    public void test_toXDMData_Full() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();
        props.setECID(new ECID());
        props.setECIDSecondary(new ECID());

        // test
        Map<String, Object> xdmData = props.toXDMData(false);
        Map<String, String> flatMap = flattenMap(xdmData);

        // verify primary ECID
        assertEquals(props.getECID().toString(), flatMap.get("identityMap.ECID[0].id"));
        assertEquals("ambiguous", flatMap.get("identityMap.ECID[0].authenticatedState"));
        assertEquals("false", flatMap.get("identityMap.ECID[0].primary"));

        // verify secondary ECID
        assertEquals(props.getECIDSecondary().toString(), flatMap.get("identityMap.ECID[1].id"));
        assertEquals("ambiguous", flatMap.get("identityMap.ECID[1].authenticatedState"));
        assertEquals("false", flatMap.get("identityMap.ECID[1].primary"));
    }

    @Test
    public void test_toXDMData_OnlyPrimaryECID() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();
        props.setECID(new ECID());

        // test
        Map<String, Object> xdmMap = props.toXDMData(false);

        // verify
        assertEquals(props.getECID().toString(), flattenMap(xdmMap).get("identityMap.ECID[0].id"));
    }

    @Test
    public void test_toXDMData_OnlySecondaryECID() {
        // should not set secondary ECID if primary not set
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();
        props.setECIDSecondary(new ECID());

        // test and verify, can't have secondary ECID without primary ECID
        assertEquals(0,flattenMap(props.toXDMData(false)).size());
    }


    // ======================================================================================================================
    // Tests for constructor : IdentityEdgeProperties(final Map<String, Object> xdmData)
    // ======================================================================================================================

    @Test
    public void testConstruct_FromXDMData_LoadingDataFromPersistence() {
        // setup
        Map<String,Object> persistedIdentifiers = createXDMIdentityMap(
                new TestItem("UserId", "secretID"),
                new TestItem("PushId", "token"),
                new TestECIDItem("primaryECID"),
                new TestECIDItem("secondaryECID")
        );

        // test
        IdentityEdgeProperties props = new IdentityEdgeProperties(persistedIdentifiers);

        // verify
        Map<String, String> flatMap = flattenMap(props.toXDMData(false));
        assertEquals(12,flatMap.size()); // 4x3
        assertEquals("primaryECID", props.getECID().toString());
        assertEquals("secondaryECID", props.getECIDSecondary().toString());
        assertEquals("secretID", flatMap.get("identityMap.UserId[0].id"));
        assertEquals("token", flatMap.get("identityMap.PushId[0].id"));
    }

    @Test
    public void testConstruct_FromXDMData_NothingFromPersistence() {
        // test
        IdentityEdgeProperties props = new IdentityEdgeProperties(null);

        // verify
        assertEquals(0,flattenMap(props.toXDMData(false)).size());
    }


    // ======================================================================================================================
    // Tests for method : setECID(final ECID newEcid)
    // ======================================================================================================================

    @Test
    public void test_setECID_WillReplaceTheOldECID() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();

        // test 1
        props.setECID(new ECID("primary"));

        // verify
        Map<String, String> flatMap = flattenMap(props.toXDMData(false));
        assertEquals(3,flatMap.size());
        assertEquals("primary", flatMap.get("identityMap.ECID[0].id"));
        assertEquals("false", flatMap.get("identityMap.ECID[0].primary"));
        assertEquals("primary", props.getECID().toString());

        // test 2 - call setECID again to replace the old one
        props.setECID(new ECID("primaryAgain"));

        // verify
        flatMap = flattenMap(props.toXDMData(false));
        assertEquals(3,flatMap.size());
        assertEquals("primaryAgain", flatMap.get("identityMap.ECID[0].id"));
        assertEquals("false", flatMap.get("identityMap.ECID[0].primary"));
        assertEquals("primaryAgain", props.getECID().toString());
    }

    @Test
    public void test_setECID_NullRemovesFromIdentityMap() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();

        // test 1 - set a valid ECID and then to null
        props.setECID(new ECID("primary"));
        props.setECID(null);

        // verify
        assertEquals(0,flattenMap(props.toXDMData(false)).size());
        assertNull(props.getECID());
    }


    // ======================================================================================================================
    // Tests for method : setECIDSecondary(final ECID newEcid)
    // ======================================================================================================================

    @Test
    public void test_setECIDSecondary_WillReplaceTheOldECID() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();

        // test 1
        props.setECID(new ECID("primary"));
        props.setECIDSecondary(new ECID("secondary"));

        // verify
        Map<String, String> flatMap = flattenMap(props.toXDMData(false));
        assertEquals(6,flatMap.size());
        assertEquals("secondary", flatMap.get("identityMap.ECID[1].id"));
        assertEquals("false", flatMap.get("identityMap.ECID[1].primary"));
        assertEquals("secondary", props.getECIDSecondary().toString());

        // test 2 - call setECIDSecondary again to replace the old one
        props.setECIDSecondary(new ECID("secondaryAgain"));

        // verify
        flatMap = flattenMap(props.toXDMData(false));
        assertEquals(6,flatMap.size());
        assertEquals("secondaryAgain", flatMap.get("identityMap.ECID[1].id"));
        assertEquals("false", flatMap.get("identityMap.ECID[1].primary"));
        assertEquals("secondaryAgain", props.getECIDSecondary().toString());
    }

    @Test
    public void test_setECIDSecondary_NullRemovesFromIdentityMap() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties(createXDMIdentityMap(
                new TestECIDItem("primary"),
                new TestECIDItem("secondary")
        ));
        assertEquals(6, flattenMap(props.toXDMData(false)).size());

        // test
        props.setECIDSecondary(null);

        // verify
        assertEquals(3, flattenMap(props.toXDMData(false)).size());
        assertNull(props.getECIDSecondary());
    }


    @Test
    public void test_clearPrimaryECID_alsoClearsSecondaryECID() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties(createXDMIdentityMap(
                new TestECIDItem("primary"),
                new TestECIDItem("secondary")
        ));

        // test
        props.setECID(null);

        // verify
        assertEquals(0, flattenMap(props.toXDMData(false)).size());
        assertNull(props.getECIDSecondary());
        assertNull(props.getECID());
    }


    @Test
    public void test_setPrimaryECIDPreservesSecondaryECID() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties(createXDMIdentityMap(
                new TestECIDItem("primary"),
                new TestECIDItem("secondary")
        ));

        // test
        props.setECID(new ECID("primaryAgain"));

        // verify
        assertEquals(6, flattenMap(props.toXDMData(false)).size());
        assertEquals("secondary",props.getECIDSecondary().toString());
        assertEquals("primaryAgain",props.getECID().toString());
    }

    @Test
    public void test_primaryECIDIsAlwaysTheFirstElement() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();

        // test
        props.setECID(new ECID("primary"));
        props.setECIDSecondary(new ECID("secondary"));
        props.setECID(new ECID("primaryAgain"));

        // verify
        Map<String,String> flatMap = flattenMap(props.toXDMData(false));
        assertEquals(6, flatMap.size());
        assertEquals("primaryAgain", flatMap.get("identityMap.ECID[0].id"));
        assertEquals("secondary", flatMap.get("identityMap.ECID[1].id"));

    }
    

    // ======================================================================================================================
    // Tests for "updateCustomerIdentifiers" is already covered in "handleUpdateRequest" tests in IdentityEdgeExtensionTests
    // ======================================================================================================================


    // ======================================================================================================================
    // Tests for "removeCustomerIdentifiers" is already covered in handleRemoveRequest tests in IdentityEdgeExtensionTests
    // ======================================================================================================================


}
