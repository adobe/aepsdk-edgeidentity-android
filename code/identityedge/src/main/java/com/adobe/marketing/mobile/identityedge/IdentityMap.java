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
 * Defines a map containing a set of end user identities, keyed on either namespace integration
 * code or the namespace ID of the identity. Within each namespace, the identity is unique.
 * The values of the map are an array, meaning that more than one identity of each namespace
 * may be carried.
 *
 * @see <a href="https://github.com/adobe/xdm/blob/master/docs/reference/context/identitymap.schema.md">IdentityMap Schema</a>
 */
@SuppressWarnings("unused")
class IdentityMap {
    private static final String LOG_TAG = "IdentityMap";

    private static final String JSON_KEY_ID = "id";
    private static final String JSON_KEY_AUTHENTICATION_STATE = "authenticationState";
    private static final String JSON_KEY_PRIMARY = "primary";

    enum AuthenticationState {
        AMBIGUOUS("ambiguous"),
        AUTHENTICATED("authenticated"),
        LOGGED_OUT("loggedOut");

        private String name;

        AuthenticationState(final String name) {
            this.name = name;
        }

        String getName() {
            return name;
        }

        AuthenticationState fromString(final String state) {
            if ("authenticated".equalsIgnoreCase(state)) {
                return AUTHENTICATED;
            } else if ("loggedOut".equalsIgnoreCase(state)) {
                return LOGGED_OUT;
            } else {
                return AMBIGUOUS;
            }
        }
    }

    private Map<String, List<Map<String, Object>>> identityItems;

    IdentityMap() {
        identityItems = new HashMap<>();
    }

    /**
     * Gets the IdentityItem for the namespace
     * @param namespace namespace for the id
     * @return IdentityItem for the namespace, null if not found
     */
    List<Map<String, Object>> getIdentityItemForNamespace(final String namespace) {
        return identityItems.get(namespace);
    }

    /**
     * Add an identity item which is used to clearly distinguish entities that are interacting
     * with digital experiences.
     *
     * @param namespace the namespace integration code or namespace ID of the identity
     * @param id identity of the consumer in the related namespace
     * @param state the state this identity is authenticated as for this observed ExperienceEvent.
     *              Default is {@link AuthenticationState.AMBIGUOUS}.
     * @param primary Indicates this identity is the preferred identity. Is used as a hint to help
     *                systems better organize how identities are queried. Default is false.
     *
     * @see <a href="https://github.com/adobe/xdm/blob/master/docs/reference/context/identityitem.schema.md">IdentityItem Schema</a>
     */
    void addItem(final String namespace, final String id, final AuthenticationState state, final boolean primary) {
        if (Utils.isNullOrEmpty(id)) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "IdentityMap add item ignored as must contain a non-null/non-empty id.");
            return;
        }

        if (Utils.isNullOrEmpty(namespace)) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
                    "IdentityMap add item ignored as must contain a non-null/non-empty namespace.");
            return;
        }

        Map<String, Object> item = new HashMap<>();
        item.put(JSON_KEY_ID, id);
        item.put(JSON_KEY_PRIMARY, primary);

        if (state != null) {
            item.put(JSON_KEY_AUTHENTICATION_STATE, state.getName());
        }

        addItemToMap(namespace, item);
    }

    /**
     * Add an identity item which is used to clearly distinguish entities that are interacting
     * with digital experiences. Uses default authentication state and primary as defined by the Experience Platform.
     *
     * @param namespace the namespace integration code or namespace ID of the identity
     * @param id identity of the consumer in the related namespace
     *
     * @see <a href="https://github.com/adobe/xdm/blob/master/docs/reference/context/identityitem.schema.md">IdentityItem Schema</a>
     */
    void addItem(final String namespace, final String id) {
        if (Utils.isNullOrEmpty(id)) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "IdentityMap add item ignored as must contain a non-null/non-empty id.");
            return;
        }

        if (Utils.isNullOrEmpty(namespace)) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
                    "IdentityMap add item ignored as must contain a non-null/non-empty namespace.");
            return;
        }

        Map<String, Object> item = new HashMap<>();
        item.put(JSON_KEY_ID, id);

        addItemToMap(namespace, item);
    }

    private void addItemToMap(final String namespace, final Map<String, Object> item) {
        // check if namespace exists
        List<Map<String, Object>> itemList;

        if (identityItems.containsKey(namespace)) {
            itemList = identityItems.get(namespace);
        } else {
            itemList = new ArrayList<>();
        }

        itemList.add(item);
        this.identityItems.put(namespace, itemList);
    }

    Map<String, List<Map<String, Object>>> toObjectMap() {
        return identityItems;
    }

    static IdentityMap fromData(Map<String, Object> data) {
        if (data == null) { return null; }
        final Map<String, Object> identityMapDict = (HashMap<String, Object>) data.get(IdentityEdgeConstants.XDMKeys.IDENTITY_MAP);
        if (identityMapDict == null) { return null; }

        final IdentityMap identityMap = new IdentityMap();

        for (String namespace : identityMapDict.keySet()) {
            try {
                final ArrayList<HashMap<String, Object>> idArr = (ArrayList<HashMap<String, Object>>) identityMapDict.get(namespace);
                for (Object idMap: idArr) {
                    identityMap.addItemToMap(namespace, (Map<String, Object>) idMap);
                }
            } catch (ClassCastException e) {
                MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Failed to create IdentityMap from data.");
            }
        }

        return identityMap;
    }
}