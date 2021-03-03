package com.adobe.marketing.mobile.identityedge;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.MobileCore;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Event.class, MobileCore.class, ExtensionApi.class})
public class IdentityEdgeExtensionTests {
    private IdentityEdgeExtension extension;

    @Mock
    ExtensionApi mockExtensionApi;

    @Mock
    Application mockApplication;

    @Mock
    Context mockContext;

    @Mock
    SharedPreferences mockSharedPreference;

    @Mock
    SharedPreferences.Editor mockSharedPreferenceEditor;

    @Before
    public void setup() {
        PowerMockito.mockStatic(MobileCore.class);

        Mockito.when(MobileCore.getApplication()).thenReturn(mockApplication);
        Mockito.when(mockApplication.getApplicationContext()).thenReturn(mockContext);
        Mockito.when(mockContext.getSharedPreferences(IdentityEdgeConstants.DataStoreKey.DATASTORE_NAME, 0)).thenReturn(mockSharedPreference);
        Mockito.when(mockSharedPreference.edit()).thenReturn(mockSharedPreferenceEditor);

        extension = new IdentityEdgeExtension(mockExtensionApi);
    }

    // ========================================================================================
    // constructor
    // ========================================================================================
    @Test
    public void test_ListenersRegistration() {
        // setup
        final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

        // test
        // constructor is called in the setup step()

        // verify 2 listeners are registered
        verify(mockExtensionApi, times(2)).registerEventListener(anyString(),
                anyString(), any(Class.class), any(ExtensionErrorCallback.class));

        // verify listeners are registered with correct event source and type
        verify(mockExtensionApi, times(1)).registerEventListener(eq(IdentityEdgeConstants.EventType.IDENTITY_EDGE),
                eq(IdentityEdgeConstants.EventSource.REQUEST_IDENTITY), eq(ListenerIdentityRequestIdentity.class), callbackCaptor.capture());
        verify(mockExtensionApi, times(1)).registerEventListener(eq(IdentityEdgeConstants.EventType.GENERIC_IDENTITY),
                eq(IdentityEdgeConstants.EventSource.REQUEST_CONTENT), eq(ListenerGenericIdentityRequestContent.class), callbackCaptor.capture());

        // verify the callback
        ExtensionErrorCallback extensionErrorCallback = callbackCaptor.getValue();
        Assert.assertNotNull("The extension callback should not be null", extensionErrorCallback);

        // TODO - enable when ExtensionError creation is available
        //extensionErrorCallback.error(ExtensionError.UNEXPECTED_ERROR);
    }

    // ========================================================================================
    // getName
    // ========================================================================================
    @Test
    public void test_getName() {
        // test
        String moduleName = extension.getName();
        assertEquals("getName should return the correct module name", IdentityEdgeConstants.EXTENSION_NAME, moduleName);
    }

    // ========================================================================================
    // getVersion
    // ========================================================================================
    @Test
    public void test_getVersion() {
        // test
        String moduleVersion = extension.getVersion();
        assertEquals("getVersion should return the correct module version", IdentityEdgeConstants.EXTENSION_VERSION,
                moduleVersion);
    }

    // ========================================================================================
    // handleIdentityRequest
    // ========================================================================================

    @Test
    public void test_handleIdentityRequest_nullEvent() {
        // test
        extension.handleIdentityRequest(null);
    }
    @Test
    public void test_handleIdentityRequest() {
        // setup
        Event event = new Event.Builder("Test event", IdentityEdgeConstants.EventType.IDENTITY_EDGE, IdentityEdgeConstants.EventSource.REQUEST_IDENTITY).build();
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        extension.handleIdentityRequest(event);
    }

    // ========================================================================================
    // handleConfigurationResponse
    // ========================================================================================
}
