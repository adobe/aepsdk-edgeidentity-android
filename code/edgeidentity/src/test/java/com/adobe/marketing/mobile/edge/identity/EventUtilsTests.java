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

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import com.adobe.marketing.mobile.Event;
import java.util.HashMap;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class EventUtilsTests {

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

	// Test helpers

	/**
	 * Creates an event with the given data.
	 * @param data to set for the created {@link Event}
	 * @return {@link Event} with the given event data set
	 */
	private Event createGenericIdentityEvent(final HashMap<String, Object> data) {
		return new Event.Builder(
			"Test event",
			IdentityConstants.EventType.GENERIC_IDENTITY,
			IdentityConstants.EventSource.REQUEST_IDENTITY
		)
			.setEventData(data)
			.build();
	}
}
