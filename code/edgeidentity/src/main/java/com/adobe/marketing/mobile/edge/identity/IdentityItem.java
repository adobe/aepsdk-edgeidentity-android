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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an identity item
 */
public final class IdentityItem {
    private final String id;
    private final AuthenticatedState authenticatedState;
    private final boolean primary;

    private static final String LOG_TAG = "IdentityItem";

    /**
     * Creates a new {@link IdentityItem}
     * @param id id for the item
     * @param authenticatedState {@link AuthenticatedState} for the item
     * @param primary primary flag for the item
     * @throws IllegalArgumentException if id is null
     */
    public IdentityItem(final String id, final AuthenticatedState authenticatedState, final boolean primary) {
        if (id == null) {
            throw new IllegalArgumentException("id must be non-null");
        }
        this.id = id;
        this.authenticatedState = authenticatedState != null ? authenticatedState : AuthenticatedState.AMBIGUOUS;
        this.primary = primary;
    }

    /**
     * Creates a new {@link IdentityItem} with default values
     * authenticatedState is set to AMBIGUOUS
     * primary is set to false
     * @param id the id for this {@link IdentityItem}
     */
    public IdentityItem(final String id) {
        this(id, AuthenticatedState.AMBIGUOUS, false);
    }

    /**
     * Creates a copy of item
     * @param item A {@link IdentityItem} to be copied
     */
    public IdentityItem(final IdentityItem item) {
        this(item.id, item.authenticatedState, item.primary);
    }

    /**
     * Converts this object into a map representation
     * @return this object in a map representation
     */
    Map<String, Object> toObjectMap() {
        Map<String, Object> map = new HashMap<>();
        if (id != null) {
            map.put(IdentityConstants.XDMKeys.ID, id);
        }

        if (authenticatedState != null) {
            map.put(IdentityConstants.XDMKeys.AUTHENTICATED_STATE, authenticatedState.getName());
        } else {
            map.put(IdentityConstants.XDMKeys.AUTHENTICATED_STATE, AuthenticatedState.AMBIGUOUS.getName());
        }

        map.put(IdentityConstants.XDMKeys.PRIMARY, primary);
        return map;
    }

    /**
     * @return The id for this identity item
     */
    public String getId() {
        return id;
    }

    /**
     * @return Current {@link AuthenticatedState} for this item
     */
    public AuthenticatedState getAuthenticatedState() {
        return authenticatedState;
    }

    /**
     * @return true if this item is primary, false otherwise
     */
    public boolean isPrimary() {
        return primary;
    }

    /**
     * Creates an {@link IdentityItem} from the data
     * @param data the data representing an {@link IdentityItem}
     * @return an initialized {@link IdentityItem} based on the data, null if data is invalid
     */
    static IdentityItem fromData(final Map<String, Object> data) {
        if (data == null) { return null; }

        try {
            final String id = (String) data.get(IdentityConstants.XDMKeys.ID);
            AuthenticatedState authenticatedState = AuthenticatedState.fromString((String) data.get(IdentityConstants.XDMKeys.AUTHENTICATED_STATE));
            if (authenticatedState == null) {
                authenticatedState = AuthenticatedState.AMBIGUOUS;
            }

            boolean primary = false;
            if (data.get(IdentityConstants.XDMKeys.PRIMARY) != null) {
                primary = (boolean) data.get(IdentityConstants.XDMKeys.PRIMARY);
            }

            return new IdentityItem(id, authenticatedState, primary);
        } catch (ClassCastException e) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Failed to create IdentityItem from data.");
            return null;
        }
    }

    @Override
    public String toString() {
        return "{"
                + "\"" + IdentityConstants.XDMKeys.ID + "\": \"" + id + "\", "
                + "\"" + IdentityConstants.XDMKeys.AUTHENTICATED_STATE + "\": \"" + (authenticatedState == null ? "null" : authenticatedState.getName()) + "\", "
                + "\"" + IdentityConstants.XDMKeys.PRIMARY + "\": " + primary
                + "}";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdentityItem that = (IdentityItem) o;
        return id.equalsIgnoreCase(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
