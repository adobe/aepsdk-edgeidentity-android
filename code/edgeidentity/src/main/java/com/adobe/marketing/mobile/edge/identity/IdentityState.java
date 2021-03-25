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

import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;


/**
 * Manages the business logic of this Identity extension
 */
class IdentityState {
    private static String LOG_TAG = "IdentityState";
    private IdentityProperties identityProperties;

    /**
     * Creates a new {@link IdentityState} with the given {@link IdentityProperties}
     *
     * @param identityProperties identity properties
     */
    IdentityState(final IdentityProperties identityProperties) {
        this.identityProperties = identityProperties;
    }

    /**
     * @return The current {@link IdentityProperties} for this identity state
     */
    IdentityProperties getIdentityProperties() {
        return identityProperties;
    }


    /**
     * Completes init for the Identity extension.
     * Attempts to load the already persisted identities from persistence into {@link #identityProperties}
     * If no ECID is loaded from persistence (ideally meaning first launch), then we attempt to read ECID for the direct Identity Extension.
     * If there is no ECID loaded from the persistence of direct Identity Extension, then and new ECID is generated and persisted finishing the bootUp sequence.
     */
    void bootUp() {
        // Load properties from local storage
        identityProperties = IdentityStorageService.loadPropertiesFromPersistence();

        if (identityProperties == null) {
            identityProperties = new IdentityProperties();
        }

        // Generate new ECID on first launch
        if (identityProperties.getECID() == null) {
            final ECID directIdentityEcid = IdentityStorageService.loadEcidFromDirectIdentityPersistence();
            if (directIdentityEcid == null) {
                identityProperties.setECID(new ECID());
                MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Bootup - Generating new ECID '" + identityProperties.getECID().toString() + "'");
            } else {
                identityProperties.setECID(directIdentityEcid);
                MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Bootup - Loading ECID from direct Identity extension '" + directIdentityEcid + "'");
            }
            IdentityStorageService.savePropertiesToPersistence(identityProperties);
        }

        MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Edge Identity has successfully booted up");
    }

    /**
     * Clears all identities and regenerates a new ECID value, then saves the new identities to persistence.
     */
    void resetIdentifiers() {
        // TODO: AMSDK-11208 Determine if we should dispatch consent event

        identityProperties = new IdentityProperties();
        identityProperties.setECID(new ECID());
        identityProperties.setECIDSecondary(null);
        IdentityStorageService.savePropertiesToPersistence(identityProperties);

        // TODO: AMSDK-11208 Use return value to tell Identity to dispatch consent ad id update
    }


    /**
     * Update the customer identifiers by merging the passed in {@link IdentityMap} with the current identifiers present in {@link #identityProperties}.
     *
     * @param map the {@code IdentityMap} containing customer identifiers to add or update with the current customer identifiers
     */
    void updateCustomerIdentifiers(final IdentityMap map) {
        identityProperties.updateCustomerIdentifiers(map);
        IdentityStorageService.savePropertiesToPersistence(identityProperties);
    }

    /**
     * Remove customer identifiers specified in passed in {@link IdentityMap} from the current identifiers present in {@link #identityProperties}.
     *
     * @param map the {@code IdentityMap} with items to remove from current identifiers
     */
    void removeCustomerIdentifiers(final IdentityMap map) {
        identityProperties.removeCustomerIdentifiers(map);
        IdentityStorageService.savePropertiesToPersistence(identityProperties);
    }

    /**
     * Update the legacy ECID property with {@code legacyEcid} provided it does not equal the primary or secondary ECIDs
     * currently in {@code IdentityProperties}.
     *
     * @param legacyEcid the current ECID from the direct Identity extension
     * @return true if the legacy ECID was updated in {@code IdentityProperties}
     */
    boolean updateLegacyExperienceCloudId(final ECID legacyEcid) {
        final ECID ecid = identityProperties.getECID();
        final ECID ecidSecondary = identityProperties.getECIDSecondary();

        if ((legacyEcid != null) && (legacyEcid.equals(ecid) || legacyEcid.equals(ecidSecondary))) {
            return false;
        }

        // no need to clear secondaryECID if its already null
         if (legacyEcid == null && ecidSecondary == null){
            return false;
        }

        identityProperties.setECIDSecondary(legacyEcid);
        IdentityStorageService.savePropertiesToPersistence(identityProperties);
        MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Identity direct ECID updated to '" + legacyEcid + "', updating the IdentityMap");
        return true;
    }

}
