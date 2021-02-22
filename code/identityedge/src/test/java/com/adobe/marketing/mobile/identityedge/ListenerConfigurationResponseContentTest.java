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

public class ListenerConfigurationResponseContentTest {

    @Mock
    private IdentityEdgeExtension mockIdentityEdgeExtension;

    private ListenerConfigurationResponseContent listener;

    @Before
    public void setup() {
        mockIdentityEdgeExtension = Mockito.mock(IdentityEdgeExtension.class);
        MobileCore.start(null);
        listener = spy(new ListenerConfigurationResponseContent(null, IdentityEdgeConstants.EventType.CONFIGURATION, IdentityEdgeConstants.EventSource.RESPONSE_CONTENT));
    }

    @Test
    public void testHear() {
        // setup
        Event event = new Event.Builder("Configuration response event", IdentityEdgeConstants.EventType.CONFIGURATION,
                IdentityEdgeConstants.EventSource.RESPONSE_CONTENT).build();
        doReturn(mockIdentityEdgeExtension).when(listener).getIdentityEdgeExtension();

        // test
        listener.hear(event);

        // verify
        verify(mockIdentityEdgeExtension, times(1)).handleConfigurationResponse(event);
    }

    @Test
    public void testHear_WhenParentExtensionNull() {
        // setup
        Event event = new Event.Builder("Configuration response event", IdentityEdgeConstants.EventType.CONFIGURATION,
                IdentityEdgeConstants.EventSource.RESPONSE_CONTENT).build();
        doReturn(null).when(listener).getIdentityEdgeExtension();

        // test
        listener.hear(event);

        // verify
        verify(mockIdentityEdgeExtension, times(0)).handleConfigurationResponse(any(Event.class));
    }

    @Test
    public void testHear_WhenEventNull() {
        // setup
        doReturn(null).when(listener).getIdentityEdgeExtension();
        doReturn(mockIdentityEdgeExtension).when(listener).getIdentityEdgeExtension();

        // test
        listener.hear(null);

        // verify
        verify(mockIdentityEdgeExtension, times(0)).handleConfigurationResponse(any(Event.class));
    }
}
