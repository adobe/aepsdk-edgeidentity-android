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
    private static final List<String> reservedNamespaces = new ArrayList<String>() {{
        add(IdentityEdgeConstants.Namespaces.ECID);
        add(IdentityEdgeConstants.Namespaces.GAID);
        add(IdentityEdgeConstants.Namespaces.IDFA);
    }};

    private final IdentityMap identityMap;

    IdentityEdgeProperties() {
        this.identityMap = new IdentityMap();
    }

    /**
     * Creates a identity edge properties instance based on the map
     *
     * @param xdmData a map representing an identity edge properties instance
     */
    IdentityEdgeProperties(final Map<String, Object> xdmData) {
        IdentityMap map = IdentityMap.fromData(xdmData);
        this.identityMap = map == null ? new IdentityMap() : map; // always keep an empty identity map so there is no need for null check
    }

    /**
     * Sets the current {@link ECID}
     *
     * @param newEcid the new {@code ECID}
     */
    void setECID(final ECID newEcid) {
        // delete the previous ECID from the identity map if exist
        final ECID currentECID = getECID();
        if (currentECID != null) {
            final IdentityItem previousECIDItem = new IdentityItem(currentECID.toString());
            identityMap.removeItem(previousECIDItem, IdentityEdgeConstants.Namespaces.ECID);
        }

        // if primary ecid is null, clear off all the existing ECID's
        if (newEcid == null) {
            setECIDSecondary(null);
            identityMap.clearItemsForNamespace(IdentityEdgeConstants.Namespaces.ECID);
        } else {
            // And add the new primary Ecid as a first element of Identity map
            final IdentityItem newECIDItem = new IdentityItem(newEcid.toString(), AuthenticatedState.AMBIGUOUS, false);
            identityMap.addItem(newECIDItem, IdentityEdgeConstants.Namespaces.ECID, true);
        }
    }

    /**
     * Retrieves the current {@link ECID}
     *
     * @return current {@code ECID}
     */
    ECID getECID() {
        final List<IdentityItem> ecidItems = identityMap.getIdentityItemsForNamespace(IdentityEdgeConstants.Namespaces.ECID);
        if (ecidItems != null && !ecidItems.isEmpty() && ecidItems.get(0) != null && !Utils.isNullOrEmpty(ecidItems.get(0).getId())) {
            return new ECID(ecidItems.get(0).getId());
        }
        return null;
    }

    /**
     * Sets a secondary {@link ECID}
     *
     * @param newSecondaryEcid a new secondary {@code ECID}
     */
    void setECIDSecondary(final ECID newSecondaryEcid) {
        // delete the previous secondary ECID from the identity map if exist
        final ECID ecidSecondary = getECIDSecondary();
        if (ecidSecondary != null) {
            final IdentityItem previousECIDItem = new IdentityItem(ecidSecondary.toString());
            identityMap.removeItem(previousECIDItem, IdentityEdgeConstants.Namespaces.ECID);
        }

        // do not set secondary ECID if primary ECID is not set
        if (getECID() == null) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Cannot set secondary ECID value as no primary ECID exists.");
            return;
        }

        // add the new secondary ECID to Identity map
        if (newSecondaryEcid != null) {
            final IdentityItem newSecondaryECIDItem = new IdentityItem(newSecondaryEcid.toString(), AuthenticatedState.AMBIGUOUS, false);
            identityMap.addItem(newSecondaryECIDItem, IdentityEdgeConstants.Namespaces.ECID);
        }
    }

    /**
     * Retrieves the secondary {@link ECID}.
     *
     * @return secondary {@code ECID}
     */
    ECID getECIDSecondary() {
        final List<IdentityItem> ecidItems = identityMap.getIdentityItemsForNamespace(IdentityEdgeConstants.Namespaces.ECID);
        if (ecidItems != null && ecidItems.size() > 1 && ecidItems.get(1) != null && !Utils.isNullOrEmpty(ecidItems.get(1).getId())) {
            return new ECID(ecidItems.get(1).getId());
        }
        return null;
    }

    /**
     * Update the customer identifiers by merging the passed in {@link IdentityMap} with the current identifiers.
     * <p>
     * Any identifier in the passed in {@code IdentityMap} which has the same id in the same namespace will update the current identifier.
     * Any new identifier in the passed in {@code IdentityMap} will be added to the current identifiers
     * Certain namespaces are not allowed to be modified and if exist in the given customer identifiers will be removed before the update operation is executed.
     * The namespaces which cannot be modified through this function call include:
     * - ECID
     * - IDFA
     * - GAID
     *
     * @param map the {@code IdentityMap} containing customer identifiers to add or update with the current customer identifiers
     */
    void updateCustomerIdentifiers(final IdentityMap map) {
        removeIdentitiesWithReservedNamespaces(map);
        identityMap.merge(map);
    }

    /**
     * Remove customer identifiers specified in passed in {@link IdentityMap} from the current identifiers.
     * <p>
     * Identifiers with following namespaces are prohibited from removing using the API
     * - ECID
     * - IDFA
     * - GAID
     *
     * @param map the {@code IdentityMap} with items to remove from current identifiers
     */
    void removeCustomerIdentifiers(final IdentityMap map) {
        removeIdentitiesWithReservedNamespaces(map);
        identityMap.remove(map);
    }

    /**
     * Converts this into an event data representation in XDM format
     *
     * @param allowEmpty If this {@link IdentityEdgeProperties} contains no data, return a dictionary with a single {@link IdentityMap} key
     * @return A dictionary representing this in XDM format
     */
    Map<String, Object> toXDMData(final boolean allowEmpty) {
        final Map<String, Object> map = new HashMap<>();

        final Map<String, List<Map<String, Object>>> dict = identityMap.toObjectMap();
        if (dict != null && (!dict.isEmpty() || allowEmpty)) {
            map.put(IdentityEdgeConstants.XDMKeys.IDENTITY_MAP, dict);
        }

        return map;
    }

    /**
     * Filter out any items contained in reserved namespaces from the given {@link IdentityMap}.
     * The list of reserved namespaces can be found at {@link #reservedNamespaces}.
     *
     * @param identityMap the {@code IdentityMap} to filter out items contained in reserved namespaces.
     */
    private void removeIdentitiesWithReservedNamespaces(final IdentityMap identityMap) {
        for (final String reservedNamespace : reservedNamespaces) {
            if (identityMap.clearItemsForNamespace(reservedNamespace)) {
                MobileCore.log(LoggingMode.DEBUG, LOG_TAG, String.format("Updating/Removing identifiers in namespace %s is not allowed.", reservedNamespace));
            }
        }
    }

}
