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

import static com.adobe.marketing.mobile.edge.identity.IdentityTestUtil.createXDMIdentityMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.SharedStateStatus;
import com.adobe.marketing.mobile.services.DataStoring;
import com.adobe.marketing.mobile.services.NamedCollection;
import com.adobe.marketing.mobile.util.JSONAsserts;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class IdentityStateTests {

	@Mock
	private DataStoring mockDataStoreService;

	@Mock
	private NamedCollection mockEdgeIdentityNamedCollection;

	@Mock
	private NamedCollection mockDirectIdentityNamedCollection;

	@Mock
	private IdentityStorageManager mockIdentityStorageManager;

	@Mock
	private SharedStateCallback mockSharedStateCallback;

	@Before
	public void before() throws Exception {
		MockitoAnnotations.openMocks(this);

		when(mockDataStoreService.getNamedCollection(IdentityConstants.DataStoreKey.DATASTORE_NAME))
			.thenReturn(mockEdgeIdentityNamedCollection);
		when(mockDataStoreService.getNamedCollection(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_DATASTORE_NAME))
			.thenReturn(mockDirectIdentityNamedCollection);
	}

	@Test
	public void testBootUpIfReady_persistedECIDIsReused() {
		final IdentityProperties persistedProperties = new IdentityProperties();
		final ECID persistedECID = new ECID();
		persistedProperties.setECID(persistedECID);

		when(mockIdentityStorageManager.loadPropertiesFromPersistence()).thenReturn(persistedProperties);
		final IdentityState identityState = new IdentityState(mockIdentityStorageManager);

		assertTrue(identityState.bootupIfReady(mockSharedStateCallback));
		assertEquals(persistedProperties.getECID(), identityState.getIdentityProperties().getECID());
		verify(mockIdentityStorageManager, never()).savePropertiesToPersistence(identityState.getIdentityProperties());
		verify(mockSharedStateCallback)
			.createXDMSharedState(identityState.getIdentityProperties().toXDMData(false), null);
	}

	@Test
	public void testBootUpIfReady_waitsForHubSharedState_hubStateIsNull() {
		final IdentityState identityState = new IdentityState(mockIdentityStorageManager);
		when(mockSharedStateCallback.getSharedState(IdentityConstants.SharedState.Hub.NAME, null)).thenReturn(null);

		assertFalse(identityState.bootupIfReady(mockSharedStateCallback));
	}

	@Test
	public void testBootUpIfReady_waitsForHubSharedState_hubStateStatusIsPending() {
		final IdentityState identityState = new IdentityState(mockIdentityStorageManager);
		when(mockSharedStateCallback.getSharedState(IdentityConstants.SharedState.Hub.NAME, null))
			.thenReturn(new SharedStateResult(SharedStateStatus.PENDING, Collections.EMPTY_MAP));

		assertFalse(identityState.bootupIfReady(mockSharedStateCallback));
	}

	@Test
	public void testBootUpIfReady_waitsForHubSharedState_hubStateStatusIsSet() {
		final IdentityState identityState = new IdentityState(mockIdentityStorageManager);
		when(mockSharedStateCallback.getSharedState(IdentityConstants.SharedState.Hub.NAME, null))
			.thenReturn(new SharedStateResult(SharedStateStatus.SET, Collections.EMPTY_MAP));

		assertTrue(identityState.bootupIfReady(mockSharedStateCallback));
		assertNotNull(identityState.getIdentityProperties().getECID());
		verify(mockIdentityStorageManager).savePropertiesToPersistence(identityState.getIdentityProperties());
		verify(mockSharedStateCallback)
			.createXDMSharedState(identityState.getIdentityProperties().toXDMData(false), null);
	}

	@Test
	public void testBootUpIfReady_reUsesIdentityDirectEcidWhenAvailableAndNoPersistedECIDExists() {
		final IdentityState identityState = new IdentityState(mockIdentityStorageManager);
		final Map<String, Object> hubSharedState = new HashMap<>();
		hubSharedState.put(
			"extensions",
			new HashMap<String, Object>() {
				{
					put(
						"com.adobe.module.identity",
						new HashMap<String, String>() {
							{
								put("friendlyName", "Identity");
								put("version", "2.0.0");
							}
						}
					);
				}
			}
		);

		when(mockSharedStateCallback.getSharedState(IdentityConstants.SharedState.Hub.NAME, null))
			.thenReturn(new SharedStateResult(SharedStateStatus.SET, hubSharedState));
		final ECID fetchedDirectIdentityECID = new ECID();
		when(mockSharedStateCallback.getSharedState(IdentityConstants.SharedState.IdentityDirect.NAME, null))
			.thenReturn(
				new SharedStateResult(
					SharedStateStatus.SET,
					Collections.singletonMap(
						IdentityConstants.SharedState.IdentityDirect.ECID,
						fetchedDirectIdentityECID.toString()
					)
				)
			);

		assertTrue(identityState.bootupIfReady(mockSharedStateCallback));
		assertNotNull(identityState.getIdentityProperties().getECID());
		assertEquals(fetchedDirectIdentityECID, identityState.getIdentityProperties().getECID());
		verify(mockIdentityStorageManager).savePropertiesToPersistence(identityState.getIdentityProperties());
		verify(mockSharedStateCallback)
			.createXDMSharedState(identityState.getIdentityProperties().toXDMData(false), null);
	}

	@Test
	public void testBootUpIfReady_prefersPersistedIdentityDirectEcidOverFetchingFromState() {
		final IdentityState identityState = new IdentityState(mockIdentityStorageManager);

		final ECID persistedDirectIdentityECID = new ECID();
		when(mockIdentityStorageManager.loadEcidFromDirectIdentityPersistence())
			.thenReturn(persistedDirectIdentityECID);

		final Map<String, Object> hubSharedState = new HashMap<>();
		hubSharedState.put(
			"extensions",
			new HashMap<String, Object>() {
				{
					put(
						"com.adobe.module.identity",
						new HashMap<String, String>() {
							{
								put("friendlyName", "Identity");
								put("version", "2.0.0");
							}
						}
					);
				}
			}
		);

		when(mockSharedStateCallback.getSharedState(IdentityConstants.SharedState.Hub.NAME, null))
			.thenReturn(new SharedStateResult(SharedStateStatus.SET, hubSharedState));
		final ECID fetchedIdentityDirectECID = new ECID();
		when(mockSharedStateCallback.getSharedState(IdentityConstants.SharedState.IdentityDirect.NAME, null))
			.thenReturn(
				new SharedStateResult(
					SharedStateStatus.SET,
					Collections.singletonMap(
						IdentityConstants.SharedState.IdentityDirect.ECID,
						fetchedIdentityDirectECID.toString() // not the same persisted Identity ECID for the sake of tests
					)
				)
			);

		assertTrue(identityState.bootupIfReady(mockSharedStateCallback));
		assertNotNull(identityState.getIdentityProperties().getECID());
		assertEquals(persistedDirectIdentityECID, identityState.getIdentityProperties().getECID());
		verify(mockIdentityStorageManager).savePropertiesToPersistence(identityState.getIdentityProperties());
		verify(mockSharedStateCallback)
			.createXDMSharedState(identityState.getIdentityProperties().toXDMData(false), null);
	}

	@Test
	public void testBootUpIfReady_waitsForIdentityDirectStateIfRegistered_stateStatusIsNone() {
		final IdentityState identityState = new IdentityState(mockIdentityStorageManager);
		final Map<String, Object> hubSharedState = new HashMap<>();
		hubSharedState.put(
			"extensions",
			new HashMap<String, Object>() {
				{
					put(
						"com.adobe.module.identity",
						new HashMap<String, String>() {
							{
								put("friendlyName", "Identity");
								put("version", "2.0.0");
							}
						}
					);
				}
			}
		);

		when(mockSharedStateCallback.getSharedState(IdentityConstants.SharedState.Hub.NAME, null))
			.thenReturn(new SharedStateResult(SharedStateStatus.SET, hubSharedState));
		when(mockSharedStateCallback.getSharedState(IdentityConstants.SharedState.IdentityDirect.NAME, null))
			.thenReturn(new SharedStateResult(SharedStateStatus.NONE, null));

		assertFalse(identityState.bootupIfReady(mockSharedStateCallback));
		assertNull(identityState.getIdentityProperties().getECID());
		verify(mockIdentityStorageManager, never()).savePropertiesToPersistence(identityState.getIdentityProperties());
		verify(mockSharedStateCallback, times(0)).createXDMSharedState(any(), any());
	}

	@Test
	public void testBootUpIfReady_waitsForIdentityDirectStateIfRegistered() {
		final IdentityState identityState = new IdentityState(mockIdentityStorageManager);
		final Map<String, Object> hubSharedState = new HashMap<>();
		hubSharedState.put(
			"extensions",
			new HashMap<String, Object>() {
				{
					put(
						"com.adobe.module.identity",
						new HashMap<String, String>() {
							{
								put("friendlyName", "Identity");
								put("version", "2.0.0");
							}
						}
					);
				}
			}
		);

		when(mockSharedStateCallback.getSharedState(IdentityConstants.SharedState.Hub.NAME, null))
			.thenReturn(new SharedStateResult(SharedStateStatus.SET, hubSharedState));
		when(mockSharedStateCallback.getSharedState(IdentityConstants.SharedState.IdentityDirect.NAME, null))
			.thenReturn(null);

		assertFalse(identityState.bootupIfReady(mockSharedStateCallback));
		assertNull(identityState.getIdentityProperties().getECID());
		verify(mockIdentityStorageManager, never()).savePropertiesToPersistence(identityState.getIdentityProperties());
		verify(mockSharedStateCallback, times(0)).createXDMSharedState(any(), any());
	}

	@Test
	public void testBootUpIfReady_waitsForPendingIdentityDirectStateIfRegistered() {
		final IdentityState identityState = new IdentityState(mockIdentityStorageManager);
		final Map<String, Object> hubSharedState = new HashMap<>();
		hubSharedState.put(
			"extensions",
			new HashMap<String, Object>() {
				{
					put(
						"com.adobe.module.identity",
						new HashMap<String, String>() {
							{
								put("friendlyName", "Identity");
								put("version", "2.0.0");
							}
						}
					);
				}
			}
		);

		when(mockSharedStateCallback.getSharedState(IdentityConstants.SharedState.Hub.NAME, null))
			.thenReturn(new SharedStateResult(SharedStateStatus.SET, hubSharedState));
		when(mockSharedStateCallback.getSharedState(IdentityConstants.SharedState.IdentityDirect.NAME, null))
			.thenReturn(new SharedStateResult(SharedStateStatus.PENDING, null));

		assertFalse(identityState.bootupIfReady(mockSharedStateCallback));
		assertNull(identityState.getIdentityProperties().getECID());
		verify(mockIdentityStorageManager, never()).savePropertiesToPersistence(identityState.getIdentityProperties());
		verify(mockSharedStateCallback, times(0)).createXDMSharedState(any(), any());
	}

	@Test
	public void testBootUpIfReady_regeneratesECIDWhenIdentityDirectStateECIDIsNull() {
		final IdentityState identityState = new IdentityState(mockIdentityStorageManager);
		final Map<String, Object> hubSharedState = new HashMap<>();
		hubSharedState.put(
			"extensions",
			new HashMap<String, Object>() {
				{
					put(
						"com.adobe.module.identity",
						new HashMap<String, String>() {
							{
								put("friendlyName", "Identity");
								put("version", "2.0.0");
							}
						}
					);
				}
			}
		);

		when(mockSharedStateCallback.getSharedState(IdentityConstants.SharedState.Hub.NAME, null))
			.thenReturn(new SharedStateResult(SharedStateStatus.SET, hubSharedState));
		when(mockSharedStateCallback.getSharedState(IdentityConstants.SharedState.IdentityDirect.NAME, null))
			.thenReturn(new SharedStateResult(SharedStateStatus.SET, null));

		assertTrue(identityState.bootupIfReady(mockSharedStateCallback));
		assertNotNull(identityState.getIdentityProperties().getECID());
		verify(mockIdentityStorageManager).savePropertiesToPersistence(identityState.getIdentityProperties());
		verify(mockSharedStateCallback)
			.createXDMSharedState(identityState.getIdentityProperties().toXDMData(false), null);
	}

	@Test
	public void testBootUpIfReady_generatesNewECIDWhenDirectStateAndPersistedStateAreUnavailable() {
		// No persisted properties
		final IdentityProperties persistedProperties = new IdentityProperties();
		when(mockIdentityStorageManager.loadPropertiesFromPersistence()).thenReturn(persistedProperties);

		// No Identity direct extensions
		final Map<String, Object> hubSharedState = new HashMap<>();
		hubSharedState.put("extensions", new HashMap<String, Object>());
		when(mockSharedStateCallback.getSharedState(IdentityConstants.SharedState.Hub.NAME, null))
			.thenReturn(new SharedStateResult(SharedStateStatus.SET, hubSharedState));

		final IdentityState identityState = new IdentityState(mockIdentityStorageManager);

		assertTrue(identityState.bootupIfReady(mockSharedStateCallback));
		assertNotNull(identityState.getIdentityProperties().getECID());
		verify(mockIdentityStorageManager).savePropertiesToPersistence(identityState.getIdentityProperties());
		verify(mockSharedStateCallback)
			.createXDMSharedState(identityState.getIdentityProperties().toXDMData(false), null);
	}

	@Test
	public void testBootUpIfReady_doesNotBootMoreThanOnce() {
		// Simulate generating a new ECID  and booting
		// No persisted properties
		final IdentityProperties persistedProperties = new IdentityProperties();
		when(mockIdentityStorageManager.loadPropertiesFromPersistence()).thenReturn(persistedProperties);

		// No Identity direct extensions
		final Map<String, Object> hubSharedState = new HashMap<>();
		hubSharedState.put("extensions", new HashMap<String, Object>());
		when(mockSharedStateCallback.getSharedState(IdentityConstants.SharedState.Hub.NAME, null))
			.thenReturn(new SharedStateResult(SharedStateStatus.SET, hubSharedState));

		final IdentityState identityState = new IdentityState(mockIdentityStorageManager);

		// Verify that extension has booted
		assertTrue(identityState.bootupIfReady(mockSharedStateCallback));

		// Try calling boot up if ready once more
		assertTrue(identityState.bootupIfReady(mockSharedStateCallback));

		assertNotNull(identityState.getIdentityProperties().getECID());
		// verify that properties are set and saved only once
		verify(mockIdentityStorageManager, times(1)).savePropertiesToPersistence(identityState.getIdentityProperties());
		verify(mockSharedStateCallback, times(1))
			.createXDMSharedState(identityState.getIdentityProperties().toXDMData(false), null);
	}

	// ======================================================================================================================
	// Tests for method : resetIdentifiers()
	// ======================================================================================================================

	@Test
	public void testResetIdentifiers() {
		// setup
		final IdentityState state = new IdentityState(mockIdentityStorageManager);
		state.getIdentityProperties().setECID(new ECID());
		state.getIdentityProperties().setECIDSecondary(new ECID());
		state.getIdentityProperties().setAdId("adID");
		final ECID existingEcid = state.getIdentityProperties().getECID();

		try (MockedStatic<MobileCore> mockedStaticCore = Mockito.mockStatic(MobileCore.class)) {
			// test
			state.resetIdentifiers();

			// verify
			assertNotEquals(existingEcid, state.getIdentityProperties().getECID()); // ECID should be regenerated
			assertFalse(state.getIdentityProperties().getECID().toString().isEmpty()); // ECID should not be empty
			assertNull(state.getIdentityProperties().getECIDSecondary()); // should be cleared
			assertNull(state.getIdentityProperties().getAdId()); // should be cleared
			verify(mockIdentityStorageManager, times(1)).savePropertiesToPersistence(state.getIdentityProperties()); // should save to data store

			// Verify consent event not sent (or any event). Consent should not be dispatched by resetIdentifiers
			mockedStaticCore.verify(
				() -> {
					MobileCore.dispatchEvent(any());
				},
				never()
			);
		}
	}

	// ======================================================================================================================
	// Tests for method : updateCustomerIdentifiers(final IdentityMap map)
	// ======================================================================================================================

	@Test
	public void testUpdateCustomerIdentifiers_happy() throws Exception {
		// setup
		final IdentityState state = new IdentityState(mockIdentityStorageManager);

		// test
		final Map<String, Object> identityXDM = createXDMIdentityMap(
			new IdentityTestUtil.TestItem("UserId", "secretID")
		);
		state.updateCustomerIdentifiers(IdentityMap.fromXDMMap(identityXDM));

		// verify persistence
		final ArgumentCaptor<IdentityProperties> identityPropertiesArgumentCaptor = ArgumentCaptor.forClass(
			IdentityProperties.class
		);
		verify(mockIdentityStorageManager).savePropertiesToPersistence(identityPropertiesArgumentCaptor.capture());
		final IdentityProperties capturedIdentityProperties = identityPropertiesArgumentCaptor.getValue();
		assertEquals(identityXDM, capturedIdentityProperties.toXDMData(false));
	}

	@Test
	public void testUpdateCustomerIdentifiers_doesNotUpdateReservedNamespace() throws Exception {
		// setup
		IdentityState state = new IdentityState(mockIdentityStorageManager);
		state.getIdentityProperties().setECID(new ECID("internalECID"));

		// test
		final Map<String, Object> inputIdentityXDM = createXDMIdentityMap(
			new IdentityTestUtil.TestItem("ECID", "somevalue"),
			new IdentityTestUtil.TestItem("GAID", "somevalue"),
			new IdentityTestUtil.TestItem("IDFA", "somevalue"),
			new IdentityTestUtil.TestItem("IdFA", "somevalue"),
			new IdentityTestUtil.TestItem("gaid", "somevalue"),
			new IdentityTestUtil.TestItem("UserId", "somevalue")
		);
		state.updateCustomerIdentifiers(IdentityMap.fromXDMMap(inputIdentityXDM));

		// verify persistence
		final ArgumentCaptor<IdentityProperties> identityPropertiesArgumentCaptor = ArgumentCaptor.forClass(
			IdentityProperties.class
		);
		verify(mockIdentityStorageManager).savePropertiesToPersistence(identityPropertiesArgumentCaptor.capture());
		final IdentityProperties capturedIdentityProperties = identityPropertiesArgumentCaptor.getValue();
		final Map<String, Object> expectedIdentityXDM = createXDMIdentityMap(
			new IdentityTestUtil.TestItem("ECID", "internalECID"),
			new IdentityTestUtil.TestItem("UserId", "somevalue")
		);
		assertEquals(expectedIdentityXDM, capturedIdentityProperties.toXDMData(false));
	}

	@Test
	public void testUpdateCustomerIdentifiers_whenCaseSensitiveNamespace_storesAll() throws Exception {
		// setup
		final IdentityState state = new IdentityState(mockIdentityStorageManager);
		state.getIdentityProperties().setECID(new ECID("internalECID"));

		// test
		final Map<String, Object> identityXDM = createXDMIdentityMap(
			new IdentityTestUtil.TestItem("caseSensitive", "somevalue"),
			new IdentityTestUtil.TestItem("CASESENSITIVE", "SOMEVALUE")
		);
		state.updateCustomerIdentifiers(IdentityMap.fromXDMMap(identityXDM));

		// verify persistence
		final ArgumentCaptor<IdentityProperties> identityPropertiesArgumentCaptor = ArgumentCaptor.forClass(
			IdentityProperties.class
		);
		verify(mockIdentityStorageManager).savePropertiesToPersistence(identityPropertiesArgumentCaptor.capture());
		final Map<String, Object> expectedIdentityXDM = createXDMIdentityMap(
			new IdentityTestUtil.TestItem("ECID", "internalECID"),
			new IdentityTestUtil.TestItem("caseSensitive", "somevalue"),
			new IdentityTestUtil.TestItem("CASESENSITIVE", "SOMEVALUE")
		);
		final IdentityProperties capturedIdentityProperties = identityPropertiesArgumentCaptor.getValue();
		assertEquals(expectedIdentityXDM, capturedIdentityProperties.toXDMData(false));
	}

	// ======================================================================================================================
	// Tests for method : removeCustomerIdentifiers(final IdentityMap map)
	// ======================================================================================================================

	@Test
	public void testRemoveCustomerIdentifiers_happy() throws Exception {
		// setup
		final Map<String, Object> initialIdentityXDM = createXDMIdentityMap(
			new IdentityTestUtil.TestItem("UserId", "secretID"),
			new IdentityTestUtil.TestItem("PushId", "token")
		);
		final IdentityState state = new IdentityState(mockIdentityStorageManager);
		state.getIdentityProperties().updateCustomerIdentifiers(IdentityMap.fromXDMMap(initialIdentityXDM));
		state.getIdentityProperties().setECID(new ECID("internalECID"));

		// test
		final Map<String, Object> removedIdentityXDM = createXDMIdentityMap(
			new IdentityTestUtil.TestItem("UserId", "secretID")
		);
		state.removeCustomerIdentifiers(IdentityMap.fromXDMMap(removedIdentityXDM));

		final ArgumentCaptor<IdentityProperties> identityPropertiesArgumentCaptor = ArgumentCaptor.forClass(
			IdentityProperties.class
		);
		verify(mockIdentityStorageManager).savePropertiesToPersistence(identityPropertiesArgumentCaptor.capture());
		final Map<String, Object> expectedIdentityXDM = createXDMIdentityMap(
			new IdentityTestUtil.TestItem("ECID", "internalECID"),
			new IdentityTestUtil.TestItem("PushId", "token")
		);
		final IdentityProperties capturedIdentityProperties = identityPropertiesArgumentCaptor.getValue();
		assertEquals(expectedIdentityXDM, capturedIdentityProperties.toXDMData(false));
	}

	@Test
	public void testRemoveCustomerIdentifiers_doesNotRemoveReservedNamespace() throws Exception {
		final IdentityState state = new IdentityState(mockIdentityStorageManager);
		final ECID initialECID = new ECID();
		state.getIdentityProperties().setECID(initialECID);
		state.getIdentityProperties().setAdId("initialADID");

		final IdentityProperties initialProperties = state.getIdentityProperties();

		// test
		final Map<String, Object> removedIdentityXDM = createXDMIdentityMap(
			new IdentityTestUtil.TestItem("GAID", "initialECID"),
			new IdentityTestUtil.TestItem("ECID", initialECID.toString())
		);
		state.removeCustomerIdentifiers(IdentityMap.fromXDMMap(removedIdentityXDM));

		final ArgumentCaptor<IdentityProperties> identityPropertiesArgumentCaptor = ArgumentCaptor.forClass(
			IdentityProperties.class
		);
		verify(mockIdentityStorageManager).savePropertiesToPersistence(identityPropertiesArgumentCaptor.capture());
		final IdentityProperties capturedProperties = identityPropertiesArgumentCaptor.getValue();
		assertEquals(initialProperties.toXDMData(false), capturedProperties.toXDMData(false));
	}

	// ======================================================================================================================
	// Tests for method : updateLegacyExperienceCloudId(final IdentityMap map)
	// ======================================================================================================================

	@Test
	public void testUpdateLegacyExperienceCloudId() {
		final IdentityState state = new IdentityState(mockIdentityStorageManager);
		final ECID ecid = new ECID();
		state.getIdentityProperties().setECID(ecid);
		final ECID legacyEcid = new ECID();

		// test
		state.updateLegacyExperienceCloudId(legacyEcid);

		// verify
		assertEquals(legacyEcid, state.getIdentityProperties().getECIDSecondary());
		assertEquals(ecid, state.getIdentityProperties().getECID());
		verify(mockIdentityStorageManager).savePropertiesToPersistence(state.getIdentityProperties());
	}

	@Test
	public void testUpdateLegacyExperienceCloudId_notSetWhenECIDSame() {
		final IdentityState state = new IdentityState(mockIdentityStorageManager);
		final ECID legacyEcid = new ECID();
		state.getIdentityProperties().setECID(legacyEcid);

		state.updateLegacyExperienceCloudId(legacyEcid);

		// verify
		assertNull(state.getIdentityProperties().getECIDSecondary());
		verify(mockIdentityStorageManager, times(0)).savePropertiesToPersistence(any());
	}

	@Test
	public void testUpdateLegacyExperienceCloudId_notSetWhenSecondaryECIDSame() {
		final IdentityState state = new IdentityState(mockIdentityStorageManager);
		state.getIdentityProperties().setECID(new ECID());
		final ECID legacyEcid = new ECID();
		state.getIdentityProperties().setECIDSecondary(legacyEcid);

		state.updateLegacyExperienceCloudId(legacyEcid);

		assertEquals(legacyEcid, state.getIdentityProperties().getECIDSecondary());
		verify(mockIdentityStorageManager, times(0)).savePropertiesToPersistence(any());
	}

	@Test
	public void testUpdateLegacyExperienceCloudId_clearsOnNull() {
		final IdentityState state = new IdentityState(mockIdentityStorageManager);
		state.getIdentityProperties().setECID(new ECID());
		state.getIdentityProperties().setECIDSecondary(new ECID());

		state.updateLegacyExperienceCloudId(null);

		assertNull(state.getIdentityProperties().getECIDSecondary());
		verify(mockIdentityStorageManager, times(1)).savePropertiesToPersistence(state.getIdentityProperties());
	}

	@Test
	public void testUpdateLegacyExperienceCloudId_notSetWhenExistingIsNull() {
		final IdentityState state = new IdentityState(mockIdentityStorageManager);
		state.getIdentityProperties().setECID(new ECID());

		state.updateLegacyExperienceCloudId(null);

		assertNull(state.getIdentityProperties().getECIDSecondary());
		verify(mockIdentityStorageManager, times(0)).savePropertiesToPersistence(any());
	}

	// ======================================================================================================================
	// Tests for method : updateAdvertisingIdentifier(final Event event, final SharedStateCallback callback)
	// ======================================================================================================================

	// With consent change
	@Test
	public void testUpdateAdvertisingIdentifier_notSet_whenInitializingIdentityState() {
		final IdentityState state = new IdentityState(mockIdentityStorageManager);
		state.getIdentityProperties().setECID(new ECID());

		assertNull(state.getIdentityProperties().getAdId());
		verify(mockIdentityStorageManager, times(0)).savePropertiesToPersistence(any());
	}

	@Test
	public void testUpdateAdvertisingIdentifier_whenNull_thenChangedToValid() throws Exception {
		assertUpdateAdvertisingIdentifier(null, "adId", "adId", "y", true);
	}

	@Test
	public void testUpdateAdvertisingIdentifier_whenEmpty_thenChangedToValid() throws Exception {
		assertUpdateAdvertisingIdentifier("", "adId", "adId", "y", true);
	}

	@Test
	public void testUpdateAdvertisingIdentifier_whenValid_thenChangedToEmpty() throws Exception {
		assertUpdateAdvertisingIdentifier("oldAdId", "", null, "n", true);
	}

	@Test
	public void testUpdateAdvertisingIdentifier_whenValid_thenChangedToAllZeros() throws Exception {
		assertUpdateAdvertisingIdentifier("oldAdId", IdentityConstants.Default.ZERO_ADVERTISING_ID, null, "n", true);
	}

	@Test
	public void testUpdateAdvertisingIdentifier_whenValid_thenChangedToNull() throws Exception {
		assertUpdateAdvertisingIdentifier("oldAdId", null, null, "n", true);
	}

	// Without consent change
	@Test
	public void testUpdateAdvertisingIdentifier_whenValid_thenDifferentValid() throws Exception {
		assertUpdateAdvertisingIdentifier("oldAdId", "adId", "adId", null, true);
	}

	// Ad ID not updated
	@Test
	public void testUpdateAdvertisingIdentifier_whenNull_thenEmpty() throws Exception {
		assertUpdateAdvertisingIdentifier(null, "", null, null, false);
	}

	@Test
	public void testUpdateAdvertisingIdentifier_whenNull_thenAllZeros() throws Exception {
		assertUpdateAdvertisingIdentifier(null, IdentityConstants.Default.ZERO_ADVERTISING_ID, null, null, false);
	}

	@Test
	public void testUpdateAdvertisingIdentifier_whenEmpty_thenSame() throws Exception {
		assertUpdateAdvertisingIdentifier("", "", null, null, false);
	}

	@Test
	public void testUpdateAdvertisingIdentifier_whenEmpty_thenAllZeros() throws Exception {
		assertUpdateAdvertisingIdentifier("", IdentityConstants.Default.ZERO_ADVERTISING_ID, null, null, false);
	}

	@Test
	public void testUpdateAdvertisingIdentifier_whenValid_thenSame() throws Exception {
		assertUpdateAdvertisingIdentifier("adId", "adId", "adId", null, false);
	}

	// Check:
	// 1. Shared state only updated once
	//     mockSharedPreferenceEditor times(1) check
	// 2. Consent event only dispatched once
	//     verifyStatic mobilecore.dispatchevent
	//     2a. check validity of consent event; event data format exactly matches expected value
	//         consentEventCaptor -> consentEventData -> assertEquals
	// 3. Identity props ad ID matches the expected one after event dispatched
	//     assertEquals state.getIdentityProperties().getAdId()
	// 4. Identity props saved to persistence
	//     mockSharedPreferenceEditor
	//
	// Expected values should be hard-coded to prevent tautological test (one where the test must pass by definition,
	// because the expectation uses the same logic as the code being tested); has effect of checking:
	// - Exact text - values and case sensitivity
	// - Hierarchy of event data object

	/**
	 * Main entrypoint for advertising identifier state checks
	 *
	 * @param persistedAdId the ad ID that should be pre-set in IdentityState
	 * @param newAdId the ad ID that will be extracted from a generic Identity event to set the new ad ID
	 * @param expectedAdId the ad ID value that should be set in IdentityState after the updateAdvertisingIdentifier flow
	 * @param expectedConsent the consent value that should be dispatched after the updateAdvertisingIdentifier
	 *                           flow; should be null if no consent event should be dispatched
	 * @param isSharedStateUpdateExpected true if the shared state should be updated based on the
	 *                                       perisitedAdId -> newAdId combination; this will check the
	 *                                       persistent store, shared state, and identity map
	 * @throws Exception
	 */
	private void assertUpdateAdvertisingIdentifier(
		String persistedAdId,
		String newAdId,
		String expectedAdId,
		String expectedConsent,
		boolean isSharedStateUpdateExpected
	) throws Exception {
		// Setup

		final IdentityState state = new IdentityState(mockIdentityStorageManager);
		state.getIdentityProperties().setECID(new ECID());
		state.getIdentityProperties().setAdId(persistedAdId);
		final Event event = fakeGenericIdentityEvent(newAdId);
		try (MockedStatic<MobileCore> mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class)) {
			state.updateAdvertisingIdentifier(event, mockSharedStateCallback);

			// Verify consent event
			if (expectedConsent == null) {
				mockedStaticMobileCore.verify(() -> MobileCore.dispatchEvent(any()), never());
			} else {
				final ArgumentCaptor<Event> consentEventCaptor = ArgumentCaptor.forClass(Event.class);
				mockedStaticMobileCore.verify(() -> MobileCore.dispatchEvent(consentEventCaptor.capture()), times(1));

				final Event consentEvent = consentEventCaptor.getValue();

				String expected =
					"{" +
					"  \"consents\": {" +
					"    \"adID\": {" +
					"      \"val\": \"" +
					expectedConsent +
					"\"," +
					"      \"idType\": \"GAID\"" +
					"    }" +
					"  }" +
					"}";

				JSONAsserts.assertEquals(expected, consentEvent.getEventData());
			}
		}

		if (isSharedStateUpdateExpected) {
			final ArgumentCaptor<IdentityProperties> identityPropertiesArgumentCaptor = ArgumentCaptor.forClass(
				IdentityProperties.class
			);
			verify(mockIdentityStorageManager).savePropertiesToPersistence(identityPropertiesArgumentCaptor.capture());
			final IdentityProperties capturedProperties = identityPropertiesArgumentCaptor.getValue();
			final Map<String, Object> IdentityProertieseMap = capturedProperties.toXDMData(false);
			verifyIdentityMap(IdentityProertieseMap, expectedAdId, state.getIdentityProperties().getECID().toString());
			mockSharedStateCallback.createXDMSharedState(capturedProperties.toXDMData(false), event);
		} else {
			verify(mockIdentityStorageManager, times(0)).savePropertiesToPersistence(any());
			mockSharedStateCallback.createXDMSharedState(any(), any());
		}
		// Verify identity map
		final Map<String, Object> IdentityProertieseMap = state.getIdentityProperties().toXDMData(false);
		verifyIdentityMap(IdentityProertieseMap, expectedAdId, state.getIdentityProperties().getECID().toString());
		// Verify shared state and properties
		assertEquals(expectedAdId, state.getIdentityProperties().getAdId());
	}

	// Test helpers

	/**
	 * Creates an event with the given adId in data
	 * @param adId
	 * @return
	 */
	private Event fakeGenericIdentityEvent(final String adId) {
		return new Event.Builder("Test event", EventType.GENERIC_IDENTITY, EventSource.REQUEST_IDENTITY)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(IdentityConstants.EventDataKeys.ADVERTISING_IDENTIFIER, adId);
					}
				}
			)
			.build();
	}

	/**
	 * Verifies the Identity map contains the required ad ID and ECID
	 * Valid ECID string and identity map is always required
	 * @param IdentityMap the identity map to check
	 * @param expectedAdId the ad ID to check, can be null if no ad ID should be present; then the absence of ad ID will be verified
	 * @param expectedECID the ECID string to check; must not be null (since this is the booted state)
	 * @return true if identity map contains the required identity properties, false otherwise
	 */
	private void verifyIdentityMap(
		@NonNull final Map<String, Object> IdentityMap,
		@Nullable final String expectedAdId,
		@NonNull final String expectedECID
	) {
		if (expectedAdId != null) {
			String expected =
				"{" +
				"  \"identityMap\": {" +
				"    \"GAID\": [" +
				"      {" +
				"        \"id\": \"" +
				expectedAdId +
				"\"," +
				"        \"authenticatedState\": \"ambiguous\"," +
				"        \"primary\": false" +
				"      }" +
				"    ]," +
				"    \"ECID\": [" +
				"      {" +
				"        \"id\": \"" +
				expectedECID +
				"\"," +
				"        \"authenticatedState\": \"ambiguous\"," +
				"        \"primary\": false" +
				"      }" +
				"    ]" +
				"  }" +
				"}";

			JSONAsserts.assertEquals(expected, IdentityMap);
		}
	}
}
