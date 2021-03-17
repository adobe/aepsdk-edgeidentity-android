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
import static org.powermock.api.mockito.PowerMockito.when;

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
        verify(mockExtensionApi, times(6)).registerEventListener(anyString(),
                anyString(), any(Class.class), any(ExtensionErrorCallback.class));

        // verify listeners are registered with correct event source and type
        verify(mockExtensionApi, times(1)).registerEventListener(eq(IdentityEdgeConstants.EventType.EDGE_IDENTITY),
                eq(IdentityEdgeConstants.EventSource.REQUEST_IDENTITY), eq(ListenerIdentityRequestIdentity.class), callbackCaptor.capture());
        verify(mockExtensionApi, times(1)).registerEventListener(eq(IdentityEdgeConstants.EventType.GENERIC_IDENTITY),
                eq(IdentityEdgeConstants.EventSource.REQUEST_CONTENT), eq(ListenerGenericIdentityRequestContent.class), callbackCaptor.capture());
        verify(mockExtensionApi, times(1)).registerEventListener(eq(IdentityEdgeConstants.EventType.EDGE_IDENTITY),
                eq(IdentityEdgeConstants.EventSource.UPDATE_IDENTITY), eq(ListenerIdentityEdgeUpdateIdentity.class), callbackCaptor.capture());
        verify(mockExtensionApi, times(1)).registerEventListener(eq(IdentityEdgeConstants.EventType.EDGE_IDENTITY),
                eq(IdentityEdgeConstants.EventSource.REMOVE_IDENTITY), eq(ListenerIdentityEdgeRemoveIdentity.class), callbackCaptor.capture());
        verify(mockExtensionApi, times(1)).registerEventListener(eq(IdentityEdgeConstants.EventType.EDGE_IDENTITY),
                eq(IdentityEdgeConstants.EventSource.REQUEST_RESET), eq(ListenerIdentityRequestReset.class), callbackCaptor.capture());
        verify(mockExtensionApi, times(1)).registerEventListener(eq(IdentityEdgeConstants.EventType.HUB),
                                                                 eq(IdentityEdgeConstants.EventSource.SHARED_STATE), eq(ListenerHubSharedState.class), callbackCaptor.capture());

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
        Event event = new Event.Builder("Test event", IdentityEdgeConstants.EventType.EDGE_IDENTITY, IdentityEdgeConstants.EventSource.REQUEST_IDENTITY).build();
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
        final String ecid = identityMap.getIdentityItemsForNamespace("ECID").get(0).getId();

        assertNotNull(ecid);
        assertTrue(ecid.length() > 0);
    }

    @Test
    public void test_handleIdentityRequest_loadsPersistedECID() {
        // setup
        final ECID existingECID = new ECID();
        setupExistingIdentityEdgeProps(existingECID);

        Event event = new Event.Builder("Test event", IdentityEdgeConstants.EventType.EDGE_IDENTITY, IdentityEdgeConstants.EventSource.REQUEST_IDENTITY).build();
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
        final String ecid = identityMap.getIdentityItemsForNamespace("ECID").get(0).getId();

        assertEquals(existingECID.toString(), ecid);
    }

    @Test
    public void test_handleIdentityRequest_noIdentifiers_emptyIdentityMap() {
        // setup
        IdentityEdgeProperties emptyProps = new IdentityEdgeProperties();
        PowerMockito.stub(PowerMockito.method(IdentityEdgeState.class, "getIdentityEdgeProperties")).toReturn(emptyProps);
        
        Event event = new Event.Builder("Test event", IdentityEdgeConstants.EventType.EDGE_IDENTITY, IdentityEdgeConstants.EventSource.REQUEST_IDENTITY).build();
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
        Event event = new Event.Builder("Test event", IdentityEdgeConstants.EventType.EDGE_IDENTITY, IdentityEdgeConstants.EventSource.REQUEST_RESET).build();
        final ArgumentCaptor<Map> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);

        // test
        extension.handleRequestReset(event);

        // verify
        verify(mockExtensionApi, times(1)).setXDMSharedEventState(sharedStateCaptor.capture(), eq(event), any(ExtensionErrorCallback.class));
        Map<String, Object> sharedState = sharedStateCaptor.getValue();
        IdentityItem sharedEcid = getItemFromIdentityMap(sharedState, "ECID", 0);
        assertNotNull(sharedEcid);
        assertTrue(sharedEcid.getId().length() > 0);
    }

    @Test
    public void test_handleHubSharedState_updateLegacyEcidOnDirectIdentityStateChange() {
        when(mockExtensionApi.getSharedEventState(eq(IdentityEdgeConstants.SharedStateKeys.IDENTITY_DIRECT),
                                                  any(Event.class),
                                                  any(ExtensionErrorCallback.class)))
                .thenReturn(new HashMap<String, Object>() {{
                    put(IdentityEdgeConstants.EventDataKeys.VISITOR_ID_ECID, "1234");
                }});

        Event event = new Event.Builder("Test event",
                                        IdentityEdgeConstants.EventType.HUB,
                                        IdentityEdgeConstants.EventSource.SHARED_STATE)
                .setEventData(new HashMap<String, Object>(){{
                    put(IdentityEdgeConstants.EventDataKeys.STATE_OWNER, IdentityEdgeConstants.SharedStateKeys.IDENTITY_DIRECT);
                }}).build();

        extension.handleHubSharedState(event);

        final ArgumentCaptor<Map<String, Object>> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockExtensionApi, times(1)).setXDMSharedEventState(sharedStateCaptor.capture(), eq(event), any(ExtensionErrorCallback.class));
        Map<String, Object> sharedState = sharedStateCaptor.getValue();
        IdentityItem legacyEcidItem = getItemFromIdentityMap(sharedState, "ECID", 1);
        assertNotNull(legacyEcidItem);
        assertEquals("1234", legacyEcidItem.getId());
    }

    @Test
    public void test_handleHubSharedState_noOpNullEvent() {
        when(mockExtensionApi.getSharedEventState(eq(IdentityEdgeConstants.SharedStateKeys.IDENTITY_DIRECT),
                                                  any(Event.class),
                                                  any(ExtensionErrorCallback.class)))
                .thenReturn(new HashMap<String, Object>() {{
                    put(IdentityEdgeConstants.EventDataKeys.VISITOR_ID_ECID, "1234");
                }});

        extension.handleHubSharedState(null);

        verify(mockExtensionApi, times(0)).setXDMSharedEventState(any(Map.class), any(Event.class), any(ExtensionErrorCallback.class));
    }

    @Test
    public void test_handleHubSharedState_noOpNullEventData() {
        when(mockExtensionApi.getSharedEventState(eq(IdentityEdgeConstants.SharedStateKeys.IDENTITY_DIRECT),
                                                  any(Event.class),
                                                  any(ExtensionErrorCallback.class)))
                .thenReturn(new HashMap<String, Object>() {{
                    put(IdentityEdgeConstants.EventDataKeys.VISITOR_ID_ECID, "1234");
                }});

        Event event = new Event.Builder("Test event",
                                        IdentityEdgeConstants.EventType.HUB,
                                        IdentityEdgeConstants.EventSource.SHARED_STATE)
                .build();

        extension.handleHubSharedState(event);

        verify(mockExtensionApi, times(0)).setXDMSharedEventState(any(Map.class), any(Event.class), any(ExtensionErrorCallback.class));
    }

    @Test
    public void test_handleHubSharedState_noOpNotDirectIdentityStateChange() {
        when(mockExtensionApi.getSharedEventState(eq(IdentityEdgeConstants.SharedStateKeys.IDENTITY_DIRECT),
                                                  any(Event.class),
                                                  any(ExtensionErrorCallback.class)))
                .thenReturn(new HashMap<String, Object>() {{
                    put(IdentityEdgeConstants.EventDataKeys.VISITOR_ID_ECID, "1234");
                }});

        Event event = new Event.Builder("Test event",
                                        IdentityEdgeConstants.EventType.HUB,
                                        IdentityEdgeConstants.EventSource.SHARED_STATE)
                .setEventData(new HashMap<String, Object>(){{
                    put(IdentityEdgeConstants.EventDataKeys.STATE_OWNER, "com.adobe.module.configuration");
                }}).build();

        extension.handleHubSharedState(event);

        verify(mockExtensionApi, times(0)).setXDMSharedEventState(any(Map.class), any(Event.class), any(ExtensionErrorCallback.class));
    }

    @Test
    public void test_handleHubSharedState_noOpNoDirectIdentitySharedState() {
        when(mockExtensionApi.getSharedEventState(eq(IdentityEdgeConstants.SharedStateKeys.IDENTITY_DIRECT),
                                                  any(Event.class),
                                                  any(ExtensionErrorCallback.class)))
                .thenReturn(null);

        Event event = new Event.Builder("Test event",
                                        IdentityEdgeConstants.EventType.HUB,
                                        IdentityEdgeConstants.EventSource.SHARED_STATE)
                .setEventData(new HashMap<String, Object>(){{
                    put(IdentityEdgeConstants.EventDataKeys.STATE_OWNER, IdentityEdgeConstants.SharedStateKeys.IDENTITY_DIRECT);
                }}).build();

        extension.handleHubSharedState(event);

        verify(mockExtensionApi, times(0)).setXDMSharedEventState(any(Map.class), any(Event.class), any(ExtensionErrorCallback.class));
    }

    @Test
    public void test_handleHubSharedState_doesNotShareStateIfLegacyECIDDoesNotChange() {
        when(mockExtensionApi.getSharedEventState(eq(IdentityEdgeConstants.SharedStateKeys.IDENTITY_DIRECT),
                                                  any(Event.class),
                                                  any(ExtensionErrorCallback.class)))
                .thenReturn(new HashMap<String, Object>() {{
                    put(IdentityEdgeConstants.EventDataKeys.VISITOR_ID_ECID, "1234");
                }});

        Event event = new Event.Builder("Test event",
                                        IdentityEdgeConstants.EventType.HUB,
                                        IdentityEdgeConstants.EventSource.SHARED_STATE)
                .setEventData(new HashMap<String, Object>(){{
                    put(IdentityEdgeConstants.EventDataKeys.STATE_OWNER, IdentityEdgeConstants.SharedStateKeys.IDENTITY_DIRECT);
                }}).build();

        // IdentityState.updateLegacyExperienceCloudId returns false if Legacy ECID was not updated
        PowerMockito.stub(PowerMockito.method(IdentityEdgeState.class, "updateLegacyExperienceCloudId")).toReturn(false);

        extension.handleHubSharedState(event);

        final ArgumentCaptor<Map<String, Object>> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockExtensionApi, times(0)).setXDMSharedEventState(sharedStateCaptor.capture(), eq(event), any(ExtensionErrorCallback.class));
    }

    // ========================================================================================
    // private helper methods
    // ========================================================================================

    private void setupExistingIdentityEdgeProps(final ECID ecid) {
        IdentityEdgeProperties persistedProps = new IdentityEdgeProperties();
        persistedProps.setECID(ecid);
        final JSONObject jsonObject = new JSONObject(persistedProps.toXDMData(false));
        final String propsJSON = jsonObject.toString();
        Mockito.when(mockSharedPreference.getString(IdentityEdgeConstants.DataStoreKey.IDENTITY_PROPERTIES, null)).thenReturn(propsJSON);
    }

    private static IdentityItem getItemFromIdentityMap(final Map<String, Object> xdmMap, final String namespace, final int itemIndex) {
        if (xdmMap == null) { return null; }
        Map<String, Object> identityMap = (Map<String, Object>) xdmMap.get("identityMap");
        if (identityMap == null) { return null; }
        List<Object> itemList = (List<Object>) identityMap.get(namespace);
        if (itemList == null || itemList.size() <= itemIndex) { return null; }
        return IdentityItem.fromData((Map<String, Object>)itemList.get(itemIndex));
    }

}
