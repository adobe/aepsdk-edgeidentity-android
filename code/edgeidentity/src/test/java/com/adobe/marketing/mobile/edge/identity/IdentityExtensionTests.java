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

import static com.adobe.marketing.mobile.edge.identity.IdentityTestUtil.buildUpdateIdentityRequest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.clearInvocations;
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
		Mockito.when(mockContext.getSharedPreferences(IdentityConstants.DataStoreKey.DATASTORE_NAME,
					 0)).thenReturn(mockSharedPreference);
		Mockito.when(mockSharedPreference.edit()).thenReturn(mockSharedPreferenceEditor);

		extension = new IdentityExtension(mockExtensionApi);

		// simulate bootup
		extension.bootupIfReady();
		verify(mockExtensionApi, times(1)).setXDMSharedEventState(any(Map.class), nullable(Event.class),
				any(ExtensionErrorCallback.class));
		clearInvocations(mockExtensionApi);
	}

	// ========================================================================================
	// constructor
	// ========================================================================================
	@Test
	public void test_ListenersRegistration() {
		// setup
		final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

		// test
		extension = new IdentityExtension(mockExtensionApi);

		// verify 2 listeners are registered
		verify(mockExtensionApi, times(6)).registerEventListener(anyString(),
				anyString(), any(Class.class), any(ExtensionErrorCallback.class));

		// verify listeners are registered with correct event source and type
		verify(mockExtensionApi, times(1)).registerEventListener(eq(IdentityConstants.EventType.EDGE_IDENTITY),
				eq(IdentityConstants.EventSource.REQUEST_IDENTITY), eq(ListenerEdgeIdentityRequestIdentity.class),
				callbackCaptor.capture());
		verify(mockExtensionApi, times(1)).registerEventListener(eq(IdentityConstants.EventType.EDGE_IDENTITY),
				eq(IdentityConstants.EventSource.UPDATE_IDENTITY), eq(ListenerEdgeIdentityUpdateIdentity.class),
				callbackCaptor.capture());
		verify(mockExtensionApi, times(1)).registerEventListener(eq(IdentityConstants.EventType.EDGE_IDENTITY),
				eq(IdentityConstants.EventSource.REMOVE_IDENTITY), eq(ListenerEdgeIdentityRemoveIdentity.class),
				callbackCaptor.capture());
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
		Event event = new Event.Builder("Test event", IdentityConstants.EventType.EDGE_IDENTITY,
										IdentityConstants.EventSource.REQUEST_IDENTITY).build();
		final ArgumentCaptor<Event> responseEventCaptor = ArgumentCaptor.forClass(Event.class);
		final ArgumentCaptor<Event> requestEventCaptor = ArgumentCaptor.forClass(Event.class);

		// test
		extension.handleIdentityRequest(event);

		// verify
		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchResponseEvent(responseEventCaptor.capture(), requestEventCaptor.capture(),
										 any(ExtensionErrorCallback.class));

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
		ECID existingECID = new ECID();
		IdentityProperties persistedProps = new IdentityProperties();
		persistedProps.setECID(existingECID);
		PowerMockito.stub(PowerMockito.method(IdentityState.class, "getIdentityProperties")).toReturn(persistedProps);

		Event event = new Event.Builder("Test event", IdentityConstants.EventType.EDGE_IDENTITY,
										IdentityConstants.EventSource.REQUEST_IDENTITY).build();
		final ArgumentCaptor<Event> responseEventCaptor = ArgumentCaptor.forClass(Event.class);
		final ArgumentCaptor<Event> requestEventCaptor = ArgumentCaptor.forClass(Event.class);

		// test
		extension = new IdentityExtension(mockExtensionApi);
		extension.handleIdentityRequest(event);

		// verify
		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchResponseEvent(responseEventCaptor.capture(), requestEventCaptor.capture(),
										 any(ExtensionErrorCallback.class));

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

		Event event = new Event.Builder("Test event", IdentityConstants.EventType.EDGE_IDENTITY,
										IdentityConstants.EventSource.REQUEST_IDENTITY).build();
		final ArgumentCaptor<Event> responseEventCaptor = ArgumentCaptor.forClass(Event.class);
		final ArgumentCaptor<Event> requestEventCaptor = ArgumentCaptor.forClass(Event.class);

		// test
		extension.handleIdentityRequest(event);

		// verify
		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchResponseEvent(responseEventCaptor.capture(), requestEventCaptor.capture(),
										 any(ExtensionErrorCallback.class));

		// verify response event containing ECID is dispatched
		Event ecidResponseEvent = responseEventCaptor.getAllValues().get(0);
		final Map<String, Object> xdmData = ecidResponseEvent.getEventData();
		final Map<String, Object> identityMap = (Map<String, Object>) xdmData.get("identityMap");

		assertTrue(identityMap.isEmpty());
	}

	@Test
	public void test_handleIdentityResetRequest() {
		// setup
		Event event = new Event.Builder("Test event", IdentityConstants.EventType.GENERIC_IDENTITY,
										IdentityConstants.EventSource.REQUEST_RESET).build();
		final ArgumentCaptor<Map> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

		// test
		extension.handleRequestReset(event);

		// verify
		verify(mockExtensionApi, times(1)).setXDMSharedEventState(sharedStateCaptor.capture(), eq(event),
				any(ExtensionErrorCallback.class));
		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

		Map<String, String> sharedState = flattenMap(sharedStateCaptor.getValue());
		assertTrue(sharedState.get("identityMap.ECID[0].id").length() > 0);
	}

	@Test
	public void test_handleHubSharedState_updateLegacyEcidOnDirectIdentityStateChange() {
		final ECID existingECID = new ECID();
		setupExistingIdentityProps(existingECID);
		setIdentityDirectSharedState("1234");

		Event event = new Event.Builder("Test event",
										IdentityConstants.EventType.HUB,
										IdentityConstants.EventSource.SHARED_STATE)
		.setEventData(new HashMap<String, Object>() {
			{
				put(IdentityConstants.SharedState.STATE_OWNER, IdentityConstants.SharedState.IdentityDirect.NAME);
			}
		}).build();

		extension.handleHubSharedState(event);

		final ArgumentCaptor<Map<String, Object>> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);
		verify(mockExtensionApi, times(1)).setXDMSharedEventState(sharedStateCaptor.capture(), eq(event),
				any(ExtensionErrorCallback.class));
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
		setIdentityDirectSharedState("1234");

		extension.handleHubSharedState(null);

		verify(mockExtensionApi, times(0)).setXDMSharedEventState(any(Map.class), any(Event.class),
				any(ExtensionErrorCallback.class));
	}

	@Test
	public void test_handleHubSharedState_noOpNullEventData() {
		setIdentityDirectSharedState("1234");

		Event event = new Event.Builder("Test event",
										IdentityConstants.EventType.HUB,
										IdentityConstants.EventSource.SHARED_STATE)
		.build();

		extension.handleHubSharedState(event);

		verify(mockExtensionApi, times(0)).setXDMSharedEventState(any(Map.class), any(Event.class),
				any(ExtensionErrorCallback.class));
	}

	@Test
	public void test_handleHubSharedState_noOpNotDirectIdentityStateChange() {
		setIdentityDirectSharedState("1234");

		Event event = new Event.Builder("Test event",
										IdentityConstants.EventType.HUB,
										IdentityConstants.EventSource.SHARED_STATE)
		.setEventData(new HashMap<String, Object>() {
			{
				put(IdentityConstants.SharedState.STATE_OWNER, "com.adobe.module.configuration");
			}
		}).build();

		extension.handleHubSharedState(event);

		verify(mockExtensionApi, times(0)).setXDMSharedEventState(any(Map.class), any(Event.class),
				any(ExtensionErrorCallback.class));
	}

	@Test
	public void test_handleHubSharedState_noOpNoDirectIdentitySharedState() {
		when(mockExtensionApi.getSharedEventState(eq(IdentityConstants.SharedState.IdentityDirect.NAME),
				any(Event.class),
				any(ExtensionErrorCallback.class)))
		.thenReturn(null);

		Event event = new Event.Builder("Test event",
										IdentityConstants.EventType.HUB,
										IdentityConstants.EventSource.SHARED_STATE)
		.setEventData(new HashMap<String, Object>() {
			{
				put(IdentityConstants.SharedState.STATE_OWNER, IdentityConstants.SharedState.IdentityDirect.NAME);
			}
		}).build();

		extension.handleHubSharedState(event);

		verify(mockExtensionApi, times(0)).setXDMSharedEventState(any(Map.class), any(Event.class),
				any(ExtensionErrorCallback.class));
	}

	@Test
	public void test_handleHubSharedState_doesNotShareStateIfLegacyECIDDoesNotChange() {
		setIdentityDirectSharedState("1234");

		Event event = new Event.Builder("Test event",
										IdentityConstants.EventType.HUB,
										IdentityConstants.EventSource.SHARED_STATE)
		.setEventData(new HashMap<String, Object>() {
			{
				put(IdentityConstants.SharedState.STATE_OWNER, IdentityConstants.SharedState.IdentityDirect.NAME);
			}
		}).build();

		// IdentityState.updateLegacyExperienceCloudId returns false if Legacy ECID was not updated
		PowerMockito.stub(PowerMockito.method(IdentityState.class, "updateLegacyExperienceCloudId")).toReturn(false);

		extension.handleHubSharedState(event);

		final ArgumentCaptor<Map<String, Object>> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);
		verify(mockExtensionApi, times(0)).setXDMSharedEventState(sharedStateCaptor.capture(), eq(event),
				any(ExtensionErrorCallback.class));
	}

	// ========================================================================================
	// handleUpdateIdentities
	// ========================================================================================

	@Test
	public void test_handleUpdateIdentities_whenValidData_updatesCustomerIdentifiers_updatesSharedState() {
		// setup
		MockIdentityState mockIdentityState = new MockIdentityState(new IdentityProperties());
		extension.state = mockIdentityState;

		// test
		Map<String, Object> identityXDM = createXDMIdentityMap(
											  new TestItem("id1", "somevalue"),
											  new TestItem("id2", "othervalue")
										  );
		Event updateIdentityEvent = buildUpdateIdentityRequest(identityXDM);
		extension.handleUpdateIdentities(updateIdentityEvent);

		// verify identifiers updated
		assertEquals(1, mockIdentityState.updateCustomerIdentifiersCalledTimes);
		assertEquals(identityXDM, mockIdentityState.updateCustomerIdentifiersParams.get(0).asXDMMap());

		// verify no event dispatched
		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(0));
		MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
	}

	@Test
	public void test_handleUpdateIdentities_nullEventData_returns() {
		// setup
		MockIdentityState mockIdentityState = new MockIdentityState(new IdentityProperties());
		extension.state = mockIdentityState;

		// test
		Event updateIdentityEvent = buildUpdateIdentityRequest(null);
		extension.handleUpdateIdentities(updateIdentityEvent);

		// verify identifiers not updated
		assertEquals(0, mockIdentityState.updateCustomerIdentifiersCalledTimes);

		// verify shared state
		verify(mockExtensionApi, times(0)).setXDMSharedEventState(any(Map.class), any(Event.class),
				any(ExtensionErrorCallback.class));
	}

	// ========================================================================================
	// handleRemoveIdentity
	// ========================================================================================
	@Test
	public void test_handleRemoveIdentity_whenValidData_removesCustomerIdentifiers_updatesSharedState() {
		// setup
		Map<String, Object> identityXDM = createXDMIdentityMap(
											  new TestItem("UserId", "secretID"),
											  new TestItem("PushId", "token")
										  );
		MockIdentityState mockIdentityState = new MockIdentityState(new IdentityProperties(identityXDM));
		extension.state = mockIdentityState;

		// test
		Map<String, Object> removedIdentityXDM = createXDMIdentityMap(
					new TestItem("UserId", "secretID")
				);
		Event removeIdentityEvent = buildRemoveIdentityRequest(removedIdentityXDM);
		extension.handleRemoveIdentity(removeIdentityEvent);

		// verify identifiers removed
		assertEquals(1, mockIdentityState.removeCustomerIdentifiersCalledTimes);
		assertEquals(removedIdentityXDM, mockIdentityState.removeCustomerIdentifiersParams.get(0).asXDMMap());

		// verify shared state
		final ArgumentCaptor<Map> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);
		verify(mockExtensionApi, times(1)).setXDMSharedEventState(sharedStateCaptor.capture(), eq(removeIdentityEvent),
				any(ExtensionErrorCallback.class));

		// verify no event dispatched
		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(0));
		MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
	}

	@Test
	public void test_handleRemoveIdentity_whenNullData_returns() {
		// setup
		Map<String, Object> identityXDM = createXDMIdentityMap(
											  new TestItem("PushId", "token")
										  );
		MockIdentityState mockIdentityState = new MockIdentityState(new IdentityProperties(identityXDM));
		extension.state = mockIdentityState;

		// test
		Event removeIdentityEvent = buildRemoveIdentityRequest(null);
		extension.handleRemoveIdentity(removeIdentityEvent);

		// verify identifiers not removed
		assertEquals(0, mockIdentityState.removeCustomerIdentifiersCalledTimes);

		// verify shared state
		verify(mockExtensionApi, times(0)).setXDMSharedEventState(any(Map.class), eq(removeIdentityEvent),
				any(ExtensionErrorCallback.class));
	}

	@Test
	public void test_processCachedEvents_returnsWhenNotBooted() {
		Map<String, Object> identityXDM = createXDMIdentityMap(
											  new TestItem("space", "moon")
										  );
		MockIdentityState mockIdentityState = new MockIdentityState(new IdentityProperties(identityXDM));
		extension.state = mockIdentityState;

		// test
		extension.processAddEvent(buildUpdateIdentityRequest(identityXDM));
		extension.processAddEvent(buildUpdateIdentityRequest(identityXDM));

		// verify
		verify(mockExtensionApi, times(0)).setXDMSharedEventState(any(Map.class), any(Event.class),
				any(ExtensionErrorCallback.class));

	}

	@Test
	public void test_processCachedEvents_processesWhenBooted() {
		Map<String, Object> identityXDM = createXDMIdentityMap(
											  new TestItem("space", "moon")
										  );
		MockIdentityState mockIdentityState = new MockIdentityState(new IdentityProperties(identityXDM));
		mockIdentityState.hasBooted = true;
		extension.state = mockIdentityState;

		// test
		extension.processAddEvent(buildUpdateIdentityRequest(identityXDM));
		extension.processAddEvent(buildRemoveIdentityRequest(identityXDM));
		extension.processAddEvent(new Event.Builder("Test event", IdentityConstants.EventType.EDGE_IDENTITY,
								  IdentityConstants.EventSource.REQUEST_IDENTITY).build());
		extension.processAddEvent(new Event.Builder("Test event", IdentityConstants.EventType.GENERIC_IDENTITY,
								  IdentityConstants.EventSource.REQUEST_RESET).build());

		// verify
		verify(mockExtensionApi, times(3)).setXDMSharedEventState(any(Map.class), any(Event.class),
				any(ExtensionErrorCallback.class)); // request identity does not update shared state

	}

	// ========================================================================================
	// private helper methods
	// ========================================================================================

	private void setupExistingIdentityProps(final ECID ecid) {
		IdentityProperties persistedProps = new IdentityProperties();
		persistedProps.setECID(ecid);
		final JSONObject jsonObject = new JSONObject(persistedProps.toXDMData(false));
		final String propsJSON = jsonObject.toString();
		Mockito.when(mockSharedPreference.getString(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES,
					 null)).thenReturn(propsJSON);
	}

	private void setIdentityDirectSharedState(final String ecid) {
		when(mockExtensionApi.getSharedEventState(eq(IdentityConstants.SharedState.IdentityDirect.NAME),
				any(Event.class),
				any(ExtensionErrorCallback.class)))
		.thenReturn(new HashMap<String, Object>() {
			{
				put(IdentityConstants.SharedState.IdentityDirect.ECID, ecid);
			}
		});
	}
}
