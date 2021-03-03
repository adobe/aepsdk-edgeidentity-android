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

import com.adobe.marketing.mobile.MobilePrivacyStatus;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class IdentityEdgePropertiesTests {

    @Test
    public void testIdentityEdgeProperties_Empty() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();

        // test
        Map<String, Object> map = props.toXDMData(false);

        // verify
        assertTrue(map.isEmpty());
    }

    @Test
    public void testIdentityEdgeProperties_toMapFull() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();
        props.setECID(new ECID());
        props.setPrivacyStatus(MobilePrivacyStatus.OPT_IN);

        // test
        Map<String, Object> map = props.toMap();

        // verify
        assertEquals(props.getECID().getEcidString(), map.get(IdentityEdgeProperties.PersistentKeys.ECID));
        assertEquals(props.getPrivacyStatus().getValue(), map.get(IdentityEdgeProperties.PersistentKeys.PRIVACY_STATUS));
    }

    @Test
    public void testIdentityEdgeProperties_toMapMissingECID() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();
        props.setPrivacyStatus(MobilePrivacyStatus.OPT_IN);

        // test
        Map<String, Object> map = props.toMap();

        // verify
        assertNull(map.get(IdentityEdgeProperties.PersistentKeys.ECID));
        assertEquals(props.getPrivacyStatus().getValue(), map.get(IdentityEdgeProperties.PersistentKeys.PRIVACY_STATUS));
    }

    @Test
    public void testIdentityEdgeProperties_toMapMissingPrivacy() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();
        props.setECID(new ECID());

        // test
        Map<String, Object> map = props.toMap();

        // verify
        assertEquals(props.getECID().getEcidString(), map.get(IdentityEdgeProperties.PersistentKeys.ECID));
        assertEquals(MobilePrivacyStatus.UNKNOWN.getValue(), map.get(IdentityEdgeProperties.PersistentKeys.PRIVACY_STATUS));
    }

    @Test
    public void testIdentityEdgeProperties_fromMapFull() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();
        props.setECID(new ECID());
        props.setPrivacyStatus(MobilePrivacyStatus.OPT_IN);

        // test
        Map<String, Object> map = props.toMap();
        IdentityEdgeProperties loadedProps = new IdentityEdgeProperties(map);

        // verify
        assertEquals(map.get(IdentityEdgeProperties.PersistentKeys.ECID), loadedProps.getECID().getEcidString());
        assertEquals(map.get(IdentityEdgeProperties.PersistentKeys.PRIVACY_STATUS), loadedProps.getPrivacyStatus().getValue());
    }

    @Test
    public void testIdentityEdgeProperties_fromMapMissingECID() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();
        props.setPrivacyStatus(MobilePrivacyStatus.OPT_IN);

        // test
        Map<String, Object> map = props.toMap();
        IdentityEdgeProperties loadedProps = new IdentityEdgeProperties(map);

        // verify
        assertNull(loadedProps.getECID());
        assertEquals(map.get(IdentityEdgeProperties.PersistentKeys.PRIVACY_STATUS), loadedProps.getPrivacyStatus().getValue());
    }

    @Test
    public void testIdentityEdgeProperties_fromMapMissingPrivacy() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();
        props.setECID(new ECID());

        // test
        Map<String, Object> map = props.toMap();
        IdentityEdgeProperties loadedProps = new IdentityEdgeProperties(map);

        // verify
        assertEquals(map.get(IdentityEdgeProperties.PersistentKeys.ECID), loadedProps.getECID().getEcidString());
        assertEquals(MobilePrivacyStatus.UNKNOWN, loadedProps.getPrivacyStatus());
    }

    @Test
    public void testIdentityEdgeProperties_toXDMDataWithECID() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();
        props.setECID(new ECID());

        // test
        Map<String, Object> xdmMap = props.toXDMData(false);

        // verify
        Map<String, Object> identityMap = (HashMap<String, Object>) xdmMap.get("identityMap");
        List<Object> ecidArr = (ArrayList<Object>) identityMap.get("ECID");
        Map<String, Object> ecidDict = (HashMap<String, Object>) ecidArr.get(0);
        String ecid = (String) ecidDict.get("id");
        assertEquals(props.getECID().getEcidString(), ecid);
    }

    @Test
    public void testIdentityEdgeProperties_toXDMDataMissingECID() {
        // setup
        IdentityEdgeProperties props = new IdentityEdgeProperties();

        // test
        Map<String, Object> xdmMap = props.toXDMData(false);

        // verify
        assertNull(xdmMap.get("identityMap"));
    }
}
