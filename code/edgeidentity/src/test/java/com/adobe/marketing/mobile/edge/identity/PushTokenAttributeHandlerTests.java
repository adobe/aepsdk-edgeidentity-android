/*
  Copyright 2026 Adobe. All rights reserved.
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

public class PushTokenAttributeHandlerTests {

	@Mock
	private ProfileAttributeStore mockStore;

	private PushTokenAttributeHandler handler;

	@Before
	public void before() {
		MockitoAnnotations.openMocks(this);
		handler = new PushTokenAttributeHandler(mockStore);
	}

	@Test
	public void testCollectFromEvent_whenChanged_persistsAndReturnsAttribute() {
		when(mockStore.getString(IdentityConstants.DeviceAttributes.PUSH_IDENTIFIER)).thenReturn(null);

		final Map<String, Object> result = handler.collectFromEvent(fakePushEvent("token-123"), true);

		verify(mockStore).setString(IdentityConstants.DeviceAttributes.PUSH_IDENTIFIER, "token-123");
		assertEquals(Map.of(IdentityConstants.DeviceAttributes.PUSH_IDENTIFIER, "token-123"), result);
	}

	@Test
	public void testCollectFromEvent_whenUnchangedAndDedupEnabled_returnsNull() {
		when(mockStore.getString(IdentityConstants.DeviceAttributes.PUSH_IDENTIFIER)).thenReturn("token-123");

		assertNull(handler.collectFromEvent(fakePushEvent("token-123"), true));
		verify(mockStore, never()).setString(eq(IdentityConstants.DeviceAttributes.PUSH_IDENTIFIER), any());
	}

	@Test
	public void testCollectFromEvent_whenUnchangedAndDedupDisabled_persistsAndReturnsAttribute() {
		assertEquals(
			Map.of(IdentityConstants.DeviceAttributes.PUSH_IDENTIFIER, "token-123"),
			handler.collectFromEvent(fakePushEvent("token-123"), false)
		);
		verify(mockStore).setString(IdentityConstants.DeviceAttributes.PUSH_IDENTIFIER, "token-123");
	}

	@Test
	public void testCollectFromEvent_whenEmpty_returnsNull() {
		assertNull(handler.collectFromEvent(fakePushEvent(""), true));
		verify(mockStore, never()).setString(eq(IdentityConstants.DeviceAttributes.PUSH_IDENTIFIER), any());
	}

	private Event fakePushEvent(final String token) {
		final Map<String, Object> eventData = new HashMap<>();
		eventData.put(IdentityConstants.DeviceAttributes.PUSH_IDENTIFIER, token);
		return new Event.Builder(
			IdentityConstants.EventNames.UPDATE_DEVICE_ATTRIBUTES,
			EventType.EDGE_IDENTITY,
			EventSource.REQUEST_CONTENT
		)
			.setEventData(eventData)
			.build();
	}
}
