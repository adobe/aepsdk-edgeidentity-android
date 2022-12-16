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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionError;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.util.JSONUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class IdentityTests {

	@Before
	public void setup() {
		MockitoAnnotations.openMocks(this);
	}

	// ========================================================================================
	// extensionVersion
	// ========================================================================================

	@Test
	public void test_extensionVersionAPI() {
		// test
		String extensionVersion = Identity.extensionVersion();
		assertEquals(
			"The Extension version API should return the correct value",
			IdentityConstants.EXTENSION_VERSION,
			extensionVersion
		);
	}

	// ========================================================================================
	// registerExtension
	// ========================================================================================
	@Test
	public void testRegistration() {
		try (MockedStatic<MobileCore> mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class)) {
			// test
			Identity.registerExtension();

			// verify
			final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(
				ExtensionErrorCallback.class
			);
			mockedStaticMobileCore.verify(() ->
				MobileCore.registerExtension(eq(IdentityExtension.class), callbackCaptor.capture())
			);

			final ExtensionErrorCallback extensionErrorCallback = callbackCaptor.getValue();
			assertNotNull("The extension callback should not be null", extensionErrorCallback);

			// verify that the callback invocation does not throw an exception
			extensionErrorCallback.error(ExtensionError.UNEXPECTED_ERROR);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	// ========================================================================================
	// getExperienceCloudId API
	// ========================================================================================
	@Test
	public void testGetExperienceCloudId() {
		// setup
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		final ArgumentCaptor<AdobeCallbackWithError> adobeCallbackCaptor = ArgumentCaptor.forClass(
			AdobeCallbackWithError.class
		);

		final List<String> callbackReturnValues = new ArrayList<>();

		try (MockedStatic<MobileCore> mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class)) {
			// test
			Identity.getExperienceCloudId(callbackReturnValues::add);

			//verify
			mockedStaticMobileCore.verify(() ->
				MobileCore.dispatchEventWithResponseCallback(
					eventCaptor.capture(),
					eq(500L),
					adobeCallbackCaptor.capture()
				)
			);

			// verify the dispatched event details
			final Event dispatchedEvent = eventCaptor.getValue();
			assertEquals(IdentityConstants.EventNames.IDENTITY_REQUEST_IDENTITY_ECID, dispatchedEvent.getName());
			assertEquals(EventType.EDGE_IDENTITY, dispatchedEvent.getType());
			assertEquals(EventSource.REQUEST_IDENTITY, dispatchedEvent.getSource());
			assertNull(dispatchedEvent.getEventData());

			// verify callback responses
			final ECID ecid = new ECID();
			final Map<String, Object> ecidDict = new HashMap<>();
			ecidDict.put("id", ecid.toString());
			final ArrayList<Object> ecidArr = new ArrayList<>();
			ecidArr.add(ecidDict);
			final Map<String, Object> identityMap = new HashMap<>();
			identityMap.put("ECID", ecidArr);
			final Map<String, Object> xdmData = new HashMap<>();
			xdmData.put("identityMap", identityMap);

			assertNotNull(adobeCallbackCaptor.getValue());
			adobeCallbackCaptor.getValue().call(buildIdentityResponseEvent(xdmData));
			assertEquals(ecid.toString(), callbackReturnValues.get(0));
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testGetExperienceCloudId_invokeCallbackOnfail() {
		// setup
		final String KEY_IS_ERROR_CALLBACK_CALLED = "errorCallBackCalled";
		final String KEY_CAPTURED_ERROR_CALLBACK = "capturedErrorCallback";
		final Map<String, Object> errorCapture = new HashMap<>();
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		final ArgumentCaptor<AdobeCallbackWithError> adobeCallbackCaptor = ArgumentCaptor.forClass(
			AdobeCallbackWithError.class
		);
		final AdobeCallbackWithError<String> callbackWithError = new AdobeCallbackWithError<String>() {
			@Override
			public void fail(AdobeError adobeError) {
				errorCapture.put(KEY_IS_ERROR_CALLBACK_CALLED, true);
				errorCapture.put(KEY_CAPTURED_ERROR_CALLBACK, adobeError);
			}

			@Override
			public void call(String ecid) {}
		};

		try (MockedStatic<MobileCore> mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class)) {
			// test
			Identity.getExperienceCloudId(callbackWithError);

			//verify
			mockedStaticMobileCore.verify(() ->
				MobileCore.dispatchEventWithResponseCallback(
					eventCaptor.capture(),
					eq(500L),
					adobeCallbackCaptor.capture()
				)
			);
		} catch (Exception e) {
			fail(e.getMessage());
		}

		// set response event to null
		adobeCallbackCaptor.getValue().fail(AdobeError.UNEXPECTED_ERROR);

		// verify
		assertTrue(((boolean) errorCapture.get(KEY_IS_ERROR_CALLBACK_CALLED)));
		assertEquals(AdobeError.UNEXPECTED_ERROR, errorCapture.get(KEY_CAPTURED_ERROR_CALLBACK));
	}

	@Test
	public void testGetExperienceCloudId_nullCallback() {
		try (MockedStatic<MobileCore> mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class)) {
			// test
			Identity.getExperienceCloudId(null);

			//verify
			mockedStaticMobileCore.verify(
				() ->
					MobileCore.dispatchEventWithResponseCallback(
						any(Event.class),
						anyLong(),
						any(AdobeCallbackWithError.class)
					),
				times(0)
			);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testGetExperienceCloudId_nullResponseEvent() {
		// setup
		final String KEY_IS_ERROR_CALLBACK_CALLED = "errorCallBackCalled";
		final String KEY_CAPTURED_ERROR_CALLBACK = "capturedErrorCallback";
		final Map<String, Object> errorCapture = new HashMap<>();
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		final ArgumentCaptor<AdobeCallbackWithError> adobeCallbackCaptor = ArgumentCaptor.forClass(
			AdobeCallbackWithError.class
		);
		final AdobeCallbackWithError<String> callbackWithError = new AdobeCallbackWithError<String>() {
			@Override
			public void fail(AdobeError adobeError) {
				errorCapture.put(KEY_IS_ERROR_CALLBACK_CALLED, true);
				errorCapture.put(KEY_CAPTURED_ERROR_CALLBACK, adobeError);
			}

			@Override
			public void call(String ecid) {}
		};

		try (MockedStatic<MobileCore> mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class)) {
			// test
			Identity.getExperienceCloudId(callbackWithError);

			//verify
			mockedStaticMobileCore.verify(() ->
				MobileCore.dispatchEventWithResponseCallback(
					eventCaptor.capture(),
					eq(500L),
					adobeCallbackCaptor.capture()
				)
			);
		} catch (Exception e) {
			fail(e.getMessage());
		}

		// set response event to null
		adobeCallbackCaptor.getValue().call(null);

		// verify
		assertTrue(((boolean) errorCapture.get(KEY_IS_ERROR_CALLBACK_CALLED)));
		assertEquals(AdobeError.UNEXPECTED_ERROR, errorCapture.get(KEY_CAPTURED_ERROR_CALLBACK));
	}

	@Test
	public void testGetExperienceCloudId_invalidEventData() {
		// setup
		final String KEY_IS_ERROR_CALLBACK_CALLED = "errorCallBackCalled";
		final String KEY_CAPTURED_ERROR_CALLBACK = "capturedErrorCallback";
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		final Map<String, Object> errorCapture = new HashMap<>();
		final ArgumentCaptor<AdobeCallbackWithError> adobeCallbackCaptor = ArgumentCaptor.forClass(
			AdobeCallbackWithError.class
		);
		final AdobeCallbackWithError<String> callbackWithError = new AdobeCallbackWithError<String>() {
			@Override
			public void fail(AdobeError adobeError) {
				errorCapture.put(KEY_IS_ERROR_CALLBACK_CALLED, true);
				errorCapture.put(KEY_CAPTURED_ERROR_CALLBACK, adobeError);
			}

			@Override
			public void call(String ecid) {}
		};

		try (MockedStatic<MobileCore> mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class)) {
			Identity.getExperienceCloudId(callbackWithError);

			mockedStaticMobileCore.verify(() ->
				MobileCore.dispatchEventWithResponseCallback(
					eventCaptor.capture(),
					eq(500L),
					adobeCallbackCaptor.capture()
				)
			);
		} catch (Exception e) {
			fail(e.getMessage());
		}

		// set response event to null
		Map<String, Object> eventData = new HashMap<>();
		eventData.put("someKey", "someValue");
		adobeCallbackCaptor.getValue().call(buildIdentityResponseEvent(eventData));

		// verify
		assertTrue((boolean) errorCapture.get(KEY_IS_ERROR_CALLBACK_CALLED));
		assertEquals(AdobeError.UNEXPECTED_ERROR, errorCapture.get(KEY_CAPTURED_ERROR_CALLBACK));
	}

	@Test
	public void testGetExperienceCloudId_missingECID() {
		// setup
		final String KEY_IS_ERRORCALLBACK_CALLED = "errorCallBackCalled";
		final String KEY_CAPTUREDERRORCALLBACK = "capturedErrorCallback";
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		final Map<String, Object> errorCapture = new HashMap<>();
		final ArgumentCaptor<AdobeCallbackWithError> adobeCallbackCaptor = ArgumentCaptor.forClass(
			AdobeCallbackWithError.class
		);
		final AdobeCallbackWithError callbackWithError = new AdobeCallbackWithError() {
			@Override
			public void fail(AdobeError adobeError) {
				errorCapture.put(KEY_IS_ERRORCALLBACK_CALLED, true);
				errorCapture.put(KEY_CAPTUREDERRORCALLBACK, adobeError);
			}

			@Override
			public void call(Object o) {}
		};

		try (MockedStatic<MobileCore> mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class)) {
			// test
			Identity.getExperienceCloudId(callbackWithError);

			//verify
			mockedStaticMobileCore.verify(() ->
				MobileCore.dispatchEventWithResponseCallback(
					eventCaptor.capture(),
					eq(500L),
					adobeCallbackCaptor.capture()
				)
			);
		} catch (Exception e) {
			fail(e.getMessage());
		}

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
	public void testGetUrlVariables() {
		// setup
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		final ArgumentCaptor<AdobeCallbackWithError> adobeCallbackCaptor = ArgumentCaptor.forClass(
			AdobeCallbackWithError.class
		);
		final List<String> callbackReturnValues = new ArrayList<>();

		try (MockedStatic<MobileCore> mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class)) {
			// test
			Identity.getUrlVariables(
				new AdobeCallback<String>() {
					@Override
					public void call(String s) {
						callbackReturnValues.add(s);
					}
				}
			);

			mockedStaticMobileCore.verify(() ->
				MobileCore.dispatchEventWithResponseCallback(
					eventCaptor.capture(),
					eq(500L),
					adobeCallbackCaptor.capture()
				)
			);
		} catch (Exception e) {
			fail(e.getMessage());
		}

		// verify the dispatched event details
		final Event dispatchedEvent = eventCaptor.getValue();
		assertEquals(IdentityConstants.EventNames.IDENTITY_REQUEST_URL_VARIABLES, dispatchedEvent.getName());
		assertEquals(EventType.EDGE_IDENTITY, dispatchedEvent.getType());
		assertEquals(EventSource.REQUEST_IDENTITY, dispatchedEvent.getSource());
		assertTrue(dispatchedEvent.getEventData().containsKey("urlvariables"));
		assertTrue((boolean) dispatchedEvent.getEventData().get("urlvariables"));

		// verify callback responses
		final Map<String, Object> urlVariablesResponse = new HashMap<>();
		urlVariablesResponse.put("urlvariables", "test-url-variable-string");

		adobeCallbackCaptor.getValue().call(buildUrlVariablesResponseEvent(urlVariablesResponse));
		assertEquals("test-url-variable-string", callbackReturnValues.get(0));
	}

	@Test
	public void testGetUrlVariables_nullCallback() {
		try (MockedStatic<MobileCore> mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class)) {
			// test
			Identity.getUrlVariables(null);

			mockedStaticMobileCore.verify(
				() ->
					MobileCore.dispatchEventWithResponseCallback(
						any(Event.class),
						anyLong(),
						any(AdobeCallbackWithError.class)
					),
				never()
			);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testGetUrlVariables_nullResponseEvent() {
		// setup
		final String KEY_IS_ERROR_CALLBACK_CALLED = "errorCallBackCalled";
		final String KEY_CAPTURED_ERROR_CALLBACK = "capturedErrorCallback";
		final Map<String, Object> errorCapture = new HashMap<>();
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		final ArgumentCaptor<AdobeCallbackWithError> adobeCallbackCaptor = ArgumentCaptor.forClass(
			AdobeCallbackWithError.class
		);
		final AdobeCallbackWithError<String> callbackWithError = new AdobeCallbackWithError<String>() {
			@Override
			public void fail(AdobeError adobeError) {
				errorCapture.put(KEY_IS_ERROR_CALLBACK_CALLED, true);
				errorCapture.put(KEY_CAPTURED_ERROR_CALLBACK, adobeError);
			}

			@Override
			public void call(String o) {}
		};

		try (MockedStatic<MobileCore> mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class)) {
			// test
			Identity.getUrlVariables(callbackWithError);

			mockedStaticMobileCore.verify(() ->
				MobileCore.dispatchEventWithResponseCallback(
					eventCaptor.capture(),
					eq(500L),
					adobeCallbackCaptor.capture()
				)
			);
		} catch (Exception e) {
			fail(e.getMessage());
		}

		final Event dispatchedEvent = eventCaptor.getValue();
		assertEquals(IdentityConstants.EventNames.IDENTITY_REQUEST_URL_VARIABLES, dispatchedEvent.getName());
		assertEquals(EventType.EDGE_IDENTITY, dispatchedEvent.getType());
		assertEquals(EventSource.REQUEST_IDENTITY, dispatchedEvent.getSource());
		assertTrue(dispatchedEvent.getEventData().containsKey("urlvariables"));
		assertTrue((boolean) dispatchedEvent.getEventData().get("urlvariables"));

		// set response event to null
		adobeCallbackCaptor.getValue().call(null);

		// verify
		assertEquals(true, errorCapture.get(KEY_IS_ERROR_CALLBACK_CALLED));
		assertEquals(AdobeError.UNEXPECTED_ERROR, errorCapture.get(KEY_CAPTURED_ERROR_CALLBACK));
	}

	@Test
	public void testGetUrlVariables_invalidEventData() {
		// setup
		final String KEY_IS_ERROR_CALLBACK_CALLED = "errorCallBackCalled";
		final String KEY_CAPTURED_ERROR_CALLBACK = "capturedErrorCallback";
		final Map<String, Object> errorCapture = new HashMap<>();
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		final ArgumentCaptor<AdobeCallbackWithError> adobeCallbackCaptor = ArgumentCaptor.forClass(
			AdobeCallbackWithError.class
		);
		final AdobeCallbackWithError<String> callbackWithError = new AdobeCallbackWithError<String>() {
			@Override
			public void fail(AdobeError adobeError) {
				errorCapture.put(KEY_IS_ERROR_CALLBACK_CALLED, true);
				errorCapture.put(KEY_CAPTURED_ERROR_CALLBACK, adobeError);
			}

			@Override
			public void call(String o) {}
		};

		try (MockedStatic<MobileCore> mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class)) {
			// test
			Identity.getUrlVariables(callbackWithError);

			mockedStaticMobileCore.verify(() ->
				MobileCore.dispatchEventWithResponseCallback(
					eventCaptor.capture(),
					eq(500L),
					adobeCallbackCaptor.capture()
				)
			);
		} catch (Exception e) {
			fail(e.getMessage());
		}

		// set response event data to not have urlvariables key
		final Map<String, Object> eventData = new HashMap<>();
		eventData.put("someKey", "someValue");
		eventData.put("urlvariables", true);
		adobeCallbackCaptor.getValue().call(buildUrlVariablesResponseEvent(eventData));

		// verify
		assertEquals(true, errorCapture.get(KEY_IS_ERROR_CALLBACK_CALLED));
		assertEquals(AdobeError.UNEXPECTED_ERROR, errorCapture.get(KEY_CAPTURED_ERROR_CALLBACK));
	}

	@Test
	public void testGetUrlVariables_NullUrlVariablesStringInResponseData() {
		// setup
		final String KEY_IS_ERROR_CALLBACK_CALLED = "errorCallBackCalled";
		final String KEY_CAPTURED_ERROR_CALLBACK = "capturedErrorCallback";
		final Map<String, Object> errorCapture = new HashMap<>();
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		final ArgumentCaptor<AdobeCallbackWithError> adobeCallbackCaptor = ArgumentCaptor.forClass(
			AdobeCallbackWithError.class
		);
		final AdobeCallbackWithError<String> callbackWithError = new AdobeCallbackWithError<String>() {
			@Override
			public void fail(AdobeError adobeError) {
				errorCapture.put(KEY_IS_ERROR_CALLBACK_CALLED, true);
				errorCapture.put(KEY_CAPTURED_ERROR_CALLBACK, adobeError);
			}

			@Override
			public void call(String o) {}
		};

		// test
		try (MockedStatic<MobileCore> mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class)) {
			Identity.getUrlVariables(callbackWithError);

			mockedStaticMobileCore.verify(() ->
				MobileCore.dispatchEventWithResponseCallback(
					eventCaptor.capture(),
					eq(500L),
					adobeCallbackCaptor.capture()
				)
			);
		} catch (Exception e) {
			fail(e.getMessage());
		}

		// set response event to have urlvariables map to null value
		Map<String, Object> nullUrlVariablesData = new HashMap<>();
		nullUrlVariablesData.put("urlvariables", null);
		adobeCallbackCaptor.getValue().call(buildUrlVariablesResponseEvent(nullUrlVariablesData));

		// verify
		assertEquals(true, errorCapture.get(KEY_IS_ERROR_CALLBACK_CALLED));
		assertEquals(AdobeError.UNEXPECTED_ERROR, errorCapture.get(KEY_CAPTURED_ERROR_CALLBACK));
	}

	@Test
	public void testGetUrlVariables_callbackOnFail() {
		// setup
		final String KEY_IS_ERROR_CALLBACK_CALLED = "errorCallBackCalled";
		final String KEY_CAPTURED_ERROR_CALLBACK = "capturedErrorCallback";
		final Map<String, Object> errorCapture = new HashMap<>();
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		final ArgumentCaptor<AdobeCallbackWithError> adobeCallbackCaptor = ArgumentCaptor.forClass(
			AdobeCallbackWithError.class
		);
		final AdobeCallbackWithError<String> callbackWithError = new AdobeCallbackWithError<String>() {
			@Override
			public void fail(AdobeError adobeError) {
				errorCapture.put(KEY_IS_ERROR_CALLBACK_CALLED, true);
				errorCapture.put(KEY_CAPTURED_ERROR_CALLBACK, adobeError);
			}

			@Override
			public void call(String o) {}
		};

		// test
		try (MockedStatic<MobileCore> mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class)) {
			Identity.getUrlVariables(callbackWithError);
			mockedStaticMobileCore.verify(() ->
				MobileCore.dispatchEventWithResponseCallback(
					eventCaptor.capture(),
					eq(500L),
					adobeCallbackCaptor.capture()
				)
			);
		} catch (Exception e) {
			fail(e.getMessage());
		}

		adobeCallbackCaptor.getValue().fail(AdobeError.UNEXPECTED_ERROR);

		// verify
		assertEquals(true, errorCapture.get(KEY_IS_ERROR_CALLBACK_CALLED));
		assertEquals(AdobeError.UNEXPECTED_ERROR, errorCapture.get(KEY_CAPTURED_ERROR_CALLBACK));
	}

	// ========================================================================================
	// updateIdentities API
	// ========================================================================================
	@Test
	public void testUpdateIdentities() {
		// setup
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		final IdentityMap map = new IdentityMap();
		map.addItem(new IdentityItem("id", AuthenticatedState.AUTHENTICATED, true), "mainspace");
		map.addItem(new IdentityItem("idtwo", AuthenticatedState.LOGGED_OUT, false), "secondspace");

		try (MockedStatic<MobileCore> mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class)) {
			// test
			Identity.updateIdentities(map);

			mockedStaticMobileCore.verify(() -> MobileCore.dispatchEvent(eventCaptor.capture()));
		} catch (Exception e) {
			fail(e.getMessage());
		}

		// verify the dispatched event details
		Event dispatchedEvent = eventCaptor.getValue();
		assertEquals(IdentityConstants.EventNames.UPDATE_IDENTITIES, dispatchedEvent.getName());
		assertEquals(EventType.EDGE_IDENTITY, dispatchedEvent.getType());
		assertEquals(EventSource.UPDATE_IDENTITY, dispatchedEvent.getSource());
		assertEquals(map.asXDMMap(), dispatchedEvent.getEventData());
	}

	@Test
	public void testUpdateIdentitiesNullMap() {
		// test
		try (MockedStatic<MobileCore> mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class)) {
			// test
			Identity.updateIdentities(null);

			// verify that no event is dispatched
			mockedStaticMobileCore.verify(() -> MobileCore.dispatchEvent(any(Event.class)), never());
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testUpdateIdentities_EmptyMap() {
		// test
		try (MockedStatic<MobileCore> mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class)) {
			// test
			Identity.updateIdentities(new IdentityMap());

			// verify that no event is dispatched
			mockedStaticMobileCore.verify(() -> MobileCore.dispatchEvent(any(Event.class)), never());
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	// ========================================================================================
	// removeIdentity API
	// ========================================================================================

	@Test
	public void testRemoveIdentity() {
		// setup
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		final IdentityItem sampleItem = new IdentityItem("sample", AuthenticatedState.AMBIGUOUS, false);

		try (MockedStatic<MobileCore> mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class)) {
			// test
			Identity.removeIdentity(sampleItem, "namespace");

			mockedStaticMobileCore.verify(() -> MobileCore.dispatchEvent(eventCaptor.capture()));
		} catch (Exception e) {
			fail(e.getMessage());
		}

		final Event dispatchedEvent = eventCaptor.getValue();
		assertEquals(IdentityConstants.EventNames.REMOVE_IDENTITIES, dispatchedEvent.getName());
		assertEquals(EventType.EDGE_IDENTITY, dispatchedEvent.getType());
		assertEquals(EventSource.REMOVE_IDENTITY, dispatchedEvent.getSource());

		final IdentityMap expectedIdentityMap = new IdentityMap();
		expectedIdentityMap.addItem(sampleItem, "namespace");
		assertEquals(expectedIdentityMap.asXDMMap(), dispatchedEvent.getEventData());
	}

	@Test
	public void testRemoveIdentity_WithInvalidInputs() {
		// setup
		IdentityItem sampleItem = new IdentityItem("sample", AuthenticatedState.AMBIGUOUS, false);

		try (MockedStatic<MobileCore> mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class)) {
			// test
			Identity.removeIdentity(null, "namespace");
			Identity.removeIdentity(sampleItem, "");
			Identity.removeIdentity(sampleItem, null);

			// verify that no event is dispatched
			mockedStaticMobileCore.verify(() -> MobileCore.dispatchEvent(any(Event.class)), never());
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	// ========================================================================================
	// getIdentities API
	// ========================================================================================
	@Test
	public void testGetIdentities() throws Exception {
		// setup
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		final ArgumentCaptor<AdobeCallbackWithError> adobeCallbackCaptor = ArgumentCaptor.forClass(
			AdobeCallbackWithError.class
		);
		final List<IdentityMap> callbackReturnValues = new ArrayList<>();

		try (MockedStatic<MobileCore> mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class)) {
			Identity.getIdentities(
				new AdobeCallback<IdentityMap>() {
					@Override
					public void call(IdentityMap map) {
						callbackReturnValues.add(map);
					}
				}
			);

			// verify
			mockedStaticMobileCore.verify(() ->
				MobileCore.dispatchEventWithResponseCallback(
					eventCaptor.capture(),
					eq(500L),
					adobeCallbackCaptor.capture()
				)
			);
		} catch (Exception e) {
			fail(e.getMessage());
		}

		// verify the dispatched event details
		final Event dispatchedEvent = eventCaptor.getValue();
		assertEquals(IdentityConstants.EventNames.REQUEST_IDENTITIES, dispatchedEvent.getName());
		assertEquals(EventType.EDGE_IDENTITY, dispatchedEvent.getType());
		assertEquals(EventSource.REQUEST_IDENTITY, dispatchedEvent.getSource());
		assertNull(dispatchedEvent.getEventData());

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
		final Map<String, Object> xdmData = JSONUtils.toMap(jsonObject);

		adobeCallbackCaptor.getValue().call(buildIdentityResponseEvent(xdmData));
		final IdentityItem ecidItem = callbackReturnValues.get(0).getIdentityItemsForNamespace("ECID").get(0);
		final IdentityItem coreItem = callbackReturnValues.get(0).getIdentityItemsForNamespace("CORE").get(0);

		assertEquals(ecid.toString(), ecidItem.getId());
		assertEquals(AuthenticatedState.AMBIGUOUS, ecidItem.getAuthenticatedState());
		assertEquals(true, ecidItem.isPrimary());

		assertEquals(coreId, coreItem.getId());
		assertEquals(AuthenticatedState.AUTHENTICATED, coreItem.getAuthenticatedState());
		assertEquals(false, coreItem.isPrimary());
	}

	@Test
	public void testGetIdentities_nullCallback() {
		// setup
		try (MockedStatic<MobileCore> mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class)) {
			Identity.getIdentities(null);

			// verify
			mockedStaticMobileCore.verify(
				() ->
					MobileCore.dispatchEventWithResponseCallback(
						any(Event.class),
						anyLong(),
						any(AdobeCallbackWithError.class)
					),
				never()
			);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testGetIdentities_nullResponseEvent() {
		// setup
		final String KEY_IS_ERROR_CALLBACK_CALLED = "errorCallBackCalled";
		final String KEY_CAPTURED_ERROR_CALLBACK = "capturedErrorCallback";
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		final Map<String, Object> errorCapture = new HashMap<>();
		final ArgumentCaptor<AdobeCallbackWithError> adobeCallbackCaptor = ArgumentCaptor.forClass(
			AdobeCallbackWithError.class
		);
		final AdobeCallbackWithError callbackWithError = new AdobeCallbackWithError() {
			@Override
			public void fail(AdobeError adobeError) {
				errorCapture.put(KEY_IS_ERROR_CALLBACK_CALLED, true);
				errorCapture.put(KEY_CAPTURED_ERROR_CALLBACK, adobeError);
			}

			@Override
			public void call(Object o) {}
		};

		try (MockedStatic<MobileCore> mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class)) {
			Identity.getIdentities(callbackWithError);

			// verify
			mockedStaticMobileCore.verify(() ->
				MobileCore.dispatchEventWithResponseCallback(
					eventCaptor.capture(),
					eq(500L),
					adobeCallbackCaptor.capture()
				)
			);
		} catch (Exception e) {
			fail(e.getMessage());
		}

		// set response event to null
		adobeCallbackCaptor.getValue().call(null);

		// verify
		assertTrue((boolean) errorCapture.get(KEY_IS_ERROR_CALLBACK_CALLED));
		assertEquals(AdobeError.UNEXPECTED_ERROR, errorCapture.get(KEY_CAPTURED_ERROR_CALLBACK));
	}

	@Test
	public void testGetIdentities_invalidEventData() {
		// setup
		final String KEY_IS_ERROR_CALLBACK_CALLED = "errorCallBackCalled";
		final String KEY_CAPTURED_ERROR_CALLBACK = "capturedErrorCallback";
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		final Map<String, Object> errorCapture = new HashMap<>();
		final ArgumentCaptor<AdobeCallbackWithError> adobeCallbackCaptor = ArgumentCaptor.forClass(
			AdobeCallbackWithError.class
		);
		final AdobeCallbackWithError callbackWithError = new AdobeCallbackWithError() {
			@Override
			public void fail(AdobeError adobeError) {
				errorCapture.put(KEY_IS_ERROR_CALLBACK_CALLED, true);
				errorCapture.put(KEY_CAPTURED_ERROR_CALLBACK, adobeError);
			}

			@Override
			public void call(Object o) {}
		};

		try (MockedStatic<MobileCore> mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class)) {
			Identity.getIdentities(callbackWithError);

			// verify
			mockedStaticMobileCore.verify(() ->
				MobileCore.dispatchEventWithResponseCallback(
					eventCaptor.capture(),
					eq(500L),
					adobeCallbackCaptor.capture()
				)
			);
		} catch (Exception e) {
			fail(e.getMessage());
		}

		// set response event
		Map<String, Object> eventData = new HashMap<>();
		eventData.put("someKey", "someValue");
		adobeCallbackCaptor.getValue().call(buildIdentityResponseEvent(eventData));

		// verify
		assertTrue((boolean) errorCapture.get(KEY_IS_ERROR_CALLBACK_CALLED));
		assertEquals(AdobeError.UNEXPECTED_ERROR, errorCapture.get(KEY_CAPTURED_ERROR_CALLBACK));
	}

	@Test
	public void testGetIdentities_missingIdentityMap() {
		// setup
		final String KEY_IS_ERROR_CALLBACK_CALLED = "errorCallBackCalled";
		final String KEY_CAPTURED_ERROR_CALLBACK = "capturedErrorCallback";
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		final Map<String, Object> errorCapture = new HashMap<>();
		final ArgumentCaptor<AdobeCallbackWithError> adobeCallbackCaptor = ArgumentCaptor.forClass(
			AdobeCallbackWithError.class
		);
		final AdobeCallbackWithError callbackWithError = new AdobeCallbackWithError() {
			@Override
			public void fail(AdobeError adobeError) {
				errorCapture.put(KEY_IS_ERROR_CALLBACK_CALLED, true);
				errorCapture.put(KEY_CAPTURED_ERROR_CALLBACK, adobeError);
			}

			@Override
			public void call(Object o) {}
		};

		try (MockedStatic<MobileCore> mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class)) {
			Identity.getIdentities(callbackWithError);

			// verify
			mockedStaticMobileCore.verify(() ->
				MobileCore.dispatchEventWithResponseCallback(
					eventCaptor.capture(),
					eq(500L),
					adobeCallbackCaptor.capture()
				)
			);
		} catch (Exception e) {
			fail(e.getMessage());
		}

		// set response event with empty data
		Map<String, Object> eventData = new HashMap<>();
		adobeCallbackCaptor.getValue().call(buildIdentityResponseEvent(eventData));

		// verify
		assertTrue((boolean) errorCapture.get(KEY_IS_ERROR_CALLBACK_CALLED));
		assertEquals(AdobeError.UNEXPECTED_ERROR, errorCapture.get(KEY_CAPTURED_ERROR_CALLBACK));
	}

	@Test
	public void testGetIdentities_callbackOnFail() {
		// setup
		final String KEY_IS_ERROR_CALLBACK_CALLED = "errorCallBackCalled";
		final String KEY_CAPTURED_ERROR_CALLBACK = "capturedErrorCallback";
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		final Map<String, Object> errorCapture = new HashMap<>();
		final ArgumentCaptor<AdobeCallbackWithError> adobeCallbackCaptor = ArgumentCaptor.forClass(
			AdobeCallbackWithError.class
		);
		final AdobeCallbackWithError callbackWithError = new AdobeCallbackWithError() {
			@Override
			public void fail(AdobeError adobeError) {
				errorCapture.put(KEY_IS_ERROR_CALLBACK_CALLED, true);
				errorCapture.put(KEY_CAPTURED_ERROR_CALLBACK, adobeError);
			}

			@Override
			public void call(Object o) {}
		};

		try (MockedStatic<MobileCore> mockedStaticMobileCore = Mockito.mockStatic(MobileCore.class)) {
			Identity.getIdentities(callbackWithError);

			// verify
			mockedStaticMobileCore.verify(() ->
				MobileCore.dispatchEventWithResponseCallback(
					eventCaptor.capture(),
					eq(500L),
					adobeCallbackCaptor.capture()
				)
			);
		} catch (Exception e) {
			fail(e.getMessage());
		}

		// set response event with empty data
		adobeCallbackCaptor.getValue().fail(AdobeError.UNEXPECTED_ERROR);

		// verify
		assertTrue((boolean) errorCapture.get(KEY_IS_ERROR_CALLBACK_CALLED));
		assertEquals(AdobeError.UNEXPECTED_ERROR, errorCapture.get(KEY_CAPTURED_ERROR_CALLBACK));
	}

	// ========================================================================================
	// Private methods
	// ========================================================================================
	private Event buildIdentityResponseEvent(final Map<String, Object> eventData) {
		return new Event.Builder(
			IdentityConstants.EventNames.IDENTITY_REQUEST_IDENTITY_ECID,
			EventType.EDGE_IDENTITY,
			EventSource.RESPONSE_IDENTITY
		)
			.setEventData(eventData)
			.build();
	}

	private Event buildUrlVariablesResponseEvent(final Map<String, Object> eventData) {
		return new Event.Builder(
			IdentityConstants.EventNames.IDENTITY_REQUEST_URL_VARIABLES,
			EventType.EDGE_IDENTITY,
			EventSource.RESPONSE_IDENTITY
		)
			.setEventData(eventData)
			.build();
	}
}
