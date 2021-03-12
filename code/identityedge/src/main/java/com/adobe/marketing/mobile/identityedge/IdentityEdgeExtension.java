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

import java.util.Map;


class IdentityEdgeExtension extends Extension {
    private final String LOG_TAG = "IdentityEdgeExtension";
    private IdentityEdgeState state = new IdentityEdgeState(new IdentityEdgeProperties());

    /**
     * Constructor.
     *
     * <p>
     * Called during the Identity extension's registration.
     * The following listeners are registered during this extension's registration.
     * <ul>
     *     <li> Listener {@link ListenerIdentityRequestIdentity} to listen for event with eventType {@link IdentityEdgeConstants.EventType#IDENTITY_EDGE}
     *     and EventSource {@link IdentityEdgeConstants.EventSource#REQUEST_IDENTITY}</li>
     *     <li> Listener {@link ListenerGenericIdentityRequestContent} to listen for event with eventType {@link IdentityEdgeConstants.EventType#GENERIC_IDENTITY}
     *     and EventSource {@link IdentityEdgeConstants.EventSource#REQUEST_CONTENT}</li>
     *     <li> Listener {@link ListenerIdentityEdgeUpdateIdentity} to listen for event with eventType {@link IdentityEdgeConstants.EventType#IDENTITY_EDGE}
     *     and EventSource {@link IdentityEdgeConstants.EventSource#UPDATE_IDENTITY}</li>
     *     <li> Listener {@link ListenerIdentityEdgeRemoveIdentity} to listen for event with eventType {@link IdentityEdgeConstants.EventType#IDENTITY_EDGE}
     *     and EventSource {@link IdentityEdgeConstants.EventSource#REMOVE_IDENTITY}</li>
     *     and EventSource {@link IdentityEdgeConstants.EventSource#REQUEST_CONTENT}</li>
     *     <li> Listener {@link ListenerIdentityRequestReset} to listen for event with eventType {@link IdentityEdgeConstants.EventType#IDENTITY_EDGE}
     *     and EventSource {@link IdentityEdgeConstants.EventSource#REQUEST_RESET}</li>
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
        extensionApi.registerEventListener(IdentityEdgeConstants.EventType.IDENTITY_EDGE, IdentityEdgeConstants.EventSource.UPDATE_IDENTITY, ListenerIdentityEdgeUpdateIdentity.class, listenerErrorCallback);
        extensionApi.registerEventListener(IdentityEdgeConstants.EventType.IDENTITY_EDGE, IdentityEdgeConstants.EventSource.REMOVE_IDENTITY, ListenerIdentityEdgeRemoveIdentity.class, listenerErrorCallback);
        extensionApi.registerEventListener(IdentityEdgeConstants.EventType.IDENTITY_EDGE, IdentityEdgeConstants.EventSource.REQUEST_RESET, ListenerIdentityRequestReset.class, listenerErrorCallback);
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

    // TODO: Docme
    void handleUpdateIdentities(final Event event) {
        // TODO
    }

    // TODO: Docme
    void handleRemoveIdentity(final Event event) {
        // TODO
    }

    void handleGenericIdentityRequest(final Event event) {
        // TODO
    }

    /**
     * Handles events requesting for identifiers. Dispatches response event containing the identifiers. Called by listener registered with event hub.
     * @param event the identity request event
     */
    void handleIdentityRequest(final Event event) {
        if (!canProcessEvents(event)) { return; }

        Map<String, Object> xdmData = state.getIdentityEdgeProperties().toXDMData(true);
        Event responseEvent = new Event.Builder(IdentityEdgeConstants.EventNames.IDENTITY_RESPONSE_CONTENT_ONE_TIME,
                IdentityEdgeConstants.EventType.IDENTITY_EDGE,
                IdentityEdgeConstants.EventSource.RESPONSE_IDENTITY)
                .setEventData(xdmData)
                .build();

        MobileCore.dispatchResponseEvent(responseEvent, event, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Failed to dispatch Identity Edge response event for event " +
                        event.getUniqueIdentifier() +
                        " with error " +
                        extensionError.getErrorName());
            }
        });
    }

    /**
     * Handles IdentityEdge request reset events.
     * @param event the identity request reset event
     */
    void handleRequestReset(final Event event) {
        if (!canProcessEvents(event)) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Unable to process request reset event. canProcessEvents returned false.");
            return;
        }
        state.resetIdentifiers();

        final ExtensionApi extensionApi = super.getApi();
        if (extensionApi == null ) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "ExtensionApi is null, unable to share XDM shared state for reset identities");
            return;
        }

        // set the shared state
        ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                MobileCore.log(LoggingMode.DEBUG, LOG_TAG, String.format("Failed create XDM shared state. Error : %s.", extensionError.getErrorName()));
            }
        };

        extensionApi.setXDMSharedEventState(state.getIdentityEdgeProperties().toXDMData(false), event, errorCallback);
    }

    /**
     * Determines if Identity Edge is ready to handle events, this is determined by if the Identity Edge extension has booted up
     * @param event An {@link Event}
     * @return True if we can process events, false otherwise
     */
    private boolean canProcessEvents(final Event event) {
        if (state.hasBooted()) { return true; } // we have booted, return true

        final ExtensionApi extensionApi = super.getApi();
        if (extensionApi == null ) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "ExtensionApi is null, unable to process events");
            return false;
        }

        if (state.bootupIfReady()) {
            extensionApi.setXDMSharedEventState(state.getIdentityEdgeProperties().toXDMData(false), null, null);
            return true;
        }

        return false; // cannot handle any events until we have booted
    }
}
