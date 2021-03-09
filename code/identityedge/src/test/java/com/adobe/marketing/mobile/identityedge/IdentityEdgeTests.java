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

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.MobileCore;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MobileCore.class})
public class IdentityEdgeTests {

    @Before
    public void setup() {
        PowerMockito.mockStatic(MobileCore.class);
    }

    // ========================================================================================
    // extensionVersion
    // ========================================================================================

    @Test
    public void test_extensionVersionAPI() {
        // test
        String extensionVersion = IdentityEdge.extensionVersion();
        assertEquals("The Extension version API returns the correct value", IdentityEdgeConstants.EXTENSION_VERSION,
                extensionVersion);
    }

    // ========================================================================================
    // registerExtension
    // ========================================================================================
    @Test
    public void testRegistration() {
        // test
        IdentityEdge.registerExtension();
        final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

        // The identity edge extension should register with core
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.registerExtension(ArgumentMatchers.eq(IdentityEdgeExtension.class), callbackCaptor.capture());

        // verify the callback
        ExtensionErrorCallback extensionErrorCallback = callbackCaptor.getValue();
        assertNotNull("The extension callback should not be null", extensionErrorCallback);

        // TODO - enable when ExtensionError creation is available
        // should not crash on calling the callback
        //extensionErrorCallback.error(ExtensionError.UNEXPECTED_ERROR);
    }

    // ========================================================================================
    // getExperienceCloudId API
    // ========================================================================================
    @Test
    public void testGetExperienceCloudId() {
        // setup
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        final ArgumentCaptor<AdobeCallback> adobeCallbackCaptor = ArgumentCaptor.forClass(AdobeCallback.class);
        final ArgumentCaptor<ExtensionErrorCallback> extensionErrorCallbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);
        final List<String> callbackReturnValues = new ArrayList<>();

        // test
        IdentityEdge.getExperienceCloudId(new AdobeCallback<String>() {
            @Override
            public void call(String s) {
                callbackReturnValues.add(s);
            }
        });

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEventWithResponseCallback(eventCaptor.capture(), adobeCallbackCaptor.capture(), extensionErrorCallbackCaptor.capture());

        // verify the dispatched event details
        Event dispatchedEvent = eventCaptor.getValue();
        assertEquals(IdentityEdgeConstants.EventNames.IDENTITY_REQUEST_IDENTITY_ECID, dispatchedEvent.getName());
        assertEquals(IdentityEdgeConstants.EventType.IDENTITY_EDGE.toLowerCase(), dispatchedEvent.getType());
        assertEquals(IdentityEdgeConstants.EventSource.REQUEST_CONTENT.toLowerCase(), dispatchedEvent.getSource());
        assertTrue(dispatchedEvent.getEventData().isEmpty());

        // verify callback responses
        IdentityEdgeProperties props = new IdentityEdgeProperties();
        ECID ecid = new ECID();
        props.setECID(ecid);
        adobeCallbackCaptor.getValue().call(buildECIDResponseEvent(props.toXDMData(false)));
        assertEquals(ecid.getEcidString(), callbackReturnValues.get(0));

        // TODO - enable when ExtensionError creation is available
        // should not crash on calling the callback
        //extensionErrorCallback.error(ExtensionError.UNEXPECTED_ERROR);
    }

    @Test
    public void testGetExperienceCloudId_NullCallback() {
        // test
        IdentityEdge.getExperienceCloudId(null);

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(0));
        MobileCore.dispatchEventWithResponseCallback(any(Event.class), any(AdobeCallback.class), any(ExtensionErrorCallback.class));
    }

    @Test
    public void testGetExperienceCloudId_NullResponseEvent() {
        // setup
        final String KEY_IS_ERRORCALLBACK_CALLED = "errorCallBackCalled";
        final String KEY_CAPTUREDERRORCALLBACK = "capturedErrorCallback";
        final Map<String, Object> errorCapture = new HashMap<>();
        final ArgumentCaptor<AdobeCallback> adobeCallbackCaptor = ArgumentCaptor.forClass(AdobeCallback.class);
        final AdobeCallbackWithError callbackWithError = new AdobeCallbackWithError() {
            @Override
            public void fail(AdobeError adobeError) {
                errorCapture.put(KEY_IS_ERRORCALLBACK_CALLED, true);
                errorCapture.put(KEY_CAPTUREDERRORCALLBACK, adobeError);
            }

            @Override
            public void call(Object o) { }
        };

        // test
        IdentityEdge.getExperienceCloudId(callbackWithError);

        // verify if the event is dispatched
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEventWithResponseCallback(any(Event.class), adobeCallbackCaptor.capture(), any(ExtensionErrorCallback.class));

        // set response event to null
        adobeCallbackCaptor.getValue().call(null);

        // verify
        assertTrue((boolean)errorCapture.get(KEY_IS_ERRORCALLBACK_CALLED));
        assertEquals(AdobeError.UNEXPECTED_ERROR, errorCapture.get(KEY_CAPTUREDERRORCALLBACK));
    }

    @Test
    public void testGetExperienceCloudId_MissingECID() {
        // setup
        final String KEY_IS_ERRORCALLBACK_CALLED = "errorCallBackCalled";
        final String KEY_CAPTUREDERRORCALLBACK = "capturedErrorCallback";
        final Map<String, Object> errorCapture = new HashMap<>();
        final ArgumentCaptor<AdobeCallback> adobeCallbackCaptor = ArgumentCaptor.forClass(AdobeCallback.class);
        final AdobeCallbackWithError callbackWithError = new AdobeCallbackWithError() {
            @Override
            public void fail(AdobeError adobeError) {
                errorCapture.put(KEY_IS_ERRORCALLBACK_CALLED, true);
                errorCapture.put(KEY_CAPTUREDERRORCALLBACK, adobeError);
            }

            @Override
            public void call(Object o) { }
        };

        // test
        IdentityEdge.getExperienceCloudId(callbackWithError);

        // verify if the event is dispatched
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEventWithResponseCallback(any(Event.class), adobeCallbackCaptor.capture(), any(ExtensionErrorCallback.class));

        // set response event to map missing ECID
        IdentityEdgeProperties props = new IdentityEdgeProperties();
        adobeCallbackCaptor.getValue().call(buildECIDResponseEvent(props.toXDMData(false)));

        // verify
        assertTrue((boolean)errorCapture.get(KEY_IS_ERRORCALLBACK_CALLED));
        assertEquals(AdobeError.UNEXPECTED_ERROR, errorCapture.get(KEY_CAPTUREDERRORCALLBACK));
    }

    // ========================================================================================
    // Private method
    // ========================================================================================
    private Event buildECIDResponseEvent (final Map<String, Object> eventData) {
        return new Event.Builder(IdentityEdgeConstants.EventNames.IDENTITY_REQUEST_IDENTITY_ECID, IdentityEdgeConstants.EventType.IDENTITY_EDGE, IdentityEdgeConstants.EventSource.RESPONSE_IDENTITY).setEventData(eventData).build();
    }

}
