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

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class IdentityItemTests {

    @Test
    public void testIdentityItem_toObjectMap_full() {
        // setup
        IdentityItem item = new IdentityItem("id", AuthenticationState.AUTHENTICATED, true);

        // test
        Map<String, Object> data = item.toObjectMap();

        // verify
        assertEquals("id", (String) data.get("id"));
        assertEquals("AUTHENTICATED", (String) data.get("authenticationState"));
        assertEquals(true, (boolean) data.get("primary"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIdentityItem_toObjectMap_missingId() {
        // setup
        IdentityItem item = new IdentityItem(null, AuthenticationState.AUTHENTICATED, true);

        // test
        Map<String, Object> data = item.toObjectMap();
    }

    @Test
    public void testIdentityItem_toObjectMap_missingAuthState() {
        // setup
        IdentityItem item = new IdentityItem("id", null, true);

        // test
        Map<String, Object> data = item.toObjectMap();

        // verify
        assertEquals("id", (String) data.get("id"));
        assertEquals("AMBIGUOUS", (String) data.get("authenticationState"));
        assertEquals(true, (boolean) data.get("primary"));
    }

    @Test
    public void testIdentityItem_fromData_full() {
        // setup
        Map<String, Object> map = new HashMap<>();
        map.put("id", "test-id");
        map.put("authenticationState", "loggedOut");
        map.put("primary", true);

        // test
        IdentityItem item = IdentityItem.fromData(map);

        // verify
        assertEquals("test-id", item.getId());
        assertEquals("LOGGED_OUT", item.getAuthenticationState().toString());
        assertEquals(true, item.isPrimary());
    }

    @Test
    public void testIdentityItem_fromData_missingAuthState() {
        // setup
        Map<String, Object> map = new HashMap<>();
        map.put("id", "test-id");
        map.put("primary", true);

        // test
        IdentityItem item = IdentityItem.fromData(map);

        // verify
        assertEquals("test-id", item.getId());
        assertEquals("AMBIGUOUS", item.getAuthenticationState().toString());
        assertEquals(true, item.isPrimary());
    }

    @Test
    public void testIdentityItem_fromData_missingPrimary() {
        // setup
        Map<String, Object> map = new HashMap<>();
        map.put("id", "test-id");
        map.put("authenticationState", "loggedOut");

        // test
        IdentityItem item = IdentityItem.fromData(map);

        // verify
        assertEquals("test-id", item.getId());
        assertEquals("LOGGED_OUT", item.getAuthenticationState().toString());
        assertEquals(false, item.isPrimary());
    }

    @Test
    public void testIdentityItem_isEqualShouldReturnTrue() {
        IdentityItem item1 = new IdentityItem("id", AuthenticationState.AMBIGUOUS , false);
        IdentityItem item2 = new IdentityItem("id", AuthenticationState.AUTHENTICATED , true);

        assertTrue(item1.equals(item2));
    }

    @Test
    public void testIdentityItem_isEqualShouldReturnFalse() {
        IdentityItem item1 = new IdentityItem("id", AuthenticationState.AMBIGUOUS , false);
        IdentityItem item2 = new IdentityItem("id2", AuthenticationState.AUTHENTICATED , true);

        assertFalse(item1.equals(item2));
    }
}
