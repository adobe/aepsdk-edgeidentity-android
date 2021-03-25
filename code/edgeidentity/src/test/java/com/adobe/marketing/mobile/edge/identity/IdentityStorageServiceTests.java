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

import com.adobe.marketing.mobile.MobileCore;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MobileCore.class})
public class IdentityStorageServiceTests {

    @Mock
    Application mockApplication;

    @Mock
    Context mockContext;

    @Mock
    SharedPreferences mockSharedPreference;

    @Mock
    SharedPreferences.Editor mockSharedPreferenceEditor;

    @Before
    public void before() throws Exception {
        PowerMockito.mockStatic(MobileCore.class);

        Mockito.when(MobileCore.getApplication()).thenReturn(mockApplication);
        Mockito.when(mockApplication.getApplicationContext()).thenReturn(mockContext);
        Mockito.when(mockContext.getSharedPreferences(IdentityConstants.DataStoreKey.DATASTORE_NAME, 0)).thenReturn(mockSharedPreference);
        Mockito.when(mockSharedPreference.edit()).thenReturn(mockSharedPreferenceEditor);
    }

    @Test
    public void testLoadPropertiesFromPersistence_nullSharedPrefs() {
        // setup
        Mockito.when(mockApplication.getApplicationContext()).thenReturn(null);

        // test
        IdentityProperties props = IdentityStorageService.loadPropertiesFromPersistence();

        // verify
        assertNull(props);
    }

    @Test
    public void testLoadPropertiesFromPersistence_emptyPrefs() {
        // setup
        Mockito.when(mockSharedPreference.getString(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES, null)).thenReturn(null);

        // test
        IdentityProperties props = IdentityStorageService.loadPropertiesFromPersistence();

        // verify
        assertNull(props);
    }

    @Test
    public void testLoadPropertiesFromPersistence_invalidJSON() {
        // setup
        Mockito.when(mockSharedPreference.getString(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES, null)).thenReturn("{");

        // test
        IdentityProperties props = IdentityStorageService.loadPropertiesFromPersistence();

        // verify
        assertNull(props);
    }

    @Test
    public void testLoadPropertiesFromPersistence_validJSON() {
        // setup
        IdentityProperties persistedProps = new IdentityProperties();
        persistedProps.setECID(new ECID());
        final JSONObject jsonObject = new JSONObject(persistedProps.toXDMData(false));
        final String propsJSON = jsonObject.toString();
        Mockito.when(mockSharedPreference.getString(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES, null)).thenReturn(propsJSON);

        // test
        IdentityProperties props = IdentityStorageService.loadPropertiesFromPersistence();

        // verify
        assertEquals(persistedProps.toXDMData(false), props.toXDMData(false));
    }

    @Test
    public void testSavePropertiesToPersistence_nullSharedPrefs() {
        // setup
        Mockito.when(mockApplication.getApplicationContext()).thenReturn(null);

        // test
        IdentityProperties props = new IdentityProperties();
        IdentityStorageService.savePropertiesToPersistence(props);

        // verify
        verify(mockSharedPreferenceEditor, never()).apply();
    }

    @Test
    public void testSavePropertiesToPersistence_nullEditor() {
        // setup
        Mockito.when(mockSharedPreference.edit()).thenReturn(null);

        // test
        IdentityProperties props = new IdentityProperties();
        IdentityStorageService.savePropertiesToPersistence(props);

        // verify
        verify(mockSharedPreferenceEditor, never()).apply();
    }

    @Test
    public void testSavePropertiesToPersistence_nullProps() {
        // test
        IdentityStorageService.savePropertiesToPersistence(null);

        // verify
        verify(mockSharedPreferenceEditor, Mockito.times(1)).remove(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES);
        verify(mockSharedPreferenceEditor, Mockito.times(1)).apply();
    }

    @Test
    public void testSavePropertiesToPersistence_validProps() {
        // test
        IdentityProperties props = new IdentityProperties();
        props.setECID(new ECID());
        IdentityStorageService.savePropertiesToPersistence(props);

        // verify
        final JSONObject jsonObject = new JSONObject(props.toXDMData(false));
        final String expectedJSON = jsonObject.toString();
        verify(mockSharedPreferenceEditor, Mockito.times(1)).putString(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES, expectedJSON);
        verify(mockSharedPreferenceEditor, Mockito.times(1)).apply();
    }

    @Test
    public void testLoadEcidFromDirectIdentityPersistence_loadECID() {
        ECID ecid = new ECID();
        Mockito.when(mockContext.getSharedPreferences(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_DATASTORE_NAME, 0)).thenReturn(mockSharedPreference);
        Mockito.when(mockSharedPreference.getString(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_ECID_KEY, null)).thenReturn(ecid.toString());

        assertEquals(ecid, IdentityStorageService.loadEcidFromDirectIdentityPersistence());
    }

    @Test
    public void testLoadEcidFromDirectIdentityPersistence_whenNullECID() {
        Mockito.when(mockContext.getSharedPreferences(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_DATASTORE_NAME, 0)).thenReturn(mockSharedPreference);
        Mockito.when(mockSharedPreference.getString(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_ECID_KEY, null)).thenReturn(null);

        assertNull(IdentityStorageService.loadEcidFromDirectIdentityPersistence());
    }

    @Test
    public void testLoadEcidFromDirectIdentityPersistence_whenEmptyECID() {
        Mockito.when(mockContext.getSharedPreferences(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_DATASTORE_NAME, 0)).thenReturn(mockSharedPreference);
        Mockito.when(mockSharedPreference.getString(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_ECID_KEY, null)).thenReturn("");

        assertNull(IdentityStorageService.loadEcidFromDirectIdentityPersistence());
    }

}
