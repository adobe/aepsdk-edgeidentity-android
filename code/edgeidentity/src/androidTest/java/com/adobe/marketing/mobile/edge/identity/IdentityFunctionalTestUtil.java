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

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.TestHelper;
import com.adobe.marketing.mobile.TestPersistenceHelper;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.adobe.marketing.mobile.TestHelper.getXDMSharedStateFor;
import static com.adobe.marketing.mobile.TestHelper.resetTestExpectations;
import static com.adobe.marketing.mobile.edge.identity.IdentityTestUtil.flattenMap;
import static com.adobe.marketing.mobile.edge.identity.IdentityTestUtil.getExperienceCloudIdSync;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class IdentityFunctionalTestUtil {

	/**
	 * Register's Edge Identity Extension and start the Core
	 */
	static void registerEdgeIdentityExtension() throws InterruptedException {
		com.adobe.marketing.mobile.edge.identity.Identity.registerExtension();

		final ADBCountDownLatch latch = new ADBCountDownLatch(1);
		MobileCore.start(new AdobeCallback() {
			@Override
			public void call(Object o) {
				latch.countDown();
			}
		});

		latch.await(1000, TimeUnit.MILLISECONDS);
		TestHelper.waitForThreads(2000);
		resetTestExpectations();
	}

	/**
	 * Register's Identity Direct Extension and start the Core
	 */
	static void registerIdentityDirectExtension() throws Exception {
		HashMap<String, Object> config = new HashMap<String, Object>() {
			{
				put("global.privacy", "optedin");
				put("experienceCloud.org", "testOrg@AdobeOrg");
				put("experienceCloud.server", "notaserver");
			}
		};
		MobileCore.updateConfiguration(config);
		com.adobe.marketing.mobile.Identity.registerExtension();

		final ADBCountDownLatch latch = new ADBCountDownLatch(1);
		MobileCore.start(new AdobeCallback() {
			@Override
			public void call(Object o) {
				latch.countDown();
			}
		});

		latch.await(1000, TimeUnit.MILLISECONDS);
		TestHelper.waitForThreads(2000);
		resetTestExpectations();
	}

	/**
	 * Register's Identity Direct and Edge Identity Extension. And then starts the MobileCore
	 */
	static void registerBothIdentityExtensions() throws Exception {
		HashMap<String, Object> config = new HashMap<String, Object>() {
			{
				put("global.privacy", "optedin");
				put("experienceCloud.org", "testOrg@AdobeOrg");
				put("experienceCloud.server", "notaserver");
			}
		};
		MobileCore.updateConfiguration(config);

		com.adobe.marketing.mobile.edge.identity.Identity.registerExtension();
		com.adobe.marketing.mobile.Identity.registerExtension();

		final ADBCountDownLatch latch = new ADBCountDownLatch(1);
		MobileCore.start(new AdobeCallback() {
			@Override
			public void call(Object o) {
				latch.countDown();
			}
		});

		latch.await();
		TestHelper.waitForThreads(2000);
		resetTestExpectations();
	}

	/**
	 * Set the ECID in persistence for Identity Direct extension.
	 */
	static void setIdentityDirectPersistedECID(final String legacyECID) {
		TestPersistenceHelper.updatePersistence(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_DATASTORE_NAME,
												IdentityConstants.DataStoreKey.IDENTITY_DIRECT_ECID_KEY, legacyECID);
	}

	/**
	 * Set the persistence data for Edge Identity extension.
	 */
	static void setEdgeIdentityPersistence(final Map<String, Object> persistedData) {
		if (persistedData != null) {
			final JSONObject persistedJSON = new JSONObject(persistedData);
			TestPersistenceHelper.updatePersistence(IdentityConstants.DataStoreKey.DATASTORE_NAME,
													IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES, persistedJSON.toString());
		}
	}

	/**
	 * Method to get the ECID from Identity Direct extension synchronously.
	 */
	static String getIdentityDirectECIDSync() {
		try {
			final HashMap<String, String> getExperienceCloudIdResponse = new HashMap<>();
			final ADBCountDownLatch latch = new ADBCountDownLatch(1);
			com.adobe.marketing.mobile.Identity.getExperienceCloudId(new AdobeCallback<String>() {
				@Override
				public void call(final String ecid) {
					getExperienceCloudIdResponse.put(IdentityTestConstants.GetIdentitiesHelper.VALUE, ecid);
					latch.countDown();
				}
			});
			latch.await();

			return getExperienceCloudIdResponse.get(IdentityTestConstants.GetIdentitiesHelper.VALUE);
		} catch (Exception exp) {
			return null;
		}
	}


	// --------------------------------------------------------------------------------------------
	// Verifiers
	// --------------------------------------------------------------------------------------------

	/**
	 * Verifies that primary ECID is not null for the Edge Identity extension.
	 * This method checks for the data in shared state, persistence and through getExperienceCloudId API.
	 */
	static void verifyPrimaryECIDNotNull() throws InterruptedException {
		String ecid = getExperienceCloudIdSync();
		assertNotNull(ecid);

		// verify xdm shared state is has ECID
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertNotNull(xdmSharedState.get("identityMap.ECID[0].id"));
	}

	/**
	 * Verifies that primary ECID for the Edge Identity Extension is equal to the value provided.
	 * This method checks for the data in shared state, persistence and through getExperienceCloudId API.
	 */
	static void verifyPrimaryECID(final String primaryECID) throws Exception {
		String ecid = getExperienceCloudIdSync();
		assertEquals(primaryECID, ecid);

		// verify xdm shared state is has correct primary ECID
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertEquals(primaryECID, xdmSharedState.get("identityMap.ECID[0].id"));

		// verify primary ECID in persistence
		final String persistedJson = TestPersistenceHelper.readPersistedData(IdentityConstants.DataStoreKey.DATASTORE_NAME,
									 IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES);
		Map<String, String> persistedMap = flattenMap(IdentityTestUtil.toMap(new JSONObject(persistedJson)));
		assertEquals(primaryECID, persistedMap.get("identityMap.ECID[0].id"));
	}

	/**
	 * Verifies that secondary ECID for the Edge Identity Extension is equal to the value provided
	 * This method checks for the data in shared state and persistence.
	 */
	static void verifySecondaryECID(final String secondaryECID) throws Exception {
		// verify xdm shared state is has correct secondary ECID
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(IdentityConstants.EXTENSION_NAME, 1000));
		assertEquals(secondaryECID, xdmSharedState.get("identityMap.ECID[1].id"));

		// verify secondary ECID in persistence
		final String persistedJson = TestPersistenceHelper.readPersistedData(IdentityConstants.DataStoreKey.DATASTORE_NAME,
									 IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES);
		Map<String, String> persistedMap = flattenMap(IdentityTestUtil.toMap(new JSONObject(persistedJson)));
		assertEquals(secondaryECID, persistedMap.get("identityMap.ECID[1].id"));
	}
}
