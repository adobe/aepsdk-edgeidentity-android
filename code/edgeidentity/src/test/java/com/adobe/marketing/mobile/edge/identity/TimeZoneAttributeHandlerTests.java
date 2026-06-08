/*
  Copyright 2025 Adobe. All rights reserved.
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
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TimeZoneAttributeHandlerTests {

	// Must mirror TimeZoneAttributeHandler's STORE_KEY constant. Tied to upgrade compat (do not change).
	private static final String STORE_KEY = "timezone";
	// Must mirror TimeZoneAttributeHandler's EVENT_KEY constant. Public Core API contract.
	private static final String EVENT_KEY = "timezone";
	// Must mirror TimeZoneAttributeHandler's PAYLOAD_KEY constant. XDM convention.
	private static final String PAYLOAD_KEY = "timeZone";

	@Mock
	private ProfileAttributeStore mockStore;

	private TimeZoneAttributeHandler handler;

	@Before
	public void before() {
		MockitoAnnotations.openMocks(this);
		handler = new TimeZoneAttributeHandler(mockStore);
	}

	@Test
	public void testGetAttributeKey_returnsEventKey() {
		assertEquals(EVENT_KEY, handler.getAttributeKey());
	}

	@Test
	public void testCollectFromEvent_whenChanged_persistsAndReturnsPayloadKeyedContribution() {
		when(mockStore.getString(STORE_KEY)).thenReturn(null);

		final Map<String, Object> result = handler.collectFromEvent(fakeTimeZoneEvent("America/New_York"));

		verify(mockStore, times(1)).setString(STORE_KEY, "America/New_York");
		assertEquals(Map.of(PAYLOAD_KEY, "America/New_York"), result);
	}

	@Test
	public void testCollectFromEvent_whenUnchanged_returnsNull() {
		when(mockStore.getString(STORE_KEY)).thenReturn("America/New_York");

		final Map<String, Object> result = handler.collectFromEvent(fakeTimeZoneEvent("America/New_York"));

		verify(mockStore, never()).setString(eq(STORE_KEY), any());
		assertNull(result);
	}

	@Test
	public void testCollectFromEvent_whenEmpty_returnsNull() {
		final Map<String, Object> result = handler.collectFromEvent(fakeTimeZoneEvent(""));

		verify(mockStore, never()).setString(eq(STORE_KEY), any());
		assertNull(result);
	}

	@Test
	public void testCollectFromEvent_whenKeyAbsent_returnsNull() {
		final Event event = new Event.Builder(
			"Update Profile Attributes",
			EventType.PROFILE_ATTRIBUTE,
			EventSource.REQUEST_CONTENT
		)
			.setEventData(new HashMap<>())
			.build();

		final Map<String, Object> result = handler.collectFromEvent(event);

		verify(mockStore, never()).setString(eq(STORE_KEY), any());
		assertNull(result);
	}

	@Test
	public void testCollectFromStorage_whenStored_returnsPayloadKeyedContribution() {
		when(mockStore.getString(STORE_KEY)).thenReturn("Asia/Kolkata");

		assertEquals(Map.of(PAYLOAD_KEY, "Asia/Kolkata"), handler.collectFromStorage());
	}

	@Test
	public void testCollectFromStorage_whenEmpty_returnsNull() {
		when(mockStore.getString(STORE_KEY)).thenReturn(null);

		assertNull(handler.collectFromStorage());
	}

	private Event fakeTimeZoneEvent(final String timeZone) {
		final Map<String, Object> eventData = new HashMap<>();
		eventData.put(EVENT_KEY, timeZone);
		return new Event.Builder("Update Profile Attributes", EventType.PROFILE_ATTRIBUTE, EventSource.REQUEST_CONTENT)
			.setEventData(eventData)
			.build();
	}
}
