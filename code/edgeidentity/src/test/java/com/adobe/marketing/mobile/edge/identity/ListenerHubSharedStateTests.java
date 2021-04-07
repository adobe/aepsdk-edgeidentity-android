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

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.MobileCore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ListenerHubSharedStateTests {
	@Mock
	private IdentityExtension mockIdentityExtension;

	private ListenerHubSharedState listener;
	private ExecutorService testExecutor;

	@Before
	public void setup() {
		testExecutor = Executors.newSingleThreadExecutor();
		mockIdentityExtension = Mockito.mock(IdentityExtension.class);
		doReturn(testExecutor).when(mockIdentityExtension).getExecutor();
		MobileCore.start(null);
		listener = spy(new ListenerHubSharedState(null, IdentityConstants.EventType.HUB,
					   IdentityConstants.EventSource.SHARED_STATE));
	}

	@Test
	public void testHear() throws Exception {
		// setup
		Event event = new Event.Builder("Shared State Change", IdentityConstants.EventType.HUB,
										IdentityConstants.EventSource.SHARED_STATE).build();
		doReturn(mockIdentityExtension).when(listener).getIdentityExtension();

		// test
		listener.hear(event);

		// verify
		testExecutor.awaitTermination(100, TimeUnit.MILLISECONDS);
		verify(mockIdentityExtension, times(1)).handleHubSharedState(event);
	}

	@Test
	public void testHear_WhenParentExtensionNull() throws Exception {
		// setup
		Event event = new Event.Builder("Shared State Change", IdentityConstants.EventType.HUB,
										IdentityConstants.EventSource.SHARED_STATE).build();
		doReturn(null).when(listener).getIdentityExtension();

		// test
		listener.hear(event);

		// verify
		testExecutor.awaitTermination(100, TimeUnit.MILLISECONDS);
		verify(mockIdentityExtension, times(0)).handleHubSharedState(any(Event.class));
	}

	@Test
	public void testHear_WhenEventNull() throws Exception {
		// setup
		doReturn(null).when(listener).getIdentityExtension();
		doReturn(mockIdentityExtension).when(listener).getIdentityExtension();

		// test
		listener.hear(null);

		// verify
		testExecutor.awaitTermination(100, TimeUnit.MILLISECONDS);
		verify(mockIdentityExtension, times(0)).handleHubSharedState(any(Event.class));
	}
}
