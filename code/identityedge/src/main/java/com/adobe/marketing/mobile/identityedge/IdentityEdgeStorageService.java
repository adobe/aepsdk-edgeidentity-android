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

import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Manages persistence for the Identity Edge extension
 */
class IdentityEdgeStorageService {
    private static final String LOG_TAG = "IdentityEdgeStorageService";

    /**
     * Loads identity edge properties from local storage, returns null if not found.
     * @return properties stored in local storage if present, otherwise null.
     */
    static IdentityEdgeProperties loadPropertiesFromPersistence() {
        final SharedPreferences sharedPreferences = getSharedPreference(IdentityEdgeConstants.DataStoreKey.DATASTORE_NAME);
        if (sharedPreferences == null) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Shared Preference value is null. Unable to load saved identity properties from persistence.");
            return null;
        }

        final String jsonString = sharedPreferences.getString(IdentityEdgeConstants.DataStoreKey.IDENTITY_PROPERTIES, null);

        if (jsonString == null) {
            MobileCore.log(LoggingMode.VERBOSE, LOG_TAG, "No previous properties were stored in persistence. Current identity properties are null");
            return null;
        }

        try {
            final JSONObject jsonObject = new JSONObject(jsonString);
            final Map<String, Object> propertyMap = Utils.toMap(jsonObject);
            final IdentityEdgeProperties loadedProperties = new IdentityEdgeProperties(propertyMap);
            return loadedProperties;
        } catch (JSONException exception) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Serialization error while reading properties jsonString from persistence. Unable to load saved identity properties from persistence.");
            return null;
        }
    }

    /**
     * Saves the properties to local storage
     * @param properties properties to be stored
     */
    static void savePropertiesToPersistence(final IdentityEdgeProperties properties) {
        final SharedPreferences sharedPreferences = getSharedPreference(IdentityEdgeConstants.DataStoreKey.DATASTORE_NAME);
        if (sharedPreferences == null) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Shared Preference value is null. Unable to write identity properties to persistence.");
            return;
        }

        final SharedPreferences.Editor editor = sharedPreferences.edit();

        if (editor == null) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Shared Preference Editor is null. Unable to write identity properties to persistence.");
            return;
        }

        if (properties == null) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Identity Properties are null, removing them from persistence.");
            editor.remove(IdentityEdgeConstants.DataStoreKey.IDENTITY_PROPERTIES);
            editor.apply();
            return;
        }

        final JSONObject jsonObject = new JSONObject(properties.toXDMData(false));
        final String jsonString = jsonObject.toString();
        editor.putString(IdentityEdgeConstants.DataStoreKey.IDENTITY_PROPERTIES, jsonString);
        editor.apply();
    }

    /**
     * Retrieves the direct Identity extension ECID value stored in persistence.
     * @return {@link ECID} stored in direct Identity extension's persistence, or null if no ECID value is stored.
     */
    static ECID loadEcidFromDirectIdentityPersistence() {
        final SharedPreferences sharedPreferences = getSharedPreference(IdentityEdgeConstants.DataStoreKey.IDENTITY_DIRECT_DATASTORE_NAME);
        if (sharedPreferences == null) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Shared Preference value is null. Unable to load saved direct identity ECID from persistence.");
            return null;
        }

        final String ecidString = sharedPreferences.getString(IdentityEdgeConstants.DataStoreKey.IDENTITY_DIRECT_ECID_KEY, null);

        if (ecidString == null || ecidString.isEmpty()) {
            return null;
        }

        return new ECID(ecidString);
    }

    /**
     * Getter for the applications {@link SharedPreferences}
     * <p>
     * Returns null if the app or app context is not available
     *
     * @param datastoreName the name of the data store to get
     *
     * @return a {@code SharedPreferences} instance
     */
    private static SharedPreferences getSharedPreference(final String datastoreName) {
        final Application application = MobileCore.getApplication();
        if (application == null) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Application value is null. Unable to read/write data from persistence.");
            return null;
        }

        final Context context = application.getApplicationContext();
        if (context == null) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Context value is null. Unable to read/write data from persistence.");
            return null;
        }

        return context.getSharedPreferences(datastoreName, Context.MODE_PRIVATE);
    }
}
