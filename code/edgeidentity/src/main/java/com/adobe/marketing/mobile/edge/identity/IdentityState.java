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

import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;

import java.util.HashMap;
import java.util.Map;

import static com.adobe.marketing.mobile.edge.identity.IdentityConstants.LOG_TAG;

/**
 * Manages the business logic of this Identity extension
 */
class IdentityState {
	private IdentityProperties identityProperties;
	private boolean hasBooted;

	/**
	 * Creates a new {@link IdentityState} with the given {@link IdentityProperties}
	 *
	 * @param identityProperties identity properties
	 */
	IdentityState(final IdentityProperties identityProperties) {
		this.identityProperties = identityProperties;
	}

	/**
	 * @return the current bootup status
	 */
	boolean hasBooted() {
		return hasBooted;
	}

	/**
	 * @return The current {@link IdentityProperties} for this identity state
	 */
	IdentityProperties getIdentityProperties() {
		return identityProperties;
	}

	/**
	 * Completes init for the Identity extension.
	 * Attempts to load the already persisted identities from persistence into {@link #identityProperties}
	 * If no ECID is loaded from persistence (ideally meaning first launch), then we attempt to read ECID for the direct Identity Extension.
	 * If there is no ECID loaded from the persistence of direct Identity Extension, then and new ECID is generated and persisted finishing the bootUp sequence.
	 * @return True if it should share state after bootup, false otherwise
	 */
	boolean bootupIfReady(final SharedStateCallback callback) {
		if (hasBooted) {
			return false;
		}

		// Load properties from local storage
		identityProperties = IdentityStorageService.loadPropertiesFromPersistence();

		if (identityProperties == null) {
			identityProperties = new IdentityProperties();
		}

		// Reuse the ECID from Identity Direct (if registered) or generate new ECID on first launch
		if (identityProperties.getECID() == null) {
			final ECID directIdentityEcid = IdentityStorageService.loadEcidFromDirectIdentityPersistence();
			final Map<String, Object> identityDirectSharedState = callback.getSharedState(IdentityConstants.SharedState.IdentityDirect.NAME, null);

			if (directIdentityEcid != null) {
				identityProperties.setECID(directIdentityEcid);
				MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
						"IdentityState -  On bootup Loading ECID from direct Identity extension '" + directIdentityEcid + "'");
			} else if (identityDirectSharedState != null) { // identity direct shared state is set
				handleInstallWithIdentityDirectECID(EventUtils.getECID(identityDirectSharedState));
			} else if (isIdentityDirectRegistered(callback)) {
				MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "IdentityState - On bootup direct Identity extension is registered, waiting for its state change.");
				return false; // If no ECID to migrate but Identity direct is registered, wait for Identity direct shared state
			} else {
				identityProperties.setECID(new ECID());
				MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
						"IdentityState - Generating new ECID on bootup '" + identityProperties.getECID().toString() + "'");
			}

			IdentityStorageService.savePropertiesToPersistence(identityProperties);
		}

		hasBooted = true;
		MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "IdentityState - Edge Identity has successfully booted up");
		return true;
	}

	/**
	 * Clears all identities and regenerates a new ECID value, then saves the new identities to persistence.
	 */
	void resetIdentifiers() {
		// TODO: AMSDK-11208 Determine if we should dispatch consent event

		identityProperties = new IdentityProperties();
		identityProperties.setECID(new ECID());
		identityProperties.setECIDSecondary(null);
		IdentityStorageService.savePropertiesToPersistence(identityProperties);

		// TODO: AMSDK-11208 Use return value to tell Identity to dispatch consent ad id update
	}

	/**
	 * Update the customer identifiers by merging the passed in {@link IdentityMap} with the current identifiers present in {@link #identityProperties}.
	 *
	 * @param map the {@code IdentityMap} containing customer identifiers to add or update with the current customer identifiers
	 */
	void updateCustomerIdentifiers(final IdentityMap map) {
		identityProperties.updateCustomerIdentifiers(map);
		IdentityStorageService.savePropertiesToPersistence(identityProperties);
	}

	/**
	 * Remove customer identifiers specified in passed in {@link IdentityMap} from the current identifiers present in {@link #identityProperties}.
	 *
	 * @param map the {@code IdentityMap} with items to remove from current identifiers
	 */
	void removeCustomerIdentifiers(final IdentityMap map) {
		identityProperties.removeCustomerIdentifiers(map);
		IdentityStorageService.savePropertiesToPersistence(identityProperties);
	}

	/**
	 * Update the legacy ECID property with {@code legacyEcid} provided it does not equal the primary or secondary ECIDs
	 * currently in {@code IdentityProperties}.
	 *
	 * @param legacyEcid the current ECID from the direct Identity extension
	 * @return true if the legacy ECID was updated in {@code IdentityProperties}
	 */
	boolean updateLegacyExperienceCloudId(final ECID legacyEcid) {
		final ECID ecid = identityProperties.getECID();
		final ECID ecidSecondary = identityProperties.getECIDSecondary();

		if ((legacyEcid != null) && (legacyEcid.equals(ecid) || legacyEcid.equals(ecidSecondary))) {
			return false;
		}

		// no need to clear secondaryECID if its already null
		if (legacyEcid == null && ecidSecondary == null) {
			return false;
		}

		identityProperties.setECIDSecondary(legacyEcid);
		IdentityStorageService.savePropertiesToPersistence(identityProperties);
		MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
					   "IdentityState - Identity direct ECID updated to '" + legacyEcid + "', updating the IdentityMap");
		return true;
	}

	/**
	 * This method is called when the primary Edge ECID is null and the Identity Direct shared state has been updated (install scenario when Identity Direct is registered).
	 * Sets the {@code legacyEcid} as primary ECID when not null, otherwise generates a new ECID.
	 *
	 * @param legacyEcid the current ECID from the direct Identity extension
	 */
	private void handleInstallWithIdentityDirectECID(final ECID legacyEcid) {
		if (legacyEcid != null) {
			identityProperties.setECID(legacyEcid); // set legacy ECID as main ECID
			MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
					"IdentityState - Identity direct ECID '" + legacyEcid + "' was migrated to Edge Identity, updating the IdentityMap");

		} else { // opt-out scenario or an unexpected state for Identity direct, generate new ECID
			identityProperties.setECID(new ECID());
			MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
					"IdentityState - Identity direct ECID is null, generating new ECID '" + identityProperties.getECID() + "', updating the IdentityMap");
		}
	}

	/**
	 * Check if the Identity direct extension is registered by checking the EventHub's shared state list of registered extensions.
	 * @param callback the {@link SharedStateCallback} to be used for fetching the EventHub Shared state
	 * @return true if the Identity direct extension is registered with the EventHub
	 */
	private boolean isIdentityDirectRegistered(final SharedStateCallback callback) {
		Map<String, Object> registeredExtensionsWithHub = callback.getSharedState(IdentityConstants.SharedState.Hub.NAME, null);
		Map<String, Object> identityDirectInfo = null;

		if (registeredExtensionsWithHub != null) {
			try {
				final Map<String, Object> extensions = (HashMap<String, Object>) registeredExtensionsWithHub.get(
						IdentityConstants.SharedState.Hub.EXTENSIONS);

				if (extensions != null) {
					identityDirectInfo = (HashMap<String, Object>) extensions.get(IdentityConstants.SharedState.IdentityDirect.NAME);
				}
			} catch (ClassCastException e) {
				MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
						"IdentityState - Unable to fetch com.adobe.module.identity info from Hub State due to invalid format, expected Map");
			}
		}

		return !Utils.isNullOrEmpty(identityDirectInfo);
	}

}
