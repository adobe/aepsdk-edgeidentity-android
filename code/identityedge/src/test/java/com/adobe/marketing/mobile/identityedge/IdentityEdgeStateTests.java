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
public class IdentityEdgeStateTests {

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
        Mockito.when(mockContext.getSharedPreferences(IdentityEdgeConstants.DataStoreKey.DATASTORE_NAME, 0)).thenReturn(mockSharedPreference);
        Mockito.when(mockSharedPreference.edit()).thenReturn(mockSharedPreferenceEditor);
    }


    @Test
    public void testIdentityEdgeState_BootupIfReadyGeneratesECID() {
        // setup
        IdentityEdgeState state = new IdentityEdgeState(new IdentityEdgeProperties());
        assertNull(state.getIdentityEdgeProperties().getECID());

        // test
        boolean result = state.bootupIfReady();
        verify(mockSharedPreferenceEditor, Mockito.times(1)).apply(); // saves to data store

        // verify
        assertTrue(result);
        assertNotNull(state.getIdentityEdgeProperties().getECID());
    }

    @Test
    public void testIdentityEdgeState_BootupIfReadyLoadsDirectIdentityECID() {
        // setup
        ECID ecid = new ECID();
        Mockito.when(mockContext.getSharedPreferences(IdentityEdgeConstants.DataStoreKey.IDENTITY_DIRECT_DATASTORE_NAME, 0)).thenReturn(mockSharedPreference);
        Mockito.when(mockSharedPreference.getString(IdentityEdgeConstants.DataStoreKey.IDENTITY_DIRECT_ECID_KEY, null)).thenReturn(ecid.toString());

        IdentityEdgeState state = new IdentityEdgeState(new IdentityEdgeProperties());
        assertNull(state.getIdentityEdgeProperties().getECID());

        // test
        boolean result = state.bootupIfReady();
        verify(mockSharedPreferenceEditor, Mockito.times(1)).apply(); // saves to data store

        // verify
        assertTrue(result);
        assertEquals(ecid, state.getIdentityEdgeProperties().getECID());
    }

    @Test
    public void testIdentityEdgeState_BootupIfReadyGenratesECIDWhenDirectECIDIsNull() {
        // setup
        Mockito.when(mockContext.getSharedPreferences(IdentityEdgeConstants.DataStoreKey.IDENTITY_DIRECT_DATASTORE_NAME, 0)).thenReturn(mockSharedPreference);
        Mockito.when(mockSharedPreference.getString(IdentityEdgeConstants.DataStoreKey.IDENTITY_DIRECT_ECID_KEY, null)).thenReturn(null);

        IdentityEdgeState state = new IdentityEdgeState(new IdentityEdgeProperties());
        assertNull(state.getIdentityEdgeProperties().getECID());

        // test
        boolean result = state.bootupIfReady();
        verify(mockSharedPreferenceEditor, Mockito.times(1)).apply(); // saves to data store

        // verify
        assertTrue(result);
        assertNotNull(state.getIdentityEdgeProperties().getECID());
    }

    @Test
    public void testIdentityEdgeState_BootupIfReadyLoadsFromPersistence() {
        // setup
        IdentityEdgeState state = new IdentityEdgeState(new IdentityEdgeProperties());

        IdentityEdgeProperties persistedProps = new IdentityEdgeProperties();
        persistedProps.setECID(new ECID());
        final JSONObject jsonObject = new JSONObject(persistedProps.toXDMData(false));
        final String propsJSON = jsonObject.toString();
        Mockito.when(mockSharedPreference.getString(IdentityEdgeConstants.DataStoreKey.IDENTITY_PROPERTIES, null)).thenReturn(propsJSON);

        // test
        boolean result = state.bootupIfReady();
        verify(mockSharedPreferenceEditor, never()).apply();

        // verify
        assertTrue(result);
        assertEquals(persistedProps.getECID().toString(), state.getIdentityEdgeProperties().getECID().toString());
    }

    @Test
    public void testIdentityEdgeState_BootupIfReadyLoadsFromPersistenceWhenDirectECIDIsValid() {
        // setup
        ECID ecid = new ECID();
        Mockito.when(mockContext.getSharedPreferences(IdentityEdgeConstants.DataStoreKey.IDENTITY_DIRECT_DATASTORE_NAME, 0)).thenReturn(mockSharedPreference);
        Mockito.when(mockSharedPreference.getString(IdentityEdgeConstants.DataStoreKey.IDENTITY_DIRECT_ECID_KEY, null)).thenReturn(ecid.toString());

        IdentityEdgeState state = new IdentityEdgeState(new IdentityEdgeProperties());

        IdentityEdgeProperties persistedProps = new IdentityEdgeProperties();
        persistedProps.setECID(new ECID());
        final JSONObject jsonObject = new JSONObject(persistedProps.toXDMData(false));
        final String propsJSON = jsonObject.toString();
        Mockito.when(mockSharedPreference.getString(IdentityEdgeConstants.DataStoreKey.IDENTITY_PROPERTIES, null)).thenReturn(propsJSON);

        // test
        boolean result = state.bootupIfReady();
        verify(mockSharedPreferenceEditor, never()).apply();

        // verify
        assertTrue(result);
        assertEquals(persistedProps.getECID(), state.getIdentityEdgeProperties().getECID());
    }

    @Test
    public void testIdentityEdgeState_resetIdentifiers() {
        // setup
        IdentityEdgeState state = new IdentityEdgeState(new IdentityEdgeProperties());
        state.getIdentityEdgeProperties().setECID(new ECID());
        state.getIdentityEdgeProperties().setECIDSecondary(new ECID());
        ECID existingEcid = state.getIdentityEdgeProperties().getECID();

        // test
        state.resetIdentifiers();

        // verify
        assertNotEquals(existingEcid, state.getIdentityEdgeProperties().getECID()); // ECID should be regenerated
        assertFalse(state.getIdentityEdgeProperties().getECID().toString().isEmpty()); // ECID should not be empty
        assertNull(state.getIdentityEdgeProperties().getECIDSecondary()); // should be cleared
        verify(mockSharedPreferenceEditor, Mockito.times(1)).apply(); // should save to data store
    }

    @Test
    public void testIdentityEdgeState_updateLegacyExperienceCloudId() {
        IdentityEdgeState state = new IdentityEdgeState(new IdentityEdgeProperties());
        ECID legacyEcid = new ECID();

        state.updateLegacyExperienceCloudId(legacyEcid);

        assertEquals(legacyEcid, state.getIdentityEdgeProperties().getECIDSecondary());
        verify(mockSharedPreferenceEditor, Mockito.times(1)).apply();
    }

    @Test
    public void testIdentityEdgeState_updateLegacyExperienceCloudId_notSetWhenECIDSame() {
        IdentityEdgeState state = new IdentityEdgeState(new IdentityEdgeProperties());
        ECID legacyEcid = new ECID();
        state.getIdentityEdgeProperties().setECID(legacyEcid);

        state.updateLegacyExperienceCloudId(legacyEcid);

        assertNull(state.getIdentityEdgeProperties().getECIDSecondary());
        verify(mockSharedPreferenceEditor, Mockito.times(0)).apply();
    }

    @Test
    public void testIdentityEdgeState_updateLegacyExperienceCloudId_notSetWhenSecondaryECIDSame() {
        IdentityEdgeState state = new IdentityEdgeState(new IdentityEdgeProperties());
        ECID legacyEcid = new ECID();
        state.getIdentityEdgeProperties().setECIDSecondary(legacyEcid);

        state.updateLegacyExperienceCloudId(legacyEcid);

        assertEquals(legacyEcid, state.getIdentityEdgeProperties().getECIDSecondary());
        verify(mockSharedPreferenceEditor, Mockito.times(0)).apply();
    }

    @Test
    public void testIdentityEdgeState_updateLegacyExperienceCloudId_clearsOnNull() {
        IdentityEdgeState state = new IdentityEdgeState(new IdentityEdgeProperties());
        state.getIdentityEdgeProperties().setECID(new ECID());
        state.getIdentityEdgeProperties().setECIDSecondary(new ECID());

        state.updateLegacyExperienceCloudId(null);

        assertNull(state.getIdentityEdgeProperties().getECIDSecondary());
        verify(mockSharedPreferenceEditor, Mockito.times(1)).apply();
    }

    @Test
    public void testIdentityEdgeState_updateLegacyExperienceCloudId_notSetWhenExistingIsNull() {
        IdentityEdgeState state = new IdentityEdgeState(new IdentityEdgeProperties());
        state.getIdentityEdgeProperties().setECID(new ECID());

        state.updateLegacyExperienceCloudId(null);

        assertNull(state.getIdentityEdgeProperties().getECIDSecondary());
        verify(mockSharedPreferenceEditor, Mockito.times(0)).apply();
    }
}
