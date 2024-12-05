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

package com.adobe.marketing.mobile.edge.identity.util;

import static com.adobe.marketing.mobile.util.TestHelper.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.edge.identity.AuthenticatedState;
import com.adobe.marketing.mobile.edge.identity.Identity;
import com.adobe.marketing.mobile.edge.identity.IdentityItem;
import com.adobe.marketing.mobile.edge.identity.IdentityMap;
import com.adobe.marketing.mobile.util.ADBCountDownLatch;
import com.adobe.marketing.mobile.util.CollectionEqualCount;
import com.adobe.marketing.mobile.util.JSONAsserts;
import com.adobe.marketing.mobile.util.TestHelper;
import com.adobe.marketing.mobile.util.TestPersistenceHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

public class IdentityFunctionalTestUtil {

	private static final String LOG_SOURCE = "IdentityFunctionalTestUtil";
	private static final long REGISTRATION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(2);

	/**
	 * Updates configuration shared state with an orgId
	 */
	public static void setupConfiguration() throws Exception {
		HashMap<String, Object> config = new HashMap<String, Object>() {
			{
				put("experienceCloud.org", "testOrg@AdobeOrg");
			}
		};
		MobileCore.updateConfiguration(config);
		TestHelper.waitForThreads(2000);
	}

	/**
	 * Set the ECID in persistence for Identity Direct extension.
	 */
	public static void setIdentityDirectPersistedECID(final String legacyECID) {
		TestPersistenceHelper.updatePersistence(
			IdentityTestConstants.DataStoreKey.IDENTITY_DIRECT_DATASTORE,
			IdentityTestConstants.DataStoreKey.IDENTITY_DIRECT_ECID_KEY,
			legacyECID
		);
	}

	/**
	 * Set the persistence data for Edge Identity extension.
	 */
	public static void setEdgeIdentityPersistence(final Map<String, Object> persistedData) {
		if (persistedData != null) {
			final JSONObject persistedJSON = new JSONObject(persistedData);
			TestPersistenceHelper.updatePersistence(
				IdentityTestConstants.DataStoreKey.IDENTITY_DATASTORE,
				IdentityTestConstants.DataStoreKey.IDENTITY_PROPERTIES,
				persistedJSON.toString()
			);
		}
	}

	/**
	 * Method to get the ECID from Identity Direct extension synchronously.
	 */
	public static String getIdentityDirectECIDSync() {
		try {
			final HashMap<String, String> getExperienceCloudIdResponse = new HashMap<>();
			final ADBCountDownLatch latch = new ADBCountDownLatch(1);
			com.adobe.marketing.mobile.Identity.getExperienceCloudId(
				new AdobeCallback<String>() {
					@Override
					public void call(final String ecid) {
						getExperienceCloudIdResponse.put(IdentityTestConstants.GetIdentitiesHelper.VALUE, ecid);
						latch.countDown();
					}
				}
			);
			latch.await();

			return getExperienceCloudIdResponse.get(IdentityTestConstants.GetIdentitiesHelper.VALUE);
		} catch (Exception exp) {
			return null;
		}
	}

	/**
	 * Helper method to create IdentityXDM Map using {@link TestItem}s
	 */
	public static Map<String, Object> createXDMIdentityMap(TestItem... items) {
		final Map<String, List<Map<String, Object>>> allItems = new HashMap<>();

		for (TestItem item : items) {
			final Map<String, Object> itemMap = new HashMap<>();
			itemMap.put(IdentityTestConstants.XDMKeys.ID, item.id);
			itemMap.put(IdentityTestConstants.XDMKeys.AUTHENTICATED_STATE, "ambiguous");
			itemMap.put(IdentityTestConstants.XDMKeys.PRIMARY, item.isPrimary);
			List<Map<String, Object>> nameSpaceItems = allItems.get(item.namespace);

			if (nameSpaceItems == null) {
				nameSpaceItems = new ArrayList<>();
			}

			nameSpaceItems.add(itemMap);
			allItems.put(item.namespace, nameSpaceItems);
		}

		final Map<String, Object> identityMapDict = new HashMap<>();
		identityMapDict.put(IdentityTestConstants.XDMKeys.IDENTITY_MAP, allItems);
		return identityMapDict;
	}

	/**
	 * Class similar to {@link IdentityItem} for a specific namespace used for easier testing.
	 * For simplicity this class does not involve authenticatedState and primary key
	 */
	public static class TestItem {

		private final String namespace;
		private final String id;
		private final boolean isPrimary = false;

		public TestItem(String namespace, String id) {
			this.namespace = namespace;
			this.id = id;
		}
	}

	/**
	 * Retrieves identities from Identity extension synchronously
	 * @return a {@code Map<String, Object>} of identities if retrieved successfully;
	 *          null in case of a failure to retrieve it within timeout.
	 */
	public static Map<String, Object> getIdentitiesSync() {
		try {
			final HashMap<String, Object> getIdentityResponse = new HashMap<>();
			final ADBCountDownLatch latch = new ADBCountDownLatch(1);
			Identity.getIdentities(
				new AdobeCallbackWithError<IdentityMap>() {
					@Override
					public void call(final IdentityMap identities) {
						getIdentityResponse.put(IdentityTestConstants.GetIdentitiesHelper.VALUE, identities);
						latch.countDown();
					}

					@Override
					public void fail(final AdobeError adobeError) {
						getIdentityResponse.put(IdentityTestConstants.GetIdentitiesHelper.ERROR, adobeError);
						latch.countDown();
					}
				}
			);
			latch.await(2000, TimeUnit.MILLISECONDS);

			return getIdentityResponse;
		} catch (Exception exp) {
			return null;
		}
	}

	/**
	 * Retrieves Experience Cloud Id from Identity extension synchronously
	 * @return an ECID if retrieved successfully;
	 *          null in case of a failure to retrieve it within timeout.
	 */
	public static String getExperienceCloudIdSync() {
		try {
			final HashMap<String, String> getExperienceCloudIdResponse = new HashMap<>();
			final ADBCountDownLatch latch = new ADBCountDownLatch(1);
			Identity.getExperienceCloudId(
				new AdobeCallback<String>() {
					@Override
					public void call(final String ecid) {
						getExperienceCloudIdResponse.put(IdentityTestConstants.GetIdentitiesHelper.VALUE, ecid);
						latch.countDown();
					}
				}
			);
			latch.await(2000, TimeUnit.MILLISECONDS);
			return getExperienceCloudIdResponse.get(IdentityTestConstants.GetIdentitiesHelper.VALUE);
		} catch (Exception exp) {
			return null;
		}
	}

	/**
	 * Retrieves url variables from Identity extension synchronously
	 * @return a url variable string if retrieved successfully;
	 *          null in case of a failure to retrieve it within timeout.
	 */
	public static String getUrlVariablesSync() {
		try {
			final HashMap<String, String> getUrlVariablesResponse = new HashMap<>();
			final ADBCountDownLatch latch = new ADBCountDownLatch(1);
			Identity.getUrlVariables(
				new AdobeCallback<String>() {
					@Override
					public void call(final String urlVariables) {
						getUrlVariablesResponse.put(IdentityTestConstants.GetIdentitiesHelper.VALUE, urlVariables);
						latch.countDown();
					}
				}
			);
			latch.await(2000, TimeUnit.MILLISECONDS);
			return getUrlVariablesResponse.get(IdentityTestConstants.GetIdentitiesHelper.VALUE);
		} catch (Exception exp) {
			return null;
		}
	}

	public static IdentityMap createIdentityMap(final String namespace, final String id) {
		return createIdentityMap(namespace, id, AuthenticatedState.AMBIGUOUS, false);
	}

	public static IdentityMap createIdentityMap(
		final String namespace,
		final String id,
		final AuthenticatedState state,
		final boolean isPrimary
	) {
		IdentityMap map = new IdentityMap();
		IdentityItem item = new IdentityItem(id, state, isPrimary);
		map.addItem(item, namespace);
		return map;
	}

	// --------------------------------------------------------------------------------------------
	// Verifiers
	// --------------------------------------------------------------------------------------------

	/**
	 * Verifies that primary ECID is not null for the Edge Identity extension.
	 * This method checks for the data in shared state, persistence and through getExperienceCloudId API.
	 */
	public static void verifyPrimaryECIDNotNull() throws InterruptedException {
		String ecid = getExperienceCloudIdSync();
		assertNotNull(ecid);

		Map<String, Object> xdmSharedState = getXDMSharedStateFor(IdentityTestConstants.EXTENSION_NAME, 1000);

		// Validates "ECID" array has a single object element, with no restriction on what is inside that element
		String expected = "{\"identityMap\": {\"ECID\": [{}]}}";

		JSONAsserts.assertTypeMatch(expected, xdmSharedState);
	}

	/**
	 * Verifies that primary ECID for the Edge Identity Extension is equal to the value provided.
	 * This method checks for the data in shared state, persistence and through getExperienceCloudId API.
	 */
	public static void verifyPrimaryECID(final String primaryECID) throws Exception {
		String ecid = getExperienceCloudIdSync();
		assertEquals(primaryECID, ecid);

		// verify xdm shared state is has correct primary ECID
		Map<String, Object> xdmSharedState = getXDMSharedStateFor(IdentityTestConstants.EXTENSION_NAME, 1000);

		String expected =
			"{\n" +
			"  \"identityMap\": {\n" +
			"    \"ECID\": [\n" +
			"      {\n" +
			"        \"id\": \"" +
			primaryECID +
			"\"\n" +
			"      }\n" +
			"    ]\n" +
			"  }\n" +
			"}";

		JSONAsserts.assertExactMatch(expected, xdmSharedState);

		// verify primary ECID in persistence
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityTestConstants.DataStoreKey.IDENTITY_DATASTORE,
			IdentityTestConstants.DataStoreKey.IDENTITY_PROPERTIES
		);

		JSONAsserts.assertExactMatch(expected, persistedJson);
	}

	/**
	 * Verifies that secondary ECID for the Edge Identity Extension is equal to the value provided
	 * This method checks for the data in shared state and persistence.
	 */
	public static void verifySecondaryECID(final String secondaryECID) throws Exception {
		// verify xdm shared state is has correct secondary ECID
		Map<String, Object> xdmSharedState = getXDMSharedStateFor(IdentityTestConstants.EXTENSION_NAME, 1000);

		if (secondaryECID == null) {
			String json = "{" + "  \"identityMap\": {" + "    \"ECID\": [" + "      {}" + "    ]" + "  }" + "}";
			JSONAsserts.assertTypeMatch(json, xdmSharedState, new CollectionEqualCount("identityMap.ECID"));
			return;
		}
		String expected =
			"{" +
			"  \"identityMap\": {" +
			"    \"ECID\": [" +
			"      {}," + // Empty first ECID object
			"      {" +
			"        \"id\": \"" +
			secondaryECID +
			"\"" +
			"      }" +
			"    ]" +
			"  }" +
			"}";
		JSONAsserts.assertTypeMatch(expected, xdmSharedState);

		// verify secondary ECID in persistence
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			IdentityTestConstants.DataStoreKey.IDENTITY_DATASTORE,
			IdentityTestConstants.DataStoreKey.IDENTITY_PROPERTIES
		);
		JSONAsserts.assertTypeMatch(expected, persistedJson);
	}
}
