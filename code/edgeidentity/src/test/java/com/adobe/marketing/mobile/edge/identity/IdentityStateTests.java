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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
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

    @Before
    public void before() throws Exception {
        PowerMockito.mockStatic(MobileCore.class);

        Mockito.when(MobileCore.getApplication()).thenReturn(mockApplication);
        Mockito.when(mockApplication.getApplicationContext()).thenReturn(mockContext);
        Mockito.when(mockContext.getSharedPreferences(IdentityConstants.DataStoreKey.DATASTORE_NAME, 0)).thenReturn(mockSharedPreference);
        Mockito.when(mockSharedPreference.edit()).thenReturn(mockSharedPreferenceEditor);
    }


    @Test
    public void testBootUp_GeneratesECID() {
        // setup
        IdentityState state = new IdentityState(new IdentityProperties());
        assertNull(state.getIdentityProperties().getECID());

        // test
        state.bootUp();
        verify(mockSharedPreferenceEditor, Mockito.times(1)).apply(); // saves to data store

        // verify
        assertNotNull(state.getIdentityProperties().getECID());
    }

    @Test
    public void testBootUp_LoadsDirectIdentityECID() {
        // setup
        ECID ecid = new ECID();
        Mockito.when(mockContext.getSharedPreferences(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_DATASTORE_NAME, 0)).thenReturn(mockSharedPreference);
        Mockito.when(mockSharedPreference.getString(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_ECID_KEY, null)).thenReturn(ecid.toString());

        IdentityState state = new IdentityState(new IdentityProperties());
        assertNull(state.getIdentityProperties().getECID());

        // test
        state.bootUp();
        verify(mockSharedPreferenceEditor, Mockito.times(1)).apply(); // saves to data store

        // verify
        assertEquals(ecid, state.getIdentityProperties().getECID());
    }

    @Test
    public void testBootUp_GeneratesECIDWhenDirectECIDIsNull() {
        // setup
        Mockito.when(mockContext.getSharedPreferences(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_DATASTORE_NAME, 0)).thenReturn(mockSharedPreference);
        Mockito.when(mockSharedPreference.getString(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_ECID_KEY, null)).thenReturn(null);

        IdentityState state = new IdentityState(new IdentityProperties());
        assertNull(state.getIdentityProperties().getECID());

        // test
        state.bootUp();
        verify(mockSharedPreferenceEditor, Mockito.times(1)).apply(); // saves to data store

        // verify
        assertNotNull(state.getIdentityProperties().getECID());
    }

    @Test
    public void testBootUp_LoadsFromPersistence() {
        // setup
        IdentityState state = new IdentityState(new IdentityProperties());

        IdentityProperties persistedProps = new IdentityProperties();
        persistedProps.setECID(new ECID());
        final JSONObject jsonObject = new JSONObject(persistedProps.toXDMData(false));
        final String propsJSON = jsonObject.toString();
        Mockito.when(mockSharedPreference.getString(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES, null)).thenReturn(propsJSON);

        // test
        state.bootUp();
        verify(mockSharedPreferenceEditor, never()).apply();

        // verify
        assertEquals(persistedProps.getECID().toString(), state.getIdentityProperties().getECID().toString());
    }

    @Test
    public void testBootUp_IfReadyLoadsFromPersistenceWhenDirectECIDIsValid() {
        // setup
        ECID ecid = new ECID();
        Mockito.when(mockContext.getSharedPreferences(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_DATASTORE_NAME, 0)).thenReturn(mockSharedPreference);
        Mockito.when(mockSharedPreference.getString(IdentityConstants.DataStoreKey.IDENTITY_DIRECT_ECID_KEY, null)).thenReturn(ecid.toString());

        IdentityState state = new IdentityState(new IdentityProperties());

        IdentityProperties persistedProps = new IdentityProperties();
        persistedProps.setECID(new ECID());
        final JSONObject jsonObject = new JSONObject(persistedProps.toXDMData(false));
        final String propsJSON = jsonObject.toString();
        Mockito.when(mockSharedPreference.getString(IdentityConstants.DataStoreKey.IDENTITY_PROPERTIES, null)).thenReturn(propsJSON);

        // test
        state. bootUp();
        verify(mockSharedPreferenceEditor, never()).apply();

        // verify
        assertEquals(persistedProps.getECID(), state.getIdentityProperties().getECID());
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


    // ======================================================================================================================
    // Tests for "updateCustomerIdentifiers" is already covered in "handleUpdateRequest" tests in IdentityExtensionTests
    // ======================================================================================================================


    // ======================================================================================================================
    // Tests for "removeCustomerIdentifiers" is already covered in handleRemoveRequest tests in IdentityExtensionTests
    // ======================================================================================================================

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
