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

import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a type which contains instances variables for the Identity Edge extension
 */
class IdentityEdgeProperties {

    private static final String LOG_TAG = "IdentityEdgeProperties";

    // The current Experience Cloud ID
    private ECID ecid;

    IdentityEdgeProperties() { }

    /**
     * Creates a identity edge properties instance based on the map
     * @param xdmData a map representing an identity edge properties instance
     */
    IdentityEdgeProperties(final Map<String, Object> xdmData) {
        if (xdmData == null || xdmData.isEmpty()) {
            return;
        }

        IdentityMap identityMap = IdentityMap.fromData(xdmData);
        ecid = readECIDFromIdentityMap(identityMap);
    }

    /**
     * Sets the current {@link ECID}
     * @param ecid the new {@link ECID}
     */
    void setECID(final ECID ecid) {
        this.ecid = ecid;
    }

    /**
     * Retrieves the current {@link ECID}
     * @return current {@link ECID}
     */
    ECID getECID() {
        return ecid;
    }

    /**
     * Converts this into an event data representation in XDM format
     * @param allowEmpty  If this {@link IdentityEdgeProperties} contains no data, return a dictionary with a single {@link IdentityMap} key
     * @return A dictionary representing this in XDM format
     */
    Map<String, Object> toXDMData(final boolean allowEmpty) {
        final Map<String, Object> map = new HashMap<>();
        final IdentityMap identityMap = new IdentityMap();

        if (ecid != null) {
            identityMap.addItem(IdentityEdgeConstants.Namespaces.ECID, ecid.getEcidString());
        }

        final Map<String, List<Map<String, Object>>> dict = identityMap.toObjectMap();
        if (dict != null && (!dict.isEmpty() || allowEmpty)) {
            map.put(IdentityEdgeConstants.XDMKeys.IDENTITY_MAP, dict);
        }

        return map;
    }

    /**
     * Reads the ECID from an IdentityMap
     * @param identityMap an IdentityMap
     * @return ECID stored in the IdentityMap or null if not found
     */
    static ECID readECIDFromIdentityMap(IdentityMap identityMap) {
        if (identityMap == null) { return null; }
        final List<Map<String, Object>>ecidArr = identityMap.getIdentityItemForNamespace(IdentityEdgeConstants.Namespaces.ECID);
        if (ecidArr == null) { return null; }
        final Map<String, Object> ecidDict = ecidArr.get(0);
        if (ecidDict == null) { return null; }
        String ecidStr = null;
        try {
            ecidStr = (String) ecidDict.get(IdentityEdgeConstants.XDMKeys.ID);
        } catch (ClassCastException e) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Failed to create read ECID from IdentityMap");
        }

        if (ecidStr == null) { return null; }
        return new ECID(ecidStr);
    }

}
