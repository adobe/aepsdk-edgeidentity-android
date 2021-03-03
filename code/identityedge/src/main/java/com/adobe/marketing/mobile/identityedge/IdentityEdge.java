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

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionError;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;

import java.util.HashMap;
import java.util.Map;

public class IdentityEdge {
    private static final String LOG_TAG = "IdentityEdge";

    private IdentityEdge() {}

    /**
     * Returns the version of the {@link IdentityEdge} extension
     * @return The version as {@code String}
     */
    public static String extensionVersion() {
        return IdentityEdgeConstants.EXTENSION_VERSION;
    }

    /**
     * Registers the extension with the Mobile SDK. This method should be called only once in your application class.
     */
    public static void registerExtension() {
        MobileCore.registerExtension(IdentityEdgeExtension.class, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                MobileCore.log(LoggingMode.ERROR, LOG_TAG,
                        "There was an error registering the Identity Edge extension: " + extensionError.getErrorName());
            }
        });
    }

    /**
     * Returns the Experience Cloud ID. An empty string is returned if the Experience Cloud ID was previously cleared.
     * @param callback callback which will be invoked once Experience Cloud ID is available
     */
    public static void getExperienceCloudId(final AdobeCallback<String> callback) {
        final Event event = new Event.Builder(IdentityEdgeConstants.EventNames.IDENTITY_REQUEST_IDENTITY_ECID,
                IdentityEdgeConstants.EventType.IDENTITY_EDGE,
                IdentityEdgeConstants.EventSource.REQUEST_CONTENT).build();

        final ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Failed to dispatch Identity Edge request event with error: " + extensionError.getErrorName());
            }
        };

        MobileCore.dispatchEventWithResponseCallback(event, new AdobeCallback<Event>() {
            @Override
            public void call(Event responseEvent) {
                if (responseEvent == null || responseEvent.getEventData() == null) {
                    MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Response event for event " + event.getUniqueIdentifier() + " was null.");
                    return;
                }

                final Map<String, Object> data = responseEvent.getEventData();
                // TODO: Parse
                final IdentityMap identityMap = IdentityMap.fromEventData(data);

            }
        }, errorCallback);
    }
}
