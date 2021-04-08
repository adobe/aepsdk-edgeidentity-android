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
	 * Completes init for this Identity extension.
	 * Attempts to load the already persisted identities from persistence into {@link #identityProperties}
	 * If no ECID is loaded from persistence (ideally meaning first launch), attempts to migrate existing ECID
	 * from the direct Identity Extension, either from its persisted store or from its shared state if the
	 * direct Identity extension is registered. If no ECID is found for migration, then a new ECID is generated.
	 * Stores the {@code identityProperties} once an ECID is set and creates the first shared state.
	 *
	 * @param callback {@link SharedStateCallback} used to get the EventHub and/or Identity direct shared state
	 *             		and create a shared state on the EventHub; should not be null
	 * @return True if the bootup is complete
	 */
	boolean bootupIfReady(final SharedStateCallback callback) {
		if (hasBooted) {
			return true;
		}

		// Load properties from local storage
		identityProperties = IdentityStorageService.loadPropertiesFromPersistence();

		if (identityProperties == null) {
			identityProperties = new IdentityProperties();
		}

		// Reuse the ECID from Identity Direct (if registered) or generate new ECID on first launch
		if (identityProperties.getECID() == null) {

			// Attempt to get ECID from direct Identity persistence to migrate an existing ECID
			final ECID directIdentityEcid = IdentityStorageService.loadEcidFromDirectIdentityPersistence();

			if (directIdentityEcid != null) {
				identityProperties.setECID(directIdentityEcid);
				MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
							   "IdentityState -  On bootup Loading ECID from direct Identity extension '" + directIdentityEcid + "'");
			}

			// If direct Identity has no persisted ECID, check if direct Identity is registered with the SDK
			else if (isIdentityDirectRegistered(callback)) {
				final Map<String, Object> identityDirectSharedState = callback.getSharedState(
							IdentityConstants.SharedState.IdentityDirect.NAME, null);

				// If the direct Identity extension is registered, attempt to get its shared state
				if (identityDirectSharedState != null) { // identity direct shared state is set
					handleECIDFromIdentityDirect(EventUtils.getECID(identityDirectSharedState));
				}
				// If there is no direct Identity shared state, abort boot-up and try again when direct Identity shares its state
				else {
					MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
								   "IdentityState - On bootup direct Identity extension is registered, waiting for its state change.");
					return false; // If no ECID to migrate but Identity direct is registered, wait for Identity direct shared state
				}
			}
			// Generate a new ECID as the direct Identity extension is not registered with the SDK and there was no direct Identity persisted ECID
			else {
				identityProperties.setECID(new ECID());
				MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
							   "IdentityState - Generating new ECID on bootup '" + identityProperties.getECID().toString() + "'");
			}

			IdentityStorageService.savePropertiesToPersistence(identityProperties);
		}

		hasBooted = true;
		MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "IdentityState - Edge Identity has successfully booted up");
		callback.setXDMSharedEventState(identityProperties.toXDMData(false), null);

		return hasBooted;
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
	 * This method is called when the primary Edge ECID is null and the Identity Direct shared state has been updated
	 * (install scenario when Identity Direct is registered).
	 * Sets the {@code legacyEcid} as primary ECID when not null, otherwise generates a new ECID.
	 *
	 * @param legacyEcid the current ECID from the direct Identity extension
	 */
	private void handleECIDFromIdentityDirect(final ECID legacyEcid) {
		if (legacyEcid != null) {
			identityProperties.setECID(legacyEcid); // migrate legacy ECID
			MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
						   "IdentityState - Identity direct ECID '" + legacyEcid + "' was migrated to Edge Identity, updating the IdentityMap");

		} else { // opt-out scenario or an unexpected state for Identity direct, generate new ECID
			identityProperties.setECID(new ECID());
			MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
						   "IdentityState - Identity direct ECID is null, generating new ECID '" + identityProperties.getECID() +
						   "', updating the IdentityMap");
		}
	}

	/**
	 * Check if the Identity direct extension is registered by checking the EventHub's shared state list of registered extensions.
	 * @param callback the {@link SharedStateCallback} to be used for fetching the EventHub Shared state; should not be null
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
