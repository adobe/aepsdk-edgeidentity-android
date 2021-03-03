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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a type which contains instances variables for the Identity extension
 */
class IdentityEdgeProperties {

    /**
     * Helper class to store datastore keys
     */
    class PersistentKeys {
        static final String ECID = "ecid";
        static final String PRIVACY_STATUS = "privacy.status";
        private PersistentKeys(){ }
    }

    // The current Experience Cloud ID
    private ECID ecid;

    // The current privacy status provided by the Configuration extension, defaults to `unknown`
    private MobilePrivacyStatus privacyStatus = Utils.privacyFromString(IdentityEdgeConstants.Defaults.DEFAULT_MOBILE_PRIVACY);

    IdentityEdgeProperties() { }

    /**
     * Creates a identity edge properties instance based on the map
     * @param map a map representing an identity edge properties instance
     */
    IdentityEdgeProperties(final Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return;
        }

        try {
            final String ecidStr = (String) map.get(PersistentKeys.ECID);
            if (ecidStr != null) {
                ecid = new ECID(ecidStr);
            }
            privacyStatus = Utils.privacyFromString((String) map.get(PersistentKeys.PRIVACY_STATUS));
        } catch (ClassCastException e) {
            // add log
        }
    }

    /**
     * Sets the current {@link ECID}
     * @param ecid the new {@link ECID}
     */
    void setECID(ECID ecid) {
        this.ecid = ecid;
    }

    /**
     * Retrieves the current {@link ECID}
     * @return current {@link ECID}
     */
    public ECID getECID() {
        return ecid;
    }

    /**
     * Retrieves the current {@link MobilePrivacyStatus}
     * @return current {@link MobilePrivacyStatus}
     */
    public MobilePrivacyStatus getPrivacyStatus() {
        return privacyStatus;
    }

    /**
     * Sets the current privacy status
     * @param privacyStatus a privacy status
     */
    public void setPrivacyStatus(MobilePrivacyStatus privacyStatus) {
        this.privacyStatus = privacyStatus;
    }

    /**
     * Converts this instance into a map representation
     * @return this identity edge properties as a map
     */
    Map<String, Object> toMap() {
        final Map<String, Object> map = new HashMap<>();

        if (ecid != null) {
            map.put(PersistentKeys.ECID, ecid.getEcidString());
        }
        if (privacyStatus != null) {
            map.put(PersistentKeys.PRIVACY_STATUS, privacyStatus.getValue());
        }

        return map;
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

}
