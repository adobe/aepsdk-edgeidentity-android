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

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.StringUtils;
import java.util.Map;

/**
 * {@link AttributeHandler} for the device push token.
 */
final class PushTokenAttributeHandler implements AttributeHandler {

	private final ProfileAttributeStore store;

	PushTokenAttributeHandler(final ProfileAttributeStore store) {
		this.store = store;
	}

	@Override
	public String getAttributeKey() {
		return IdentityConstants.DeviceAttributes.PUSH_IDENTIFIER;
	}

	@Override
	public Map<String, Object> collectFromEvent(final Event event, final boolean dedup) {
		final String pushToken = DataReader.optString(event.getEventData(), getAttributeKey(), null);
		if (StringUtils.isNullOrEmpty(pushToken)) {
			return null;
		}
		if (dedup && pushToken.equals(store.getString(getAttributeKey()))) {
			return null;
		}
		store.setString(getAttributeKey(), pushToken);
		return Map.of(getAttributeKey(), pushToken);
	}

	@Override
	public Map<String, Object> collectFromStorage() {
		final String storedPushToken = store.getString(getAttributeKey());
		return StringUtils.isNullOrEmpty(storedPushToken) ? null : Map.of(getAttributeKey(), storedPushToken);
	}
}
