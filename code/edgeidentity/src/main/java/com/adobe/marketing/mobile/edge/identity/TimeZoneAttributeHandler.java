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

import static com.adobe.marketing.mobile.edge.identity.IdentityConstants.LOG_TAG;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.StringUtils;

import java.util.Map;

/**
 * {@link ProfileAttributeHandler} for the device timezone. Reads the IANA identifier from the
 * incoming event under {@code "timezone"}, persists the last-synced value through the
 * {@link ProfileAttributeStore}, and contributes to the outgoing payload under {@code "timeZone"}.
 */
final class TimeZoneAttributeHandler implements ProfileAttributeHandler {

	private static final String LOG_SOURCE = "TimeZoneAttributeHandler";

	private final ProfileAttributeStore store;

	TimeZoneAttributeHandler(final ProfileAttributeStore store) {
		this.store = store;
	}

	@Override
	public String getAttributeKey() {
		return "timezone";
	}

	@Override
	public Map<String, Object> collectFromEvent(final Event event) {
		final String newTimeZone = DataReader.optString(event.getEventData(), getAttributeKey(), null);
		if (StringUtils.isNullOrEmpty(newTimeZone)) {
			return null;
		}
		if (newTimeZone.equals(store.getString(getAttributeKey()))) {
			Log.debug(LOG_TAG, LOG_SOURCE, "Timezone '" + newTimeZone + "' is unchanged, skipping sync.");
			return null;
		}
		// Persist before the caller dispatches, so an event dropped due to collect consent still leaves
		// the pending value in storage for a later re-sync.
		store.setString(getAttributeKey(), newTimeZone);
		return Map.of(getAttributeKey(), newTimeZone);
	}

	@Override
	public String collectFromStorage() {
		final String storedTimeZone = store.getString(getAttributeKey()                         );
		return StringUtils.isNullOrEmpty(storedTimeZone) ? null : storedTimeZone;
	}
}
