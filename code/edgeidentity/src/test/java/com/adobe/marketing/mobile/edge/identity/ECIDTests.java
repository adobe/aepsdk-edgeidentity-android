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

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertNotEquals;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class ECIDTests {

    @Test
    public void testECID_correctLength() {
        assertEquals(38, new ECID().toString().length());
    }

    @Test
    public void testECID_correctWithConstructor() {
        // setup
        ECID ecid = new ECID();

        // test
        ECID constructedEcid = new ECID(ecid.toString());
        assertEquals(ecid.toString(), constructedEcid.toString());
    }

    @Test
    public void testECID_correctWithConstructor_null() {
        // test
        ECID constructedEcid = new ECID(null);
        assertNotNull(constructedEcid.toString());
    }

    @Test
    public void testECID_correctWithConstructor_emptyString() {
        // test
        ECID constructedEcid = new ECID(null);
        assertNotNull(constructedEcid.toString());
    }

    @Test
    public void testECID_onlyContainsNumbers() {
        // contains only digits
        String regex = "[0-9]+";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(new ECID().toString());

        assertTrue(m.matches());
    }

    @Test
    public void testECID_ReasonablyRandom() {
        // setup
        int count = 1000;
        Set<String> ecids = new HashSet<String>();

        // test
        for (int i = 0; i < count; i++) {
            ecids.add(new ECID().toString());
        }

        // verify
        assertEquals(count, ecids.size());
    }

    @Test
    public void testECID_hashCode_reasonablyRandom() {
        // setup
        int count = 1000;
        Set<ECID> ecids = new HashSet<ECID>(count);

        // test
        for (int i = 0; i < count; i++) {
            ecids.add(new ECID());
        }

        // verify
        assertEquals(count, ecids.size());
    }

    @Test
    public void testECID_hashCode() {
        ECID a = new ECID();
        ECID b = new ECID(a.toString());

        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(a.hashCode(), a.hashCode());

        assertNotEquals(a.hashCode(), new ECID().hashCode());
        assertNotEquals(a.hashCode(), new NotECID(a.toString()).hashCode());
    }

    @Test
    public void testECID_equals() {
        ECID a = new ECID();
        ECID b = new ECID(a.toString());

        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertTrue(a.equals(a));
        assertTrue(b.equals(b));

        assertFalse(a.equals(null));
        assertFalse(a.equals(new ECID()));

        assertFalse(a.equals(new NotECID(a.toString())));
    }

    private class NotECID {

        private final String ecidString;

        NotECID(final String s) {
            this.ecidString = s;
        }
    }
}
