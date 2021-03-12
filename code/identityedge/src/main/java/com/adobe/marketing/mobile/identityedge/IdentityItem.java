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
import java.util.Map;
import java.util.Objects;

/**
 * Represents an identity item
 */
public final class IdentityItem {
    private String id;
    private AuthenticationState authenticationState;
    private boolean primary;

    private static final String LOG_TAG = "IdentityItem";
    private static final String JSON_KEY_ID = "id";
    private static final String JSON_KEY_AUTHENTICATION_STATE = "authenticationState";
    private static final String JSON_KEY_PRIMARY = "primary";

    /**
     * Creates a new {@link IdentityItem}
     * @param id id for the item
     * @param authenticationState {@link AuthenticationState} for the item
     * @param primary primary flag for the item
     */
    public IdentityItem(final String id, final AuthenticationState authenticationState, final boolean primary) {
        if (id == null) {
            throw new IllegalArgumentException("id must be non-null");
        }
        this.id = id;
        this.authenticationState = authenticationState;
        if (authenticationState == null) {
            this.authenticationState = AuthenticationState.AMBIGUOUS;
        }
        this.primary = primary;
    }

    /**
     * Creates a new {@link IdentityItem} with default values
     * authenticationState is set to {@link AuthenticationState.AMBIGUOUS}
     * primary is set to false
     * @param id the id for this {@link IdentityItem}
     */
    public IdentityItem(final String id) {
        this(id, AuthenticationState.AMBIGUOUS, false);
    }

    /**
     * Converts this object into a map representation
     * @return this object in a map representation
     */
    Map<String, Object> toObjectMap() {
        Map<String, Object> map = new HashMap<>();
        if (id != null) {
            map.put(JSON_KEY_ID, id);
        }

        if (authenticationState != null) {
            map.put(JSON_KEY_AUTHENTICATION_STATE, authenticationState.toString());
        } else {
            map.put(JSON_KEY_AUTHENTICATION_STATE, AuthenticationState.AMBIGUOUS.toString());
        }

        map.put(JSON_KEY_PRIMARY, primary);
        return map;
    }

    /**
     * @return The id for this identity item
     */
    public String getId() {
        return id;
    }

    /**
     * @return Current {@link AuthenticationState} for this item
     */
    public AuthenticationState getAuthenticationState() {
        return authenticationState;
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
            final String id = (String) data.get(JSON_KEY_ID);
            AuthenticationState authenticationState = AuthenticationState.fromString((String) data.get(JSON_KEY_AUTHENTICATION_STATE));
            if (authenticationState == null) {
                authenticationState = AuthenticationState.AMBIGUOUS;
            }

            boolean primary = false;
            if (data.get(JSON_KEY_PRIMARY) != null) {
                primary = (boolean) data.get(JSON_KEY_PRIMARY);
            }

            return new IdentityItem(id, authenticationState, primary);
        } catch (ClassCastException e) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Failed to create IdentityItem from data.");
            return null;
        }
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
