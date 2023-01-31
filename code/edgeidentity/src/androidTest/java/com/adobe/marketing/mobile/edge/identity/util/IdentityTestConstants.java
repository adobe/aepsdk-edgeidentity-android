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

package com.adobe.marketing.mobile.edge.identity.util;

public class IdentityTestConstants {

	public static final String LOG_TAG = "EdgeIdentity";
	static final String EXTENSION_NAME = "com.adobe.edge.identity";

	static final class DataStoreKey {

		static final String CONFIG_DATASTORE = "AdobeMobile_ConfigState";
		static final String IDENTITY_DATASTORE = "com.adobe.edge.identity";
		public static final String IDENTITY_DIRECT_DATASTORE = "visitorIDServiceDataStore";
		public static final String IDENTITY_DIRECT_ECID_KEY = "ADOBEMOBILE_PERSISTED_MID";
		public static final String IDENTITY_PROPERTIES = "identity.properties";

		private DataStoreKey() {}
	}

	public static final class SharedStateName {

		public static final String CONFIG = "com.adobe.module.configuration";
		public static final String EVENT_HUB = "com.adobe.module.eventhub";

		private SharedStateName() {}
	}

	public static final class GetIdentitiesHelper {

		public static final String VALUE = "getConsentValue";
		public static final String ERROR = "getConsentError";

		private GetIdentitiesHelper() {}
	}

	public static class EventType {

		static final String MONITOR = "com.adobe.functional.eventType.monitor";

		private EventType() {}
	}

	public static class EventSource {

		// Used by Monitor Extension
		static final String XDM_SHARED_STATE_REQUEST = "com.adobe.eventSource.xdmsharedStateRequest";
		static final String XDM_SHARED_STATE_RESPONSE = "com.adobe.eventSource.xdmsharedStateResponse";
		static final String SHARED_STATE_REQUEST = "com.adobe.eventSource.sharedStateRequest";
		static final String SHARED_STATE_RESPONSE = "com.adobe.eventSource.sharedStateResponse";
		static final String UNREGISTER = "com.adobe.eventSource.unregister";

		private EventSource() {}
	}

	public static class EventDataKey {

		static final String STATE_OWNER = "stateowner";

		private EventDataKey() {}
	}

	static final class XDMKeys {

		static final String IDENTITY_MAP = "identityMap";
		static final String ID = "id";
		static final String AUTHENTICATED_STATE = "authenticatedState";
		static final String PRIMARY = "primary";

		private XDMKeys() {}
	}
}
