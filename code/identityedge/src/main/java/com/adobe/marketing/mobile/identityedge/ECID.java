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

import java.util.Locale;
import java.util.UUID;

/**
 * This class represents an ECID
 */
class ECID {
    private final String ecidString;

    /**
     * Initializes and generates a new ECID
     */
    ECID() {
        final UUID uuid = UUID.randomUUID();
        final long most = uuid.getMostSignificantBits();
        final long least = uuid.getLeastSignificantBits();
        // return formatted string, flip negatives if they're set.
        ecidString = String.format(Locale.US, "%019d%019d", most < 0 ? -most : most, least < 0 ? -least : least);
    }

    /**
     * Creates a new ECID with the passed in string
     * @param ecidString a valid ECID string representation
     */
    ECID(final String ecidString) {
        this.ecidString = ecidString;
    }

    /**
     * Retrieves the string representation of the ECID
     * @return string representation of the ECID
     */
    public String getEcidString() {
        return ecidString;
    }

    @Override
    public String toString() {
        return ecidString;
    }
}
