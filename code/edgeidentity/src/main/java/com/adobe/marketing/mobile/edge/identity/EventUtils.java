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
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.StringUtils;
import java.util.Map;

/**
 * Class for Event / Event data specific helpers.
 */
final class EventUtils {

	/**
	 * Checks if the provided {@code event}'s data contains the key {@link IdentityConstants.EventDataKeys#ADVERTISING_IDENTIFIER}
	 *
	 * @param event the event to verify
	 * @return {@code true} if key is present
	 */
	static boolean isAdIdEvent(final Event event) {
		final Map<String, Object> data = event.getEventData();
		return data.containsKey(IdentityConstants.EventDataKeys.ADVERTISING_IDENTIFIER);
	}

	/**
	 * Reads the url variables flag from the event data, returns false if not present
	 * Note: This API needs to be used with isRequestIdentityEvent API to determine the correct event type and event source
	 * @param event the event to verify
	 * @return true if urlVariables key is present in the event data and has a value of true
	 */
	static boolean isGetUrlVariablesRequestEvent(final Event event) {
		return (
			event != null &&
			DataReader.optBoolean(event.getEventData(), IdentityConstants.EventDataKeys.URL_VARIABLES, false)
		);
	}

	/**
	 * Checks if the provided {@code event} is a shared state update event for {@code stateOwnerName}
	 *
	 * @param stateOwnerName the shared state owner name; should not be null
	 * @param event          current event to check; should not be null
	 * @return {@code boolean} indicating if it is the shared state update for the provided {@code stateOwnerName}
	 */
	static boolean isSharedStateUpdateFor(final String stateOwnerName, final Event event) {
		if (StringUtils.isNullOrEmpty(stateOwnerName) || event == null) {
			return false;
		}

		final String stateOwner = DataReader.optString(
			event.getEventData(),
			IdentityConstants.EventDataKeys.STATE_OWNER,
			""
		);
		return stateOwnerName.equals(stateOwner);
	}

	/**
	 * Gets the advertising ID from the event data using the key
	 * {@link IdentityConstants.EventDataKeys#ADVERTISING_IDENTIFIER}.
	 *
	 * Performs a sanitization of values, converting {@code null}, {@code ""}, and
	 * {@link IdentityConstants.Default#ZERO_ADVERTISING_ID} into {@code ""}.
	 *
	 * This method should not be used to detect whether the event is an ad ID event or not;
	 * use {@link #isAdIdEvent(Event)} instead.
	 *
	 * @param event the event containing the advertising ID
	 * @return the adID
	 */
	static String getAdId(final Event event) {
		final Map<String, Object> data = event.getEventData();
		final String adID = DataReader.optString(data, IdentityConstants.EventDataKeys.ADVERTISING_IDENTIFIER, null);

		return (adID == null || IdentityConstants.Default.ZERO_ADVERTISING_ID.equals(adID) ? "" : adID);
	}

	/**
	 * Extracts the ECID from the Identity Direct shared state and returns it as an {@link ECID} object
	 *
	 * @param identityDirectSharedState the Identity Direct shared state data
	 * @return the ECID or null if not found or unable to parse the payload
	 */
	static ECID getECID(final Map<String, Object> identityDirectSharedState) {
		final String legacyEcidString = DataReader.optString(
			identityDirectSharedState,
			IdentityConstants.SharedState.IdentityDirect.ECID,
			null
		);
		return (legacyEcidString == null ? null : new ECID(legacyEcidString));
	}

	/**
	 * Extracts the Experience Cloud Org Id from the Configuration shared state
	 *
	 * @param configurationSharedState the configuration shared state data
	 * @return the Experience Cloud Org Id or null if not found or unable to parse the payload
	 */
	static String getOrgId(final Map<String, Object> configurationSharedState) {
		return DataReader.optString(
			configurationSharedState,
			IdentityConstants.SharedState.Configuration.EXPERIENCE_CLOUD_ORGID,
			null
		);
	}
}
