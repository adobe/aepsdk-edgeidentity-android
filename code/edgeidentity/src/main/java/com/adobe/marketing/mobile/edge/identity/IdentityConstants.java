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
	static final String EXTENSION_FRIENDLY_NAME = "Edge Identity";
	static final String EXTENSION_VERSION = "3.0.1";

	static final class Default {

		static final String ZERO_ADVERTISING_ID = "00000000-0000-0000-0000-000000000000";

		private Default() {}
	}

	static final class EventNames {

		static final String CONSENT_UPDATE_REQUEST_AD_ID = "Consent Update Request for Ad ID";
		static final String IDENTITY_REQUEST_IDENTITY_ECID = "Edge Identity Request ECID";
		static final String IDENTITY_REQUEST_URL_VARIABLES = "Edge Identity Request URL Variables";
		static final String IDENTITY_RESPONSE_CONTENT_ONE_TIME = "Edge Identity Response Content One Time";
		static final String IDENTITY_RESPONSE_URL_VARIABLES = "Edge Identity Response URL Variables";
		static final String UPDATE_IDENTITIES = "Edge Identity Update Identities";
		static final String REMOVE_IDENTITIES = "Edge Identity Remove Identities";
		static final String REQUEST_IDENTITIES = "Edge Identity Request Identities";
		static final String RESET_IDENTITIES_COMPLETE = "Edge Identity Reset Identities Complete";
		static final String UPDATE_PROFILE_ATTRIBUTES = "Update Profile Attributes";

		private EventNames() {}
	}

	static final class EventDataKeys {

		static final String ADVERTISING_IDENTIFIER = "advertisingidentifier";
		static final String STATE_OWNER = "stateowner";
		static final String URL_VARIABLES = "urlvariables";

		private EventDataKeys() {}
	}

	static final class SharedState {

		static final class Hub {

			static final String NAME = "com.adobe.module.eventhub";
			static final String EXTENSIONS = "extensions";

			private Hub() {}
		}

		static final class Configuration {

			static final String NAME = "com.adobe.module.configuration";
			static final String EXPERIENCE_CLOUD_ORGID = "experienceCloud.org";

			private Configuration() {}
		}

		static final class IdentityDirect {

			static final String NAME = "com.adobe.module.identity";
			static final String ECID = "mid";

			private IdentityDirect() {}
		}

		private SharedState() {}
	}

	static final class Namespaces {

		static final String ECID = "ECID";
		static final String IDFA = "IDFA";
		static final String GAID = "GAID";

		private Namespaces() {}
	}

	static final class XDMKeys {

		static final String IDENTITY_MAP = "identityMap";
		static final String PROFILE_ATTRIBUTES = "profileAttributes";
		static final String ID = "id";
		static final String AUTHENTICATED_STATE = "authenticatedState";
		static final String PRIMARY = "primary";

		static final class Consent {

			static final String AD_ID = "adID";
			static final String CONSENTS = "consents";
			static final String ID_TYPE = "idType";
			static final String NO = "n";
			static final String VAL = "val";
			static final String YES = "y";

			private Consent() {}
		}

		private XDMKeys() {}
	}

	static final class DataStoreKey {

		static final String DATASTORE_NAME = EXTENSION_NAME;
		static final String IDENTITY_PROPERTIES = "identity.properties";
		static final String IDENTITY_DIRECT_DATASTORE_NAME = "visitorIDServiceDataStore";
		static final String IDENTITY_DIRECT_ECID_KEY = "ADOBEMOBILE_PERSISTED_MID";
		static final String PROFILE_ATTRIBUTES_DATASTORE_NAME = "com.adobe.mobilecore.profileAttributes";

		private DataStoreKey() {}
	}

	static final class UrlKeys {

		static final String TS = "TS";
		static final String EXPERIENCE_CLOUD_ORG_ID = "MCORGID";
		static final String EXPERIENCE_CLOUD_ID = "MCMID";
		static final String PAYLOAD = "adobe_mc";

		private UrlKeys() {}
	}

	/**
	 * Constants for the profile attribute sync feature driven by
	 * {@code MobileCore.updateProfileAttributes}.
	 */
	static final class ProfileAttributes {

		// Keys for the outgoing generic EDGE request event payload. The event carries the collated
		// data from every profile-attribute helper under a single "profile.updateAttributes" type.
		// Per-attribute event-data keys, payload keys, and persistence keys are owned by the
		// individual ProfileAttributeHandler implementations.
		static final String XDM = "xdm";
		static final String DATA = "data";
		static final String EVENT_TYPE = "eventType";
		static final String XDM_EVENT_TYPE_UPDATE_ATTRIBUTES = "profile.updateAttributes";

		private ProfileAttributes() {}
	}

	private IdentityConstants() {}
}
