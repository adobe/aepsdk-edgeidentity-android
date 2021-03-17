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

    // The current Experience Cloud ID
    private ECID ecid;
    private IdentityMap identityMap = new IdentityMap();

    IdentityEdgeProperties() { }

    /**
     * Creates a identity edge properties instance based on the map
     *
     * @param xdmData a map representing an identity edge properties instance
     */
    IdentityEdgeProperties(final Map<String, Object> xdmData) {
        if (Utils.isNullOrEmpty(xdmData)) {
            return;
        }

        identityMap = IdentityMap.fromData(xdmData);
        if (identityMap != null) {
            final List<IdentityItem> ecidItems = identityMap.getIdentityItemsForNamespace(IdentityEdgeConstants.Namespaces.ECID);
            boolean containsEcid = ecidItems != null && !ecidItems.isEmpty() && ecidItems.get(0).getId() != null;
            if (containsEcid) {
                ecid = new ECID(ecidItems.get(0).getId());
            }
        }
    }

    /**
     * Sets the current {@link ECID}
     *
     * @param ecid the new {@link ECID}
     */
    void setECID(final ECID ecid) {
        this.ecid = ecid;
        IdentityItem ecidItem = new IdentityItem(ecid.toString(), AuthenticationState.AMBIGUOUS, true);
        identityMap.addItem(IdentityEdgeConstants.Namespaces.ECID ,ecidItem);
    }

    /**
     * Retrieves the current {@link ECID}
     *
     * @return current {@link ECID}
     */
    ECID getECID() {
        return ecid;
    }

    /**
     * Update the customer identifiers by merging the passed in {@link IdentityMap} with the current identifiers.
     * <p>
     * Any identifier in the passed in {@link IdentityMap} which has the same id in the same namespace will update the current identifier.
     * Any new identifier in the passed in {@link IdentityMap} will be added to the current identifiers
     * Certain namespaces are not allowed to be modified and if exist in the given customer identifiers will be removed before the update operation is executed.
     * The namespaces which cannot be modified through this function call include:
     * - ECID
     * - IDFA
     * - GAID
     *
     * @param map the {@link IdentityMap} containing customer identifiers to add or update with the current customer identifiers
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
     * @param map the {@link IdentityMap} with items to remove from current identifiers
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
        for (final String namespace : reservedNamespaces) {
            if (identityMap.removeAllIdentityItemsForNamespace(namespace)) {
                MobileCore.log(LoggingMode.DEBUG, LOG_TAG, String.format("Updating/Removing identifiers in namespace %s is not allowed.", namespace));
            }
        }
    }

}
