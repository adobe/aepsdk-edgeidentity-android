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

    static IdentityEdgeProperties loadPropertiesFromPersistence() {
        final SharedPreferences sharedPreferences = getSharedPreference();
        if (sharedPreferences == null) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Shared Preference value is null. Unable to load saved identity edge properties from persistence.");
            return null;
        }

        final String jsonString = sharedPreferences.getString(IdentityEdgeConstants.DataStoreKey.IDENTITY_PROPERTIES, null);

        if (jsonString == null) {
            MobileCore.log(LoggingMode.VERBOSE, LOG_TAG, "No previous properties were stored in persistence. Current identity edge properties are null");
            return null;
        }

        try {
            final JSONObject jsonObject = new JSONObject(jsonString);
            final Map<String, Object> propertyMap = Utils.toMap(jsonObject);
            final IdentityEdgeProperties loadedProperties = new IdentityEdgeProperties(propertyMap);
            return loadedProperties;
        } catch (JSONException exception) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Serialization error while reading properties jsonString from persistence. Unable to load saved identity edge properties from persistence.");
            return null;
        }
    }

    static void savePropertiesToPersistence(final IdentityEdgeProperties properties) {
        SharedPreferences sharedPreferences = getSharedPreference();
        if (sharedPreferences == null) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Shared Preference value is null. Unable to write identity edge properties to persistence.");
            return;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (editor == null) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Shared Preference Editor is null. Unable to write identity edge properties to persistence.");
            return;
        }

        final JSONObject jsonObject = new JSONObject(properties.toMap());
        final String jsonString = jsonObject.toString();
        editor.putString(IdentityEdgeConstants.DataStoreKey.IDENTITY_PROPERTIES, jsonString);
        editor.apply();
    }

    /**
     * Getter for the applications {@link SharedPreferences}
     * <p>
     * Returns null if the app or app context is not available
     *
     * @return a {@code SharedPreferences} instance
     */
    private static SharedPreferences getSharedPreference() {
        final Application application = MobileCore.getApplication();
        if (application == null) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Application value is null. Unable to read/write consent data from persistence.");
            return null;
        }

        final Context context = application.getApplicationContext();
        if (context == null) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Context value is null. Unable to read/write consent data from persistence.");
            return null;
        }

        return context.getSharedPreferences(IdentityEdgeConstants.DataStoreKey.DATASTORE_NAME, Context.MODE_PRIVATE);
    }
}
