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
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.ExtensionError;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;


class IdentityEdgeExtension extends Extension {

    /**
     * Constructor.
     *
     * <p>
     * Called during the Identity extension's registration.
     * The following listeners are registered during this extension's registration.
     * <ul>
     *     <li> Listener {@link ListenerIdentityRequestIdentity} to listen for event with eventType {@link IdentityEdgeConstants.EventType#IDENTITY}
     *     and EventSource {@link IdentityEdgeConstants.EventSource#REQUEST_IDENTITY}</li>
     *     <li> Listener {@link ListenerGenericIdentityRequestContent} to listen for event with eventType {@link IdentityEdgeConstants.EventType#GENERIC_IDENTITY}
     *  *     and EventSource {@link IdentityEdgeConstants.EventSource#REQUEST_CONTENT}</li>
     * </ul>
     * <p>
     * Thread : Background thread created by MobileCore
     *
     * @param extensionApi {@link ExtensionApi} instance
     */
    protected IdentityEdgeExtension(ExtensionApi extensionApi) {
        super(extensionApi);
        ExtensionErrorCallback<ExtensionError> listenerErrorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                MobileCore.log(LoggingMode.ERROR, IdentityEdgeConstants.LOG_TAG, String.format("Failed to register listener, error: %s",
                        extensionError.getErrorName()));
            }
        };

        extensionApi.registerEventListener(IdentityEdgeConstants.EventType.IDENTITY_EDGE, IdentityEdgeConstants.EventSource.REQUEST_IDENTITY, ListenerIdentityRequestIdentity.class, listenerErrorCallback);
        extensionApi.registerEventListener(IdentityEdgeConstants.EventType.GENERIC_IDENTITY, IdentityEdgeConstants.EventSource.REQUEST_CONTENT, ListenerGenericIdentityRequestContent.class, listenerErrorCallback);
    }

    /**
     * Required override. Each extension must have a unique name within the application.
     * @return unique name of this extension
     */
    @Override
    protected String getName() {
        return IdentityEdgeConstants.EXTENSION_NAME;
    }

    /**
     * Optional override.
     * @return the version of this extension
     */
    @Override
    protected String getVersion() {
        return IdentityEdgeConstants.EXTENSION_VERSION;
    }


    void handleGenericIdentityRequest(final Event event) {
        // TODO
    }

    void handleIdentityRequest(final Event event) {
        // TODO
    }

}
