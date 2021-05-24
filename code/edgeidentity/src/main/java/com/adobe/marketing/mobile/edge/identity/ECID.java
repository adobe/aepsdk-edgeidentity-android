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
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * This class represents an ECID
 */
final class ECID {

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
     * @param ecidString a valid (38-digit UUID) ECID string representation, if null or empty a new ECID will be generated
     */
    ECID(final String ecidString) {
        if (Utils.isNullOrEmpty(ecidString)) {
            MobileCore.log(
                LoggingMode.DEBUG,
                IdentityConstants.LOG_TAG,
                "Creating an ECID with null or empty ecidString is not allowed, generating a new ECID."
            );
            this.ecidString = new ECID().toString();
            return;
        }

        this.ecidString = ecidString;
    }

    /**
     * Retrieves the string representation of the ECID
     * @return string representation of the ECID
     */
    @Override
    public String toString() {
        return ecidString;
    }

    /**
     * Determine if ECID {@code o} is equal to this ECID.
     * @param o the ECID instance to check for equality with this ECID.
     * @return true if {@code o} is equal to this ECID instance.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ECID ecid = (ECID) o;
        return Objects.equals(ecidString, ecid.ecidString);
    }

    /**
     * Get the hash code for this ECID.
     * @return hash code for this ECID.
     */
    @Override
    public int hashCode() {
        return Objects.hash(ecidString);
    }
}
