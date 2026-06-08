/*
  Copyright 2022 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.edge.identity;

import static com.adobe.marketing.mobile.edge.identity.IdentityConstants.LOG_TAG;

import com.adobe.marketing.mobile.services.DataStoring;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.NamedCollection;
import com.adobe.marketing.mobile.util.JSONUtils;
import com.adobe.marketing.mobile.util.StringUtils;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Manages persistence for this Identity extension
 */
class IdentityStorageManager {

	private static final String LOG_SOURCE = "IdentityStorageManager";
	private final NamedCollection edgeIdentityStore;
	private final NamedCollection directIdentityStore;
	private final ProfileAttributeStore profileAttributeStore;

	IdentityStorageManager(final DataStoring dataStoreService) {
		this.edgeIdentityStore = dataStoreService.getNamedCollection(IdentityConstants.DataStoreKey.DATASTORE_NAME);
		this.directIdentityStore =
			dataStoreService.getNamedCollection(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_DATASTORE_NAME);
		this.profileAttributeStore =
			new ProfileAttributeStore(
				dataStoreService.getNamedCollection(IdentityConstants.DataStoreKey.PROFILE_ATTRIBUTES_DATASTORE_NAME)
			);
	}

	/**
	 * Returns the shared {@link ProfileAttributeStore} backing every {@link ProfileAttributeHandler}.
	 *
	 * @return the profile-attributes store
	 */
	ProfileAttributeStore getProfileAttributeStore() {
		return profileAttributeStore;
	}

	/**
	 * Loads identity properties from local storage, returns null if not found.
	 *
	 * @return {@code IdentityProperties} stored in local storage if present;
	 *         null - if the content cannot be loaded from persistence or, if the content cannot be
	 *         serialized to a {@code JSONObject}
	 */
	IdentityProperties loadPropertiesFromPersistence() {
		if (edgeIdentityStore == null) {
			Log.warning(
				LOG_TAG,
				LOG_SOURCE,
				"EdgeIdentity named collection is null. Unable to load saved identity properties from persistence."
			);
			return null;
		}
		final String jsonString = edgeIdentityStore.getString(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES, null);

		if (jsonString == null) {
			Log.debug(
				LOG_TAG,
				LOG_SOURCE,
				"No previous properties were stored in persistence. Current identity properties are null"
			);
			return null;
		}

		try {
			final JSONObject jsonObject = new JSONObject(jsonString);
			final Map<String, Object> propertyMap = JSONUtils.toMap(jsonObject);
			return new IdentityProperties(propertyMap);
		} catch (JSONException exception) {
			Log.debug(
				LOG_TAG,
				LOG_SOURCE,
				"Serialization error while reading properties jsonString from persistence. Unable to load saved identity properties from persistence."
			);
			return null;
		}
	}

	/**
	 * Saves identity properties to local storage.
	 *
	 * @param properties properties to be stored
	 */
	void savePropertiesToPersistence(final IdentityProperties properties) {
		if (edgeIdentityStore == null) {
			Log.warning(
				LOG_TAG,
				LOG_SOURCE,
				"EdgeIdentity named collection is null. Unable to write identity properties to persistence."
			);
			return;
		}

		if (properties == null) {
			Log.debug(LOG_TAG, LOG_SOURCE, "Identity Properties are null, removing them from persistence.");
			edgeIdentityStore.remove(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES);
			return;
		}

		final JSONObject jsonObject = new JSONObject(properties.toXDMData());
		final String jsonString = jsonObject.toString();
		edgeIdentityStore.setString(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES, jsonString);
	}

	/**
	 * Retrieves the direct Identity extension ECID value stored in persistence.
	 *
	 * @return {@link ECID} stored in direct Identity extension's persistence, or null if no ECID value is stored.
	 */
	ECID loadEcidFromDirectIdentityPersistence() {
		if (directIdentityStore == null) {
			Log.debug(
				LOG_TAG,
				LOG_SOURCE,
				"Identity direct named collection is null. Unable to load ECID from Identity Direct persistence."
			);
			return null;
		}

		final String ecidString = directIdentityStore.getString(
			IdentityConstants.DataStoreKey.IDENTITY_DIRECT_ECID_KEY,
			null
		);

		return StringUtils.isNullOrEmpty(ecidString) ? null : new ECID(ecidString);
	}
}
