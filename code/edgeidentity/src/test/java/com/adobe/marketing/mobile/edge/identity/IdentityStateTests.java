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
import com.adobe.marketing.mobile.MobileCore;

import org.json.JSONObject;
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

import static com.adobe.marketing.mobile.edge.identity.IdentityTestUtil.createXDMIdentityMap;
import static com.adobe.marketing.mobile.edge.identity.IdentityTestUtil.flattenJSONString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MobileCore.class})
public class IdentityStateTests {

	@Mock
	Application mockApplication;

	@Mock
	Context mockContext;

	@Mock
	SharedPreferences mockSharedPreference;

	@Mock
	SharedPreferences.Editor mockSharedPreferenceEditor;

	private SharedStateCallback mockSharedStateCallback;
	private Map<String, Object> hubSharedState;
	private Map<String, Object> identityDirectSharedState;
	private int setXDMSharedEventStateCalledTimes;

	@Before
	public void before() throws Exception {
		PowerMockito.mockStatic(MobileCore.class);

		Mockito.when(MobileCore.getApplication()).thenReturn(mockApplication);
		Mockito.when(mockApplication.getApplicationContext()).thenReturn(mockContext);
		Mockito.when(mockContext.getSharedPreferences(IdentityConstants.DataStoreKey.DATASTORE_NAME,
					 0)).thenReturn(mockSharedPreference);
		Mockito.when(mockSharedPreference.edit()).thenReturn(mockSharedPreferenceEditor);

		setXDMSharedEventStateCalledTimes = 0;
		mockSharedStateCallback = new SharedStateCallback() {
			@Override
			public Map<String, Object> getSharedState(final String stateOwner, final Event event) {
				if (IdentityConstants.SharedState.Hub.NAME.equals(stateOwner)) {
					return hubSharedState;
				} else if (IdentityConstants.SharedState.IdentityDirect.NAME.equals(stateOwner)) {
					return identityDirectSharedState;
				}

				return null;
			}

			@Override
			public boolean setXDMSharedEventState(Map<String, Object> state, Event event) {
				setXDMSharedEventStateCalledTimes++;
				return true;
			}
		};
	}


	@Test
	public void testBootupIfReady_GeneratesECID() {
		// setup
		IdentityState state = new IdentityState(new IdentityProperties());
		assertNull(state.getIdentityProperties().getECID());

		// test
		state.bootupIfReady(mockSharedStateCallback);
		verify(mockSharedPreferenceEditor, Mockito.times(1)).apply(); // saves to data store

		// verify
		assertNotNull(state.getIdentityProperties().getECID());
		assertEquals(1, setXDMSharedEventStateCalledTimes);
	}

	@Test
	public void testBootupIfReady_LoadsDirectIdentityECID() {
		// setup
		ECID ecid = new ECID();
		Mockito.when(mockContext.getSharedPreferences(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_DATASTORE_NAME,
					 0)).thenReturn(mockSharedPreference);
		Mockito.when(mockSharedPreference.getString(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_ECID_KEY,
					 null)).thenReturn(ecid.toString());

		IdentityState state = new IdentityState(new IdentityProperties());
		assertNull(state.getIdentityProperties().getECID());

		// test
		state.bootupIfReady(mockSharedStateCallback);
		verify(mockSharedPreferenceEditor, Mockito.times(1)).apply(); // saves to data store

		// verify
		assertEquals(ecid, state.getIdentityProperties().getECID());
		assertEquals(1, setXDMSharedEventStateCalledTimes);
	}

	@Test
	public void testBootupIfReady_GeneratesECIDWhenDirectECIDIsNullInPersistence() {
		// setup
		Mockito.when(mockContext.getSharedPreferences(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_DATASTORE_NAME,
					 0)).thenReturn(mockSharedPreference);
		Mockito.when(mockSharedPreference.getString(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_ECID_KEY,
					 null)).thenReturn(null);

		IdentityState state = new IdentityState(new IdentityProperties());
		assertNull(state.getIdentityProperties().getECID());

		// test
		state.bootupIfReady(mockSharedStateCallback);
		verify(mockSharedPreferenceEditor, Mockito.times(1)).apply(); // saves to data store

		// verify
		assertNotNull(state.getIdentityProperties().getECID());
		assertEquals(1, setXDMSharedEventStateCalledTimes);
	}

	@Test
	public void testBootupIfReady_LoadsFromPersistence() {
		// setup
		IdentityState state = new IdentityState(new IdentityProperties());

		IdentityProperties persistedProps = new IdentityProperties();
		persistedProps.setECID(new ECID());
		final JSONObject jsonObject = new JSONObject(persistedProps.toXDMData(false));
		final String propsJSON = jsonObject.toString();
		Mockito.when(mockSharedPreference.getString(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES,
					 null)).thenReturn(propsJSON);

		// test
		state.bootupIfReady(mockSharedStateCallback);
		verify(mockSharedPreferenceEditor, never()).apply();

		// verify
		assertEquals(persistedProps.getECID().toString(), state.getIdentityProperties().getECID().toString());
		assertEquals(1, setXDMSharedEventStateCalledTimes);
	}

	@Test
	public void testBootupIfReady_IfReadyLoadsFromPersistenceWhenDirectECIDIsValid() {
		// setup
		ECID ecid = new ECID();
		Mockito.when(mockContext.getSharedPreferences(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_DATASTORE_NAME,
					 0)).thenReturn(mockSharedPreference);
		Mockito.when(mockSharedPreference.getString(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_ECID_KEY,
					 null)).thenReturn(ecid.toString());

		IdentityState state = new IdentityState(new IdentityProperties());

		IdentityProperties persistedProps = new IdentityProperties();
		persistedProps.setECID(new ECID());
		final JSONObject jsonObject = new JSONObject(persistedProps.toXDMData(false));
		final String propsJSON = jsonObject.toString();
		Mockito.when(mockSharedPreference.getString(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES,
					 null)).thenReturn(propsJSON);

		// test
		state.bootupIfReady(mockSharedStateCallback);
		verify(mockSharedPreferenceEditor, never()).apply();

		// verify
		assertEquals(persistedProps.getECID(), state.getIdentityProperties().getECID());
		assertEquals(1, setXDMSharedEventStateCalledTimes);
	}

	@Test
	public void testBootupIfReady_whenIdentityDirectRegistered_onFirstBoot_waitsForIdentityDirectECID() {
		// setup
		Mockito.when(mockContext.getSharedPreferences(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_DATASTORE_NAME,
					 0)).thenReturn(mockSharedPreference);
		Mockito.when(mockSharedPreference.getString(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_ECID_KEY,
					 null)).thenReturn(null);

		IdentityState state = new IdentityState(new IdentityProperties());

		// test
		hubSharedState = new HashMap<>();
		hubSharedState.put("extensions", new HashMap<String, Object>() {
			{
				put("com.adobe.module.identity", new HashMap<String, String>() {
					{
						put("friendlyName", "Identity");
						put("version", "1.2.2");
					}
				});
			}
		});
		state.bootupIfReady(mockSharedStateCallback);

		verify(mockSharedPreferenceEditor, never()).apply();

		// verify
		assertNull(state.getIdentityProperties().getECID());
		assertEquals(0, setXDMSharedEventStateCalledTimes);
	}

	@Test
	public void testBootupIfReady_whenIdentityDirectRegistered_onFirstBoot_usesIdentityDirectECID() {
		// setup
		Mockito.when(mockContext.getSharedPreferences(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_DATASTORE_NAME,
					 0)).thenReturn(mockSharedPreference);
		Mockito.when(mockSharedPreference.getString(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_ECID_KEY,
					 null)).thenReturn(null);


		IdentityState state = new IdentityState(new IdentityProperties());

		// test
		hubSharedState = new HashMap<>();
		hubSharedState.put("extensions", new HashMap<String, Object>() {
			{
				put("com.adobe.module.identity", new HashMap<String, String>() {
					{
						put("friendlyName", "Identity");
						put("version", "1.2.2");
					}
				});
			}
		});
		identityDirectSharedState = new HashMap<>();
		identityDirectSharedState.put("mid", "1234");
		state.bootupIfReady(mockSharedStateCallback);

		// verify
		assertEquals("1234", state.getIdentityProperties().getECID().toString()); // ECID from Identity direct
		assertNull(state.getIdentityProperties().getECIDSecondary()); // should be null
		verify(mockSharedPreferenceEditor, Mockito.times(1)).apply();
		assertEquals(1, setXDMSharedEventStateCalledTimes);
	}

	@Test
	public void testBootupIfReady_whenIdentityDirectRegistered_onFirstBoot_whenIdentityDirectECIDNull_generatesNew() {
		// setup
		Mockito.when(mockContext.getSharedPreferences(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_DATASTORE_NAME,
					 0)).thenReturn(mockSharedPreference);
		Mockito.when(mockSharedPreference.getString(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_ECID_KEY,
					 null)).thenReturn(null);


		IdentityState state = new IdentityState(new IdentityProperties());

		// test
		hubSharedState = new HashMap<>();
		hubSharedState.put("extensions", new HashMap<String, Object>() {
			{
				put("com.adobe.module.identity", new HashMap<String, String>() {
					{
						put("friendlyName", "Identity");
						put("version", "1.2.2");
					}
				});
			}
		});
		identityDirectSharedState = new HashMap<>(); // no mid key
		state.bootupIfReady(mockSharedStateCallback);

		// verify
		assertNotNull("1234", state.getIdentityProperties().getECID()); // new ECID generated
		assertNull(state.getIdentityProperties().getECIDSecondary()); // should be null
		verify(mockSharedPreferenceEditor, Mockito.times(1)).apply();
		assertEquals(1, setXDMSharedEventStateCalledTimes);
	}

	@Test
	public void testResetIdentifiers() {
		// setup
		IdentityState state = new IdentityState(new IdentityProperties());
		state.getIdentityProperties().setECID(new ECID());
		state.getIdentityProperties().setECIDSecondary(new ECID());
		ECID existingEcid = state.getIdentityProperties().getECID();

		// test
		state.resetIdentifiers();

		// verify
		assertNotEquals(existingEcid, state.getIdentityProperties().getECID()); // ECID should be regenerated
		assertFalse(state.getIdentityProperties().getECID().toString().isEmpty()); // ECID should not be empty
		assertNull(state.getIdentityProperties().getECIDSecondary()); // should be cleared
		verify(mockSharedPreferenceEditor, Mockito.times(1)).apply(); // should save to data store
	}

	@Test
	public void testUpdateCustomerIdentifiers_happy() throws Exception {
		// setup
		IdentityProperties properties = new IdentityProperties();
		IdentityState state = new IdentityState(properties);

		// test
		Map<String, Object> identityXDM = createXDMIdentityMap(
											  new IdentityTestUtil.TestItem("UserId", "secretID")
										  );
		state.updateCustomerIdentifiers(IdentityMap.fromXDMMap(identityXDM));

		// verify persistence
		final ArgumentCaptor<String> persistenceValueCaptor = ArgumentCaptor.forClass(String.class);
		verify(mockSharedPreferenceEditor, times(1)).putString(eq(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES),
				persistenceValueCaptor.capture());
		Map<String, String> persistedData = flattenJSONString(persistenceValueCaptor.getAllValues().get(0));
		assertEquals("secretID", persistedData.get("identityMap.UserId[0].id"));
		assertEquals("ambiguous", persistedData.get("identityMap.UserId[0].authenticatedState"));
		assertEquals("false", persistedData.get("identityMap.UserId[0].primary"));
	}


	@Test
	public void testUpdateCustomerIdentifiers_doesNotUpdateReservedNamespace() throws Exception {
		// setup
		IdentityProperties properties = new IdentityProperties();
		properties.setECID(new ECID("internalECID"));
		IdentityState state = new IdentityState(properties);

		// test
		Map<String, Object> identityXDM = createXDMIdentityMap(
											  new IdentityTestUtil.TestItem("ECID", "somevalue"),
											  new IdentityTestUtil.TestItem("GAID", "somevalue"),
											  new IdentityTestUtil.TestItem("IDFA", "somevalue"),
											  new IdentityTestUtil.TestItem("IdFA", "somevalue"),
											  new IdentityTestUtil.TestItem("gaid", "somevalue"),
											  new IdentityTestUtil.TestItem("UserId", "somevalue")
										  );
		state.updateCustomerIdentifiers(IdentityMap.fromXDMMap(identityXDM));

		// verify persistence
		final ArgumentCaptor<String> persistenceValueCaptor = ArgumentCaptor.forClass(String.class);
		verify(mockSharedPreferenceEditor, times(1)).putString(eq(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES),
				persistenceValueCaptor.capture());
		Map<String, String> persistedData = flattenJSONString(persistenceValueCaptor.getAllValues().get(0));
		assertEquals(6, persistedData.size()); // USERID identifier and initial ECID
		assertEquals("somevalue", persistedData.get("identityMap.UserId[0].id"));
		assertEquals("ambiguous", persistedData.get("identityMap.UserId[0].authenticatedState"));
		assertEquals("false", persistedData.get("identityMap.UserId[0].primary"));
		assertEquals("internalECID", persistedData.get("identityMap.ECID[0].id")); // verify that the ECID is not disturbed
		assertEquals("ambiguous", persistedData.get("identityMap.ECID[0].authenticatedState"));
		assertEquals("false", persistedData.get("identityMap.ECID[0].primary"));
	}

	@Test
	public void testUpdateCustomerIdentifiers_whenCaseSensitiveNamespace_storesAll() throws Exception {
		// setup
		IdentityProperties properties = new IdentityProperties();
		properties.setECID(new ECID("internalECID"));
		IdentityState state = new IdentityState(properties);

		// test
		Map<String, Object> identityXDM = createXDMIdentityMap(
											  new IdentityTestUtil.TestItem("caseSensitive", "somevalue"),
											  new IdentityTestUtil.TestItem("CASESENSITIVE", "SOMEVALUE")
										  );
		state.updateCustomerIdentifiers(IdentityMap.fromXDMMap(identityXDM));

		final ArgumentCaptor<Map> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);

		// verify shared state
		final ArgumentCaptor<String> persistenceValueCaptor = ArgumentCaptor.forClass(String.class);
		verify(mockSharedPreferenceEditor, times(1)).putString(eq(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES),
				persistenceValueCaptor.capture());
		Map<String, String> persistedData = flattenJSONString(persistenceValueCaptor.getAllValues().get(0));
		assertEquals(9, persistedData.size()); // updated ids + ECID
		assertEquals("somevalue", persistedData.get("identityMap.caseSensitive[0].id"));
		assertEquals("SOMEVALUE", persistedData.get("identityMap.CASESENSITIVE[0].id"));
		assertEquals("internalECID", persistedData.get("identityMap.ECID[0].id"));
	}

	@Test
	public void testRemoveCustomerIdentifiers_happy() throws Exception {
		// setup
		Map<String, Object> identityXDM = createXDMIdentityMap(
											  new IdentityTestUtil.TestItem("UserId", "secretID"),
											  new IdentityTestUtil.TestItem("PushId", "token")
										  );
		IdentityProperties properties = new IdentityProperties(identityXDM);
		IdentityState state = new IdentityState(properties);

		// test
		Map<String, Object> removedIdentityXDM = createXDMIdentityMap(
					new IdentityTestUtil.TestItem("UserId", "secretID")
				);
		state.removeCustomerIdentifiers(IdentityMap.fromXDMMap(removedIdentityXDM));

		// verify
		final ArgumentCaptor<String> persistenceValueCaptor = ArgumentCaptor.forClass(String.class);
		verify(mockSharedPreferenceEditor, times(1)).putString(eq(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES),
				persistenceValueCaptor.capture());
		Map<String, String> persistedData = flattenJSONString(persistenceValueCaptor.getAllValues().get(0));
		assertEquals(3, persistedData.size());
		assertNull(persistedData.get("identityMap.UserId[0].id"));
		assertEquals("token", persistedData.get("identityMap.PushId[0].id"));
	}

	@Test
	public void testRemoveCustomerIdentifiers_doesNotRemoveReservedNamespace() throws Exception {
		// setup
		Map<String, Object> identityXDM = createXDMIdentityMap(
											  new IdentityTestUtil.TestItem("GAID", "someGAID"),
											  new IdentityTestUtil.TestItem("ECID", "someECID"),
											  new IdentityTestUtil.TestItem("IDFA", "someIDFA")
										  );
		IdentityProperties properties = new IdentityProperties(identityXDM);
		IdentityState state = new IdentityState(properties);

		// test
		Map<String, Object> removedIdentityXDM = createXDMIdentityMap(
					new IdentityTestUtil.TestItem("GAID", "someGAID"),
					new IdentityTestUtil.TestItem("ecid", "someECID"),
					new IdentityTestUtil.TestItem("Idfa", "someIDFA")
				);
		state.removeCustomerIdentifiers(IdentityMap.fromXDMMap(removedIdentityXDM));

		// verify
		final ArgumentCaptor<String> persistenceValueCaptor = ArgumentCaptor.forClass(String.class);
		verify(mockSharedPreferenceEditor, times(1)).putString(eq(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES),
				persistenceValueCaptor.capture());
		Map<String, String> persistedData = flattenJSONString(persistenceValueCaptor.getAllValues().get(0));
		assertEquals(9, persistedData.size());
		assertEquals("someGAID", persistedData.get("identityMap.GAID[0].id"));
		assertEquals("someECID", persistedData.get("identityMap.ECID[0].id"));
		assertEquals("someIDFA", persistedData.get("identityMap.IDFA[0].id"));
	}

	@Test
	public void testUpdateLegacyExperienceCloudId() {
		IdentityState state = new IdentityState(new IdentityProperties());
		state.getIdentityProperties().setECID(new ECID());
		ECID legacyEcid = new ECID();

		// test
		state.updateLegacyExperienceCloudId(legacyEcid);

		// verify
		assertEquals(legacyEcid, state.getIdentityProperties().getECIDSecondary());
		verify(mockSharedPreferenceEditor, Mockito.times(1)).apply();
	}

	@Test
	public void testUpdateLegacyExperienceCloudId_notSetWhenECIDSame() {
		IdentityState state = new IdentityState(new IdentityProperties());
		ECID legacyEcid = new ECID();
		state.getIdentityProperties().setECID(legacyEcid);

		state.updateLegacyExperienceCloudId(legacyEcid);

		// verify
		assertNull(state.getIdentityProperties().getECIDSecondary());
		verify(mockSharedPreferenceEditor, Mockito.times(0)).apply();
	}

	@Test
	public void testUpdateLegacyExperienceCloudId_notSetWhenSecondaryECIDSame() {
		IdentityState state = new IdentityState(new IdentityProperties());
		state.getIdentityProperties().setECID(new ECID());
		ECID legacyEcid = new ECID();
		state.getIdentityProperties().setECIDSecondary(legacyEcid);

		state.updateLegacyExperienceCloudId(legacyEcid);

		assertEquals(legacyEcid, state.getIdentityProperties().getECIDSecondary());
		verify(mockSharedPreferenceEditor, Mockito.times(0)).apply();
	}

	@Test
	public void testUpdateLegacyExperienceCloudId_clearsOnNull() {
		IdentityState state = new IdentityState(new IdentityProperties());
		state.getIdentityProperties().setECID(new ECID());
		state.getIdentityProperties().setECIDSecondary(new ECID());

		state.updateLegacyExperienceCloudId(null);

		assertNull(state.getIdentityProperties().getECIDSecondary());
		verify(mockSharedPreferenceEditor, Mockito.times(1)).apply();
	}

	@Test
	public void testUpdateLegacyExperienceCloudId_notSetWhenExistingIsNull() {
		IdentityState state = new IdentityState(new IdentityProperties());
		state.getIdentityProperties().setECID(new ECID());

		state.updateLegacyExperienceCloudId(null);

		assertNull(state.getIdentityProperties().getECIDSecondary());
		verify(mockSharedPreferenceEditor, Mockito.times(0)).apply();
	}
}
