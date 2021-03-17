/*******************************************************************************
 * ADOBE CONFIDENTIAL
 *  ___________________
 *
 *  Copyright 2021 Adobe
 *  All Rights Reserved.
 *
 *  NOTICE: All information contained herein is, and remains
 *  the property of Adobe and its suppliers, if any. The intellectual
 *  and technical concepts contained herein are proprietary to Adobe
 *  and its suppliers and are protected by all applicable intellectual
 *  property laws, including trade secret and copyright laws.
 *  Dissemination of this information or reproduction of this material
 *  is strictly forbidden unless prior written permission is obtained
 *  from Adobe.
 ******************************************************************************/

package com.adobe.marketing.mobile.identityedge;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.ExtensionListener;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;

class ListenerHubSharedState extends ExtensionListener {
    /**
     * Constructor.
     *
     * @param extensionApi an instance of {@link ExtensionApi}
     * @param type         the {@link String} eventType this listener is registered to handle
     * @param source       the {@link String} eventSource this listener is registered to handle
     */
    ListenerHubSharedState(final ExtensionApi extensionApi, final String type, final String source) {
        super(extensionApi, type, source);
    }


    /**
     * Method that gets called when event with event type {@link IdentityEdgeConstants.EventType#HUB}
     * and with event source {@link IdentityEdgeConstants.EventSource#SHARED_STATE}  is dispatched through eventHub.
     *
     * @param event the identity reset request {@link Event} to be processed
     */
    @Override
    public void hear(final Event event) {
        if (event == null) {
            MobileCore.log(LoggingMode.DEBUG, IdentityEdgeConstants.LOG_TAG, "Event is null. Ignoring the event listened by ListenerHubSharedState");
            return;
        }

        final IdentityEdgeExtension parentExtension = getIdentityEdgeExtension();

        if (parentExtension == null) {
            MobileCore.log(LoggingMode.DEBUG, IdentityEdgeConstants.LOG_TAG,
                           "The parent extension, associated with the ListenerHubSharedState is null, ignoring request identity reset event.");
            return;
        }

        parentExtension.handleHubSharedState(event);
    }

    /**
     * Returns the parent extension associated with the listener.
     *
     * @return a {@link IdentityEdgeExtension} object registered with the eventHub
     */
    IdentityEdgeExtension getIdentityEdgeExtension() {
        return (IdentityEdgeExtension) getParentExtension();
    }
}
