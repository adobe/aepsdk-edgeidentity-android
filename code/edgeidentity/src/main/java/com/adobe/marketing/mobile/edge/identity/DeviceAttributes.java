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

/**
 * Input for {@link Identity#updateDeviceAttributesBypassConsent(DeviceAttributes)}.
 * Unset or empty attributes are ignored. Use the builder to configure the attributes to update.
 */
public final class DeviceAttributes {

	private final String timeZone;
	private final String pushToken;

	private DeviceAttributes(final Builder builder) {
		this.timeZone = builder.timeZone;
		this.pushToken = builder.pushToken;
	}

	/**
	 * Returns the IANA time zone identifier to synchronize, or {@code null} when unset.
	 *
	 * @return the time zone identifier
	 */
	public String getTimeZone() {
		return timeZone;
	}

	/**
	 * Returns the push token to synchronize, or {@code null} when unset.
	 *
	 * @return the push token
	 */
	public String getPushToken() {
		return pushToken;
	}

	/**
	 * Builder for {@link DeviceAttributes}.
	 */
	public static final class Builder {

		private String timeZone;
		private String pushToken;

		/**
		 * Sets the IANA time zone identifier.
		 *
		 * @param timeZone the time zone identifier
		 * @return this builder
		 */
		public Builder setTimeZone(final String timeZone) {
			this.timeZone = timeZone;
			return this;
		}

		/**
		 * Sets the push token.
		 *
		 * @param pushToken the push token
		 * @return this builder
		 */
		public Builder setPushToken(final String pushToken) {
			this.pushToken = pushToken;
			return this;
		}

		/**
		 * Builds the device attributes value.
		 *
		 * @return the configured attributes
		 */
		public DeviceAttributes build() {
			return new DeviceAttributes(this);
		}
	}
}
