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

    // A secondary (non-primary) Experience Cloud ID
    private ECID ecidSecondary;

    IdentityEdgeProperties() { }

    /**
     * Creates a identity edge properties instance based on the map
     * @param xdmData a map representing an identity edge properties instance
     */
    IdentityEdgeProperties(final Map<String, Object> xdmData) {
        if (Utils.isNullOrEmpty(xdmData)) {
            return;
        }

        IdentityMap identityMap = IdentityMap.fromData(xdmData);
        if (identityMap != null) {
            final List<IdentityItem> ecidItems = identityMap.getIdentityItemsForNamespace(IdentityEdgeConstants.Namespaces.ECID);
            if (ecidItems != null) {
                if (ecidItems.size() > 0 && ecidItems.get(0) != null && ecidItems.get(0).getId() != null) {
                    ecid = new ECID(ecidItems.get(0).getId());
                }
                if (ecidItems.size() > 1 && ecidItems.get(1) != null && ecidItems.get(1).getId() != null) {
                    ecidSecondary = new ECID(ecidItems.get(1).getId());
                }
            }
        }
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
     * Sets a secondary {@link ECID}
     * @param ecid a new secondary {@code ECID}
     */
    void setECIDSecondary(final ECID ecid) {
        this.ecidSecondary = ecid;
    }

    /**
     * Retrieves the secondary {@link ECID}.
     * @return secondary {@code ECID}
     */
    ECID getECIDSecondary() {
        return ecidSecondary;
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
            IdentityItem ecidItem = new IdentityItem(ecid.toString());
            identityMap.addItem(IdentityEdgeConstants.Namespaces.ECID, ecidItem);

            // set second ECID only if primary exists
            if (ecidSecondary != null) {
                IdentityItem ecidSecondaryItem = new IdentityItem(ecidSecondary.toString());
                identityMap.addItem(IdentityEdgeConstants.Namespaces.ECID, ecidSecondaryItem);
            }
        }

        final Map<String, List<Map<String, Object>>> dict = identityMap.toObjectMap();
        if (dict != null && (!dict.isEmpty() || allowEmpty)) {
            map.put(IdentityEdgeConstants.XDMKeys.IDENTITY_MAP, dict);
        }

        return map;
    }

}
