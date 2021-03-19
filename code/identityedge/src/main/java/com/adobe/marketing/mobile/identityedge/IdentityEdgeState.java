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


/**
 * Manages the business logic of the Identity Edge extension
 */
class IdentityEdgeState {
    private static String LOG_TAG = "IdentityEdgeState";
    private boolean hasBooted = false;
    private IdentityEdgeProperties identityProperties;

    /**
     *  Creates a new {@link IdentityEdgeState} with the given {@link IdentityEdgeProperties}
     * @param identityProperties identity edge properties
     */
    IdentityEdgeState(final IdentityEdgeProperties identityProperties) {
        this.identityProperties = identityProperties;
    }

    /**
     * @return The current {@link IdentityEdgeProperties} for this identity state
     */
    IdentityEdgeProperties getIdentityEdgeProperties() {
        return identityProperties;
    }

    /**
     * @return Returns true if IdentityEdge has booted, false otherwise
     */
    boolean hasBooted() {
        return hasBooted;
    }

    /**
     * Completes init for the Identity Edge extension.
     * @return True if we should share state after bootup, false otherwise
     */
    boolean bootupIfReady() {
        if (hasBooted) { return true; }
        // Load properties from local storage
        identityProperties = IdentityEdgeStorageService.loadPropertiesFromPersistence();

        if (identityProperties == null) {
            identityProperties = new IdentityEdgeProperties();
        }

        // Generate new ECID on first launch
        if (identityProperties.getECID() == null) {
            final ECID directIdentityEcid = IdentityEdgeStorageService.loadEcidFromDirectIdentityPersistence();
            if (directIdentityEcid == null) {
                identityProperties.setECID(new ECID());
                MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Bootup - Generating new ECID '" + identityProperties.getECID().toString() + "'");
            } else {
                identityProperties.setECID(directIdentityEcid);
                MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Bootup - Loading ECID from direct Identity extension '" + directIdentityEcid + "'");
            }
            IdentityEdgeStorageService.savePropertiesToPersistence(identityProperties);
        }

        hasBooted = true;
        MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Identity Edge has successfully booted up");
        return true;
    }

    /**
     * Clears all identities and regenerates a new ECID value, then saves the new identities to persistence.
     */
    void resetIdentifiers() {
        // TODO: AMSDK-11208 Determine if we should dispatch consent event

        identityProperties = new IdentityEdgeProperties();
        identityProperties.setECID(new ECID());
        identityProperties.setECIDSecondary(null);
        IdentityEdgeStorageService.savePropertiesToPersistence(identityProperties);

        // TODO: AMSDK-11208 Use return value to tell IdentityEdge to dispatch consent ad id update
    }


    /**
     * Update the customer identifiers by merging the passed in {@link IdentityMap} with the current identifiers present in {@link #identityProperties}.
     *
     * @param map the {@code IdentityMap} containing customer identifiers to add or update with the current customer identifiers
     */
    void updateCustomerIdentifiers(final IdentityMap map) {
        identityProperties.updateCustomerIdentifiers(map);
        IdentityEdgeStorageService.savePropertiesToPersistence(identityProperties);
    }

    /**
     * Remove customer identifiers specified in passed in {@link IdentityMap} from the current identifiers present in {@link #identityProperties}.
     *
     * @param map the {@code IdentityMap} with items to remove from current identifiers
     */
    void removeCustomerIdentifiers(final IdentityMap map) {
        identityProperties.removeCustomerIdentifiers(map);
        IdentityEdgeStorageService.savePropertiesToPersistence(identityProperties);
    }

    /**
     * Update the legacy ECID property with {@code legacyEcid} provided it does not equal the primary or secondary ECIDs
     * currently in {@code IdentityEdgePoperties}.
     * @param legacyEcid the current ECID from the direct Identity extension
     * @return true if the legacy ECID was updated in {@code IdentityEdgeProperties}
     */
    boolean updateLegacyExperienceCloudId(final ECID legacyEcid) {
        if (legacyEcid == identityProperties.getECID() || legacyEcid == identityProperties.getECIDSecondary()) {
            return false;
        }

        identityProperties.setECIDSecondary(legacyEcid);
        IdentityEdgeStorageService.savePropertiesToPersistence(identityProperties);
        MobileCore.log(LoggingMode.DEBUG, LOG_TAG,"Identity direct ECID updated to '" + legacyEcid + "', updating the IdentityMap");
        return true;
    }

}
