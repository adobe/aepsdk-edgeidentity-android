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

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.MobilePrivacyStatus;

import java.util.Map;

/**
 * Manages the business logic of the Identity Edge extension
 */
class IdentityEdgeState {
    private String LOG_TAG = "IdentityEdgeState";
    private boolean hasBooted = false;
    private IdentityEdgeProperties identityProperties;

    /**
     *  Creates a new {@link IdentityEdgeState} with the given {@link IdentityEdgeProperties}
     * @param identityProperties identity edge properties
     */
    IdentityEdgeState(final IdentityEdgeProperties identityProperties) {
        this.identityProperties = identityProperties;
    }

    /**
     * @return The current {@link IdentityEdgeProperties} for this identity state
     */
    IdentityEdgeProperties getIdentityEdgeProperties() {
        return identityProperties;
    }

    /**
     * @return Returns true if IdentityEdge has booted, false otherwise
     */
    boolean hasBooted() {
        return hasBooted;
    }

    /**
     * Completes init for the Identity Edge extension.
     * @return True if we should share state after bootup, false otherwise
     */
    boolean bootupIfReady() {
        // Load properties from local storage
        identityProperties = IdentityEdgeStorageService.loadPropertiesFromPersistence();

        if (identityProperties == null) {
            identityProperties = new IdentityEdgeProperties();
        }

        // Generate new ECID if privacy status allows
        if (identityProperties.getECID() == null) {
            identityProperties.setECID(new ECID());
        }

        hasBooted = true;
        MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Identity Edge has successfully booted up");
        return true;
    }

}
