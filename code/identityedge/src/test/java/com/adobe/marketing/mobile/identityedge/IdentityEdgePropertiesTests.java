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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // test
        Map<String, Object> xdmData = props.toXDMData(false);

        // verify
        assertEquals(props.getECID().getEcidString(), ecidFromIdentityMap(xdmData));
    }

    @Test
    public void testIdentityEdgeProperties_toXDMDataMissingECID() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();

        // test
        Map<String, Object> xdmData = props.toXDMData(false);

        // verify
        assertNull(ecidFromIdentityMap(xdmData));
    }

    @Test
    public void testIdentityEdgeProperties_toXDMDataMissingPrivacy() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();
        props.setECID(new ECID());

        // test
        Map<String, Object> xdmData = props.toXDMData(false);

        // verify
        assertEquals(props.getECID().getEcidString(), ecidFromIdentityMap(xdmData));
    }

    @Test
    public void testIdentityEdgeProperties_fromXDMDataFull() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();
        props.setECID(new ECID());

        // test
        Map<String, Object> xdmData = props.toXDMData(false);
        IdentityEdgeProperties loadedProps = new IdentityEdgeProperties(xdmData);

        // verify
        assertEquals(ecidFromIdentityMap(xdmData), loadedProps.getECID().getEcidString());
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
        assertEquals(ecidFromIdentityMap(xdmMap), loadedProps.getECID().getEcidString());
    }

    @Test
    public void testIdentityEdgeProperties_toXDMDataWithECID() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();
        props.setECID(new ECID());

        // test
        Map<String, Object> xdmMap = props.toXDMData(false);

        // verify
        assertEquals(props.getECID().getEcidString(), ecidFromIdentityMap(xdmMap));
    }

    private String ecidFromIdentityMap(Map<String, Object> xdmMap) {
        if (xdmMap == null) { return null; }
        Map<String, Object> identityMap = (HashMap<String, Object>) xdmMap.get("identityMap");
        if (identityMap == null) { return null; }
        List<Object> ecidArr = (ArrayList<Object>) identityMap.get("ECID");
        if (ecidArr == null) { return null; }
        Map<String, Object> ecidDict = (HashMap<String, Object>) ecidArr.get(0);
        if (ecidDict == null) { return null; }
        String ecid = (String) ecidDict.get("id");
        return ecid;
    }

}
