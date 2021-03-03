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

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;

@SuppressWarnings("unchecked")
public class ECIDTests {

    @Test
    public void testECID_correctLength() {
        assertEquals(38, new ECID().getEcidString().length());
    }

    @Test
    public void testECID_onlyContainsNumbers() {
        // contains only digits
        String regex = "[0-9]+";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(new ECID().getEcidString());

        assertTrue(m.matches());
    }

    @Test
    public void testECID_ReasonablyRandom() {
        // setup
        int count = 1000;
        Set<String> ecids = new HashSet<String>();

        // test
        for (int i = 0; i < count; i++) {
            ecids.add(new ECID().getEcidString());
        }

        // verify
        assertEquals(count, ecids.size());
    }

}
