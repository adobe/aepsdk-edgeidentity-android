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

import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class IdentityMapTests {

    @Test
    public void test_AddItem() {
        // test
        IdentityMap map = new IdentityMap();
        map.addItem("location",  new  IdentityItem("California"));

        // verify
        assertEquals("California", map.toObjectMap().get("location").get(0).get("id"));
    }

    @Test
    public void test_AddItem_InvalidInputs() {
        // test
        IdentityMap map = new IdentityMap();
        map.addItem("",  new  IdentityItem("California"));
        map.addItem(null,  new  IdentityItem("California"));
        map.addItem("namespace", null);

        // verify
        assertTrue(map.toObjectMap().isEmpty());
    }

    @Test
    public void test_getIdentityItemsForNamespace() {
        // setup
        IdentityMap sampleUserMap = buildSampleIdentityMap(); // 2 items with namespace "Location", 3 items with namespace "Login"

        // test
        List<IdentityItem> locationItems = sampleUserMap.getIdentityItemsForNamespace("location");

        // verify
        assertEquals(2, locationItems.size());
        assertEquals("280 Highway Lane", locationItems.get(0).getId());
        assertEquals("California", locationItems.get(1).getId());

        // verify the login namespace returns 3 items
        assertEquals(3, sampleUserMap.getIdentityItemsForNamespace("login").size());
    }


    @Test
    public void test_getIdentityItemsForNamespace_InvalidInputs() {
        // setup
        IdentityMap sampleUserMap = buildSampleIdentityMap(); // 2 items with namespace "Location", 3 items with namespace "Login"

        // test 1
        List<IdentityItem> items = sampleUserMap.getIdentityItemsForNamespace("");
        assertEquals(0,items.size());

        // test 2
        items = sampleUserMap.getIdentityItemsForNamespace(null);
        assertEquals(0,items.size());

        // test 3
        items = sampleUserMap.getIdentityItemsForNamespace("unavailable");
        assertEquals(0,items.size());
    }


    @Test
    public void test_RemoveItem() {
        // setup
        IdentityMap sampleUserMap = buildSampleIdentityMap(); // 2 items with namespace "Location", 3 items with namespace "Login"

        // test
        sampleUserMap.removeItem("location", new IdentityItem("California"));

        // verify
        assertEquals(1, sampleUserMap.toObjectMap().get("location").size());
        assertEquals(3, sampleUserMap.toObjectMap().get("login").size());

        // test 2
        sampleUserMap.removeItem("location", new IdentityItem("280 Highway Lane"));
        sampleUserMap.removeItem("login", new IdentityItem("Student"));

        // verify
        assertNull(sampleUserMap.toObjectMap().get("location"));
        assertEquals(2, sampleUserMap.toObjectMap().get("login").size());
    }

    @Test
    public void test_RemoveItem_InvalidInputs() {
        // setup
        IdentityMap sampleUserMap = buildSampleIdentityMap(); // 2 items with namespace "Location", 3 items with namespace "Login"

        // test
        sampleUserMap.removeItem("", new IdentityItem("California"));
        sampleUserMap.removeItem(null, new IdentityItem("California"));
        sampleUserMap.removeItem("location",null);

        // verify the existing identityMap is unchanged
        assertEquals(2, sampleUserMap.toObjectMap().get("location").size());
        assertEquals(3, sampleUserMap.toObjectMap().get("login").size());
    }



    @Test
    public void test_merge() {
        // setup
        IdentityMap sampleUserMap = buildSampleIdentityMap(); // 2 items with namespace "Location", 3 items with namespace "Login"

        // test
        IdentityMap newMap = new IdentityMap();
        newMap.addItem("location",  new IdentityItem("doorNumber:544"));
        sampleUserMap.merge(newMap);

        // verify the existing identityMap is unchanged
        assertEquals(3, sampleUserMap.toObjectMap().get("location").size());
        assertEquals(3, sampleUserMap.toObjectMap().get("login").size());
    }


    @Test
    public void test_merge_EmptyIdentityMap() {
        // setup
        IdentityMap sampleUserMap = buildSampleIdentityMap(); // 2 items with namespace "Location", 3 items with namespace "Login"

        // test
        sampleUserMap.merge(new IdentityMap());

        // verify the existing identityMap is unchanged
        assertEquals(2, sampleUserMap.toObjectMap().get("location").size());
        assertEquals(3, sampleUserMap.toObjectMap().get("login").size());
    }

    @Test
    public void test_merge_nullMap() {
        // setup
        IdentityMap sampleUserMap = buildSampleIdentityMap(); // 2 items with namespace "location", 3 items with namespace "login"

        // test
        sampleUserMap.merge(null);

        // verify the existing identityMap is unchanged
        assertEquals(2, sampleUserMap.toObjectMap().get("location").size());
        assertEquals(3, sampleUserMap.toObjectMap().get("login").size());
    }


    @Test
    public void test_removeAllIdentityItemsForNamespace() {
        // setup
        IdentityMap sampleUserMap = buildSampleIdentityMap(); // 2 items with namespace "location", 3 items with namespace "login"

        // test
        sampleUserMap.removeAllIdentityItemsForNamespace("location");

        // verify the existing identityMap is unchanged
        assertNull(sampleUserMap.toObjectMap().get("location"));
        assertEquals(3, sampleUserMap.toObjectMap().get("login").size());
    }


    @Test
    public void test_removeAllIdentityItemsForNamespace_InvalidNamespace() {
        // setup
        IdentityMap sampleUserMap = buildSampleIdentityMap(); // 2 items with namespace "location", 3 items with namespace "login"

        // test
        sampleUserMap.removeAllIdentityItemsForNamespace(null);
        sampleUserMap.removeAllIdentityItemsForNamespace("");

        // verify
        assertEquals(2, sampleUserMap.toObjectMap().get("location").size());
        assertEquals(3, sampleUserMap.toObjectMap().get("login").size());
    }

    @Test
    public void test_removeAllIdentityItemsForNamespace_onEmptyMap() {
        // setup
        IdentityMap emptyMap = new IdentityMap();

        // test
        emptyMap.removeAllIdentityItemsForNamespace("location");
        assertTrue(emptyMap.toObjectMap().isEmpty());
    }

    @Test
    public void test_remove() {
        // setup
        IdentityMap sampleUserMap = buildSampleIdentityMap();  // 2 items with namespace "location", 3 items with namespace "login"
        IdentityMap tobeRemovedMap = new IdentityMap();
        tobeRemovedMap.addItem("login", new IdentityItem("Student"));
        tobeRemovedMap.addItem("location", new IdentityItem("California"));

        // test
        sampleUserMap.remove(tobeRemovedMap);

        // verify
        assertEquals(1, sampleUserMap.toObjectMap().get("location").size());
        assertEquals(2, sampleUserMap.toObjectMap().get("login").size());
    }

    @Test
    public void test_remove_NullAndEmptyMap() {
        // setup
        IdentityMap sampleUserMap = buildSampleIdentityMap();  // 2 items with namespace "location", 3 items with namespace "login"

        // test
        sampleUserMap.remove(null);
        sampleUserMap.remove(new IdentityMap());

        // verify that the existing map is unchanged
        assertEquals(2, sampleUserMap.toObjectMap().get("location").size());
        assertEquals(3, sampleUserMap.toObjectMap().get("login").size());
    }

    @Test
    public void test_remove_nonexistentNamespaceAndItems() {
        // setup
        IdentityMap sampleUserMap = buildSampleIdentityMap();  // 2 items with namespace "location", 3 items with namespace "login"
        IdentityMap tobeRemovedMap = new IdentityMap();
        tobeRemovedMap.addItem("nonexistentNamespace", new IdentityItem("California"));
        tobeRemovedMap.addItem("login", new IdentityItem("nonexistentID"));

        // test
        sampleUserMap.remove(tobeRemovedMap);

        // verify that the existing map is unchanged
        assertEquals(2, sampleUserMap.toObjectMap().get("location").size());
        assertEquals(3, sampleUserMap.toObjectMap().get("login").size());
    }


    @Test
    public void test_FromData() throws Exception {
        // setup
        final String jsonStr = "{\n" +
                "      \"identityMap\": {\n" +
                "        \"ECID\": [\n" +
                "          {\n" +
                "            \"id\":" + "randomECID" + ",\n" +
                "            \"authenticatedState\": \"ambiguous\",\n" +
                "            \"primary\": true\n" +
                "          }\n" +
                "        ],\n" +
                "        \"USERID\": [\n" +
                "          {\n" +
                "            \"id\":" + "someUserID" + ",\n" +
                "            \"authenticatedState\": \"authenticated\",\n" +
                "            \"primary\": false\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "}";

        final JSONObject jsonObject = new JSONObject(jsonStr);
        final Map<String, Object> xdmData = Utils.toMap(jsonObject);

        // test
        IdentityMap map = IdentityMap.fromData(xdmData);

        //Map<String, String> flattenedMap = IdentityEdgeTestUtil.flattenMap(map.asEventData());



    }


    private IdentityMap buildSampleIdentityMap(){
        // User Login Identity Items
        IdentityItem email = new IdentityItem("john@doe", AuthenticationState.AUTHENTICATED, true);
        IdentityItem userName = new IdentityItem("John Doe", AuthenticationState.AUTHENTICATED, false);
        IdentityItem accountType = new IdentityItem("Student", AuthenticationState.AUTHENTICATED, false);

        // User Location Address Identity Items
        IdentityItem street = new IdentityItem("280 Highway Lane", AuthenticationState.AMBIGUOUS, false);
        IdentityItem state = new IdentityItem("California", AuthenticationState.AMBIGUOUS, false);

        IdentityMap adobeIdentityMap = new IdentityMap();
        adobeIdentityMap.addItem("login", email);
        adobeIdentityMap.addItem("login", userName);
        adobeIdentityMap.addItem("login", accountType);

        adobeIdentityMap.addItem("location", street);
        adobeIdentityMap.addItem("location", state);
        return adobeIdentityMap;
    }

}
