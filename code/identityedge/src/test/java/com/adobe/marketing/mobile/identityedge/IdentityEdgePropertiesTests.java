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

import static com.adobe.marketing.mobile.identityedge.IdentityEdgeTestUtil.flattenMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
    // Tests for "updateCustomerIdentifiers" is already covered in "handleUpdateRequest" tests in IdentityEdgeExtensionTests
    // ======================================================================================================================


    // ======================================================================================================================
    // Tests for "removeCustomerIdentifiers" is already covered in handleRemoveRequest tests in IdentityEdgeExtensionTests
    // ======================================================================================================================


}
