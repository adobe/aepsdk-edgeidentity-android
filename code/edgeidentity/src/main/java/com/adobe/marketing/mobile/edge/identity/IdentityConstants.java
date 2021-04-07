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

final class IdentityConstants {

	static final String LOG_TAG = "EdgeIdentity";
	static final String EXTENSION_NAME = "com.adobe.edge.identity";
	static final String EXTENSION_VERSION = "1.0.0";


	final class EventSource {
		static final String REQUEST_IDENTITY = "com.adobe.eventSource.requestIdentity";
		static final String REQUEST_CONTENT = "com.adobe.eventSource.requestContent";
		static final String RESPONSE_IDENTITY = "com.adobe.eventSource.responseIdentity";
		static final String UPDATE_IDENTITY = "com.adobe.eventSource.updateIdentity";
		static final String REMOVE_IDENTITY = "com.adobe.eventSource.removeIdentity";
		static final String REQUEST_RESET = "com.adobe.eventSource.requestReset";
		static final String SHARED_STATE = "com.adobe.eventSource.sharedState";
		static final String RESET_COMPLETE = "com.adobe.eventSource.resetComplete";
		static final String BOOTED = "com.adobe.eventSource.booted";
		private EventSource() { }
	}

	final class EventType {
		static final String GENERIC_IDENTITY = "com.adobe.eventType.generic.identity";
		static final String EDGE_IDENTITY = "com.adobe.eventType.edgeIdentity";
		static final String IDENTITY = "com.adobe.eventType.identity";
		static final String HUB = "com.adobe.eventType.hub";
		private EventType() { }
	}

	final class EventNames {
		static final String IDENTITY_REQUEST_IDENTITY_ECID = "Edge Identity Request ECID";
		static final String IDENTITY_RESPONSE_CONTENT_ONE_TIME = "Edge Identity Response Content One Time";
		static final String UPDATE_IDENTITIES = "Edge Identity Update Identities";
		static final String REMOVE_IDENTITIES = "Edge Identity Remove Identities";
		static final String REQUEST_IDENTITIES = "Edge Identity Request Identities";
		static final String RESET_IDENTITIES_COMPLETE = "Edge Identity Reset Identities Complete";
		private EventNames() { }
	}

	final class SharedState {
		static final String STATE_OWNER = "stateowner";

		final class Hub {
			static final String NAME = "com.adobe.module.eventhub";
			static final String EXTENSIONS = "extensions";
			private Hub() {}
		}

		final class IdentityDirect {
			static final String NAME = "com.adobe.module.identity";
			static final String ECID = "mid";
			private IdentityDirect() {}
		}

		private SharedState() { }
	}

	final class Namespaces {
		static final String ECID = "ECID";
		static final String IDFA = "IDFA";
		static final String GAID = "GAID";
		private Namespaces() { }
	}

	final class XDMKeys {
		static final String IDENTITY_MAP = "identityMap";
		static final String ID = "id";
		static final String AUTHENTICATED_STATE = "authenticatedState";
		static final String PRIMARY = "primary";
		private XDMKeys() { }
	}

	final class DataStoreKey {
		static final String DATASTORE_NAME = EXTENSION_NAME;
		static final String IDENTITY_PROPERTIES = "identity.properties";
		static final String IDENTITY_DIRECT_DATASTORE_NAME = "visitorIDServiceDataStore";
		static final String IDENTITY_DIRECT_ECID_KEY = "ADOBEMOBILE_PERSISTED_MID";
		private DataStoreKey() { }
	}

	private IdentityConstants() {}
}
