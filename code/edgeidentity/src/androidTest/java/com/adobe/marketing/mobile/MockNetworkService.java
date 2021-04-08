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

package com.adobe.marketing.mobile;

import com.adobe.marketing.mobile.services.HttpConnecting;
import com.adobe.marketing.mobile.services.NetworkCallback;
import com.adobe.marketing.mobile.services.NetworkRequest;
import com.adobe.marketing.mobile.services.Networking;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Mock network service used by MobileCore for functional test cases.
 * This network service returns '200' HttpConnection responses for every request.
 */
class MockNetworkService implements Networking {
	private static String TAG = "MockNetworkService";

	private static HttpConnecting dummyConnection = new HttpConnecting() {
		@Override
		public InputStream getInputStream() {
			return new ByteArrayInputStream("{}".getBytes());
		}

		@Override
		public InputStream getErrorStream() {
			return null;
		}

		@Override
		public int getResponseCode() {
			return 200;
		}

		@Override
		public String getResponseMessage() {
			return null;
		}

		@Override
		public String getResponsePropertyValue(String s) {
			return null;
		}

		@Override
		public void close() {

		}
	};

	@Override
	public void connectAsync(NetworkRequest networkRequest, NetworkCallback networkCallback) {
		if (networkRequest == null) {
			return;
		}

		MobileCore.log(LoggingMode.DEBUG, TAG, "Received async request '" + networkRequest.getUrl() + "', ignoring.");

		if (networkCallback != null) {
			networkCallback.call(dummyConnection);
		}
	}
}
