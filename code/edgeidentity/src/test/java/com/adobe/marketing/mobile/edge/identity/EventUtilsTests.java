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

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.adobe.marketing.mobile.Event;
import java.util.HashMap;
import org.junit.Test;

public class EventUtilsTests {

	@Test
	public void test_isRequestIdentityEventForGetUrlVariable_nullEvent_returnsFalse() {
		assertFalse(EventUtils.isGetUrlVariablesRequestEvent(null));
	}

	@Test
	public void test_isRequestIdentityEventForGetUrlVariable_eventContainsUrlVariablesKey_validBooleanValue_returnsSetBooleanValue() {
		Event event = new Event.Builder(
			IdentityConstants.EventNames.IDENTITY_REQUEST_URL_VARIABLES,
			IdentityConstants.EventType.EDGE_IDENTITY,
			IdentityConstants.EventSource.REQUEST_IDENTITY
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
				IdentityConstants.EventType.EDGE_IDENTITY,
				IdentityConstants.EventSource.REQUEST_IDENTITY
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
			IdentityConstants.EventType.IDENTITY,
			IdentityConstants.EventSource.REQUEST_RESET
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
				IdentityConstants.EventType.IDENTITY,
				IdentityConstants.EventSource.REQUEST_IDENTITY
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
			IdentityConstants.EventType.EDGE_IDENTITY,
			IdentityConstants.EventSource.REQUEST_IDENTITY
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
				IdentityConstants.EventType.EDGE_IDENTITY,
				IdentityConstants.EventSource.REQUEST_IDENTITY
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
}
