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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adobe.marketing.mobile.services.DataStoring;
import com.adobe.marketing.mobile.services.NamedCollection;
import com.adobe.marketing.mobile.services.ServiceProvider;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class IdentityStorageManagerTests {

	@Mock
	private ServiceProvider mockServiceProvider;

	private MockedStatic<ServiceProvider> mockedStaticServiceProvider;

	@Mock
	private DataStoring mockDataStoreService;

	@Mock
	private NamedCollection mockEdgeIdentityNamedCollection;

	@Mock
	private NamedCollection mockDirectIdentityNamedCollection;

	@Mock
	private NamedCollection mockProfileAttributesNamedCollection;

	@Before
	public void before() throws Exception {
		MockitoAnnotations.openMocks(this);

		mockedStaticServiceProvider = Mockito.mockStatic(ServiceProvider.class);
		mockedStaticServiceProvider.when(ServiceProvider::getInstance).thenReturn(mockServiceProvider);
		when(mockServiceProvider.getDataStoreService()).thenReturn(mockDataStoreService);

		when(mockDataStoreService.getNamedCollection(IdentityConstants.DataStoreKey.DATASTORE_NAME))
			.thenReturn(mockEdgeIdentityNamedCollection);
		when(mockDataStoreService.getNamedCollection(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_DATASTORE_NAME))
			.thenReturn(mockDirectIdentityNamedCollection);
		when(mockDataStoreService.getNamedCollection(IdentityConstants.DataStoreKey.PROFILE_ATTRIBUTES_DATASTORE_NAME))
			.thenReturn(mockProfileAttributesNamedCollection);
	}

	@Test
	public void testLoadPropertiesFromPersistence_edgeIdentityDataStoreIsNull() {
		// setup
		when(mockDataStoreService.getNamedCollection(IdentityConstants.DataStoreKey.DATASTORE_NAME)).thenReturn(null);
		final IdentityStorageManager identityStorageManager = new IdentityStorageManager(mockDataStoreService);

		// test
		final IdentityProperties identityProperties = identityStorageManager.loadPropertiesFromPersistence();

		// verify
		assertNull(identityProperties);
	}

	@Test
	public void testLoadPropertiesFromPersistence_identityPropertiesAreEmpty() {
		when(mockEdgeIdentityNamedCollection.getString(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES, null))
			.thenReturn(null);
		final IdentityStorageManager identityStorageManager = new IdentityStorageManager(mockDataStoreService);

		// test
		final IdentityProperties identityProperties = identityStorageManager.loadPropertiesFromPersistence();

		// verify
		assertNull(identityProperties);
	}

	@Test
	public void testLoadPropertiesFromPersistence_identityPropertiesIsInvalid() {
		when(mockEdgeIdentityNamedCollection.getString(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES, null))
			.thenReturn("{someinvalidjson}");
		final IdentityStorageManager identityStorageManager = new IdentityStorageManager(mockDataStoreService);

		// test
		final IdentityProperties identityProperties = identityStorageManager.loadPropertiesFromPersistence();

		// verify
		assertNull(identityProperties);
	}

	@Test
	public void testLoadPropertiesFromPersistence_validJSON() {
		// setup
		final IdentityProperties persistedProps = new IdentityProperties();
		persistedProps.setECID(new ECID());
		final JSONObject jsonObject = new JSONObject(persistedProps.toXDMData(false));
		final String propsJSON = jsonObject.toString();

		when(mockEdgeIdentityNamedCollection.getString(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES, null))
			.thenReturn(propsJSON);
		final IdentityStorageManager identityStorageManager = new IdentityStorageManager(mockDataStoreService);

		// test
		IdentityProperties props = identityStorageManager.loadPropertiesFromPersistence();

		// verify
		assertEquals(persistedProps.toXDMData(false), props.toXDMData(false));
	}

	@Test
	public void testSavePropertiesToPersistence_edgeIdentityStoreIsNull() {
		// setup
		when(mockDataStoreService.getNamedCollection(IdentityConstants.DataStoreKey.DATASTORE_NAME)).thenReturn(null);
		final IdentityStorageManager identityStorageManager = new IdentityStorageManager(mockDataStoreService);

		// test
		final IdentityProperties identityProperties = new IdentityProperties();
		identityStorageManager.savePropertiesToPersistence(identityProperties);

		// verify
		verify(mockEdgeIdentityNamedCollection, never()).setString(any(), any());
	}

	@Test
	public void testSavePropertiesToPersistence_nullProps() {
		final IdentityStorageManager identityStorageManager = new IdentityStorageManager(mockDataStoreService);

		// test
		identityStorageManager.savePropertiesToPersistence(null);

		// verify
		verify(mockEdgeIdentityNamedCollection, times(1)).remove(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES);
	}

	@Test
	public void testSavePropertiesToPersistence_validProps() {
		// test
		final IdentityProperties properties = new IdentityProperties();
		properties.setECID(new ECID());

		final IdentityStorageManager identityStorageManager = new IdentityStorageManager(mockDataStoreService);
		identityStorageManager.savePropertiesToPersistence(properties);

		// verify
		final JSONObject jsonObject = new JSONObject(properties.toXDMData(false));
		final String expectedJSON = jsonObject.toString();
		verify(mockEdgeIdentityNamedCollection)
			.setString(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES, expectedJSON);
	}

	@Test
	public void testLoadEcidFromDirectIdentityPersistence_DirectIdentityStoreIsNull() {
		when(mockDataStoreService.getNamedCollection(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_DATASTORE_NAME))
			.thenReturn(null);

		final IdentityStorageManager identityStorageManager = new IdentityStorageManager(mockDataStoreService);

		assertNull(identityStorageManager.loadEcidFromDirectIdentityPersistence());
	}

	@Test
	public void testLoadEcidFromDirectIdentityPersistence_loadValidECID() {
		final ECID ecid = new ECID();
		when(mockDirectIdentityNamedCollection.getString(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_ECID_KEY, null))
			.thenReturn(ecid.toString());

		final IdentityStorageManager identityStorageManager = new IdentityStorageManager(mockDataStoreService);

		assertEquals(ecid, identityStorageManager.loadEcidFromDirectIdentityPersistence());
	}

	@Test
	public void testLoadEcidFromDirectIdentityPersistence_whenNullECID() {
		when(mockDirectIdentityNamedCollection.getString(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_ECID_KEY, null))
			.thenReturn(null);

		final IdentityStorageManager identityStorageManager = new IdentityStorageManager(mockDataStoreService);

		assertNull(identityStorageManager.loadEcidFromDirectIdentityPersistence());
	}

	@Test
	public void testLoadEcidFromDirectIdentityPersistence_whenEmptyECID() {
		when(mockDirectIdentityNamedCollection.getString(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_ECID_KEY, null))
			.thenReturn("");

		final IdentityStorageManager identityStorageManager = new IdentityStorageManager(mockDataStoreService);

		assertNull(identityStorageManager.loadEcidFromDirectIdentityPersistence());
	}

	@Test
	public void testGetProfileAttributeStore_returnsStoreBackedByProfileAttributesCollection() {
		final IdentityStorageManager identityStorageManager = new IdentityStorageManager(mockDataStoreService);

		final ProfileAttributeStore store = identityStorageManager.getProfileAttributeStore();

		// Round-trip a write through the store; assert it lands on the underlying NamedCollection.
		store.setString("timezone", "Asia/Kolkata");
		verify(mockProfileAttributesNamedCollection, times(1)).setString("timezone", "Asia/Kolkata");
	}

	@Test
	public void testGetProfileAttributeStore_whenCollectionIsNull_storeNoOps() {
		when(mockDataStoreService.getNamedCollection(IdentityConstants.DataStoreKey.PROFILE_ATTRIBUTES_DATASTORE_NAME))
			.thenReturn(null);
		final IdentityStorageManager identityStorageManager = new IdentityStorageManager(mockDataStoreService);

		// Wraps a null NamedCollection but never throws; reads return null, writes are no-ops.
		final ProfileAttributeStore store = identityStorageManager.getProfileAttributeStore();
		assertNull(store.getString("timezone"));
		store.setString("timezone", "Asia/Kolkata");
		store.clearAll();
	}

	@After
	public void teardown() {
		mockedStaticServiceProvider.close();
	}
}
