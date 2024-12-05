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

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.util.JSONUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

/**
 * Util class used by both Functional and Unit tests
 */
class IdentityTestUtil {

	private static final String LOG_SOURCE = "IdentityTestUtil";
	private static final String LOG_TAG = "FunctionalTestUtils";

	/**
	 * Helper method to create IdentityXDM Map using {@link TestItem}s
	 */
	static Map<String, Object> createXDMIdentityMap(TestItem... items) {
		final Map<String, List<Map<String, Object>>> allItems = new HashMap<>();

		for (TestItem item : items) {
			final Map<String, Object> itemMap = new HashMap<>();
			itemMap.put(IdentityConstants.XDMKeys.ID, item.id);
			itemMap.put(IdentityConstants.XDMKeys.AUTHENTICATED_STATE, "ambiguous");
			itemMap.put(IdentityConstants.XDMKeys.PRIMARY, item.isPrimary);
			List<Map<String, Object>> nameSpaceItems = allItems.get(item.namespace);

			if (nameSpaceItems == null) {
				nameSpaceItems = new ArrayList<>();
			}

			nameSpaceItems.add(itemMap);
			allItems.put(item.namespace, nameSpaceItems);
		}

		final Map<String, Object> identityMapDict = new HashMap<>();
		identityMapDict.put(IdentityConstants.XDMKeys.IDENTITY_MAP, allItems);
		return identityMapDict;
	}

	/**
	 * Helper method to build remove identity request event with XDM formatted Identity jsonString
	 */
	static Event buildRemoveIdentityRequestWithJSONString(final String jsonStr) throws Exception {
		final JSONObject jsonObject = new JSONObject(jsonStr);
		final Map<String, Object> xdmData = JSONUtils.toMap(jsonObject);
		return buildRemoveIdentityRequest(xdmData);
	}

	/**
	 * Helper method to build remove identity request event with XDM formatted Identity map
	 */
	static Event buildRemoveIdentityRequest(final Map<String, Object> map) {
		return new Event.Builder("Remove Identity Event", EventType.EDGE_IDENTITY, EventSource.REMOVE_IDENTITY)
			.setEventData(map)
			.build();
	}

	/**
	 * Helper method to build update identity request event with XDM formatted Identity jsonString
	 */
	static Event buildUpdateIdentityRequestJSONString(final String jsonStr) throws Exception {
		final JSONObject jsonObject = new JSONObject(jsonStr);
		final Map<String, Object> xdmData = JSONUtils.toMap(jsonObject);
		return buildUpdateIdentityRequest(xdmData);
	}

	/**
	 * Helper method to build update identity request event with XDM formatted Identity map
	 */
	static Event buildUpdateIdentityRequest(final Map<String, Object> map) {
		return new Event.Builder("Update Identity Event", EventType.EDGE_IDENTITY, EventSource.UPDATE_IDENTITY)
			.setEventData(map)
			.build();
	}

	/**
	 * Class similar to {@link IdentityItem} for a specific namespace used for easier testing.
	 * For simplicity this class does not involve authenticatedState and primary key
	 */
	public static class TestItem {

		private final String namespace;
		private final String id;
		private final boolean isPrimary = false;

		public TestItem(String namespace, String id) {
			this.namespace = namespace;
			this.id = id;
		}
	}

	public static class TestECIDItem extends TestItem {

		public TestECIDItem(final String ecid) {
			super(IdentityConstants.Namespaces.ECID, ecid);
		}
	}
}
