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

    @Test
    public void testIdentityEdgeProperties_toXDMDataEmpty() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();

        // test
        Map<String, Object> xdmMap = props.toXDMData(false);

        // verify
        assertNull(xdmMap.get(IdentityEdgeConstants.XDMKeys.IDENTITY_MAP));
    }

    @Test
    public void testIdentityEdgeProperties_toXDMDataFull() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();
        props.setECID(new ECID());
        props.setECIDSecondary(new ECID());

        // test
        Map<String, Object> xdmData = props.toXDMData(false);

        // verify
        assertEquals(props.getECID().toString(), flattenMap(xdmData).get("identityMap.ECID[0].id"));
        assertEquals(props.getECIDSecondary().toString(), flattenMap(xdmData).get("identityMap.ECID[1].id"));
    }

    @Test
    public void testIdentityEdgeProperties_toXDMDataOnlyPrimaryECID() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();
        props.setECID(new ECID());

        // test
        Map<String, Object> xdmMap = props.toXDMData(false);

        // verify
        assertEquals(props.getECID().toString(), flattenMap(xdmMap).get("identityMap.ECID[0].id"));
    }

    @Test
    public void testIdentityEdgeProperties_toXDMDataMissingECID() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();

        // test
        Map<String, Object> xdmData = props.toXDMData(false);

        // verify
        assertNull(flattenMap(xdmData).get("identityMap.ECID[0].id"));
    }

    @Test
    public void testIdentityEdgeProperties_toXDMDataMissingPrivacy() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();
        props.setECID(new ECID());

        // test
        Map<String, Object> xdmData = props.toXDMData(false);

        // verify
        assertEquals(props.getECID().toString(), flattenMap(xdmData).get("identityMap.ECID[0].id"));
    }

    @Test
    public void testIdentityEdgeProperties_fromXDMDataFull() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();
        props.setECID(new ECID());
        props.setECIDSecondary(new ECID());

        // test
        Map<String, Object> xdmData = props.toXDMData(false);
        IdentityEdgeProperties loadedProps = new IdentityEdgeProperties(xdmData);

        // verify
        assertEquals(flattenMap(xdmData).get("identityMap.ECID[0].id"), loadedProps.getECID().toString());
        assertEquals(props.getECIDSecondary(), loadedProps.getECIDSecondary());
        assertEquals(loadedProps.getECID().toString(), flattenMap(xdmData).get("identityMap.ECID[0].id"));
    }

    @Test
    public void testIdentityEdgeProperties_fromXDMDataMissingECID() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();

        // test
        Map<String, Object> map = props.toXDMData(false);
        IdentityEdgeProperties loadedProps = new IdentityEdgeProperties(map);

        // verify
        assertNull(loadedProps.getECID());
    }

    @Test
    public void testIdentityEdgeProperties_fromXDMDataMissingPrivacy() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();
        props.setECID(new ECID());

        // test
        Map<String, Object> xdmMap = props.toXDMData(false);
        IdentityEdgeProperties loadedProps = new IdentityEdgeProperties(xdmMap);

        // verify
        assertEquals(props.getECID().toString(), flattenMap(xdmMap).get("identityMap.ECID[0].id"));
    }

    @Test
    public void testIdentityEdgeProperties_toXDMDataWithECID() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();
        props.setECID(new ECID());

        // test
        Map<String, Object> xdmMap = props.toXDMData(false);

        // verify
        assertEquals(props.getECID().toString(), flattenMap(xdmMap).get("identityMap.ECID[0].id"));
    }


    // ======================================================================================================================
    // Tests for constructor : IdentityEdgeProperties(final Map<String, Object> xdmData)
    // ======================================================================================================================

    @Test
    public void testConstruct_IdentityEdgeProperties_LoadingDataFromPersistence() {
        // setup
        Map<String,Object> persistedIdentifiers = createXDMIdentityMap(
                new TestItem("UserId", "secretID"),
                new TestItem("PushId", "token"),
                new TestPrimaryECIDItem("primaryECID"),
                new TestSecondaryECIDItem("secondaryECID")
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
    public void testConstruct_IdentityEdgeProperties_NothingFromPersistence() {
        // test
        IdentityEdgeProperties props = new IdentityEdgeProperties(null);
        Map<String, Object> xdmMap = props.toXDMData(false);

        // verify
        Map<String, String> flatMap = flattenMap(xdmMap);
        assertEquals(0,flatMap.size());
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
        assertEquals("true", flatMap.get("identityMap.ECID[0].primary"));
        assertEquals("primary", props.getECID().toString());

        // test 2 - call setECID again to replace the old one
        props.setECID(new ECID("primaryAgain"));

        // verify
        flatMap = flattenMap(props.toXDMData(false));
        assertEquals(3,flatMap.size());
        assertEquals("primaryAgain", flatMap.get("identityMap.ECID[0].id"));
        assertEquals("true", flatMap.get("identityMap.ECID[0].primary"));
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
        Map<String, String> flatMap = flattenMap(props.toXDMData(false));
        assertEquals(0,flatMap.size());
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
        props.setECIDSecondary(new ECID("secondary"));

        // verify
        Map<String, String> flatMap = flattenMap(props.toXDMData(false));
        assertEquals(3,flatMap.size());
        assertEquals("secondary", flatMap.get("identityMap.ECID[0].id"));
        assertEquals("false", flatMap.get("identityMap.ECID[0].primary"));
        assertEquals("secondary", props.getECIDSecondary().toString());

        // test 2 - call setECIDSecondary again to replace the old one
        props.setECIDSecondary(new ECID("secondaryAgain"));

        // verify
        flatMap = flattenMap(props.toXDMData(false));
        assertEquals(3,flatMap.size());
        assertEquals("secondaryAgain", flatMap.get("identityMap.ECID[0].id"));
        assertEquals("false", flatMap.get("identityMap.ECID[0].primary"));
        assertEquals("secondaryAgain", props.getECIDSecondary().toString());
    }


    @Test
    public void test_setECIDSecondary_NullRemovesFromIdentityMap() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties(createXDMIdentityMap(
                new TestSecondaryECIDItem("secondaryECID")
        ));

        // test - set secondary ECID to null
        props.setECIDSecondary(null);

        // verify
        Map<String, String> flatMap = flattenMap(props.toXDMData(false));
        assertEquals(0,flatMap.size());
        assertNull(props.getECIDSecondary());
    }








    // ======================================================================================================================
    // Tests for "updateCustomerIdentifiers" is already covered in "handleUpdateRequest" tests in IdentityEdgeExtensionTests
    // ======================================================================================================================


    // ======================================================================================================================
    // Tests for "removeCustomerIdentifiers" is already covered in handleRemoveRequest tests in IdentityEdgeExtensionTests
    // ======================================================================================================================


}
