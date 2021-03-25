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

public class ListenerEventHubBootTest {

    @Mock
    private IdentityExtension mockConsentExtension;

    private ListenerEventHubBoot listener;

    @Before
    public void setup() {
        mockConsentExtension = Mockito.mock(IdentityExtension.class);
        MobileCore.start(null);
        listener = spy(new ListenerEventHubBoot(null, IdentityConstants.EventType.HUB, IdentityConstants.EventSource.BOOTED));
    }

    @Test
    public void testHear() {
        // setup
        Event event = new Event.Builder("Event Hub Boot", IdentityConstants.EventType.HUB, IdentityConstants.EventSource.BOOTED).build();
        doReturn(mockConsentExtension).when(listener).getIdentityExtension();

        // test
        listener.hear(event);

        // verify
        verify(mockConsentExtension, times(1)).handleEventHubBoot(event);
    }

    @Test
    public void testHear_WhenParentExtensionNull() {
        // setup
        Event event = new Event.Builder("Event Hub Boot", IdentityConstants.EventType.HUB, IdentityConstants.EventSource.BOOTED).build();
        doReturn(null).when(listener).getIdentityExtension();

        // test
        listener.hear(event);

        // verify
        verify(mockConsentExtension, times(0)).handleEventHubBoot(any(Event.class));
    }

}
