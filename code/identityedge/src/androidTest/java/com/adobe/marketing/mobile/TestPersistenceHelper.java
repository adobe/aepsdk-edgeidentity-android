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

package com.adobe.marketing.mobile;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;


import com.adobe.marketing.mobile.identityedge.IdentityEdgeTestConstants;

import java.util.ArrayList;

/**
 * Helper class to update and remove persisted data to extension concerned with testing IdentityEdge.
 */
public class TestPersistenceHelper {

    private static ArrayList<String> knownDatastoreName = new ArrayList<String>() {{
        add(IdentityEdgeTestConstants.DataStoreKey.IDENTITYEDGE_DATASTORE);
        add(IdentityEdgeTestConstants.DataStoreKey.CONFIG_DATASTORE);
    }};

    /**
     * Helper method to update the {@link SharedPreferences} data.
     * @param datastore the name of the datastore to be updated
     * @param key the persisted data key that has to be updated
     * @param value the new value
     */
    public static void updatePersistence(final String datastore, final String key, final String value) {
        final Application application = TestHelper.defaultApplication;
        final Context context = application.getApplicationContext();
        SharedPreferences sharedPreferences = context.getSharedPreferences(datastore, Context.MODE_PRIVATE);;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key,value);
        editor.apply();
    }

    /**
     * Reads the requested persisted data from datastore.
     * @param datastore the name of the datastore to be read
     * @param key that needs to be read
     * @return the persisted data in {@code String}
     */
    public static String readPersistedData(final String datastore, final String key) {
        final Application application = TestHelper.defaultApplication;
        final Context context = application.getApplicationContext();
        SharedPreferences sharedPreferences = context.getSharedPreferences(datastore, Context.MODE_PRIVATE);;
        return sharedPreferences.getString(key, null);
    }

    /**
     * Clears the Configuration and Consent extension's persisted data
     */
    public static void resetKnownPersistence() {
        final Application application = TestHelper.defaultApplication;
        final Context context = application.getApplicationContext();
        for (String eachDatastore : knownDatastoreName) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(eachDatastore, Context.MODE_PRIVATE);;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();
        }
    }

}
