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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import android.util.Log;
import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.MobileCore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ MobileCore.class })
public class IdentityTests {

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
		String extensionVersion = Identity.extensionVersion();
		assertEquals(
			"The Extension version API returns the correct value",
			IdentityConstants.EXTENSION_VERSION,
			extensionVersion
		);
	}

	// ========================================================================================
	// registerExtension
	// ========================================================================================
	@Test
	public void testRegistration() {
		// test
		Identity.registerExtension();
		final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(
			ExtensionErrorCallback.class
		);

		// The identity extension should register with core
		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.registerExtension(ArgumentMatchers.eq(IdentityExtension.class), callbackCaptor.capture());

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
		final ArgumentCaptor<ExtensionErrorCallback> extensionErrorCallbackCaptor = ArgumentCaptor.forClass(
			ExtensionErrorCallback.class
		);
		final List<String> callbackReturnValues = new ArrayList<>();

		// test
		Identity.getExperienceCloudId(
			new AdobeCallback<String>() {
				@Override
				public void call(String s) {
					callbackReturnValues.add(s);
				}
			}
		);

		// verify
		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEventWithResponseCallback(
			eventCaptor.capture(),
			adobeCallbackCaptor.capture(),
			extensionErrorCallbackCaptor.capture()
		);

		// verify the dispatched event details
		Event dispatchedEvent = eventCaptor.getValue();
		assertEquals(IdentityConstants.EventNames.IDENTITY_REQUEST_IDENTITY_ECID, dispatchedEvent.getName());
		assertEquals(IdentityConstants.EventType.EDGE_IDENTITY.toLowerCase(), dispatchedEvent.getType());
		assertEquals(IdentityConstants.EventSource.REQUEST_IDENTITY.toLowerCase(), dispatchedEvent.getSource());
		assertTrue(dispatchedEvent.getEventData().isEmpty());

		// verify callback responses
		ECID ecid = new ECID();

		Map<String, Object> ecidDict = new HashMap<>();
		ecidDict.put("id", ecid.toString());
		ArrayList<Object> ecidArr = new ArrayList<>();
		ecidArr.add(ecidDict);
		Map<String, Object> identityMap = new HashMap<>();
		identityMap.put("ECID", ecidArr);
		Map<String, Object> xdmData = new HashMap<>();
		xdmData.put("identityMap", identityMap);

		adobeCallbackCaptor.getValue().call(buildIdentityResponseEvent(xdmData));
		assertEquals(ecid.toString(), callbackReturnValues.get(0));
		// TODO - enable when ExtensionError creation is available
		// should not crash on calling the callback
		//extensionErrorCallback.error(ExtensionError.UNEXPECTED_ERROR);
	}

	@Test
	public void testGetExperienceCloudId_nullCallback() {
		// test
		Identity.getExperienceCloudId(null);

		// verify
		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(0));
		MobileCore.dispatchEventWithResponseCallback(
			any(Event.class),
			any(AdobeCallback.class),
			any(ExtensionErrorCallback.class)
		);
	}

	@Test
	public void testGetExperienceCloudId_nullResponseEvent() {
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
			public void call(Object o) {}
		};

		// test
		Identity.getExperienceCloudId(callbackWithError);

		// verify if the event is dispatched
		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEventWithResponseCallback(
			any(Event.class),
			adobeCallbackCaptor.capture(),
			any(ExtensionErrorCallback.class)
		);

		// set response event to null
		adobeCallbackCaptor.getValue().call(null);

		// verify
		assertTrue((boolean) errorCapture.get(KEY_IS_ERRORCALLBACK_CALLED));
		assertEquals(AdobeError.UNEXPECTED_ERROR, errorCapture.get(KEY_CAPTUREDERRORCALLBACK));
	}

	@Test
	public void testGetExperienceCloudId_invalidEventData() {
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
			public void call(Object o) {}
		};

		// test
		Identity.getExperienceCloudId(callbackWithError);

		// verify if the event is dispatched
		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEventWithResponseCallback(
			any(Event.class),
			adobeCallbackCaptor.capture(),
			any(ExtensionErrorCallback.class)
		);

		// set response event to null
		Map<String, Object> eventData = new HashMap<>();
		eventData.put("someKey", "someValue");
		adobeCallbackCaptor.getValue().call(buildIdentityResponseEvent(eventData));

		// verify
		assertTrue((boolean) errorCapture.get(KEY_IS_ERRORCALLBACK_CALLED));
		assertEquals(AdobeError.UNEXPECTED_ERROR, errorCapture.get(KEY_CAPTUREDERRORCALLBACK));
	}

	@Test
	public void testGetExperienceCloudId_missingECID() {
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
			public void call(Object o) {}
		};

		// test
		Identity.getExperienceCloudId(callbackWithError);

		// verify if the event is dispatched
		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEventWithResponseCallback(
			any(Event.class),
			adobeCallbackCaptor.capture(),
			any(ExtensionErrorCallback.class)
		);

		// set response event to map missing ECID
		Map<String, Object> emptyXDMData = new HashMap<>();
		adobeCallbackCaptor.getValue().call(buildIdentityResponseEvent(emptyXDMData));

		// verify
		assertTrue((boolean) errorCapture.get(KEY_IS_ERRORCALLBACK_CALLED));
		assertEquals(AdobeError.UNEXPECTED_ERROR, errorCapture.get(KEY_CAPTUREDERRORCALLBACK));
	}

	// ========================================================================================
	// getUrlVariables API
	// ========================================================================================
	@Test
	public void testGetUrlVariables() throws InterruptedException {
		// setup
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		final ArgumentCaptor<AdobeCallback> adobeCallbackCaptor = ArgumentCaptor.forClass(AdobeCallback.class);
		final ArgumentCaptor<ExtensionErrorCallback> extensionErrorCallbackCaptor = ArgumentCaptor.forClass(
			ExtensionErrorCallback.class
		);
		final List<String> callbackReturnValues = new ArrayList<>();
		final ADBCountDownLatch latch = new ADBCountDownLatch(1);
		// test
		Identity.getUrlVariables(
			new AdobeCallback<String>() {
				@Override
				public void call(String s) {
					callbackReturnValues.add(s);
					latch.countDown();
				}
			}
		);
		latch.await(2000, TimeUnit.MILLISECONDS);
		// verify
		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEventWithResponseCallback(
			eventCaptor.capture(),
			adobeCallbackCaptor.capture(),
			extensionErrorCallbackCaptor.capture()
		);

		// verify the dispatched event details
		Event dispatchedEvent = eventCaptor.getValue();
		assertEquals(IdentityConstants.EventNames.IDENTITY_REQUEST_URL_VARIABLES, dispatchedEvent.getName());
		assertEquals(IdentityConstants.EventType.EDGE_IDENTITY.toLowerCase(), dispatchedEvent.getType());
		assertEquals(IdentityConstants.EventSource.REQUEST_IDENTITY.toLowerCase(), dispatchedEvent.getSource());
		assertTrue(dispatchedEvent.getEventData().containsKey("urlvariables"));
		assertTrue((boolean) dispatchedEvent.getEventData().get("urlvariables"));

		// verify callback responses
		Map<String, Object> urlVariablesResponse = new HashMap<>();
		urlVariablesResponse.put("urlvariables", "test-url-variable-string");

		adobeCallbackCaptor.getValue().call(buildUrlVariablesResponseEvent(urlVariablesResponse));
		assertEquals("test-url-variable-string", callbackReturnValues.get(0));
		// TODO - enable when ExtensionError creation is available
		// should not crash on calling the callback
		//extensionErrorCallback.error(ExtensionError.UNEXPECTED_ERROR);
	}

	@Test
	public void testGetUrlVariables_nullCallback() {
		// test
		Identity.getExperienceCloudId(null);

		// verify
		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(0));
		MobileCore.dispatchEventWithResponseCallback(
			any(Event.class),
			any(AdobeCallback.class),
			any(ExtensionErrorCallback.class)
		);
	}

	@Test
	public void testGetUrlVariables_nullResponseEvent() {
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
			public void call(Object o) {}
		};

		// test
		Identity.getUrlVariables(callbackWithError);

		// verify if the event is dispatched
		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEventWithResponseCallback(
			any(Event.class),
			adobeCallbackCaptor.capture(),
			any(ExtensionErrorCallback.class)
		);

		// set response event to null
		adobeCallbackCaptor.getValue().call(null);

		// verify
		assertTrue((boolean) errorCapture.get(KEY_IS_ERRORCALLBACK_CALLED));
		assertEquals(AdobeError.UNEXPECTED_ERROR, errorCapture.get(KEY_CAPTUREDERRORCALLBACK));
	}

	@Test
	public void testGetUrlVariables_invalidEventData() {
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
			public void call(Object o) {}
		};

		// test
		Identity.getUrlVariables(callbackWithError);

		// verify if the event is dispatched
		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEventWithResponseCallback(
			any(Event.class),
			adobeCallbackCaptor.capture(),
			any(ExtensionErrorCallback.class)
		);

		// set response event data to not have urlvariables key
		Map<String, Object> eventData = new HashMap<>();
		eventData.put("someKey", "someValue");
		eventData.put("urlvariables", true);
		adobeCallbackCaptor.getValue().call(buildUrlVariablesResponseEvent(eventData));

		// verify
		assertTrue((boolean) errorCapture.get(KEY_IS_ERRORCALLBACK_CALLED));
		assertEquals(AdobeError.UNEXPECTED_ERROR, errorCapture.get(KEY_CAPTUREDERRORCALLBACK));
	}

	@Test
	public void testGetUrlVariables_NullUrlVariablesStringInResponseData() {
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
			public void call(Object o) {
				Log.d("test", "test");
			}
		};

		// test
		Identity.getUrlVariables(callbackWithError);

		// verify if the event is dispatched
		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEventWithResponseCallback(
			any(Event.class),
			adobeCallbackCaptor.capture(),
			any(ExtensionErrorCallback.class)
		);

		// set response event to have urlvariables map to null value
		Map<String, Object> nullUrlVariablesData = new HashMap<>();
		nullUrlVariablesData.put("urlvariables", null);
		adobeCallbackCaptor.getValue().call(buildUrlVariablesResponseEvent(nullUrlVariablesData));

		// verify
		assertTrue((boolean) errorCapture.get(KEY_IS_ERRORCALLBACK_CALLED));
		assertEquals(AdobeError.UNEXPECTED_ERROR, errorCapture.get(KEY_CAPTUREDERRORCALLBACK));
	}

	// ========================================================================================
	// updateIdentities API
	// ========================================================================================
	@Test
	public void testUpdateIdentities() {
		// setup
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		final ArgumentCaptor<ExtensionErrorCallback> extensionErrorCallbackCaptor = ArgumentCaptor.forClass(
			ExtensionErrorCallback.class
		);

		// test
		IdentityMap map = new IdentityMap();
		map.addItem(new IdentityItem("id", AuthenticatedState.AUTHENTICATED, true), "mainspace");
		map.addItem(new IdentityItem("idtwo", AuthenticatedState.LOGGED_OUT, false), "secondspace");
		Identity.updateIdentities(map);

		// verify
		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEvent(eventCaptor.capture(), extensionErrorCallbackCaptor.capture());

		// TODO - enable when ExtensionError creation is available
		// should not crash on calling the callback
		//extensionErrorCallback.error(ExtensionError.UNEXPECTED_ERROR);

		// verify the dispatched event details
		Event dispatchedEvent = eventCaptor.getValue();
		assertEquals(IdentityConstants.EventNames.UPDATE_IDENTITIES, dispatchedEvent.getName());
		assertEquals(IdentityConstants.EventType.EDGE_IDENTITY.toLowerCase(), dispatchedEvent.getType());
		assertEquals(IdentityConstants.EventSource.UPDATE_IDENTITY.toLowerCase(), dispatchedEvent.getSource());
		assertEquals(map.asXDMMap(), dispatchedEvent.getEventData());
	}

	@Test
	public void testUpdateIdentitiesNullAndEmptyMap() {
		// test
		IdentityMap map = new IdentityMap();
		Identity.updateIdentities(map);
		Identity.updateIdentities(null);

		// verify none of these API calls dispatch an event
		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(0));
		MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
	}

	@Test
	public void testRemoveIdentity() {
		// setup
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		final ArgumentCaptor<ExtensionErrorCallback> extensionErrorCallbackCaptor = ArgumentCaptor.forClass(
			ExtensionErrorCallback.class
		);
		IdentityItem sampleItem = new IdentityItem("sample", AuthenticatedState.AMBIGUOUS, false);

		// test
		Identity.removeIdentity(sampleItem, "namespace");

		// verify dispatch event
		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEvent(eventCaptor.capture(), extensionErrorCallbackCaptor.capture());

		Event dispatchedEvent = eventCaptor.getValue();
		assertEquals(IdentityConstants.EventNames.REMOVE_IDENTITIES, dispatchedEvent.getName());
		assertEquals(IdentityConstants.EventType.EDGE_IDENTITY.toLowerCase(), dispatchedEvent.getType());
		assertEquals(IdentityConstants.EventSource.REMOVE_IDENTITY.toLowerCase(), dispatchedEvent.getSource());
		IdentityMap sampleInputIdentitymap = new IdentityMap();
		sampleInputIdentitymap.addItem(sampleItem, "namespace");
		assertEquals(sampleInputIdentitymap.asXDMMap(), dispatchedEvent.getEventData());
		// TODO - enable when ExtensionError creation is available
		// should not crash on calling the callback
		//extensionErrorCallback.error(ExtensionError.UNEXPECTED_ERROR);
	}

	@Test
	public void testRemoveIdentity_WithInvalidInputs() {
		// setup
		IdentityItem sampleItem = new IdentityItem("sample", AuthenticatedState.AMBIGUOUS, false);

		// test
		Identity.removeIdentity(null, "namespace");
		Identity.removeIdentity(sampleItem, "");
		Identity.removeIdentity(sampleItem, null);

		// verify none of these API calls dispatch an event
		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(0));
		MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
	}

	// ========================================================================================
	// getIdentities API
	// ========================================================================================
	@Test
	public void testGetIdentities() throws Exception {
		// setup
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		final ArgumentCaptor<AdobeCallback> adobeCallbackCaptor = ArgumentCaptor.forClass(AdobeCallback.class);
		final ArgumentCaptor<ExtensionErrorCallback> extensionErrorCallbackCaptor = ArgumentCaptor.forClass(
			ExtensionErrorCallback.class
		);
		final List<IdentityMap> callbackReturnValues = new ArrayList<>();

		// test
		Identity.getIdentities(
			new AdobeCallback<IdentityMap>() {
				@Override
				public void call(IdentityMap map) {
					callbackReturnValues.add(map);
				}
			}
		);

		// verify
		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEventWithResponseCallback(
			eventCaptor.capture(),
			adobeCallbackCaptor.capture(),
			extensionErrorCallbackCaptor.capture()
		);

		// verify the dispatched event details
		Event dispatchedEvent = eventCaptor.getValue();
		assertEquals(IdentityConstants.EventNames.REQUEST_IDENTITIES, dispatchedEvent.getName());
		assertEquals(IdentityConstants.EventType.EDGE_IDENTITY.toLowerCase(), dispatchedEvent.getType());
		assertEquals(IdentityConstants.EventSource.REQUEST_IDENTITY.toLowerCase(), dispatchedEvent.getSource());
		assertTrue(dispatchedEvent.getEventData().isEmpty());

		// verify callback responses
		final ECID ecid = new ECID();
		final String coreId = "core-test-id";
		final String jsonStr =
			"{\n" +
			"      \"identityMap\": {\n" +
			"        \"ECID\": [\n" +
			"          {\n" +
			"            \"id\":" +
			ecid.toString() +
			",\n" +
			"            \"authenticatedState\": \"ambiguous\",\n" +
			"            \"primary\": true\n" +
			"          }\n" +
			"        ],\n" +
			"        \"CORE\": [\n" +
			"          {\n" +
			"            \"id\":" +
			coreId +
			",\n" +
			"            \"authenticatedState\": \"authenticated\",\n" +
			"            \"primary\": false\n" +
			"          }\n" +
			"        ]\n" +
			"      }\n" +
			"}";

		final JSONObject jsonObject = new JSONObject(jsonStr);
		final Map<String, Object> xdmData = Utils.toMap(jsonObject);

		adobeCallbackCaptor.getValue().call(buildIdentityResponseEvent(xdmData));
		IdentityItem ecidItem = callbackReturnValues.get(0).getIdentityItemsForNamespace("ECID").get(0);
		IdentityItem coreItem = callbackReturnValues.get(0).getIdentityItemsForNamespace("CORE").get(0);

		assertEquals(ecid.toString(), ecidItem.getId());
		assertEquals(AuthenticatedState.AMBIGUOUS, ecidItem.getAuthenticatedState());
		assertEquals(true, ecidItem.isPrimary());

		assertEquals(coreId, coreItem.getId());
		assertEquals(AuthenticatedState.AUTHENTICATED, coreItem.getAuthenticatedState());
		assertEquals(false, coreItem.isPrimary());
		// TODO - enable when ExtensionError creation is available
		// should not crash on calling the callback
		//extensionErrorCallback.error(ExtensionError.UNEXPECTED_ERROR);
	}

	@Test
	public void testGetIdentities_nullCallback() {
		// test
		Identity.getIdentities(null);

		// verify
		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(0));
		MobileCore.dispatchEventWithResponseCallback(
			any(Event.class),
			any(AdobeCallback.class),
			any(ExtensionErrorCallback.class)
		);
	}

	@Test
	public void testGetIdentities_nullResponseEvent() {
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
			public void call(Object o) {}
		};

		// test
		Identity.getIdentities(callbackWithError);

		// verify if the event is dispatched
		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEventWithResponseCallback(
			any(Event.class),
			adobeCallbackCaptor.capture(),
			any(ExtensionErrorCallback.class)
		);

		// set response event to null
		adobeCallbackCaptor.getValue().call(null);

		// verify
		assertTrue((boolean) errorCapture.get(KEY_IS_ERRORCALLBACK_CALLED));
		assertEquals(AdobeError.UNEXPECTED_ERROR, errorCapture.get(KEY_CAPTUREDERRORCALLBACK));
	}

	@Test
	public void testGetIdentities_invalidEventData() {
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
			public void call(Object o) {}
		};

		// test
		Identity.getIdentities(callbackWithError);

		// verify if the event is dispatched
		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEventWithResponseCallback(
			any(Event.class),
			adobeCallbackCaptor.capture(),
			any(ExtensionErrorCallback.class)
		);

		// set response event to null
		Map<String, Object> eventData = new HashMap<>();
		eventData.put("someKey", "someValue");
		adobeCallbackCaptor.getValue().call(buildIdentityResponseEvent(eventData));

		// verify
		assertTrue((boolean) errorCapture.get(KEY_IS_ERRORCALLBACK_CALLED));
		assertEquals(AdobeError.UNEXPECTED_ERROR, errorCapture.get(KEY_CAPTUREDERRORCALLBACK));
	}

	@Test
	public void testGetIdentities_missingIdentityMap() {
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
			public void call(Object o) {}
		};

		// test
		Identity.getIdentities(callbackWithError);

		// verify if the event is dispatched
		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEventWithResponseCallback(
			any(Event.class),
			adobeCallbackCaptor.capture(),
			any(ExtensionErrorCallback.class)
		);

		// set response event to map missing IdentityMap
		Map<String, Object> emptyXDMData = new HashMap<>();
		adobeCallbackCaptor.getValue().call(buildIdentityResponseEvent(emptyXDMData));

		// verify
		assertTrue((boolean) errorCapture.get(KEY_IS_ERRORCALLBACK_CALLED));
		assertEquals(AdobeError.UNEXPECTED_ERROR, errorCapture.get(KEY_CAPTUREDERRORCALLBACK));
	}

	// ========================================================================================
	// Private method
	// ========================================================================================
	private Event buildIdentityResponseEvent(final Map<String, Object> eventData) {
		return new Event.Builder(
			IdentityConstants.EventNames.IDENTITY_REQUEST_IDENTITY_ECID,
			IdentityConstants.EventType.EDGE_IDENTITY,
			IdentityConstants.EventSource.RESPONSE_IDENTITY
		)
			.setEventData(eventData)
			.build();
	}

	private Event buildUrlVariablesResponseEvent(final Map<String, Object> eventData) {
		return new Event.Builder(
			IdentityConstants.EventNames.IDENTITY_REQUEST_URL_VARIABLES,
			IdentityConstants.EventType.EDGE_IDENTITY,
			IdentityConstants.EventSource.RESPONSE_IDENTITY
		)
			.setEventData(eventData)
			.build();
	}
}
