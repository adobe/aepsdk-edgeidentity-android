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
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;

import java.util.Map;

import static com.adobe.marketing.mobile.edge.identity.IdentityConstants.LOG_TAG;

/**
 * Class for Event / Event data specific helpers.
 */
final class EventUtils {

	/**
	 * Checks if the provided {@code event} is of type {@link IdentityConstants.EventType#EDGE_IDENTITY} and source {@link IdentityConstants.EventSource#REMOVE_IDENTITY}
	 *
	 * @param event the event to verify
	 * @return true if both type and source match
	 */
	static boolean isRemoveIdentityEvent(final Event event) {
		return event != null && IdentityConstants.EventType.EDGE_IDENTITY.equalsIgnoreCase(event.getType())
			   && IdentityConstants.EventSource.REMOVE_IDENTITY.equalsIgnoreCase(event.getSource());
	}

	/**
	 * Checks if the provided {@code event} is of type {@link IdentityConstants.EventType#EDGE_IDENTITY} and source {@link IdentityConstants.EventSource#UPDATE_IDENTITY}
	 *
	 * @param event the event to verify
	 * @return true if both type and source match
	 */
	static boolean isUpdateIdentityEvent(final Event event) {
		return event != null && IdentityConstants.EventType.EDGE_IDENTITY.equalsIgnoreCase(event.getType())
			   && IdentityConstants.EventSource.UPDATE_IDENTITY.equalsIgnoreCase(event.getSource());
	}

	/**
	 * Checks if the provided {@code event} is of type {@link IdentityConstants.EventType#EDGE_IDENTITY} and source {@link IdentityConstants.EventSource#REQUEST_IDENTITY}
	 *
	 * @param event the event to verify
	 * @return true if both type and source match
	 */
	static boolean isRequestIdentityEvent(final Event event) {
		return event != null && IdentityConstants.EventType.EDGE_IDENTITY.equalsIgnoreCase(event.getType())
			   && IdentityConstants.EventSource.REQUEST_IDENTITY.equalsIgnoreCase(event.getSource());
	}

	/**
	 * Checks if the provided {@code event} is of type {@link IdentityConstants.EventType#GENERIC_IDENTITY} and source {@link IdentityConstants.EventSource#REQUEST_RESET}
	 *
	 * @param event the event to verify
	 * @return true if both type and source match
	 */
	static boolean isRequestResetEvent(final Event event) {
		return event != null && IdentityConstants.EventType.GENERIC_IDENTITY.equalsIgnoreCase(event.getType())
			   && IdentityConstants.EventSource.REQUEST_RESET.equalsIgnoreCase(event.getSource());
	}

	/**
	 * Checks if the provided {@code event} is a shared state update event for {@code stateOwnerName}
	 *
	 * @param stateOwnerName the shared state owner name; should not be null
	 * @param event current event to check; should not be null
	 * @return {@code boolean} indicating if it is the shared state update for the provided {@code stateOwnerName}
	 */
	static boolean isSharedStateUpdateFor(final String stateOwnerName, final Event event) {
		if (Utils.isNullOrEmpty(stateOwnerName) || event == null) {
			return false;
		}

		String stateOwner;

		try {
			stateOwner = (String) event.getEventData().get(IdentityConstants.SharedState.STATE_OWNER);
		} catch (ClassCastException e) {
			return false;
		}

		return stateOwnerName.equals(stateOwner);
	}

	/**
	 * Extracts the ECID from the Identity Direct shared state and returns it as an {@link ECID} object
	 *
	 * @param identityDirectSharedState the Identity Direct shared state data
	 * @return the ECID or null if not found or unable to parse the payload
	 */
	static ECID getECID(final Map<String, Object> identityDirectSharedState) {
		ECID legacyEcid = null;

		try {
			final String legacyEcidString = (String) identityDirectSharedState.get(
												IdentityConstants.SharedState.IdentityDirect.ECID);
			legacyEcid = legacyEcidString == null ? null : new ECID(legacyEcidString);
		} catch (ClassCastException e) {
			MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
						   "EventUtils - Failed to extract ECID from Identity direct shared state, expected String: "
						   + e.getLocalizedMessage());
		}

		return legacyEcid;
	}
}
