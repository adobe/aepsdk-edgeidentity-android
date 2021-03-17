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
import java.util.Iterator;
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
public class IdentityMap {
    private static final String LOG_TAG = "IdentityMap";

    private Map<String, List<IdentityItem>> identityItems;

    IdentityMap() {
        identityItems = new HashMap<>();
    }

    /**
     * Gets the {@link IdentityItem}s for the namespace
     * returns an empty list if no {@link IdentityItem}s were found for the namespace
     *
     * @param namespace namespace for the id
     * @return IdentityItem for the namespace,
     */
    public List<IdentityItem>  getIdentityItemsForNamespace(final String namespace) {
        final List<IdentityItem> copyItems = new ArrayList<>();
        if(Utils.isNullOrEmpty(namespace)) {
            return copyItems;
        }

        final List<IdentityItem> items = identityItems.get(namespace);
        if (items == null) {
            return copyItems;
        }

        for (IdentityItem item : items) {
            copyItems.add(new IdentityItem((item)));
        }

        return copyItems;
    }

    /**
     * Add an identity item which is used to clearly distinguish entities that are interacting
     * with digital experiences.
     *
     * @param namespace the namespace integration code or namespace ID of the identity
     * @param item      {@link IdentityItem} to be added to the namespace
     * @see <a href="https://github.com/adobe/xdm/blob/master/docs/reference/context/identityitem.schema.md">IdentityItem Schema</a>
     */
    public void addItem(final String namespace, final IdentityItem item) {
        if (item == null) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "IdentityMap add item ignored as must contain a non-null IdentityItem.");
            return;
        }

        if (Utils.isNullOrEmpty(namespace)) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
                    "IdentityMap add item ignored as must contain a non-null/non-empty namespace.");
            return;
        }

        addItemToMap(namespace, item);
    }

    /**
     * Remove a single {@link IdentityItem} from this map.
     *
     * @param namespace The {@code IdentityItem} to remove from the given namespace
     * @param item      {@link IdentityItem} to be added to the namespace
     */
    public void removeItem(final String namespace, final IdentityItem item) {
        if (item == null) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "IdentityMap remove item ignored as must contain a non-null IdentityItem.");
            return;
        }

        if (Utils.isNullOrEmpty(namespace)) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
                    "IdentityMap remove item ignored as must contain a non-null/non-empty namespace.");
            return;
        }

        removeItemFromMap(namespace, item);
    }

    // ========================================================================================
    // protected methods
    // ========================================================================================

    /**
     * @return a {@link Map} representing this {@link IdentityMap} object
     */
    Map<String, List<Map<String, Object>>> toObjectMap() {
        final Map<String, List<Map<String, Object>>> map = new HashMap<>();

        for (String namespace : identityItems.keySet()) {
            final List<Map<String, Object>> namespaceIds = new ArrayList<>();

            for (IdentityItem identityItem : identityItems.get(namespace)) {
                namespaceIds.add(identityItem.toObjectMap());
            }

            map.put(namespace, namespaceIds);
        }

        return map;
    }

    /**
     * Merge the given map on to this {@link IdentityMap}. Any {@link IdentityItem} in map which shares the same
     * namespace and id as an item in this {@code IdentityMap} will replace that {@code IdentityItem}.
     *
     * @param map {@link IdentityMap} to be merged into this object
     */
    void merge(final IdentityMap map) {
        if (map == null) {
            return;
        }

        for (final String namespace : map.identityItems.keySet()) {
            for (IdentityItem identityItem : map.identityItems.get(namespace)) {
                addItem(namespace, identityItem);
            }
        }
    }

    /**
     * Remove identities present in passed in map from this {@link IdentityMap}.
     * Identities are removed which match the same namespace and id.
     *
     * @param map Identities to remove from this {@code IdentityMap}
     */
    void remove(final IdentityMap map) {
        if (map == null) {
            return;
        }

        for (final String namespace : map.identityItems.keySet()) {
            for (IdentityItem identityItem : map.identityItems.get(namespace)) {
                removeItem(namespace, identityItem);
            }
        }
    }

    boolean removeAllIdentityItemsForNamespace(final String namespace) {
        if (identityItems.containsKey(namespace)) {
            identityItems.remove(namespace);
            return true;
        }

        return false;
    }

    /**
     * Use this method to cast the {@code IdentityMap} as eventData for an SDK Event.
     *
     * @return {@link Map<String,Object>} representation of IdentityMap
     */
    Map<String, Object> asEventData() {
        return new HashMap<String, Object>(identityItems);
    }

    static IdentityMap fromData(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        final Map<String, Object> identityMapDict = (HashMap<String, Object>) data.get(IdentityEdgeConstants.XDMKeys.IDENTITY_MAP);
        if (identityMapDict == null) {
            return null;
        }

        final IdentityMap identityMap = new IdentityMap();

        for (final String namespace : identityMapDict.keySet()) {
            try {
                final ArrayList<HashMap<String, Object>> idArr = (ArrayList<HashMap<String, Object>>) identityMapDict.get(namespace);
                for (Object idMap : idArr) {
                    final IdentityItem item = IdentityItem.fromData((Map<String, Object>) idMap);
                    if (item != null) {
                        identityMap.addItemToMap(namespace, item);
                    }
                }
            } catch (ClassCastException e) {
                MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Failed to create IdentityMap from data.");
            }
        }

        return identityMap;
    }

    // ========================================================================================
    // private methods
    // ========================================================================================
    private void addItemToMap(final String namespace, final IdentityItem item) {
        // check if namespace exists
        final List<IdentityItem> itemList;

        if (identityItems.containsKey(namespace)) {
            itemList = identityItems.get(namespace);
        } else {
            itemList = new ArrayList<>();
        }

        itemList.add(item);
        this.identityItems.put(namespace, itemList);
    }

    private void removeItemFromMap(final String namespace, final IdentityItem item) {
        // check if namespace exists
        if (!identityItems.containsKey(namespace)) {
            return;
        }

        final List<IdentityItem> itemList = identityItems.get(namespace);

        Iterator<IdentityItem> it = itemList.iterator();
        while (it.hasNext()) {
            IdentityItem eachItem = it.next();
            if (item.equals(eachItem)) {
                it.remove();
                break;
            }
        }

        if(itemList.isEmpty()) {
            identityItems.remove(namespace);
        }
    }
}
