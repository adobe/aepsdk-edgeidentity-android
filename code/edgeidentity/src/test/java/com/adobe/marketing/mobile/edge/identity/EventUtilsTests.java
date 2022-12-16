/*
  Copyright 2022 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.edge.identity;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class EventUtilsTests {

	// ======================================================================================================================
	// Tests for method : isGetUrlVariablesRequestEvent(final Event event)
	// ======================================================================================================================

	@Test
	public void test_isRequestIdentityEventForGetUrlVariable_nullEvent_returnsFalse() {
		assertFalse(EventUtils.isGetUrlVariablesRequestEvent(null));
	}

	@Test
	public void test_isRequestIdentityEventForGetUrlVariable_eventContainsUrlVariablesKey_validBooleanValue_returnsSetBooleanValue() {
		Event event = new Event.Builder(
			IdentityConstants.EventNames.IDENTITY_REQUEST_URL_VARIABLES,
			EventType.EDGE_IDENTITY,
			EventSource.REQUEST_IDENTITY
		)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(IdentityConstants.EventDataKeys.URL_VARIABLES, true);
					}
				}
			)
			.build();

		assertTrue(EventUtils.isGetUrlVariablesRequestEvent(event));

		// eventType is not edgeIdentity and eventSource is not requestIdentity
		event =
			new Event.Builder(
				IdentityConstants.EventNames.IDENTITY_REQUEST_URL_VARIABLES,
				EventType.EDGE_IDENTITY,
				EventSource.REQUEST_IDENTITY
			)
				.setEventData(
					new HashMap<String, Object>() {
						{
							put(IdentityConstants.EventDataKeys.URL_VARIABLES, false);
						}
					}
				)
				.build();

		assertFalse(EventUtils.isGetUrlVariablesRequestEvent(event));
	}

	@Test
	public void test_isRequestIdentityEventForGetUrlVariable_eventTypeNotEdgeIdentity_eventSourceNotIdentityRequest_eventContainsUrlVariablesKey_validBooleanValue_returnsSetBooleanValue() {
		// eventType is not edgeIdentity and eventSource is not requestIdentity
		Event event = new Event.Builder(
			IdentityConstants.EventNames.IDENTITY_REQUEST_URL_VARIABLES,
			EventType.IDENTITY,
			EventSource.REQUEST_RESET
		)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(IdentityConstants.EventDataKeys.URL_VARIABLES, true);
					}
				}
			)
			.build();

		assertTrue(EventUtils.isGetUrlVariablesRequestEvent(event));

		// eventType is not edgeIdentity and eventSource is not requestIdentity
		event =
			new Event.Builder(
				IdentityConstants.EventNames.IDENTITY_REQUEST_URL_VARIABLES,
				EventType.IDENTITY,
				EventSource.REQUEST_IDENTITY
			)
				.setEventData(
					new HashMap<String, Object>() {
						{
							put(IdentityConstants.EventDataKeys.URL_VARIABLES, true);
						}
					}
				)
				.build();

		assertTrue(EventUtils.isGetUrlVariablesRequestEvent(event));
	}

	@Test
	public void test_RequestIdentityEventForGetUrlVariable_eventContainsUrlVariablesKey_invalidValue_returnsFalse() {
		Event event = new Event.Builder(
			IdentityConstants.EventNames.IDENTITY_REQUEST_URL_VARIABLES,
			EventType.EDGE_IDENTITY,
			EventSource.REQUEST_IDENTITY
		)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(IdentityConstants.EventDataKeys.URL_VARIABLES, "true");
					}
				}
			)
			.build();

		assertFalse(EventUtils.isGetUrlVariablesRequestEvent(event));

		// eventType is not edgeIdentity and eventSource is not requestIdentity
		event =
			new Event.Builder(
				IdentityConstants.EventNames.IDENTITY_REQUEST_URL_VARIABLES,
				EventType.EDGE_IDENTITY,
				EventSource.REQUEST_IDENTITY
			)
				.setEventData(
					new HashMap<String, Object>() {
						{
							put(IdentityConstants.EventDataKeys.URL_VARIABLES, 123);
						}
					}
				)
				.build();

		assertFalse(EventUtils.isGetUrlVariablesRequestEvent(event));
	}

	// ======================================================================================================================
	// Tests for method : getOrgId(final Map<String, Object> configurationSharedState)
	// ======================================================================================================================

	@Test
	public void test_getOrgID_validString_returnsString() {
		assertEquals(
			"org-id",
			EventUtils.getOrgId(
				new HashMap<String, Object>() {
					{
						put("experienceCloud.org", "org-id");
					}
				}
			)
		);
	}

	@Test
	public void test_getOrgID_invalidValue_returnsNull() {
		assertNull(
			EventUtils.getOrgId(
				new HashMap<String, Object>() {
					{
						put("experienceCloud.org", true);
					}
				}
			)
		);
	}

	// ======================================================================================================================
	// Tests for method : isAdIdEvent(final Event event)
	// ======================================================================================================================

	@Test
	public void test_isAdIdEvent_whenIsAdIdEvent_thenTrue() {
		final Event event = createGenericIdentityEvent(
			new HashMap<String, Object>() {
				{
					put(IdentityConstants.EventDataKeys.ADVERTISING_IDENTIFIER, "adId");
				}
			}
		);

		assertTrue(EventUtils.isAdIdEvent(event));
	}

	@Test
	public void test_isAdIdEvent_whenIsNotAdIdEvent_thenFalse() {
		final Event event = createGenericIdentityEvent(
			new HashMap<String, Object>() {
				{
					put("someKey", "someValue");
				}
			}
		);

		assertFalse(EventUtils.isAdIdEvent(event));
	}

	// ======================================================================================================================
	// Tests for method : getAdId(final Event event)
	// ======================================================================================================================

	@Test
	public void test_getAdId_whenIsNotAdIdEvent_thenEmpty() {
		final Event event = createGenericIdentityEvent(
			new HashMap<String, Object>() {
				{
					put("someKey", "someValue");
				}
			}
		);

		assertEquals("", EventUtils.getAdId(event));
	}

	@Test
	public void test_getAdId_whenAllZeros_thenEmpty() {
		final Event event = createGenericIdentityEvent(
			new HashMap<String, Object>() {
				{
					put(
						IdentityConstants.EventDataKeys.ADVERTISING_IDENTIFIER,
						IdentityConstants.Default.ZERO_ADVERTISING_ID
					);
				}
			}
		);

		assertEquals("", EventUtils.getAdId(event));
	}

	@Test
	public void test_getAdId_whenNull_thenEmpty() {
		final Event event = createGenericIdentityEvent(
			new HashMap<String, Object>() {
				{
					put(IdentityConstants.EventDataKeys.ADVERTISING_IDENTIFIER, null);
				}
			}
		);

		assertEquals("", EventUtils.getAdId(event));
	}

	@Test
	public void test_getAdId_whenEmpty_thenEmpty() {
		final Event event = createGenericIdentityEvent(
			new HashMap<String, Object>() {
				{
					put(IdentityConstants.EventDataKeys.ADVERTISING_IDENTIFIER, "");
				}
			}
		);

		assertEquals("", EventUtils.getAdId(event));
	}

	@Test
	public void test_getAdId_whenUnexpectedType_thenEmpty() {
		final Event event = createGenericIdentityEvent(
			new HashMap<String, Object>() {
				{
					put(IdentityConstants.EventDataKeys.ADVERTISING_IDENTIFIER, 123);
				}
			}
		);

		assertEquals("", EventUtils.getAdId(event));
	}

	@Test
	public void test_getAdId_whenValid_thenValid() {
		final Event event = createGenericIdentityEvent(
			new HashMap<String, Object>() {
				{
					put(IdentityConstants.EventDataKeys.ADVERTISING_IDENTIFIER, "adId");
				}
			}
		);

		assertEquals("adId", EventUtils.getAdId(event));
	}

	// ======================================================================================================================
	// Tests for method : isSharedStateUpdateFor(final String stateOwnerName, final Event event)
	// ======================================================================================================================

	@Test
	public void test_isSharedStateUpdateFor_stateOwnerIsNull() {
		final Event event = new Event.Builder(
			"Shared state event",
			EventType.GENERIC_IDENTITY,
			EventSource.REQUEST_IDENTITY
		)
			.setEventData(Collections.EMPTY_MAP)
			.build();

		assertFalse(EventUtils.isSharedStateUpdateFor(null, event));
	}

	@Test
	public void test_isSharedStateUpdateFor_eventIsNull() {
		assertFalse(EventUtils.isSharedStateUpdateFor(IdentityConstants.SharedState.Configuration.NAME, null));
	}

	@Test
	public void test_isSharedStateUpdateFor_stateOwnerValueIsNull() {
		final Event event = new Event.Builder(
			"Shared state event",
			EventType.GENERIC_IDENTITY,
			EventSource.REQUEST_IDENTITY
		)
			.setEventData(Collections.singletonMap(IdentityConstants.EventDataKeys.STATE_OWNER, null))
			.build();

		assertFalse(EventUtils.isSharedStateUpdateFor(EventType.GENERIC_IDENTITY, event));
	}

	@Test
	public void test_isSharedStateUpdateFor_stateOwnerValueIsEmpty() {
		final Event event = new Event.Builder(
			"Shared state event",
			EventType.GENERIC_IDENTITY,
			EventSource.REQUEST_IDENTITY
		)
			.setEventData(Collections.singletonMap(IdentityConstants.EventDataKeys.STATE_OWNER, ""))
			.build();

		assertFalse(EventUtils.isSharedStateUpdateFor(IdentityConstants.EXTENSION_NAME, event));
	}

	@Test
	public void test_isSharedStateUpdateFor_stateOwnerValueExists() {
		final Event event = new Event.Builder(
			"Shared state event",
			EventType.GENERIC_IDENTITY,
			EventSource.REQUEST_IDENTITY
		)
			.setEventData(
				Collections.singletonMap(IdentityConstants.EventDataKeys.STATE_OWNER, "com.adobe.module.configuration")
			)
			.build();

		assertTrue(EventUtils.isSharedStateUpdateFor("com.adobe.module.configuration", event));
		assertFalse(EventUtils.isSharedStateUpdateFor(IdentityConstants.EXTENSION_NAME, event));
	}

	// ======================================================================================================================
	// Tests for method : getECID(final Map<String, Object> identityDirectSharedState)
	// ======================================================================================================================
	@Test
	public void test_getECID_valueExists() {
		final Map<String, Object> identityDirectState = Collections.singletonMap(
			IdentityConstants.SharedState.IdentityDirect.ECID,
			"SomeRandomECID"
		);
		assertEquals("SomeRandomECID", EventUtils.getECID(identityDirectState).toString());
	}

	@Test
	public void test_getECID_valueDoesNotExist() {
		assertNull(EventUtils.getECID(Collections.EMPTY_MAP));
	}

	@Test
	public void test_getECID_valueIsNull() {
		final Map<String, Object> identityDirectState = Collections.singletonMap(
			IdentityConstants.SharedState.IdentityDirect.ECID,
			null
		);
		assertNull(EventUtils.getECID(identityDirectState));
	}

	// Test helpers

	/**
	 * Creates an event with the given data.
	 * @param data to set for the created {@link Event}
	 * @return {@link Event} with the given event data set
	 */
	private Event createGenericIdentityEvent(final HashMap<String, Object> data) {
		return new Event.Builder("Test event", EventType.GENERIC_IDENTITY, EventSource.REQUEST_IDENTITY)
			.setEventData(data)
			.build();
	}
}
