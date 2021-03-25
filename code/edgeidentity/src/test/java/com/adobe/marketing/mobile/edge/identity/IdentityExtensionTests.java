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

import java.util.HashMap;
import java.util.Map;

import static com.adobe.marketing.mobile.edge.identity.IdentityTestUtil.*;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Event.class, MobileCore.class, ExtensionApi.class, IdentityState.class})
public class IdentityExtensionTests {
    private IdentityExtension extension;

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
        Mockito.when(mockContext.getSharedPreferences(IdentityConstants.DataStoreKey.DATASTORE_NAME, 0)).thenReturn(mockSharedPreference);
        Mockito.when(mockSharedPreference.edit()).thenReturn(mockSharedPreferenceEditor);

        extension = new IdentityExtension(mockExtensionApi);
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
        verify(mockExtensionApi, times(7)).registerEventListener(anyString(),
                anyString(), any(Class.class), any(ExtensionErrorCallback.class));

        // verify listeners are registered with correct event source and type
        verify(mockExtensionApi, times(1)).registerEventListener(eq(IdentityConstants.EventType.EDGE_IDENTITY),
                eq(IdentityConstants.EventSource.REQUEST_IDENTITY), eq(ListenerEdgeIdentityRequestIdentity.class), callbackCaptor.capture());
        verify(mockExtensionApi, times(1)).registerEventListener(eq(IdentityConstants.EventType.GENERIC_IDENTITY),
                eq(IdentityConstants.EventSource.REQUEST_CONTENT), eq(ListenerGenericIdentityRequestContent.class), callbackCaptor.capture());
        verify(mockExtensionApi, times(1)).registerEventListener(eq(IdentityConstants.EventType.EDGE_IDENTITY),
                eq(IdentityConstants.EventSource.UPDATE_IDENTITY), eq(ListenerEdgeIdentityUpdateIdentity.class), callbackCaptor.capture());
        verify(mockExtensionApi, times(1)).registerEventListener(eq(IdentityConstants.EventType.EDGE_IDENTITY),
                eq(IdentityConstants.EventSource.REMOVE_IDENTITY), eq(ListenerEdgeIdentityRemoveIdentity.class), callbackCaptor.capture());
        verify(mockExtensionApi, times(1)).registerEventListener(eq(IdentityConstants.EventType.GENERIC_IDENTITY),
                eq(IdentityConstants.EventSource.REQUEST_RESET), eq(ListenerIdentityRequestReset.class), callbackCaptor.capture());
        verify(mockExtensionApi, times(1)).registerEventListener(eq(IdentityConstants.EventType.HUB),
                                                                 eq(IdentityConstants.EventSource.SHARED_STATE), eq(ListenerHubSharedState.class), callbackCaptor.capture());
        verify(mockExtensionApi, times(1)).registerEventListener(eq(IdentityConstants.EventType.HUB),
                eq(IdentityConstants.EventSource.BOOTED), eq(ListenerEventHubBoot.class), callbackCaptor.capture());

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
        assertEquals("getName should return the correct module name", IdentityConstants.EXTENSION_NAME, moduleName);
    }

    // ========================================================================================
    // getVersion
    // ========================================================================================
    @Test
    public void test_getVersion() {
        // test
        String moduleVersion = extension.getVersion();
        assertEquals("getVersion should return the correct module version", IdentityConstants.EXTENSION_VERSION,
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
        Event event = new Event.Builder("Test event", IdentityConstants.EventType.EDGE_IDENTITY, IdentityConstants.EventSource.REQUEST_IDENTITY).build();
        final ArgumentCaptor<Event> responseEventCaptor = ArgumentCaptor.forClass(Event.class);
        final ArgumentCaptor<Event> requestEventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        extension.handleIdentityRequest(event);

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchResponseEvent(responseEventCaptor.capture(), requestEventCaptor.capture(), any(ExtensionErrorCallback.class));

        // verify response event containing ECID is dispatched
        Event ecidResponseEvent = responseEventCaptor.getAllValues().get(0);
        final IdentityMap identityMap = IdentityMap.fromXDMMap(ecidResponseEvent.getEventData());
        final String ecid = identityMap.getIdentityItemsForNamespace("ECID").get(0).getId();

        assertNotNull(ecid);
        assertTrue(ecid.length() > 0);
    }

    @Test
    public void test_handleIdentityRequest_loadsPersistedECID() {
        // setup
        final ECID existingECID = new ECID();
        setupExistingIdentityProps(existingECID);

        Event event = new Event.Builder("Test event", IdentityConstants.EventType.EDGE_IDENTITY, IdentityConstants.EventSource.REQUEST_IDENTITY).build();
        final ArgumentCaptor<Event> responseEventCaptor = ArgumentCaptor.forClass(Event.class);
        final ArgumentCaptor<Event> requestEventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        extension = new IdentityExtension(mockExtensionApi);
        extension.handleIdentityRequest(event);

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchResponseEvent(responseEventCaptor.capture(), requestEventCaptor.capture(), any(ExtensionErrorCallback.class));

        // verify response event containing ECID is dispatched
        Event ecidResponseEvent = responseEventCaptor.getAllValues().get(0);
        final IdentityMap identityMap = IdentityMap.fromXDMMap(ecidResponseEvent.getEventData());
        final String ecid = identityMap.getIdentityItemsForNamespace("ECID").get(0).getId();

        assertEquals(existingECID.toString(), ecid);
    }

    @Test
    public void test_handleIdentityRequest_noIdentifiers_emptyXDMIdentityMap() {
        // setup
        IdentityProperties emptyProps = new IdentityProperties();
        PowerMockito.stub(PowerMockito.method(IdentityState.class, "getIdentityProperties")).toReturn(emptyProps);

        Event event = new Event.Builder("Test event", IdentityConstants.EventType.EDGE_IDENTITY, IdentityConstants.EventSource.REQUEST_IDENTITY).build();
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
        Event event = new Event.Builder("Test event", IdentityConstants.EventType.GENERIC_IDENTITY, IdentityConstants.EventSource.REQUEST_RESET).build();
        final ArgumentCaptor<Map> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        extension.handleRequestReset(event);

        // verify
        verify(mockExtensionApi, times(1)).setXDMSharedEventState(sharedStateCaptor.capture(), eq(event), any(ExtensionErrorCallback.class));
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        Map<String, String> sharedState = flattenMap(sharedStateCaptor.getValue());
        assertTrue(sharedState.get("identityMap.ECID[0].id").length() > 0);
    }

    @Test
    public void test_handleHubSharedState_updateLegacyEcidOnDirectIdentityStateChange() {
        when(mockExtensionApi.getSharedEventState(eq(IdentityConstants.SharedStateKeys.IDENTITY_DIRECT),
                                                  any(Event.class),
                                                  any(ExtensionErrorCallback.class)))
                .thenReturn(new HashMap<String, Object>() {{
                    put(IdentityConstants.EventDataKeys.VISITOR_ID_ECID, "1234");
                }});

        Event event = new Event.Builder("Test event",
                                        IdentityConstants.EventType.HUB,
                                        IdentityConstants.EventSource.SHARED_STATE)
                .setEventData(new HashMap<String, Object>(){{
                    put(IdentityConstants.EventDataKeys.STATE_OWNER, IdentityConstants.SharedStateKeys.IDENTITY_DIRECT);
                }}).build();

        extension.handleHubSharedState(event);

        final ArgumentCaptor<Map<String, Object>> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockExtensionApi, times(1)).setXDMSharedEventState(sharedStateCaptor.capture(), eq(event), any(ExtensionErrorCallback.class));
        Map<String, Object> sharedState = sharedStateCaptor.getValue();
        assertEquals("1234", flattenMap(sharedState).get("identityMap.ECID[1].id")); // Legacy ECID is set as a secondary ECID

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        // verify no event dispatched
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(0));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));
        assertTrue(eventCaptor.getAllValues().isEmpty());
    }

    @Test
    public void test_handleHubSharedState_noOpNullEvent() {
        when(mockExtensionApi.getSharedEventState(eq(IdentityConstants.SharedStateKeys.IDENTITY_DIRECT),
                                                  any(Event.class),
                                                  any(ExtensionErrorCallback.class)))
                .thenReturn(new HashMap<String, Object>() {{
                    put(IdentityConstants.EventDataKeys.VISITOR_ID_ECID, "1234");
                }});

        extension.handleHubSharedState(null);

        verify(mockExtensionApi, times(0)).setXDMSharedEventState(any(Map.class), any(Event.class), any(ExtensionErrorCallback.class));
    }

    @Test
    public void test_handleHubSharedState_noOpNullEventData() {
        when(mockExtensionApi.getSharedEventState(eq(IdentityConstants.SharedStateKeys.IDENTITY_DIRECT),
                                                  any(Event.class),
                                                  any(ExtensionErrorCallback.class)))
                .thenReturn(new HashMap<String, Object>() {{
                    put(IdentityConstants.EventDataKeys.VISITOR_ID_ECID, "1234");
                }});

        Event event = new Event.Builder("Test event",
                                        IdentityConstants.EventType.HUB,
                                        IdentityConstants.EventSource.SHARED_STATE)
                .build();

        extension.handleHubSharedState(event);

        verify(mockExtensionApi, times(0)).setXDMSharedEventState(any(Map.class), any(Event.class), any(ExtensionErrorCallback.class));
    }

    @Test
    public void test_handleHubSharedState_noOpNotDirectIdentityStateChange() {
        when(mockExtensionApi.getSharedEventState(eq(IdentityConstants.SharedStateKeys.IDENTITY_DIRECT),
                                                  any(Event.class),
                                                  any(ExtensionErrorCallback.class)))
                .thenReturn(new HashMap<String, Object>() {{
                    put(IdentityConstants.EventDataKeys.VISITOR_ID_ECID, "1234");
                }});

        Event event = new Event.Builder("Test event",
                                        IdentityConstants.EventType.HUB,
                                        IdentityConstants.EventSource.SHARED_STATE)
                .setEventData(new HashMap<String, Object>(){{
                    put(IdentityConstants.EventDataKeys.STATE_OWNER, "com.adobe.module.configuration");
                }}).build();

        extension.handleHubSharedState(event);

        verify(mockExtensionApi, times(0)).setXDMSharedEventState(any(Map.class), any(Event.class), any(ExtensionErrorCallback.class));
    }

    @Test
    public void test_handleHubSharedState_noOpNoDirectIdentitySharedState() {
        when(mockExtensionApi.getSharedEventState(eq(IdentityConstants.SharedStateKeys.IDENTITY_DIRECT),
                                                  any(Event.class),
                                                  any(ExtensionErrorCallback.class)))
                .thenReturn(null);

        Event event = new Event.Builder("Test event",
                                        IdentityConstants.EventType.HUB,
                                        IdentityConstants.EventSource.SHARED_STATE)
                .setEventData(new HashMap<String, Object>(){{
                    put(IdentityConstants.EventDataKeys.STATE_OWNER, IdentityConstants.SharedStateKeys.IDENTITY_DIRECT);
                }}).build();

        extension.handleHubSharedState(event);

        verify(mockExtensionApi, times(0)).setXDMSharedEventState(any(Map.class), any(Event.class), any(ExtensionErrorCallback.class));
    }

    @Test
    public void test_handleHubSharedState_doesNotShareStateIfLegacyECIDDoesNotChange() {
        when(mockExtensionApi.getSharedEventState(eq(IdentityConstants.SharedStateKeys.IDENTITY_DIRECT),
                                                  any(Event.class),
                                                  any(ExtensionErrorCallback.class)))
                .thenReturn(new HashMap<String, Object>() {{
                    put(IdentityConstants.EventDataKeys.VISITOR_ID_ECID, "1234");
                }});

        Event event = new Event.Builder("Test event",
                                        IdentityConstants.EventType.HUB,
                                        IdentityConstants.EventSource.SHARED_STATE)
                .setEventData(new HashMap<String, Object>(){{
                    put(IdentityConstants.EventDataKeys.STATE_OWNER, IdentityConstants.SharedStateKeys.IDENTITY_DIRECT);
                }}).build();

        // IdentityState.updateLegacyExperienceCloudId returns false if Legacy ECID was not updated
        PowerMockito.stub(PowerMockito.method(IdentityState.class, "updateLegacyExperienceCloudId")).toReturn(false);

        extension.handleHubSharedState(event);

        final ArgumentCaptor<Map<String, Object>> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockExtensionApi, times(0)).setXDMSharedEventState(sharedStateCaptor.capture(), eq(event), any(ExtensionErrorCallback.class));
    }

    // ========================================================================================
    // handleIdentityRequest
    // ========================================================================================
    @Test
    public void test_handleUpdateIdentities() throws Exception {
        // setup
        Map<String,Object> identityXDM = createXDMIdentityMap(
                new TestItem("UserId", "secretID")
        );
        final ArgumentCaptor<Map> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);
        final ArgumentCaptor<String> persistenceValueCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        Event updateIdentityEvent = buildUpdateIdentityRequest(identityXDM);
        extension.handleUpdateIdentities(updateIdentityEvent);

        // verify shared state
        verify(mockExtensionApi, times(1)).setXDMSharedEventState(sharedStateCaptor.capture(), eq(updateIdentityEvent), any(ExtensionErrorCallback.class));
        Map<String, String> sharedState = flattenMap(sharedStateCaptor.getValue());
        assertEquals("secretID", sharedState.get("identityMap.UserId[0].id"));
        assertEquals("ambiguous", sharedState.get("identityMap.UserId[0].authenticatedState"));
        assertEquals("false", sharedState.get("identityMap.UserId[0].primary"));

        // verify no event dispatched
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(0));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));
        assertTrue(eventCaptor.getAllValues().isEmpty());

        // verify persistence
        verify(mockSharedPreferenceEditor, times(2)).putString(eq(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES), persistenceValueCaptor.capture());
        Map<String, String> persistedData = flattenJSONString(persistenceValueCaptor.getAllValues().get(1));
        assertEquals("secretID", persistedData.get("identityMap.UserId[0].id"));
        assertEquals("ambiguous", persistedData.get("identityMap.UserId[0].authenticatedState"));
        assertEquals("false", persistedData.get("identityMap.UserId[0].primary"));
    }


    @Test
    public void test_handleUpdateIdentities_DoNotUpdateReservedNamespace() throws Exception {
        // setup
        Map<String,Object> identityXDM = createXDMIdentityMap(
                new TestItem("ECID", "somevalue"),
                new TestItem("GAID", "somevalue"),
                new TestItem("IDFA", "somevalue"),
                new TestItem("IdFA", "somevalue"),
                new TestItem("gaid", "somevalue"),
                new TestItem("UserId", "somevalue")
        );

        final ArgumentCaptor<Map> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);
        final ArgumentCaptor<String> persistenceValueCaptor = ArgumentCaptor.forClass(String.class);

        // test
        Event updateIdentityEvent = buildUpdateIdentityRequest(identityXDM);
        extension.handleUpdateIdentities(updateIdentityEvent);

        // verify shared state
        verify(mockExtensionApi, times(1)).setXDMSharedEventState(sharedStateCaptor.capture(), eq(updateIdentityEvent), any(ExtensionErrorCallback.class));
        Map<String, String> sharedState = flattenMap(sharedStateCaptor.getValue());
        assertEquals(6, sharedState.size()); // 6 represents id, primary and authState of USERID identifier and generated ECID
        assertEquals("somevalue", sharedState.get("identityMap.UserId[0].id"));
        assertNotEquals("somevalue", sharedState.get("identityMap.ECID[0].id")); // verify that the ECID is not disturbed

        // verify persistence
        verify(mockSharedPreferenceEditor, times(2)).putString(eq(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES), persistenceValueCaptor.capture());
        Map<String, String> persistedData = flattenJSONString(persistenceValueCaptor.getAllValues().get(1));
        assertEquals(6, persistedData.size()); // 3 represents id, primary and authState of USERID identifier and generated ECID
        assertEquals("somevalue", persistedData.get("identityMap.UserId[0].id"));
        assertNotEquals("somevalue", persistedData.get("identityMap.ECID[0].id")); // verify that the ECID is not disturbed
    }


    @Test
    public void test_handleUpdateIdentities_CaseSensitiveNamespace_OnCustomIdentifiers() throws Exception {
        // setup
        Map<String, Object> identityXDM = createXDMIdentityMap(
                new TestItem("pushToken", "somevalue"),
                new TestItem("PUSHTOKEN", "SOMEVALUE")
        );
        Event updateIdentityEvent = buildUpdateIdentityRequest(identityXDM);
        extension.handleUpdateIdentities(updateIdentityEvent);

        final ArgumentCaptor<Map> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);

        // verify shared state
        verify(mockExtensionApi, times(1)).setXDMSharedEventState(sharedStateCaptor.capture(), eq(updateIdentityEvent), any(ExtensionErrorCallback.class));
        Map<String, String> sharedState = flattenMap(sharedStateCaptor.getValue());
        assertEquals("somevalue", sharedState.get("identityMap.pushToken[0].id"));
        assertEquals("SOMEVALUE", sharedState.get("identityMap.PUSHTOKEN[0].id"));
    }

    @Test
    public void test_handleUpdateIdentities_nullEventData() {
        // test
        Event updateIdentityEvent = buildUpdateIdentityRequest(null);
        extension.handleUpdateIdentities(updateIdentityEvent);

        // verify shared state
        verify(mockExtensionApi, times(0)).setXDMSharedEventState(any(Map.class), any(Event.class), any(ExtensionErrorCallback.class));

        // verify persistence
        verify(mockSharedPreferenceEditor, times(1)).putString(anyString(), anyString()); // called only once while generating ECID
    }


    // ========================================================================================
    // handleRemoveRequest
    // ========================================================================================
    @Test
    public void test_handleRemoveIdentity() throws Exception {
        // setup
        Map<String,Object> identityXDM = createXDMIdentityMap(
                new TestItem("UserId", "secretID"),
                new TestItem("PushId", "token")
        );
        JSONObject identityXDMJSON = new JSONObject(identityXDM);
        Mockito.when(mockSharedPreference.getString(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES, null)).thenReturn(identityXDMJSON.toString());

        final ArgumentCaptor<Map> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);
        final ArgumentCaptor<String> persistenceValueCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        extension = new IdentityExtension(mockExtensionApi);

        // test
        Map<String,Object> removedIdentityXDM = createXDMIdentityMap(
                new TestItem("UserId", "secretID")
        );
        Event removeIdentityEvent = buildRemoveIdentityRequest(removedIdentityXDM);
        extension.handleRemoveIdentity(removeIdentityEvent);

        // verify shared state
        verify(mockExtensionApi, times(1)).setXDMSharedEventState(sharedStateCaptor.capture(), eq(removeIdentityEvent), any(ExtensionErrorCallback.class));
        Map<String, String> sharedState = flattenMap(sharedStateCaptor.getValue());
        assertNull(sharedState.get("identityMap.UserId[0].id"));
        assertEquals("token", sharedState.get("identityMap.PushId[0].id"));

        // verify no event dispatched
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(0));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));
        assertTrue(eventCaptor.getAllValues().isEmpty());

        // verify persistence
        verify(mockSharedPreferenceEditor, times(3)).putString(eq(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES), persistenceValueCaptor.capture());
        Map<String, String> persistedData = flattenJSONString(persistenceValueCaptor.getAllValues().get(2));
        assertNull(persistedData.get("identityMap.UserId[0].id"));
        assertEquals("token", persistedData.get("identityMap.PushId[0].id"));
    }

    @Test
    public void test_handleRemoveIdentity_DoNotRemoveReservedNamespace() throws Exception {
        // setup
        Map<String,Object> identityXDM = createXDMIdentityMap(
                new TestItem("GAID", "someGAID"),
                new TestItem("ECID", "someECID"),
                new TestItem("IDFA", "someIDFA")
        );
        JSONObject identityXDMJSON = new JSONObject(identityXDM);
        Mockito.when(mockSharedPreference.getString(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES, null)).thenReturn(identityXDMJSON.toString());

        final ArgumentCaptor<Map> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);
        final ArgumentCaptor<String> persistenceValueCaptor = ArgumentCaptor.forClass(String.class);
        extension = new IdentityExtension(mockExtensionApi);

        // test
        Map<String,Object> removedIdentityXDM = createXDMIdentityMap(
                new TestItem("GAID", "someGAID"),
                new TestItem("ecid", "someECID"),
                new TestItem("Idfa", "someIDFA")
        );
        Event removeIdentityEvent = buildRemoveIdentityRequest(removedIdentityXDM);
        extension.handleRemoveIdentity(removeIdentityEvent);

        // verify shared state
        verify(mockExtensionApi, times(1)).setXDMSharedEventState(sharedStateCaptor.capture(), eq(removeIdentityEvent), any(ExtensionErrorCallback.class));
        Map<String, String> sharedState = flattenMap(sharedStateCaptor.getValue());
        assertEquals("someGAID", sharedState.get("identityMap.GAID[0].id"));
        assertEquals("someECID", sharedState.get("identityMap.ECID[0].id"));
        assertEquals("someIDFA", sharedState.get("identityMap.IDFA[0].id"));


        // verify persistence
        verify(mockSharedPreferenceEditor, times(2)).putString(eq(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES), persistenceValueCaptor.capture());
        Map<String, String> persistedData = flattenJSONString(persistenceValueCaptor.getAllValues().get(1));
        assertEquals("someGAID", persistedData.get("identityMap.GAID[0].id"));
        assertEquals("someECID", persistedData.get("identityMap.ECID[0].id"));
        assertEquals("someIDFA", persistedData.get("identityMap.IDFA[0].id"));
    }


    @Test
    public void test_handleRemoveIdentity_NullData() throws Exception {
        // setup
        Map<String,Object> identityXDM = createXDMIdentityMap(
                new TestItem("PushId", "token")
        );
        JSONObject identityXDMJSON = new JSONObject(identityXDM);
        Mockito.when(mockSharedPreference.getString(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES, null)).thenReturn(identityXDMJSON.toString());
        extension = new IdentityExtension(mockExtensionApi);

        // test
        Event removeIdentityEvent = buildRemoveIdentityRequest(null);
        extension.handleRemoveIdentity(removeIdentityEvent);

        // verify shared state
        verify(mockExtensionApi, times(0)).setXDMSharedEventState(any(Map.class), eq(removeIdentityEvent), any(ExtensionErrorCallback.class));

        // verify persistence
        verify(mockSharedPreferenceEditor, times(2)).putString(eq(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES), anyString());// once during constructor and other during remove IdentityEvent
    }


    // ========================================================================================
    // private helper methods
    // ========================================================================================

    private void setupExistingIdentityProps(final ECID ecid) {
        IdentityProperties persistedProps = new IdentityProperties();
        persistedProps.setECID(ecid);
        final JSONObject jsonObject = new JSONObject(persistedProps.toXDMData(false));
        final String propsJSON = jsonObject.toString();
        Mockito.when(mockSharedPreference.getString(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES, null)).thenReturn(propsJSON);
    }

}
