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

import static com.adobe.marketing.mobile.edge.identity.IdentityTestUtil.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.SharedStateResolution;
import com.adobe.marketing.mobile.SharedStateResolver;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.SharedStateStatus;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class IdentityExtensionTests {

	private IdentityExtension extension;

	@Mock
	ExtensionApi mockExtensionApi;

	@Mock
	SharedStateResolver mockSharedStateResolver;

	@Mock
	IdentityState mockIdentityState;

	@Before
	public void setup() {
		MockitoAnnotations.openMocks(this);
	}

	// ========================================================================================
	// onRegistered
	// ========================================================================================
	@Test
	public void test_onRegistered_registersListeners() {
		// test
		extension = new IdentityExtension(mockExtensionApi);
		extension.onRegistered();

		// verify
		verify(mockExtensionApi)
			.registerEventListener(eq(EventType.GENERIC_IDENTITY), eq(EventSource.REQUEST_CONTENT), any());
		verify(mockExtensionApi)
			.registerEventListener(eq(EventType.GENERIC_IDENTITY), eq(EventSource.REQUEST_RESET), any());
		verify(mockExtensionApi)
			.registerEventListener(eq(EventType.EDGE_IDENTITY), eq(EventSource.REQUEST_IDENTITY), any());
		verify(mockExtensionApi)
			.registerEventListener(eq(EventType.EDGE_IDENTITY), eq(EventSource.UPDATE_IDENTITY), any());
		verify(mockExtensionApi)
			.registerEventListener(eq(EventType.EDGE_IDENTITY), eq(EventSource.REMOVE_IDENTITY), any());
		verify(mockExtensionApi).registerEventListener(eq(EventType.HUB), eq(EventSource.SHARED_STATE), any());

		verifyNoMoreInteractions(mockExtensionApi);
	}

	// ========================================================================================
	// getName
	// ========================================================================================
	@Test
	public void test_getName() {
		// test
		extension = new IdentityExtension(mockExtensionApi);
		final String moduleName = extension.getName();
		assertEquals("getName should return the correct module name", IdentityConstants.EXTENSION_NAME, moduleName);
	}

	// ========================================================================================
	// getFriendlyName
	// ========================================================================================
	@Test
	public void test_getFriendlyName() {
		extension = new IdentityExtension(mockExtensionApi);

		final String extensionFriendlyName = extension.getFriendlyName();
		assertEquals(
			"getFriendlyName should return the correct friendly name",
			IdentityConstants.EXTENSION_FRIENDLY_NAME,
			extensionFriendlyName
		);
	}

	// ========================================================================================
	// getVersion
	// ========================================================================================
	@Test
	public void test_getVersion() {
		// test
		extension = new IdentityExtension(mockExtensionApi);
		final String moduleVersion = extension.getVersion();
		assertEquals(
			"getVersion should return the correct module version",
			IdentityConstants.EXTENSION_VERSION,
			moduleVersion
		);
	}

	// ========================================================================================
	// readyForEvent(Event event)
	// ========================================================================================
	@Test
	public void test_readyForEvent_cannotBoot() {
		// setup
		when(mockIdentityState.bootupIfReady(any())).thenReturn(false);

		// test and verify
		extension = new IdentityExtension(mockExtensionApi, mockIdentityState);
		assertFalse(extension.readyForEvent(buildUpdateIdentityRequest(Collections.EMPTY_MAP)));
	}

	@Test
	public void test_readyForEvent_GetUrlVariablesRequestButConfigurationStateUnavailable() {
		// setup
		when(mockIdentityState.bootupIfReady(any())).thenReturn(true);
		Event event = new Event.Builder("Test event", EventType.EDGE_IDENTITY, EventSource.REQUEST_IDENTITY)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put("urlvariables", true);
					}
				}
			)
			.build();

		when(
			mockExtensionApi.getSharedState(
				IdentityConstants.SharedState.Configuration.NAME,
				event,
				false,
				SharedStateResolution.LAST_SET
			)
		)
			.thenReturn(null);

		// test and verify
		extension = new IdentityExtension(mockExtensionApi, mockIdentityState);
		assertFalse(extension.readyForEvent(event));
	}

	@Test
	public void test_readyForEvent_GetUrlVariablesRequest_ConfigurationStatePending() {
		// setup
		when(mockIdentityState.bootupIfReady(any())).thenReturn(true);
		Event event = new Event.Builder("Test event", EventType.EDGE_IDENTITY, EventSource.REQUEST_IDENTITY)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put("urlvariables", true);
					}
				}
			)
			.build();

		when(
			mockExtensionApi.getSharedState(
				IdentityConstants.SharedState.Configuration.NAME,
				event,
				false,
				SharedStateResolution.LAST_SET
			)
		)
			.thenReturn(new SharedStateResult(SharedStateStatus.PENDING, null));

		// test and verify
		extension = new IdentityExtension(mockExtensionApi, mockIdentityState);
		assertFalse(extension.readyForEvent(event));
	}

	@Test
	public void test_readyForEvent_GetUrlVariablesRequest_ConfigurationStateSet() {
		// setup
		when(mockIdentityState.bootupIfReady(any())).thenReturn(true);
		Event event = new Event.Builder("Test event", EventType.EDGE_IDENTITY, EventSource.REQUEST_IDENTITY)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put("urlvariables", true);
					}
				}
			)
			.build();

		when(
			mockExtensionApi.getSharedState(
				IdentityConstants.SharedState.Configuration.NAME,
				event,
				false,
				SharedStateResolution.LAST_SET
			)
		)
			.thenReturn(new SharedStateResult(SharedStateStatus.SET, Collections.EMPTY_MAP));

		// test and verify
		extension = new IdentityExtension(mockExtensionApi, mockIdentityState);
		assertTrue(extension.readyForEvent(event));
	}

	@Test
	public void test_readyForEvent_OtherEvent_canBoot() {
		// setup
		when(mockIdentityState.bootupIfReady(any())).thenReturn(true);

		// test and verify
		extension = new IdentityExtension(mockExtensionApi, mockIdentityState);
		assertTrue(extension.readyForEvent(buildUpdateIdentityRequest(Collections.EMPTY_MAP)));
	}

	// ========================================================================================
	// handleRequestIdentity
	// ========================================================================================

	@Test
	public void test_handleRequestIdentity_DispatchesResponseEvent_PersistedECIDExists() {
		// setup
		final IdentityProperties properties = new IdentityProperties();
		properties.setECID(new ECID()); // simulate persisted ECID
		when(mockIdentityState.getIdentityProperties()).thenReturn(properties);

		extension = new IdentityExtension(mockExtensionApi, mockIdentityState);

		Event event = new Event.Builder("Test event", EventType.EDGE_IDENTITY, EventSource.REQUEST_IDENTITY).build();
		final ArgumentCaptor<Event> responseEventCaptor = ArgumentCaptor.forClass(Event.class);

		// test
		extension.handleRequestIdentity(event);

		// verify that an event is dispatched
		verify(mockExtensionApi).dispatch(responseEventCaptor.capture());

		// verify that it is a response event to the incident event
		final Event capturedResponseEvent = responseEventCaptor.getValue();
		assertEquals(capturedResponseEvent.getResponseID(), event.getUniqueIdentifier());

		// verify that the data sent in the response event contains the ecid from properties
		final Event ecidResponseEvent = responseEventCaptor.getAllValues().get(0);
		final IdentityMap identityMap = IdentityMap.fromXDMMap(ecidResponseEvent.getEventData());
		assertNotNull(identityMap);
		final String ecid = identityMap.getIdentityItemsForNamespace("ECID").get(0).getId();
		assertEquals(ecid, properties.getECID().toString());
	}

	// ========================================================================================
	// handleIdentityDirectECIDUpdate
	// ========================================================================================

	@Test
	public void test_handleIdentityDirectECIDUpdate_notAnIdentityDirectStateUpdate() {
		final Event event = new Event.Builder(
			"Not an IdentityDirect State event",
			EventType.HUB,
			EventSource.SHARED_STATE
		)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(IdentityConstants.EventDataKeys.STATE_OWNER, "Some.Other.Extension.Name");
					}
				}
			)
			.build();

		extension = new IdentityExtension(mockExtensionApi, mockIdentityState);
		extension.handleIdentityDirectECIDUpdate(event);

		verify(mockExtensionApi, never()).getSharedState(any(), any(), anyBoolean(), any());
		verify(mockIdentityState, never()).updateLegacyExperienceCloudId(any());
		verify(mockExtensionApi, never()).createXDMSharedState(any(), any());
	}

	@Test
	public void test_handleIdentityDirectECIDUpdate_identityDirectStateResultIsNull() {
		final Event event = new Event.Builder("IdentityDirect State event", EventType.HUB, EventSource.SHARED_STATE)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(
							IdentityConstants.EventDataKeys.STATE_OWNER,
							IdentityConstants.SharedState.IdentityDirect.NAME
						);
					}
				}
			)
			.build();
		when(
			mockExtensionApi.getSharedState(
				IdentityConstants.SharedState.IdentityDirect.NAME,
				event,
				false,
				SharedStateResolution.LAST_SET
			)
		)
			.thenReturn(null);

		extension = new IdentityExtension(mockExtensionApi, mockIdentityState);
		extension.handleIdentityDirectECIDUpdate(event);

		verify(mockExtensionApi)
			.getSharedState(
				IdentityConstants.SharedState.IdentityDirect.NAME,
				event,
				false,
				SharedStateResolution.LAST_SET
			);
		verify(mockIdentityState, never()).updateLegacyExperienceCloudId(any());
		verify(mockExtensionApi, never()).createXDMSharedState(any(), any());
	}

	@Test
	public void test_handleIdentityDirectECIDUpdate_identityDirectStateIsNull() {
		final Event event = new Event.Builder("IdentityDirect State event", EventType.HUB, EventSource.SHARED_STATE)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(
							IdentityConstants.EventDataKeys.STATE_OWNER,
							IdentityConstants.SharedState.IdentityDirect.NAME
						);
					}
				}
			)
			.build();
		when(
			mockExtensionApi.getSharedState(
				IdentityConstants.SharedState.IdentityDirect.NAME,
				event,
				false,
				SharedStateResolution.LAST_SET
			)
		)
			.thenReturn(new SharedStateResult(SharedStateStatus.SET, null));

		extension = new IdentityExtension(mockExtensionApi, mockIdentityState);
		extension.handleIdentityDirectECIDUpdate(event);

		verify(mockExtensionApi)
			.getSharedState(
				IdentityConstants.SharedState.IdentityDirect.NAME,
				event,
				false,
				SharedStateResolution.LAST_SET
			);
		verify(mockIdentityState, never()).updateLegacyExperienceCloudId(any());
		verify(mockExtensionApi, never()).createXDMSharedState(any(), any());
	}

	@Test
	public void test_handleIdentityDirectECIDUpdate_legacyEcidUpdateFailed() {
		final Event event = new Event.Builder("IdentityDirect State event", EventType.HUB, EventSource.SHARED_STATE)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(
							IdentityConstants.EventDataKeys.STATE_OWNER,
							IdentityConstants.SharedState.IdentityDirect.NAME
						);
					}
				}
			)
			.build();

		when(
			mockExtensionApi.getSharedState(
				IdentityConstants.SharedState.IdentityDirect.NAME,
				event,
				false,
				SharedStateResolution.LAST_SET
			)
		)
			.thenReturn(
				new SharedStateResult(
					SharedStateStatus.SET,
					Collections.singletonMap(IdentityConstants.SharedState.IdentityDirect.ECID, null)
				)
			);
		when(mockIdentityState.updateLegacyExperienceCloudId(any())).thenReturn(false);

		extension = new IdentityExtension(mockExtensionApi, mockIdentityState);
		extension.handleIdentityDirectECIDUpdate(event);

		verify(mockExtensionApi)
			.getSharedState(
				IdentityConstants.SharedState.IdentityDirect.NAME,
				event,
				false,
				SharedStateResolution.LAST_SET
			);
		verify(mockIdentityState).updateLegacyExperienceCloudId(null);
		verify(mockExtensionApi, never()).createXDMSharedState(any(), any());
	}

	@Test
	public void test_handleIdentityDirectECIDUpdate_identityDirectStateValidECID() {
		final Event event = new Event.Builder("IdentityDirect State event", EventType.HUB, EventSource.SHARED_STATE)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(
							IdentityConstants.EventDataKeys.STATE_OWNER,
							IdentityConstants.SharedState.IdentityDirect.NAME
						);
					}
				}
			)
			.build();

		final ECID legacyEcid = new ECID();

		when(
			mockExtensionApi.getSharedState(
				IdentityConstants.SharedState.IdentityDirect.NAME,
				event,
				false,
				SharedStateResolution.LAST_SET
			)
		)
			.thenReturn(
				new SharedStateResult(
					SharedStateStatus.SET,
					new HashMap<String, Object>() {
						{
							{
								put(IdentityConstants.SharedState.IdentityDirect.ECID, legacyEcid.toString());
							}
						}
					}
				)
			);
		when(mockIdentityState.updateLegacyExperienceCloudId(any())).thenReturn(true);

		extension = new IdentityExtension(mockExtensionApi, mockIdentityState);
		when(mockIdentityState.getIdentityProperties()).thenReturn(new IdentityProperties());

		extension.handleIdentityDirectECIDUpdate(event);

		verify(mockExtensionApi)
			.getSharedState(
				IdentityConstants.SharedState.IdentityDirect.NAME,
				event,
				false,
				SharedStateResolution.LAST_SET
			);
		verify(mockIdentityState).updateLegacyExperienceCloudId(legacyEcid);
		verify(mockExtensionApi)
			.createXDMSharedState(mockIdentityState.getIdentityProperties().toXDMData(false), event);
	}

	// ========================================================================================
	// handleUrlVariablesRequest
	// ========================================================================================

	@Test
	public void test_handleUrlVariablesRequest_whenOrgIdAndECIDNotPresent_returnsNull() {
		// setup
		Event event = new Event.Builder("Test event", EventType.EDGE_IDENTITY, EventSource.REQUEST_IDENTITY)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put("urlvariables", true);
					}
				}
			)
			.build();
		// Simulate null config state
		when(
			mockExtensionApi.getSharedState(
				IdentityConstants.SharedState.Configuration.NAME,
				event,
				false,
				SharedStateResolution.LAST_SET
			)
		)
			.thenReturn(null);

		final IdentityProperties properties = new IdentityProperties();
		when(mockIdentityState.getIdentityProperties()).thenReturn(properties);
		extension = new IdentityExtension(mockExtensionApi, mockIdentityState);

		final ArgumentCaptor<Event> responseEventCaptor = ArgumentCaptor.forClass(Event.class);

		// test
		extension.handleUrlVariablesRequest(event);

		// verify that response event is dispatched
		verify(mockExtensionApi).dispatch(responseEventCaptor.capture());

		// verify that the response event contains "urlvariables" property
		final Event capturedResponseEvent = responseEventCaptor.getValue();
		final Map<String, Object> data = capturedResponseEvent.getEventData();
		assertTrue(data.containsKey("urlvariables"));

		// verify that the response event data has no values for "urlvariables" key
		final String urlvariables = (String) data.get("urlvariables");
		assertNull(urlvariables);
	}

	@Test
	public void test_handleUrlVariablesRequest_whenOrgIdPresentAndECIDNotPresent_returnsNull() {
		// setup
		Event event = new Event.Builder("Test event", EventType.EDGE_IDENTITY, EventSource.REQUEST_IDENTITY)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put("urlvariables", true);
					}
				}
			)
			.build();

		// Simulate valid config state
		final SharedStateResult configSharedStateResult = new SharedStateResult(
			SharedStateStatus.SET,
			Collections.singletonMap(
				IdentityConstants.SharedState.Configuration.EXPERIENCE_CLOUD_ORGID,
				"SomeOrgId@AdobeOrg"
			)
		);
		when(
			mockExtensionApi.getSharedState(
				IdentityConstants.SharedState.Configuration.NAME,
				event,
				false,
				SharedStateResolution.LAST_SET
			)
		)
			.thenReturn(configSharedStateResult);

		final IdentityProperties properties = new IdentityProperties(); // Simulate Absent ECID
		when(mockIdentityState.getIdentityProperties()).thenReturn(properties);
		extension = new IdentityExtension(mockExtensionApi, mockIdentityState);

		final ArgumentCaptor<Event> responseEventCaptor = ArgumentCaptor.forClass(Event.class);

		// test
		extension.handleUrlVariablesRequest(event);

		// verify that response event is dispatched
		verify(mockExtensionApi).dispatch(responseEventCaptor.capture());
		final Event capturedResponseEvent = responseEventCaptor.getValue();

		// verify that the response event is sent to the incident event
		assertEquals(event.getUniqueIdentifier(), capturedResponseEvent.getResponseID());

		// verify that the response event contains "urlvariables" property
		final Map<String, Object> data = capturedResponseEvent.getEventData();
		assertTrue(data.containsKey("urlvariables"));

		// verify that the response event data has no values for "urlvariables" key
		final String urlvariables = (String) data.get("urlvariables");
		assertNull(urlvariables);
	}

	@Test
	public void test_handleUrlVariablesRequest_whenOrgIdAndECIDPresent_returnsValidUrlVariables() {
		// setup
		Event event = new Event.Builder("Test event", EventType.EDGE_IDENTITY, EventSource.REQUEST_IDENTITY)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put("urlvariables", true);
					}
				}
			)
			.build();

		// Simulate valid config state
		final SharedStateResult configSharedStateResult = new SharedStateResult(
			SharedStateStatus.SET,
			Collections.singletonMap(
				IdentityConstants.SharedState.Configuration.EXPERIENCE_CLOUD_ORGID,
				"SomeOrgId@AdobeOrg"
			)
		);
		when(
			mockExtensionApi.getSharedState(
				IdentityConstants.SharedState.Configuration.NAME,
				event,
				false,
				SharedStateResolution.LAST_SET
			)
		)
			.thenReturn(configSharedStateResult);

		final IdentityProperties properties = new IdentityProperties();
		properties.setECID(new ECID());
		when(mockIdentityState.getIdentityProperties()).thenReturn(properties);

		extension = new IdentityExtension(mockExtensionApi, mockIdentityState);

		// test
		extension.handleUrlVariablesRequest(event);

		// verify that response event is dispatched
		final ArgumentCaptor<Event> responseEventCaptor = ArgumentCaptor.forClass(Event.class);
		verify(mockExtensionApi).dispatch(responseEventCaptor.capture());
		final Event capturedResponseEvent = responseEventCaptor.getValue();

		// verify that the response event is sent to the incident event
		assertEquals(event.getUniqueIdentifier(), capturedResponseEvent.getResponseID());

		// verify that the response event contains "urlvariables" property
		final Map<String, Object> data = capturedResponseEvent.getEventData();
		assertTrue(data.containsKey("urlvariables"));

		final String urlvariables = (String) data.get("urlvariables");
		final String expectedUrlVariableTSString = "adobe_mc=TS%3";
		final String expectedUrlVariableIdentifiersString =
			"%7CMCMID%3D" + properties.getECID() + "%7CMCORGID%3DSomeOrgId%40AdobeOrg";

		assertNotNull(urlvariables);
		assertTrue(urlvariables.contains("adobe_mc="));
		assertTrue(urlvariables.contains(expectedUrlVariableTSString));
		assertTrue(urlvariables.contains(expectedUrlVariableIdentifiersString));
	}

	// ========================================================================================
	// handleUpdateIdentities
	// ========================================================================================

	@Test
	public void test_handleUpdateIdentities_whenValidData_updatesCustomerIdentifiers_updatesSharedState() {
		// setup
		final IdentityProperties properties = new IdentityProperties();
		when(mockIdentityState.getIdentityProperties()).thenReturn(properties);

		// simulate an update to the identity properties
		doAnswer(invocation -> {
				final IdentityMap arg = (IdentityMap) invocation.getArgument(0);
				properties.updateCustomerIdentifiers(arg);
				return null;
			})
			.when(mockIdentityState)
			.updateCustomerIdentifiers(any());
		when(mockExtensionApi.createPendingXDMSharedState(any())).thenReturn(mockSharedStateResolver);

		extension = new IdentityExtension(mockExtensionApi, mockIdentityState);

		// test
		final Map<String, Object> identityXDM = createXDMIdentityMap(
			new TestItem("id1", "somevalue"),
			new TestItem("id2", "othervalue")
		);
		final Event updateIdentityEvent = new Event.Builder(
			"Update Identity Event",
			EventType.EDGE_IDENTITY,
			EventSource.UPDATE_IDENTITY
		)
			.setEventData(identityXDM)
			.build();
		extension.handleUpdateIdentities(updateIdentityEvent);

		// verify identifiers updated
		final ArgumentCaptor<IdentityMap> identityMapCaptor = ArgumentCaptor.forClass(IdentityMap.class);
		verify(mockIdentityState).updateCustomerIdentifiers(identityMapCaptor.capture());
		assertEquals(identityXDM, identityMapCaptor.getValue().asXDMMap());

		// verify pending state is created and resolved
		verify(mockExtensionApi).createPendingXDMSharedState(eq(updateIdentityEvent));
		verify(mockSharedStateResolver).resolve(eq(properties.toXDMData(false)));

		verify(mockExtensionApi, never()).dispatch(any());
	}

	@Test
	public void test_handleUpdateIdentities_nullEventData_returns() {
		// setup
		final IdentityProperties properties = new IdentityProperties();
		when(mockIdentityState.getIdentityProperties()).thenReturn(properties);
		when(mockExtensionApi.createPendingXDMSharedState(any())).thenReturn(mockSharedStateResolver);

		extension = new IdentityExtension(mockExtensionApi, mockIdentityState);

		// test
		final Event updateIdentityEvent = new Event.Builder(
			"Update Identity Event",
			EventType.EDGE_IDENTITY,
			EventSource.UPDATE_IDENTITY
		) // no event data
			.build();
		extension.handleUpdateIdentities(updateIdentityEvent);

		// verify that identifiers are not updated
		verify(mockIdentityState, never()).updateCustomerIdentifiers(any());
		// verify that no event is dispatched
		verify(mockExtensionApi, never()).dispatch(any());

		// verify pending state is created and resolved
		verify(mockExtensionApi).createPendingXDMSharedState(eq(updateIdentityEvent));
		verify(mockSharedStateResolver).resolve(eq(properties.toXDMData(false)));
	}

	@Test
	public void test_handleUpdateIdentities_EmptyEventData_returns() {
		// setup
		final IdentityProperties properties = new IdentityProperties();
		when(mockIdentityState.getIdentityProperties()).thenReturn(properties);
		when(mockExtensionApi.createPendingXDMSharedState(any())).thenReturn(mockSharedStateResolver);
		extension = new IdentityExtension(mockExtensionApi, mockIdentityState);

		// test
		final Event updateIdentityEvent = new Event.Builder(
			"Update Identity Event",
			EventType.EDGE_IDENTITY,
			EventSource.UPDATE_IDENTITY
		)
			.setEventData(new HashMap<>())
			.build();
		extension.handleUpdateIdentities(updateIdentityEvent);

		// verify that identifiers are not updated
		verify(mockIdentityState, never()).updateCustomerIdentifiers(any());
		// verify that no event is dispatched
		verify(mockExtensionApi, never()).dispatch(any());

		// verify pending state is created and resolved
		verify(mockExtensionApi).createPendingXDMSharedState(eq(updateIdentityEvent));
		verify(mockSharedStateResolver).resolve(eq(properties.toXDMData(false)));
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
		final IdentityProperties properties = new IdentityProperties(identityXDM);
		when(mockIdentityState.getIdentityProperties()).thenReturn(properties);
		doAnswer(
			new Answer() {
				@Override
				public Object answer(InvocationOnMock invocation) throws Throwable {
					final IdentityMap args = (IdentityMap) invocation.getArgument(0);
					properties.removeCustomerIdentifiers(args);
					return null;
				}
			}
		)
			.when(mockIdentityState)
			.removeCustomerIdentifiers(any());

		when(mockExtensionApi.createPendingXDMSharedState(any())).thenReturn(mockSharedStateResolver);

		extension = new IdentityExtension(mockExtensionApi, mockIdentityState);

		// test
		final Map<String, Object> removedIdentityXDM = createXDMIdentityMap(new TestItem("UserId", "secretID"));
		final Event removeIdentityEvent = buildRemoveIdentityRequest(removedIdentityXDM);
		extension.handleRemoveIdentity(removeIdentityEvent);

		// verify identifiers removed
		final ArgumentCaptor<IdentityMap> removedIdentityMapCaptor = ArgumentCaptor.forClass(IdentityMap.class);
		verify(mockIdentityState).removeCustomerIdentifiers(removedIdentityMapCaptor.capture());
		assertEquals(
			IdentityMap.fromXDMMap(removedIdentityXDM).toString(),
			removedIdentityMapCaptor.getValue().toString()
		);

		// verify pending state is created and resolved
		verify(mockExtensionApi).createPendingXDMSharedState(eq(removeIdentityEvent));
		verify(mockSharedStateResolver).resolve(eq(properties.toXDMData(false)));
	}

	@Test
	public void test_handleRemoveIdentity_whenNullData_returns() {
		// setup
		Map<String, Object> identityXDM = createXDMIdentityMap(
			new TestItem("UserId", "secretID"),
			new TestItem("PushId", "token")
		);
		final IdentityProperties properties = new IdentityProperties(identityXDM);
		when(mockIdentityState.getIdentityProperties()).thenReturn(properties);
		when(mockExtensionApi.createPendingXDMSharedState(any())).thenReturn(mockSharedStateResolver);
		extension = new IdentityExtension(mockExtensionApi, mockIdentityState);

		// test
		final Event removeIdentityEvent = buildRemoveIdentityRequest(null);
		extension.handleRemoveIdentity(removeIdentityEvent);

		// verify identifiers not removed
		verify(mockIdentityState, never()).removeCustomerIdentifiers(any());

		// verify pending state is created and resolved
		verify(mockExtensionApi).createPendingXDMSharedState(eq(removeIdentityEvent));
		verify(mockSharedStateResolver).resolve(eq(properties.toXDMData(false)));
	}

	@Test
	public void test_handleRemoveIdentity_eventWithEmptyData_returns() {
		// setup
		final IdentityProperties properties = new IdentityProperties();
		when(mockIdentityState.getIdentityProperties()).thenReturn(properties);
		when(mockExtensionApi.createPendingXDMSharedState(any())).thenReturn(mockSharedStateResolver);
		extension = new IdentityExtension(mockExtensionApi, mockIdentityState);

		// test
		final Event notARemoveIdentityEvent = buildRemoveIdentityRequest(Collections.EMPTY_MAP);
		extension.handleRemoveIdentity(notARemoveIdentityEvent);

		// verify identifiers not removed
		verify(mockIdentityState, never()).removeCustomerIdentifiers(any());

		// verify pending state is created and resolved
		verify(mockExtensionApi).createPendingXDMSharedState(eq(notARemoveIdentityEvent));
		verify(mockSharedStateResolver).resolve(eq(properties.toXDMData(false)));
	}

	// ========================================================================================
	// handleRequestContent
	// ========================================================================================

	@Test
	public void test_handleRequestContent() {
		// Setup
		final Event event = new Event.Builder(
			"Test Ad ID event",
			EventType.GENERIC_IDENTITY,
			EventSource.REQUEST_CONTENT
		)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(IdentityConstants.EventDataKeys.ADVERTISING_IDENTIFIER, "adId");
					}
				}
			)
			.build();

		extension = new IdentityExtension(mockExtensionApi, mockIdentityState);

		// Test
		extension.handleRequestContent(event);

		verify(mockIdentityState).updateAdvertisingIdentifier(eq(event), any(SharedStateCallback.class));
	}

	@Test
	public void test_handleRequestContent_EventIsNotAdIdEvent() {
		// Setup
		final Event event = new Event.Builder(
			"Not an Ad ID event",
			EventType.GENERIC_IDENTITY,
			EventSource.REQUEST_CONTENT
		)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(IdentityConstants.EventDataKeys.URL_VARIABLES, "adId");
					}
				}
			)
			.build();

		extension = new IdentityExtension(mockExtensionApi, mockIdentityState);

		// Test
		extension.handleRequestContent(event);

		verify(mockIdentityState, never()).updateAdvertisingIdentifier(eq(event), any(SharedStateCallback.class));
	}

	// ========================================================================================
	// handleRequestReset
	// ========================================================================================

	@Test
	public void test_handleRequestReset() {
		final IdentityProperties properties = new IdentityProperties();
		when(mockIdentityState.getIdentityProperties()).thenReturn(properties);
		when(mockExtensionApi.createPendingXDMSharedState(any())).thenReturn(mockSharedStateResolver);

		extension = new IdentityExtension(mockExtensionApi, mockIdentityState);

		Event resetEvent = new Event.Builder("Test event", EventType.EDGE_IDENTITY, EventSource.REQUEST_IDENTITY)
			.build();

		extension.handleRequestReset(resetEvent);

		verify(mockIdentityState).resetIdentifiers();

		// verify pending state is created and resolved
		verify(mockExtensionApi).createPendingXDMSharedState(eq(resetEvent));
		verify(mockSharedStateResolver).resolve(eq(properties.toXDMData(false)));
	}
}
