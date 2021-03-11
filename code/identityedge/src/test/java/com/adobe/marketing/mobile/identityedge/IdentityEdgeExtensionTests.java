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

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.MobileCore;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Event.class, MobileCore.class, ExtensionApi.class, IdentityEdgeState.class})
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
        verify(mockExtensionApi, times(3)).registerEventListener(anyString(),
                anyString(), any(Class.class), any(ExtensionErrorCallback.class));

        // verify listeners are registered with correct event source and type
        verify(mockExtensionApi, times(1)).registerEventListener(eq(IdentityEdgeConstants.EventType.IDENTITY_EDGE),
                eq(IdentityEdgeConstants.EventSource.REQUEST_IDENTITY), eq(ListenerIdentityRequestIdentity.class), callbackCaptor.capture());
        verify(mockExtensionApi, times(1)).registerEventListener(eq(IdentityEdgeConstants.EventType.GENERIC_IDENTITY),
                eq(IdentityEdgeConstants.EventSource.REQUEST_CONTENT), eq(ListenerGenericIdentityRequestContent.class), callbackCaptor.capture());
        verify(mockExtensionApi, times(1)).registerEventListener(eq(IdentityEdgeConstants.EventType.IDENTITY_EDGE),
                eq(IdentityEdgeConstants.EventSource.REQUEST_RESET), eq(ListenerIdentityRequestReset.class), callbackCaptor.capture());

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
    public void test_handleIdentityRequest_nullEvent_shouldNotThrow() {
        // test
        extension.handleIdentityRequest(null);
    }

    @Test
    public void test_handleIdentityRequest_generatesNewECID() {
        // setup
        Event event = new Event.Builder("Test event", IdentityEdgeConstants.EventType.IDENTITY_EDGE, IdentityEdgeConstants.EventSource.REQUEST_IDENTITY).build();
        final ArgumentCaptor<Event> responseEventCaptor = ArgumentCaptor.forClass(Event.class);
        final ArgumentCaptor<Event> requestEventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        extension.handleIdentityRequest(event);

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchResponseEvent(responseEventCaptor.capture(), requestEventCaptor.capture(), any(ExtensionErrorCallback.class));

        // verify response event containing ECID is dispatched
        Event ecidResponseEvent = responseEventCaptor.getAllValues().get(0);
        final IdentityMap identityMap = IdentityMap.fromData(ecidResponseEvent.getEventData());
        final String ecid = identityMap.getIdentityItemForNamespace("ECID").get(0).getId();

        assertNotNull(ecid);
        assertTrue(ecid.length() > 0);
    }

    @Test
    public void test_handleIdentityRequest_loadsPersistedECID() {
        // setup
        final ECID existingECID = new ECID();
        setupExistingIdentityEdgeProps(existingECID);

        Event event = new Event.Builder("Test event", IdentityEdgeConstants.EventType.IDENTITY_EDGE, IdentityEdgeConstants.EventSource.REQUEST_IDENTITY).build();
        final ArgumentCaptor<Event> responseEventCaptor = ArgumentCaptor.forClass(Event.class);
        final ArgumentCaptor<Event> requestEventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        extension.handleIdentityRequest(event);

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchResponseEvent(responseEventCaptor.capture(), requestEventCaptor.capture(), any(ExtensionErrorCallback.class));

        // verify response event containing ECID is dispatched
        Event ecidResponseEvent = responseEventCaptor.getAllValues().get(0);
        final IdentityMap identityMap = IdentityMap.fromData(ecidResponseEvent.getEventData());
        final String ecid = identityMap.getIdentityItemForNamespace("ECID").get(0).getId();

        assertEquals(existingECID.toString(), ecid);
    }

    @Test
    public void test_handleIdentityRequest_noIdentifiers_emptyIdentityMap() {
        // setup
        IdentityEdgeProperties emptyProps = new IdentityEdgeProperties();
        PowerMockito.stub(PowerMockito.method(IdentityEdgeState.class, "getIdentityEdgeProperties")).toReturn(emptyProps);
        
        Event event = new Event.Builder("Test event", IdentityEdgeConstants.EventType.IDENTITY_EDGE, IdentityEdgeConstants.EventSource.REQUEST_IDENTITY).build();
        final ArgumentCaptor<Event> responseEventCaptor = ArgumentCaptor.forClass(Event.class);
        final ArgumentCaptor<Event> requestEventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        extension.handleIdentityRequest(event);

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchResponseEvent(responseEventCaptor.capture(), requestEventCaptor.capture(), any(ExtensionErrorCallback.class));

        // verify response event containing ECID is dispatched
        Event ecidResponseEvent = responseEventCaptor.getAllValues().get(0);
        final Map<String, Object> xdmData = ecidResponseEvent.getEventData();
        final Map<String, Object> identityMap = (Map<String, Object>) xdmData.get("identityMap");

        assertTrue(identityMap.isEmpty());
    }

    @Test
    public void test_handleIdentityResetRequest() {
        // setup
        Event event = new Event.Builder("Test event", IdentityEdgeConstants.EventType.IDENTITY_EDGE, IdentityEdgeConstants.EventSource.REQUEST_RESET).build();
        final ArgumentCaptor<Map> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);

        // test
        extension.handleRequestReset(event);

        // verify
        verify(mockExtensionApi, times(1)).setXDMSharedEventState(sharedStateCaptor.capture(), eq(event), any(ExtensionErrorCallback.class));
        Map<String, Object> sharedState = sharedStateCaptor.getValue();
        String sharedEcid = ecidFromIdentityMap(sharedState);
        assertTrue(sharedEcid.length() > 0);
    }

    private void setupExistingIdentityEdgeProps(final ECID ecid) {
        IdentityEdgeProperties persistedProps = new IdentityEdgeProperties();
        persistedProps.setECID(ecid);
        final JSONObject jsonObject = new JSONObject(persistedProps.toXDMData(false));
        final String propsJSON = jsonObject.toString();
        Mockito.when(mockSharedPreference.getString(IdentityEdgeConstants.DataStoreKey.IDENTITY_PROPERTIES, null)).thenReturn(propsJSON);
    }

    private String ecidFromIdentityMap(Map<String, Object> xdmMap) {
        if (xdmMap == null) { return null; }
        Map<String, Object> identityMap = (HashMap<String, Object>) xdmMap.get("identityMap");
        if (identityMap == null) { return null; }
        List<Object> ecidArr = (ArrayList<Object>) identityMap.get("ECID");
        if (ecidArr == null) { return null; }
        Map<String, Object> ecidDict = (HashMap<String, Object>) ecidArr.get(0);
        if (ecidDict == null) { return null; }
        String ecid = (String) ecidDict.get("id");
        return ecid;
    }


}
