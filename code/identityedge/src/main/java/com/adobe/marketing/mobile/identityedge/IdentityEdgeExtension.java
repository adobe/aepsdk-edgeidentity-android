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
    private final IdentityEdgeState state = new IdentityEdgeState(new IdentityEdgeProperties());

    /**
     * Constructor.
     *
     * <p>
     * Called during the Identity extension's registration.
     * The following listeners are registered during this extension's registration.
     * <ul>
     *     <li> Listener {@link ListenerIdentityRequestIdentity} to listen for event with eventType {@link IdentityEdgeConstants.EventType#EDGE_IDENTITY}
     *     and EventSource {@link IdentityEdgeConstants.EventSource#REQUEST_IDENTITY}</li>
     *     <li> Listener {@link ListenerGenericIdentityRequestContent} to listen for event with eventType {@link IdentityEdgeConstants.EventType#GENERIC_IDENTITY}
     *     and EventSource {@link IdentityEdgeConstants.EventSource#REQUEST_CONTENT}</li>
     *     <li> Listener {@link ListenerIdentityEdgeUpdateIdentity} to listen for event with eventType {@link IdentityEdgeConstants.EventType#EDGE_IDENTITY}
     *     and EventSource {@link IdentityEdgeConstants.EventSource#UPDATE_IDENTITY}</li>
     *     <li> Listener {@link ListenerIdentityEdgeRemoveIdentity} to listen for event with eventType {@link IdentityEdgeConstants.EventType#EDGE_IDENTITY}
     *     and EventSource {@link IdentityEdgeConstants.EventSource#REMOVE_IDENTITY}</li>
     *     <li> Listener {@link ListenerIdentityRequestReset} to listen for event with eventType {@link IdentityEdgeConstants.EventType#EDGE_IDENTITY}
     *     and EventSource {@link IdentityEdgeConstants.EventSource#REQUEST_CONTENT}</li>
     *     <li> Listener {@link ListenerIdentityRequestReset} to listen for event with eventType {@link IdentityEdgeConstants.EventType#GENERIC_IDENTITY}
     *     and EventSource {@link IdentityEdgeConstants.EventSource#REQUEST_RESET}</li>
     *     <li> Listener {@link ListenerHubSharedState} to listen for event with eventType {@link IdentityEdgeConstants.EventType#HUB}
     *     and EventSource {@link IdentityEdgeConstants.EventSource#SHARED_STATE}</li>
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

        extensionApi.registerEventListener(IdentityEdgeConstants.EventType.EDGE_IDENTITY, IdentityEdgeConstants.EventSource.REQUEST_IDENTITY, ListenerIdentityRequestIdentity.class, listenerErrorCallback);
        extensionApi.registerEventListener(IdentityEdgeConstants.EventType.GENERIC_IDENTITY, IdentityEdgeConstants.EventSource.REQUEST_CONTENT, ListenerGenericIdentityRequestContent.class, listenerErrorCallback);
        extensionApi.registerEventListener(IdentityEdgeConstants.EventType.EDGE_IDENTITY, IdentityEdgeConstants.EventSource.UPDATE_IDENTITY, ListenerIdentityEdgeUpdateIdentity.class, listenerErrorCallback);
        extensionApi.registerEventListener(IdentityEdgeConstants.EventType.EDGE_IDENTITY, IdentityEdgeConstants.EventSource.REMOVE_IDENTITY, ListenerIdentityEdgeRemoveIdentity.class, listenerErrorCallback);
        extensionApi.registerEventListener(IdentityEdgeConstants.EventType.HUB, IdentityEdgeConstants.EventSource.SHARED_STATE, ListenerHubSharedState.class, listenerErrorCallback);
        extensionApi.registerEventListener(IdentityEdgeConstants.EventType.GENERIC_IDENTITY, IdentityEdgeConstants.EventSource.REQUEST_RESET, ListenerIdentityRequestReset.class, listenerErrorCallback);
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
     * Handles events of type {@code com.adobe.eventType.hub} and source {@code com.adobe.eventSource.sharedState}.
     * If the state change event is for the direct Identity extension, get the direct Identity shared state and attempt
     * to update the legacy ECID with the direct Identity extension ECID.
     * @param event an event of type {@code com.adobe.eventType.hub} and source {@code com.adobe.eventSource.sharedState}
     */
    void handleHubSharedState(final Event event) {
        if (!canProcessEvents(event)) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Unable to process direct Identity shared state change event. canProcessEvents returned false.");
            return;
        }

        final ExtensionApi extensionApi = getApi();
        if (extensionApi == null ) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "ExtensionApi is null, unable to process direct Identity shared state change event.");
            return;
        }

        if (event == null || event.getEventData() == null) {
            return;
        }

        try {
            final String stateOwner = (String) event.getEventData().get(IdentityEdgeConstants.EventDataKeys.STATE_OWNER);
            if (!IdentityEdgeConstants.SharedStateKeys.IDENTITY_DIRECT.equals(stateOwner)) {
                return;
            }
        } catch (ClassCastException e) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Could not process direct Identity shared state change event, failed to parse event state owner as String: " + e.getLocalizedMessage());
            return;
        }

        final ExtensionErrorCallback<ExtensionError> getSharedStateCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                MobileCore.log(LoggingMode.DEBUG, LOG_TAG, String.format("Failed getting direct Identity shared state. Error : %s.", extensionError.getErrorName()));
            }
        };

        final Map<String, Object> identityState = extensionApi.getSharedEventState(IdentityEdgeConstants.SharedStateKeys.IDENTITY_DIRECT, event, getSharedStateCallback);

        if (identityState == null) {
            return;
        }

        try {
            final String legacyEcidString = (String) identityState.get(IdentityEdgeConstants.EventDataKeys.VISITOR_ID_ECID);
            final ECID legacyEcid = legacyEcidString == null ? null : new ECID(legacyEcidString);

            if (state.updateLegacyExperienceCloudId(legacyEcid)) {
                final ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
                    @Override
                    public void error(final ExtensionError extensionError) {
                        MobileCore.log(LoggingMode.DEBUG, LOG_TAG, String.format("Failed to create XDM shared state. Error : %s.", extensionError.getErrorName()));
                    }
                };
                extensionApi.setXDMSharedEventState(state.getIdentityEdgeProperties().toXDMData(false), event, errorCallback);
            }
        } catch (ClassCastException e) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Could not process direct Identity shared state change event, failed to parse stored ECID as String: " + e.getLocalizedMessage());
        }
    }

    /**
     * Handles events requesting for identifiers. Dispatches response event containing the identifiers. Called by listener registered with event hub.
     * @param event the identity request event
     */
    void handleIdentityRequest(final Event event) {
        if (!canProcessEvents(event)) { return; }

        Map<String, Object> xdmData = state.getIdentityEdgeProperties().toXDMData(true);
        Event responseEvent = new Event.Builder(IdentityEdgeConstants.EventNames.IDENTITY_RESPONSE_CONTENT_ONE_TIME,
                IdentityEdgeConstants.EventType.EDGE_IDENTITY,
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
        if (extensionApi == null) {
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

        // dispatch response event
        final Event responseEvent = new Event.Builder(IdentityEdgeConstants.EventNames.RESET_IDENTITIES_COMPLETE,
                IdentityEdgeConstants.EventType.EDGE_IDENTITY,
                IdentityEdgeConstants.EventSource.RESET_COMPLETE).build();

        MobileCore.dispatchEvent(responseEvent, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Failed to dispatch Identity Edge reset response event for event " +
                        event.getUniqueIdentifier() +
                        " with error " +
                        extensionError.getErrorName());
            }
        });
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
