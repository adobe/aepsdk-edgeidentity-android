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
import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionError;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;

import java.util.List;

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
     * @param callback  {@link AdobeCallback} of {@link String} invoked with the Experience Cloud ID
     *  If an {@link AdobeCallbackWithError} is provided, an {@link AdobeError} can be returned in the
     *  eventuality of any error that occurred while getting the Experience Cloud ID
     */
    public static void getExperienceCloudId(final AdobeCallback<String> callback) {
        if (callback == null) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Unexpected null callback, provide a callback to retrieve current ECID.");
            return;
        }

        final Event event = new Event.Builder(IdentityEdgeConstants.EventNames.IDENTITY_REQUEST_IDENTITY_ECID,
                IdentityEdgeConstants.EventType.IDENTITY_EDGE,
                IdentityEdgeConstants.EventSource.REQUEST_IDENTITY).build();

        final ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                returnError(callback, extensionError);
                MobileCore.log(LoggingMode.DEBUG, LOG_TAG, String.format("Failed to dispatch %s event: Error : %s.", IdentityEdgeConstants.EventNames.IDENTITY_REQUEST_IDENTITY_ECID,
                        extensionError.getErrorName()));
            }
        };

        MobileCore.dispatchEventWithResponseCallback(event, new AdobeCallback<Event>() {
            @Override
            public void call(Event responseEvent) {
                if (responseEvent == null || responseEvent.getEventData() == null) {
                    returnError(callback, AdobeError.UNEXPECTED_ERROR);
                    return;
                }

                final IdentityMap identityMap = IdentityMap.fromData(responseEvent.getEventData());
                if (identityMap == null) {
                    MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Failed to read IdentityMap from response event, invoking error callback with AdobeError.UNEXPECTED_ERROR");
                    returnError(callback, AdobeError.UNEXPECTED_ERROR);
                    return;
                }

                final List<IdentityItem> ecidItems = identityMap.getIdentityItemForNamespace(IdentityEdgeConstants.Namespaces.ECID);
                if (ecidItems == null || ecidItems.isEmpty() || ecidItems.get(0).getId() == null) {
                    callback.call("");
                } else {
                    callback.call(ecidItems.get(0).getId());
                }

            }
        }, errorCallback);
    }

    /**
     * Clears all Identity Edge identifiers and generates a new Experience Cloud ID (ECID).
     */
    public static void resetIdentities() {
        final Event event = new Event.Builder(IdentityEdgeConstants.EventNames.REQUEST_RESET,
                IdentityEdgeConstants.EventType.IDENTITY_EDGE,
                IdentityEdgeConstants.EventSource.REQUEST_RESET).build();

        final ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                MobileCore.log(LoggingMode.DEBUG, LOG_TAG, String.format("Failed to dispatch %s event: Error : %s.", IdentityEdgeConstants.EventNames.REQUEST_RESET,
                        extensionError.getErrorName()));
            }
        };

        MobileCore.dispatchEvent(event, errorCallback);
    }

    /**
     * When an {@link AdobeCallbackWithError} is provided, the fail method will be called with provided {@link AdobeError}.
     * @param callback should not be null, should be instance of {@code AdobeCallbackWithError}
     * @param error the {@code AdobeError} returned back in the callback
     */
    private static void returnError (final AdobeCallback<String> callback, final AdobeError error) {
        if (callback == null) {
            return;
        }

        final AdobeCallbackWithError<String> adobeCallbackWithError = callback instanceof AdobeCallbackWithError ?
                (AdobeCallbackWithError<String>) callback : null;

        if (adobeCallbackWithError != null) {
            adobeCallbackWithError.fail(error);
        }
    }
}
