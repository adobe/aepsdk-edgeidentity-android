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
import com.adobe.marketing.mobile.MobileCore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ListenerHubSharedStateTests {
    @Mock
    private IdentityEdgeExtension mockIdentityEdgeExtension;

    private ListenerHubSharedState listener;

    @Before
    public void setup() {
        mockIdentityEdgeExtension = Mockito.mock(IdentityEdgeExtension.class);
        MobileCore.start(null);
        listener = spy(new ListenerHubSharedState(null, IdentityEdgeConstants.EventType.HUB, IdentityEdgeConstants.EventSource.SHARED_STATE));
    }

    @Test
    public void testHear() {
        // setup
        Event event = new Event.Builder("Shared State Change", IdentityEdgeConstants.EventType.HUB,
                                        IdentityEdgeConstants.EventSource.SHARED_STATE).build();
        doReturn(mockIdentityEdgeExtension).when(listener).getIdentityEdgeExtension();

        // test
        listener.hear(event);

        // verify
        verify(mockIdentityEdgeExtension, times(1)).handleHubSharedState(event);
    }

    @Test
    public void testHear_WhenParentExtensionNull() {
        // setup
        Event event = new Event.Builder("Shared State Change", IdentityEdgeConstants.EventType.HUB,
                                        IdentityEdgeConstants.EventSource.SHARED_STATE).build();
        doReturn(null).when(listener).getIdentityEdgeExtension();

        // test
        listener.hear(event);

        // verify
        verify(mockIdentityEdgeExtension, times(0)).handleHubSharedState(any(Event.class));
    }

    @Test
    public void testHear_WhenEventNull() {
        // setup
        doReturn(null).when(listener).getIdentityEdgeExtension();
        doReturn(mockIdentityEdgeExtension).when(listener).getIdentityEdgeExtension();

        // test
        listener.hear(null);

        // verify
        verify(mockIdentityEdgeExtension, times(0)).handleHubSharedState(any(Event.class));
    }
}
