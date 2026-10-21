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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adobe.marketing.mobile.services.NamedCollection;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ProfileAttributeStoreTests {

	private static final String KEY = "timeZone";

	@Mock
	private NamedCollection mockCollection;

	@Before
	public void before() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	public void testGetString_withCollection_returnsStoredValue() {
		when(mockCollection.getString(KEY, null)).thenReturn("America/New_York");
		final ProfileAttributeStore store = new ProfileAttributeStore(mockCollection);

		assertEquals("America/New_York", store.getString(KEY));
	}

	@Test
	public void testGetString_withNullCollection_returnsNull() {
		final ProfileAttributeStore store = new ProfileAttributeStore(null);

		assertNull(store.getString(KEY));
	}

	@Test
	public void testSetString_withNonNullValue_writesToCollection() {
		final ProfileAttributeStore store = new ProfileAttributeStore(mockCollection);

		store.setString(KEY, "America/New_York");

		verify(mockCollection, times(1)).setString(KEY, "America/New_York");
		verify(mockCollection, never()).remove(KEY);
	}

	@Test
	public void testSetString_withNullValue_removesFromCollection() {
		final ProfileAttributeStore store = new ProfileAttributeStore(mockCollection);

		store.setString(KEY, null);

		verify(mockCollection, times(1)).remove(KEY);
		verify(mockCollection, never()).setString(KEY, null);
	}

	@Test
	public void testSetString_withNullCollection_noOps() {
		final ProfileAttributeStore store = new ProfileAttributeStore(null);

		store.setString(KEY, "America/New_York");
		// No exception thrown; nothing to verify against since there is no collection.
	}

	@Test
	public void testRemove_withCollection_removesKey() {
		final ProfileAttributeStore store = new ProfileAttributeStore(mockCollection);

		store.remove(KEY);

		verify(mockCollection, times(1)).remove(KEY);
	}

	@Test
	public void testRemove_withNullCollection_noOps() {
		final ProfileAttributeStore store = new ProfileAttributeStore(null);

		store.remove(KEY);
		// No exception thrown; nothing to verify against since there is no collection.
	}

	@Test
	public void testClearAll_withCollection_removesAllKeys() {
		final ProfileAttributeStore store = new ProfileAttributeStore(mockCollection);

		store.clearAll();

		verify(mockCollection, times(1)).removeAll();
	}

	@Test
	public void testClearAll_withNullCollection_noOps() {
		final ProfileAttributeStore store = new ProfileAttributeStore(null);

		store.clearAll();
		// No exception thrown; nothing to verify against since there is no collection.
	}
}
