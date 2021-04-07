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

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Util class used by both Functional and Unit tests
 */
class IdentityTestUtil {

	/**
	 * Method to serialize jsonObject to Map.
	 *
	 * @param jsonObject the {@link JSONObject} to be serialized
	 * @return a {@link Map} representing the serialized JSONObject
	 */

	static Map<String, Object> toMap(final JSONObject jsonObject) {
		if (jsonObject == null) {
			return null;
		}

		Map<String, Object> jsonAsMap = new HashMap();
		Iterator<String> keysIterator = jsonObject.keys();

		while (keysIterator.hasNext()) {
			String nextKey = keysIterator.next();
			Object value = null;
			Object returnValue;

			try {
				value = jsonObject.get(nextKey);
			} catch (JSONException e) {
				MobileCore.log(LoggingMode.DEBUG, "Functional Test",
							   "toMap - Unable to convert jsonObject to Map for key " + nextKey + ", skipping.");
			}

			if (value == null) {
				continue;
			}

			if (value instanceof JSONObject) {
				returnValue = toMap((JSONObject) value);
			} else if (value instanceof JSONArray) {
				returnValue = toList((JSONArray) value);
			} else {
				returnValue = value;
			}

			jsonAsMap.put(nextKey, returnValue);
		}

		return jsonAsMap;
	}

	/**
	 * Converts provided {@link JSONArray} into {@link List} for any number of levels which can be used as event data
	 * This method is recursive.
	 * The elements for which the conversion fails will be skipped.
	 *
	 * @param jsonArray to be converted
	 * @return {@link List} containing the elements from the provided json, null if {@code jsonArray} is null
	 */
	static List<Object> toList(final JSONArray jsonArray) {
		if (jsonArray == null) {
			return null;
		}

		List<Object> jsonArrayAsList = new ArrayList<Object>();
		int size = jsonArray.length();

		for (int i = 0; i < size; i++) {
			Object value = null;
			Object returnValue;

			try {
				value = jsonArray.get(i);
			} catch (JSONException e) {
				MobileCore.log(LoggingMode.DEBUG, "Functional Test",
							   "toList - Unable to convert jsonObject to List for index " + i + ", skipping.");
			}

			if (value == null) {
				continue;
			}

			if (value instanceof JSONObject) {
				returnValue = toMap((JSONObject) value);
			} else if (value instanceof JSONArray) {
				returnValue = toList((JSONArray) value);
			} else {
				returnValue = value;
			}

			jsonArrayAsList.add(returnValue);
		}

		return jsonArrayAsList;
	}

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
		final Map<String, Object> xdmData = Utils.toMap(jsonObject);
		return buildRemoveIdentityRequest(xdmData);
	}

	/**
	 * Helper method to build remove identity request event with XDM formatted Identity map
	 */
	static Event buildRemoveIdentityRequest(final Map<String, Object> map) {
		return new Event.Builder("Remove Identity Event", IdentityConstants.EventType.EDGE_IDENTITY,
								 IdentityConstants.EventSource.REMOVE_IDENTITY).setEventData(map).build();
	}

	/**
	 * Helper method to build update identity request event with XDM formatted Identity jsonString
	 */
	static Event buildUpdateIdentityRequestJSONString(final String jsonStr) throws Exception {
		final JSONObject jsonObject = new JSONObject(jsonStr);
		final Map<String, Object> xdmData = Utils.toMap(jsonObject);
		return buildUpdateIdentityRequest(xdmData);
	}

	/**
	 * Helper method to build update identity request event with XDM formatted Identity map
	 */
	static Event buildUpdateIdentityRequest(final Map<String, Object> map) {
		return new Event.Builder("Update Identity Event", IdentityConstants.EventType.EDGE_IDENTITY,
								 IdentityConstants.EventSource.UPDATE_IDENTITY).setEventData(map).build();
	}


	/**
	 * Serialize the given {@code jsonString} to a JSON Object, then flattens to {@code Map<String, String>}.
	 * If the provided string is not in JSON structure an {@link JSONException} is thrown.
	 *
	 * @param jsonString the string in JSON structure to flatten
	 * @return new map with flattened structure
	 */
	static Map<String, String> flattenJSONString(final String jsonString) throws JSONException {
		JSONObject jsonObject = new JSONObject(jsonString);
		Map<String, Object> persistenceValueMap = Utils.toMap(jsonObject);
		return flattenMap(persistenceValueMap);
	}

	/**
	 * Serialize the given {@code map} to a JSON Object, then flattens to {@code Map<String, String>}.
	 * For example, a JSON such as "{xdm: {stitchId: myID, eventType: myType}}" is flattened
	 * to two map elements "xdm.stitchId" = "myID" and "xdm.eventType" = "myType".
	 *
	 * @param map map with JSON structure to flatten
	 * @return new map with flattened structure
	 */
	static Map<String, String> flattenMap(final Map<String, Object> map) {
		if (map == null || map.isEmpty()) {
			return Collections.emptyMap();
		}

		try {
			JSONObject jsonObject = new JSONObject(map);
			Map<String, String> payloadMap = new HashMap<>();
			addKeys("", new ObjectMapper().readTree(jsonObject.toString()), payloadMap);
			return payloadMap;
		} catch (IOException e) {
			MobileCore.log(LoggingMode.ERROR, "FunctionalTestUtils", "Failed to parse JSON object to tree structure.");
		}

		return Collections.emptyMap();
	}


	/**
	 * Deserialize {@code JsonNode} and flatten to provided {@code map}.
	 * For example, a JSON such as "{xdm: {stitchId: myID, eventType: myType}}" is flattened
	 * to two map elements "xdm.stitchId" = "myID" and "xdm.eventType" = "myType".
	 * <p>
	 * Method is called recursively. To use, call with an empty path such as
	 * {@code addKeys("", new ObjectMapper().readTree(JsonNodeAsString), map);}
	 *
	 * @param currentPath the path in {@code JsonNode} to process
	 * @param jsonNode    {@link JsonNode} to deserialize
	 * @param map         {@code Map<String, String>} instance to store flattened JSON result
	 * @see <a href="https://stackoverflow.com/a/24150263">Stack Overflow post</a>
	 */
	private static void addKeys(String currentPath, JsonNode jsonNode, Map<String, String> map) {
		if (jsonNode.isObject()) {
			ObjectNode objectNode = (ObjectNode) jsonNode;
			Iterator<Map.Entry<String, JsonNode>> iter = objectNode.fields();
			String pathPrefix = currentPath.isEmpty() ? "" : currentPath + ".";

			while (iter.hasNext()) {
				Map.Entry<String, JsonNode> entry = iter.next();
				addKeys(pathPrefix + entry.getKey(), entry.getValue(), map);
			}
		} else if (jsonNode.isArray()) {
			ArrayNode arrayNode = (ArrayNode) jsonNode;

			for (int i = 0; i < arrayNode.size(); i++) {
				addKeys(currentPath + "[" + i + "]", arrayNode.get(i), map);
			}
		} else if (jsonNode.isValueNode()) {
			ValueNode valueNode = (ValueNode) jsonNode;
			map.put(currentPath, valueNode.asText());
		}
	}


	/**
	 * Class similar to {@link IdentityItem} for a specific namespace used for easier testing.
	 * For simplicity this class does not involve authenticatedState and primary key
	 */
	public static class TestItem {
		private String namespace;
		private String id;
		private boolean isPrimary = false;

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


	static Map<String, Object> getIdentitiesSync() {
		try {
			final HashMap<String, Object> getIdentityResponse = new HashMap<>();
			final ADBCountDownLatch latch = new ADBCountDownLatch(1);
			Identity.getIdentities(new AdobeCallbackWithError<IdentityMap>() {
				@Override
				public void call(final IdentityMap identities) {
					getIdentityResponse.put(IdentityTestConstants.GetIdentitiesHelper.VALUE, identities);
					latch.countDown();
				}

				@Override
				public void fail(final AdobeError adobeError) {
					getIdentityResponse.put(IdentityTestConstants.GetIdentitiesHelper.ERROR, adobeError);
					latch.countDown();
				}
			});
			latch.await(2000, TimeUnit.MILLISECONDS);

			return getIdentityResponse;
		} catch (Exception exp) {
			return null;
		}
	}

	static String getExperienceCloudIdSync() {
		try {
			final HashMap<String, String> getExperienceCloudIdResponse = new HashMap<>();
			final ADBCountDownLatch latch = new ADBCountDownLatch(1);
			Identity.getExperienceCloudId(new AdobeCallback<String>() {
				@Override
				public void call(final String ecid) {
					getExperienceCloudIdResponse.put(IdentityTestConstants.GetIdentitiesHelper.VALUE, ecid);
					latch.countDown();
				}
			});
			latch.await(2000, TimeUnit.MILLISECONDS);
			return getExperienceCloudIdResponse.get(IdentityTestConstants.GetIdentitiesHelper.VALUE);
		} catch (Exception exp) {
			return null;
		}
	}

	static IdentityMap CreateIdentityMap(final String namespace, final String id) {
		return CreateIdentityMap(namespace, id, AuthenticatedState.AMBIGUOUS, false);
	}

	static IdentityMap CreateIdentityMap(final String namespace, final String id, final AuthenticatedState state,
										 final boolean isPrimary) {
		IdentityMap map = new IdentityMap();
		IdentityItem item = new IdentityItem(id, state, isPrimary);
		map.addItem(item, namespace);
		return map;
	}
}
