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

	// Must mirror TimeZoneAttributeHandler's getAttributeKey(). Used for event input, persistence, and payload — all "timeZone".
	private static final String KEY = "timeZone";

	@Mock
	private ProfileAttributeStore mockStore;

	private TimeZoneAttributeHandler handler;

	@Before
	public void before() {
		MockitoAnnotations.openMocks(this);
		handler = new TimeZoneAttributeHandler(mockStore);
	}

	@Test
	public void testGetAttributeKey_returnsTimeZoneKey() {
		assertEquals(KEY, handler.getAttributeKey());
	}

	@Test
	public void testCollectFromEvent_whenChanged_persistsAndReturnsPayloadKeyedContribution() {
		when(mockStore.getString(KEY)).thenReturn(null);

		final Map<String, Object> result = handler.collectFromEvent(fakeTimeZoneEvent("America/New_York"));

		verify(mockStore, times(1)).setString(KEY, "America/New_York");
		assertEquals(Map.of(KEY, "America/New_York"), result);
	}

	@Test
	public void testCollectFromEvent_whenUnchanged_returnsNull() {
		when(mockStore.getString(KEY)).thenReturn("America/New_York");

		final Map<String, Object> result = handler.collectFromEvent(fakeTimeZoneEvent("America/New_York"));

		verify(mockStore, never()).setString(eq(KEY), any());
		assertNull(result);
	}

	@Test
	public void testCollectFromEvent_whenEmpty_returnsNull() {
		final Map<String, Object> result = handler.collectFromEvent(fakeTimeZoneEvent(""));

		verify(mockStore, never()).setString(eq(KEY), any());
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

		verify(mockStore, never()).setString(eq(KEY), any());
		assertNull(result);
	}

	@Test
	public void testCollectFromStorage_whenStored_returnsPayloadKeyedContribution() {
		when(mockStore.getString(KEY)).thenReturn("Asia/Kolkata");

		assertEquals(Map.of(KEY, "Asia/Kolkata"), handler.collectFromStorage());
	}

	@Test
	public void testCollectFromStorage_whenEmpty_returnsNull() {
		when(mockStore.getString(KEY)).thenReturn(null);

		assertNull(handler.collectFromStorage());
	}

	private Event fakeTimeZoneEvent(final String timeZone) {
		final Map<String, Object> eventData = new HashMap<>();
		eventData.put(KEY, timeZone);
		return new Event.Builder("Update Profile Attributes", EventType.PROFILE_ATTRIBUTE, EventSource.REQUEST_CONTENT)
			.setEventData(eventData)
			.build();
	}
}
