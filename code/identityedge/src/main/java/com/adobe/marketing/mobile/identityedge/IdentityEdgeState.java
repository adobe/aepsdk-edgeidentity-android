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
    private String LOG_TAG = "IdentityState";
    private boolean hasBooted = false;
    private IdentityEdgeProperties identityProperties;

    /**
     *  Creates a new {@link IdentityEdgeState} with the given {@link IdentityEdgeProperties}
     * @param identityProperties identity edge properties
     */
    public IdentityEdgeState(final IdentityEdgeProperties identityProperties) {
        this.identityProperties = identityProperties;
    }

    /**
     * @return The current {@link IdentityEdgeProperties} for this identity state
     */
    IdentityEdgeProperties getIdentityProperties() {
        return identityProperties;
    }

    /**
     * @return Returns true if IdentityEdge has booted, false otherwise
     */
    public boolean hasBooted() {
        return hasBooted;
    }

    /**
     * Completes init for the Identity Edge extension.
     * @param configSharedState the current configuration shared state available at registration time
     * @return True if we should share state after bootup, false otherwise
     */
    boolean bootupIfReady(final Map<String, Object> configSharedState) {
        if (configSharedState == null) {
            return false;
        }
        // Load properties from local storage
        identityProperties = IdentityEdgeStorageService.loadPropertiesFromPersistence();

        if (identityProperties == null) {
            identityProperties = new IdentityEdgeProperties();
        }

        // Load privacy status
        String privacyStr = (String) configSharedState.get(IdentityEdgeConstants.Configuration.GLOBAL_CONFIG_PRIVACY);
        if (privacyStr == null) {
            privacyStr = MobilePrivacyStatus.UNKNOWN.getValue();
        }

        final MobilePrivacyStatus privacyStatus = Utils.privacyFromString(privacyStr);
        identityProperties.setPrivacyStatus(privacyStatus);

        // Generate new ECID if privacy status allows
        if (identityProperties.getPrivacyStatus() != MobilePrivacyStatus.OPT_OUT && identityProperties.getECID() == null) {
            identityProperties.setECID(new ECID());
        }

        hasBooted = true;
        MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Identity Edge has successfully booted up");
        return true;
    }

    // Return true if should share state
    boolean processPrivacyChange(final Event event) {
        if (event == null) {
            return false;
        }

        final Map<String, Object> eventData = event.getEventData();
        if (eventData == null) {
            return false;
        }

        String privacyStr = (String) eventData.get(IdentityEdgeConstants.Configuration.GLOBAL_CONFIG_PRIVACY);
        if (privacyStr == null) {
            privacyStr = MobilePrivacyStatus.UNKNOWN.getValue();
        }

        final MobilePrivacyStatus newPrivacyStatus = Utils.privacyFromString(privacyStr);

        if (newPrivacyStatus == identityProperties.getPrivacyStatus()) {
            // privacy did not change, no need to create shared state
            return false;
        }

        identityProperties.setPrivacyStatus(newPrivacyStatus);

        if (newPrivacyStatus == MobilePrivacyStatus.OPT_OUT) {
            identityProperties.setECID(null);
            IdentityEdgeStorageService.savePropertiesToPersistence(identityProperties);
            return true;
        } else if (identityProperties.getECID() == null) {
            // When changing privacy status from optedout, need to generate a new Experience Cloud ID for the user
            identityProperties.setECID(new ECID());
            IdentityEdgeStorageService.savePropertiesToPersistence(identityProperties);
            return true;
        }

        return false;
    }

}
