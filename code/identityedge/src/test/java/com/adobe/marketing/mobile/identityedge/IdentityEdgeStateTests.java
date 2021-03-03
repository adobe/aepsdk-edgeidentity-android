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

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.MobilePrivacyStatus;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        assertNull(state.getIdentityProperties().getECID());

        // test
        Map<String, Object> config = new HashMap<String, Object>();
        boolean result = state.bootupIfReady(config);

        // verify
        assertTrue(result);
        assertNotNull(state.getIdentityProperties().getECID());
    }

    @Test
    public void testIdentityEdgeState_BootupIfReadyWithOptInPrivacyReturnsTrue() {
        // setup
        IdentityEdgeState state = new IdentityEdgeState(new IdentityEdgeProperties());

        // test
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(IdentityEdgeConstants.Configuration.GLOBAL_CONFIG_PRIVACY, "optedin");
        boolean result = state.bootupIfReady(config);

        // verify
        assertTrue(result);
        assertNotNull(state.getIdentityProperties().getECID());
        assertEquals(MobilePrivacyStatus.OPT_IN, state.getIdentityProperties().getPrivacyStatus());
    }

    @Test
    public void testIdentityEdgeState_BootupIfReadyWithOptOutPrivacyReturnsTrue() {
        // setup
        IdentityEdgeState state = new IdentityEdgeState(new IdentityEdgeProperties());

        // test
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(IdentityEdgeConstants.Configuration.GLOBAL_CONFIG_PRIVACY, "optedout");
        boolean result = state.bootupIfReady(config);

        // verify
        assertTrue(result);
        assertNull(state.getIdentityProperties().getECID());
        assertEquals(MobilePrivacyStatus.OPT_OUT, state.getIdentityProperties().getPrivacyStatus());
    }

    @Test
    public void testIdentityEdgeState_BootupIfReadyWithOptUnknownPrivacyReturnsTrue() {
        // setup
        IdentityEdgeState state = new IdentityEdgeState(new IdentityEdgeProperties());

        // test
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(IdentityEdgeConstants.Configuration.GLOBAL_CONFIG_PRIVACY, "optedunknown");
        boolean result = state.bootupIfReady(config);

        // verify
        assertTrue(result);
        assertNotNull(state.getIdentityProperties().getECID());
        assertEquals(MobilePrivacyStatus.UNKNOWN, state.getIdentityProperties().getPrivacyStatus());
    }

    @Test
    public void testIdentityEdgeState_BootupIfReadyLoadsFromPersistence() {
        // setup
        IdentityEdgeState state = new IdentityEdgeState(new IdentityEdgeProperties());

        IdentityEdgeProperties persistedProps = new IdentityEdgeProperties();
        persistedProps.setECID(new ECID());
        final JSONObject jsonObject = new JSONObject(persistedProps.toMap());
        final String propsJSON = jsonObject.toString();
        Mockito.when(mockSharedPreference.getString(IdentityEdgeConstants.DataStoreKey.IDENTITY_PROPERTIES, null)).thenReturn(propsJSON);

        // test
        Map<String, Object> config = new HashMap<String, Object>();
        boolean result = state.bootupIfReady(config);

        // verify
        assertTrue(result);
        assertEquals(persistedProps.getECID().getEcidString(), state.getIdentityProperties().getECID().getEcidString());
        assertEquals(MobilePrivacyStatus.UNKNOWN, state.getIdentityProperties().getPrivacyStatus());
    }

    @Test
    public void testIdentityEdgeState_noPrivacyInEventData() {
        // setup
        IdentityEdgeState state = new IdentityEdgeState(new IdentityEdgeProperties());
        Event event = new Event.Builder("Config response", IdentityEdgeConstants.EventType.CONFIGURATION, IdentityEdgeConstants.EventSource.RESPONSE_CONTENT).build();

        // test
        verify(mockSharedPreferenceEditor, never()).apply(); // should not be saved to persistence
        assertFalse(state.processPrivacyChange(event));

        // verify
        assertEquals(MobilePrivacyStatus.UNKNOWN, state.getIdentityProperties().getPrivacyStatus());
    }

    @Test
    public void testIdentityEdgeState_changeToOptIn_existingECID() {
        // setup
        IdentityEdgeState state = new IdentityEdgeState(new IdentityEdgeProperties());
        state.getIdentityProperties().setECID(new ECID());
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(IdentityEdgeConstants.Configuration.GLOBAL_CONFIG_PRIVACY, "optedin");
        Event event = new Event.Builder("Config response", IdentityEdgeConstants.EventType.CONFIGURATION, IdentityEdgeConstants.EventSource.RESPONSE_CONTENT)
                .setEventData(config).build();

        // test
        verify(mockSharedPreferenceEditor, never()).apply(); // should not be saved to persistence
        assertFalse(state.processPrivacyChange(event));

        // verify
        assertEquals(MobilePrivacyStatus.OPT_IN, state.getIdentityProperties().getPrivacyStatus());
    }

    @Test
    public void testIdentityEdgeState_changeToOptIn_nullECID() {
        // setup
        IdentityEdgeState state = new IdentityEdgeState(new IdentityEdgeProperties());
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(IdentityEdgeConstants.Configuration.GLOBAL_CONFIG_PRIVACY, "optedin");
        Event event = new Event.Builder("Config response", IdentityEdgeConstants.EventType.CONFIGURATION, IdentityEdgeConstants.EventSource.RESPONSE_CONTENT)
                .setEventData(config).build();

        // test
        assertTrue(state.processPrivacyChange(event));
        verify(mockSharedPreferenceEditor, Mockito.times(1)).apply(); // should be saved to persistence

        // verify
        assertEquals(MobilePrivacyStatus.OPT_IN, state.getIdentityProperties().getPrivacyStatus());
        assertNotNull(state.getIdentityProperties().getECID());
    }

    @Test
    public void testIdentityEdgeState_changeToOptOut() {
        // setup
        IdentityEdgeState state = new IdentityEdgeState(new IdentityEdgeProperties());
        state.getIdentityProperties().setPrivacyStatus(MobilePrivacyStatus.UNKNOWN);
        state.getIdentityProperties().setECID(new ECID());
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(IdentityEdgeConstants.Configuration.GLOBAL_CONFIG_PRIVACY, "optedout");
        Event event = new Event.Builder("Config response", IdentityEdgeConstants.EventType.CONFIGURATION, IdentityEdgeConstants.EventSource.RESPONSE_CONTENT)
                .setEventData(config).build();

        // test
        assertTrue(state.processPrivacyChange(event));
        verify(mockSharedPreferenceEditor, Mockito.times(1)).apply(); // should be saved to persistence

        // verify
        assertEquals(MobilePrivacyStatus.OPT_OUT, state.getIdentityProperties().getPrivacyStatus());
        assertNull(state.getIdentityProperties().getECID()); // ecid cleared
    }

    @Test
    public void testIdentityEdgeState_changeToOptIn_fromOptOut() {
        // setup
        IdentityEdgeState state = new IdentityEdgeState(new IdentityEdgeProperties());
        state.getIdentityProperties().setPrivacyStatus(MobilePrivacyStatus.OPT_OUT);
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(IdentityEdgeConstants.Configuration.GLOBAL_CONFIG_PRIVACY, "optedin");
        Event event = new Event.Builder("Config response", IdentityEdgeConstants.EventType.CONFIGURATION, IdentityEdgeConstants.EventSource.RESPONSE_CONTENT)
                .setEventData(config).build();

        // test
        assertTrue(state.processPrivacyChange(event));
        verify(mockSharedPreferenceEditor, Mockito.times(1)).apply(); // should be saved to persistence

        // verify
        assertEquals(MobilePrivacyStatus.OPT_IN, state.getIdentityProperties().getPrivacyStatus());
        assertNotNull(state.getIdentityProperties().getECID()); // ecid generated
    }

    @Test
    public void testIdentityEdgeState_change_fromOptOutToUnknown() {
        // setup
        IdentityEdgeState state = new IdentityEdgeState(new IdentityEdgeProperties());
        state.getIdentityProperties().setPrivacyStatus(MobilePrivacyStatus.OPT_OUT);
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(IdentityEdgeConstants.Configuration.GLOBAL_CONFIG_PRIVACY, "optunknown");
        Event event = new Event.Builder("Config response", IdentityEdgeConstants.EventType.CONFIGURATION, IdentityEdgeConstants.EventSource.RESPONSE_CONTENT)
                .setEventData(config).build();

        // test
        assertTrue(state.processPrivacyChange(event));
        verify(mockSharedPreferenceEditor, Mockito.times(1)).apply(); // should be saved to persistence

        // verify
        assertEquals(MobilePrivacyStatus.UNKNOWN, state.getIdentityProperties().getPrivacyStatus());
        assertNotNull(state.getIdentityProperties().getECID()); // ecid generated
    }

    @Test
    public void testIdentityEdgeState_changeToOptIn_fromOptIn() {
        // setup
        IdentityEdgeState state = new IdentityEdgeState(new IdentityEdgeProperties());
        ECID existingEcid = new ECID();
        state.getIdentityProperties().setECID(existingEcid);
        Map<String, Object> config = new HashMap<String, Object>();
        config.put(IdentityEdgeConstants.Configuration.GLOBAL_CONFIG_PRIVACY, "optedin");
        Event event = new Event.Builder("Config response", IdentityEdgeConstants.EventType.CONFIGURATION, IdentityEdgeConstants.EventSource.RESPONSE_CONTENT)
                .setEventData(config).build();

        // test
        verify(mockSharedPreferenceEditor, never()).apply(); // should be saved to persistence
        assertFalse(state.processPrivacyChange(event));

        // verify
        assertEquals(MobilePrivacyStatus.OPT_IN, state.getIdentityProperties().getPrivacyStatus());
        assertEquals(existingEcid.getEcidString(), state.getIdentityProperties().getECID().getEcidString()); // ecid should remain the same
    }

}
