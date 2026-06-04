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

import static com.adobe.marketing.mobile.edge.identity.IdentityConstants.LOG_TAG;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.SharedStateStatus;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.MapUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the business logic of this Identity extension
 */
class IdentityState {

	private static final String LOG_SOURCE = "IdentityState";

	private final IdentityStorageManager identityStorageManager;
	private IdentityProperties identityProperties;
	private boolean hasBooted;
	private final List<ProfileAttributeHandler> profileAttributeHandlers;

	IdentityState() {
		this(new IdentityStorageManager(ServiceProvider.getInstance().getDataStoreService()));
	}

	/**
	 * Loads the persisted identities (if any) into {@link #identityProperties}
	 */
	@VisibleForTesting
	IdentityState(final IdentityStorageManager identityStorageManager) {
		this.identityStorageManager = identityStorageManager;
		this.profileAttributeHandlers = List.of(new ProfileAttributeHandler.TimeZoneAttributeHandler(identityStorageManager));

		final IdentityProperties persistedProperties = identityStorageManager.loadPropertiesFromPersistence();
		this.identityProperties = (persistedProperties != null) ? persistedProperties : new IdentityProperties();
	}

	/**
	 * @return The current {@link IdentityProperties} for this identity state
	 */
	@NonNull IdentityProperties getIdentityProperties() {
		return identityProperties;
	}

	/**
	 * Completes init for this Identity extension.
	 * If no ECID is loaded from persistence (ideally meaning first launch), attempts to migrate existing ECID
	 * from the direct Identity Extension, either from its persisted store or from its shared state if the
	 * direct Identity extension is registered. If no ECID is found for migration, then a new ECID is generated.
	 * Stores the {@code identityProperties} once an ECID is set and creates the first shared state.
	 *
	 * @param callback {@link SharedStateCallback} used to get the EventHub and/or Identity direct shared state
	 *                 and create a shared state on the EventHub; should not be null
	 * @return True if the bootup is complete
	 */
	boolean bootupIfReady(final SharedStateCallback callback) {
		if (hasBooted) {
			return true;
		}

		// Reuse the ECID from Identity Direct (if registered) or generate new ECID on first launch
		if (identityProperties.getECID() == null) {
			// Wait for all extensions to be registered as forthcoming logic depends on Identity Direct state.
			// This is inferred via EventHub's shared state and is based on the assumption that EventHub
			// sets its state only when all the extensions are registered initially.
			final SharedStateResult eventHubStateResult = callback.getSharedState(
				IdentityConstants.SharedState.Hub.NAME,
				null
			);
			if (eventHubStateResult == null || eventHubStateResult.getStatus() != SharedStateStatus.SET) {
				return false;
			}

			// Attempt to get ECID from direct Identity persistence to migrate an existing ECID
			final ECID directIdentityEcid = identityStorageManager.loadEcidFromDirectIdentityPersistence();

			if (directIdentityEcid != null) {
				identityProperties.setECID(directIdentityEcid);
				Log.debug(
					LOG_TAG,
					LOG_SOURCE,
					"On bootup Loading ECID from direct Identity extension '" + directIdentityEcid + "'"
				);
			}
			// If direct Identity has no persisted ECID, check if direct Identity is registered with the SDK
			else if (isIdentityDirectRegistered(eventHubStateResult.getValue())) {
				// If the direct Identity extension is registered, attempt to get its shared state
				final SharedStateResult sharedStateResult = callback.getSharedState(
					IdentityConstants.SharedState.IdentityDirect.NAME,
					null
				);

				// If there is no direct Identity shared state, abort boot-up and try again when direct Identity shares its state
				if (sharedStateResult == null || sharedStateResult.getStatus() != SharedStateStatus.SET) {
					Log.debug(
						LOG_TAG,
						LOG_SOURCE,
						"On bootup direct Identity extension is registered, waiting for its state change."
					);
					return false;
				}

				final Map<String, Object> identityDirectSharedState = sharedStateResult.getValue();
				handleECIDFromIdentityDirect(EventUtils.getECID(identityDirectSharedState));
			}
			// Generate a new ECID as the direct Identity extension is not registered with the SDK and there was no direct Identity persisted ECID
			else {
				identityProperties.setECID(new ECID());
				Log.debug(
					LOG_TAG,
					LOG_SOURCE,
					"Generating new ECID on bootup '" + identityProperties.getECID().toString() + "'"
				);
			}

			identityStorageManager.savePropertiesToPersistence(identityProperties);
		}

		hasBooted = true;
		Log.debug(LOG_TAG, LOG_SOURCE, "Edge Identity has successfully booted up");
		callback.createXDMSharedState(identityProperties.toXDMData(), null);
		// Hydrate the profile-attributes shared state from persistence so it mirrors storage on launch.
		publishProfileAttributesSharedState(callback, null);

		return hasBooted;
	}

	/**
	 * Clears all identities and regenerates a new ECID value, then saves the new identities to persistence.
	 */
	void resetIdentifiers() {
		identityProperties = new IdentityProperties();
		identityProperties.setECID(new ECID());
		identityProperties.setECIDSecondary(null);
		identityStorageManager.savePropertiesToPersistence(identityProperties);
	}

	/**
	 * Update the customer identifiers by merging the passed in {@link IdentityMap} with the current identifiers present in {@link #identityProperties}.
	 *
	 * @param map the {@code IdentityMap} containing customer identifiers to add or update with the current customer identifiers
	 */
	void updateCustomerIdentifiers(final IdentityMap map) {
		identityProperties.updateCustomerIdentifiers(map);
		identityStorageManager.savePropertiesToPersistence(identityProperties);
	}

	/**
	 * Remove customer identifiers specified in passed in {@link IdentityMap} from the current identifiers present in {@link #identityProperties}.
	 *
	 * @param map the {@code IdentityMap} with items to remove from current identifiers
	 */
	void removeCustomerIdentifiers(final IdentityMap map) {
		identityProperties.removeCustomerIdentifiers(map);
		identityStorageManager.savePropertiesToPersistence(identityProperties);
	}

	/**
	 * This is the main entrypoint for handling ad ID changes. When an ad ID change is detected, it will:
	 * <ul>
	 *     <li>Update persistent storage with the new ad ID</li>
	 *	   <li>Share the XDM state</li>
	 * 	   <li>Dispatch consent event - only when ad ID changes from invalid/valid and vice versa</li>
	 * </ul>
	 *
	 * @param event the {@link Event} containing the advertising identifier
	 * @param callback {@link SharedStateCallback} used to create a shared state on the EventHub; should not be null
	 */
	void updateAdvertisingIdentifier(final Event event, final SharedStateCallback callback) {
		final String newAdId = EventUtils.getAdId(event);
		if (identityProperties == null) {
			identityProperties = new IdentityProperties();
		}
		String currentAdId = identityProperties.getAdId();
		if (currentAdId == null) {
			currentAdId = "";
		}

		// Check if ad ID has changed
		if (currentAdId.equals(newAdId)) {
			return; // Ad ID has not changed: no op
		}
		// Ad ID has changed:
		// Ad ID updated in local state first
		identityProperties.setAdId(newAdId);
		// Consent has changed
		if (newAdId.isEmpty() || currentAdId.isEmpty()) {
			dispatchAdIdConsentRequestEvent(
				newAdId.isEmpty() ? IdentityConstants.XDMKeys.Consent.NO : IdentityConstants.XDMKeys.Consent.YES
			);
		}

		// Save to persistence
		identityStorageManager.savePropertiesToPersistence(identityProperties);
		callback.createXDMSharedState(identityProperties.toXDMData(), event);
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
		identityStorageManager.savePropertiesToPersistence(identityProperties);
		Log.debug(
			LOG_TAG,
			LOG_SOURCE,
			"Identity direct ECID updated to '" + legacyEcid + "', updating the IdentityMap"
		);
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
			Log.debug(
				LOG_TAG,
				LOG_SOURCE,
				"Identity direct ECID '" + legacyEcid + "' " + "was migrated to Edge Identity, updating the IdentityMap"
			);
		} else { // opt-out scenario or an unexpected state for Identity direct, generate new ECID
			identityProperties.setECID(new ECID());
			Log.debug(
				LOG_TAG,
				LOG_SOURCE,
				"Identity direct ECID is null, generating new ECID '" +
				identityProperties.getECID() +
				"', updating the IdentityMap"
			);
		}
	}

	/**
	 * Check if the Identity direct extension is registered by checking the EventHub's shared state list of registered extensions.
	 *
	 * @param eventHubSharedState a {@code Map<String, Object} representing the shared state of eventhub
	 * @return true if the Identity direct extension is registered with the EventHub; false otherwise.
	 */
	private boolean isIdentityDirectRegistered(final Map<String, Object> eventHubSharedState) {
		if (eventHubSharedState == null) {
			return false;
		}

		final Map<String, Object> extensions = DataReader.optTypedMap(
			Object.class,
			eventHubSharedState,
			IdentityConstants.SharedState.Hub.EXTENSIONS,
			null
		);

		final Map<String, Object> identityDirectInfo = DataReader.optTypedMap(
			Object.class,
			extensions,
			IdentityConstants.SharedState.IdentityDirect.NAME,
			null
		);

		return !MapUtils.isNullOrEmpty(identityDirectInfo);
	}

	/**
	 * Construct the advertising identifier consent request event data using the provided consent value
	 * @param consentVal the consent value defined by {@link IdentityConstants.XDMKeys.Consent#YES}
	 *                   or {@link IdentityConstants.XDMKeys.Consent#NO}
	 * @return the event data for advertising identifier consent request
	 */
	private Map<String, Object> buildConsentAdIdRequestData(final String consentVal) {
		// build the map from the bottom level -> up
		Map<String, Object> consentValMap = new HashMap<>();
		consentValMap.put(IdentityConstants.XDMKeys.Consent.VAL, consentVal);
		consentValMap.put(IdentityConstants.XDMKeys.Consent.ID_TYPE, IdentityConstants.Namespaces.GAID);

		Map<String, Object> adIDMap = new HashMap<>();
		adIDMap.put(IdentityConstants.XDMKeys.Consent.AD_ID, consentValMap);

		Map<String, Object> consentMap = new HashMap<>();
		consentMap.put(IdentityConstants.XDMKeys.Consent.CONSENTS, adIDMap);
		return consentMap;
	}

	/**
	 * Dispatches a consent request event with the consent value passed
	 *
	 * @param consentVal the consent value to send in the event, from
	 * {@link IdentityConstants.XDMKeys.Consent#YES}/{@link IdentityConstants.XDMKeys.Consent#NO}
	 */
	private void dispatchAdIdConsentRequestEvent(final String consentVal) {
		Map<String, Object> consentData = buildConsentAdIdRequestData(consentVal);

		final Event consentEvent = new Event.Builder(
			IdentityConstants.EventNames.CONSENT_UPDATE_REQUEST_AD_ID,
			EventType.CONSENT,
			EventSource.UPDATE_CONSENT
		)
			.setEventData(consentData)
			.build();

		MobileCore.dispatchEvent(consentEvent);
	}

	/**
	 * Collector layer for profile-attribute update requests. Runs every {@link ProfileAttributeHandler}
	 * against the incoming event (each owns its own dedup + persistence) and dispatches a single
	 * collated {@code profile.updateAttributes} Edge event with whatever changed. Persistence writes
	 * happen <b>before</b> the dispatch so that an event dropped due to collect consent still leaves
	 * the pending value in storage, enabling a later re-sync.
	 *
	 * <p>When anything changed, the profile-attributes shared state is also (re)published from
	 * persistence so it stays in sync with storage.
	 *
	 * @param event the profile attributes update {@link Event}
	 * @param callback {@link SharedStateCallback} used to publish the profile-attributes shared state
	 */
	void updateProfileAttributes(final Event event, final SharedStateCallback callback) {
		final Map<String, Object> changed = new HashMap<>();
		for (final ProfileAttributeHandler handler : profileAttributeHandlers) {
			handler.collectFromUpdate(event, changed);
		}

		if (changed.isEmpty()) {
			return;
		}

		// Persistence just changed: keep the shared state in sync, then dispatch only the changes.
		publishProfileAttributesSharedState(callback, event);
		dispatchProfileAttributesEdgeEvent(changed);
	}

	/**
	 * Handles an Edge Consent response. The upstream consent logic decides whether a re-sync is
	 * needed and flags it via the {@link IdentityConstants.EventDataKeys#COLLECT_CONSENT_RESYNC_REQUIRED}
	 * boolean on the event. When that flag is {@code true}, the last stored profile attributes are
	 * replayed to the Edge Network from the SDK's persisted state (never a device/OS read); otherwise
	 * the event is ignored.
	 *
	 * @param event the edge consent response {@link Event}
	 */
	void handleCollectConsentResponse(final Event event) {
		if (!EventUtils.isCollectConsentResyncRequired(event)) {
			return;
		}

		// Replay the persisted state (the SDK's stored values), never a device/OS read. Each handler
		// contributes its stored value; the collator dispatches one unified event (no-op if empty).
		final Map<String, Object> data = new HashMap<>();
		for (final ProfileAttributeHandler handler : profileAttributeHandlers) {
			handler.collectFromStorage(data);
		}
		dispatchProfileAttributesEdgeEvent(data);
	}

	/**
	 * Clears all stored profile attributes and updates the shared state to match (now empty). Invoked
	 * on reset so that the next update re-syncs the attributes against the newly generated ECID.
	 *
	 * @param callback {@link SharedStateCallback} used to publish the updated (empty) shared state
	 * @param event the reset {@link Event} the shared state is versioned at
	 */
	void clearProfileAttributes(final SharedStateCallback callback, final Event event) {
		identityStorageManager.clearProfileAttributes();
		publishProfileAttributesSharedState(callback, event);
	}

	/**
	 * Dispatches a single generic {@code profile.updateAttributes} {@link EventType#EDGE} /
	 * {@link EventSource#REQUEST_CONTENT} event whose {@code data} is the collated contribution of all
	 * {@link ProfileAttributeHandler}s (currently only the timezone handler). No event is
	 * dispatched when {@code data} is empty. The payload shape is
	 * {@code {"xdm": {"eventType": "profile.updateAttributes"}, "data": { ...collated attributes... }}};
	 * the Edge extension enriches it with {@code _id}, {@code timestamp}, implementationDetails, and
	 * identityMap.
	 *
	 * @param data the collated profile attributes payload
	 */
	private void dispatchProfileAttributesEdgeEvent(final Map<String, Object> data) {
		if (MapUtils.isNullOrEmpty(data)) {
			Log.debug(LOG_TAG, LOG_SOURCE, "No profile attributes to sync, skipping Edge event dispatch.");
			return;
		}

		final Map<String, Object> xdm = new HashMap<>();
		xdm.put(
			IdentityConstants.ProfileAttributes.EVENT_TYPE,
			IdentityConstants.ProfileAttributes.XDM_EVENT_TYPE_UPDATE_ATTRIBUTES
		);

		final Map<String, Object> eventData = new HashMap<>();
		eventData.put(IdentityConstants.ProfileAttributes.XDM, xdm);
		eventData.put(IdentityConstants.ProfileAttributes.DATA, data);

		final Event edgeEvent = new Event.Builder(
			IdentityConstants.EventNames.UPDATE_PROFILE_ATTRIBUTES,
			EventType.EDGE,
			EventSource.REQUEST_CONTENT
		)
			.setEventData(eventData)
			.build();

		MobileCore.dispatchEvent(edgeEvent);
	}

	/**
	 * Builds the current profile attributes from persistence (collating every {@link
	 * ProfileAttributeHandler}) and publishes them as the extension's regular (non-XDM) shared state,
	 * keeping it in sync with storage. An empty map is published when nothing is stored (e.g. after
	 * reset).
	 *
	 * @param callback {@link SharedStateCallback} used to create the shared state
	 * @param event the {@link Event} the shared state is versioned at; {@code null} for the next version
	 */
	private void publishProfileAttributesSharedState(final SharedStateCallback callback, final Event event) {
		final Map<String, Object> currentAttributes = new HashMap<>();
		for (final ProfileAttributeHandler handler : profileAttributeHandlers) {
			handler.collectFromStorage(currentAttributes);
		}
		callback.createSharedState(currentAttributes, event);
	}
}
