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
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.SharedStateResolution;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.SharedStateStatus;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.StringUtils;
import com.adobe.marketing.mobile.util.TimeUtils;
import java.util.HashMap;
import java.util.Map;

class IdentityExtension extends Extension {

	private static final String LOG_SOURCE = "IdentityExtension";

	/**
	 * A {@code SharedStateCallback} to retrieve the last set state of an extension and to
	 * create an XDM state at the event provided.
	 */
	private final SharedStateCallback sharedStateHandle = new SharedStateCallback() {
		@Override
		public SharedStateResult getSharedState(final String stateOwner, final Event event) {
			return getApi().getSharedState(stateOwner, event, false, SharedStateResolution.LAST_SET);
		}

		@Override
		public void createXDMSharedState(final Map<String, Object> state, final Event event) {
			getApi().createXDMSharedState(state, event);
		}
	};

	private final IdentityState state;

	/**
	 * Constructor.
	 * Invoked on the background thread owned by an extension container that manages this extension.
	 *
	 * @param extensionApi {@link ExtensionApi} instance
	 */
	protected IdentityExtension(ExtensionApi extensionApi) {
		this(extensionApi, new IdentityState());
	}

	@VisibleForTesting
	IdentityExtension(final ExtensionApi extensionApi, final IdentityState state) {
		super(extensionApi);
		this.state = state;
	}

	@NonNull
	@Override
	protected String getName() {
		return IdentityConstants.EXTENSION_NAME;
	}

	@NonNull
	@Override
	protected String getFriendlyName() {
		return IdentityConstants.EXTENSION_FRIENDLY_NAME;
	}

	@NonNull
	@Override
	protected String getVersion() {
		return IdentityConstants.EXTENSION_VERSION;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The following listeners are registered during this extension's registration.
	 * <ul>
	 *     <li> EventType {@link EventType#GENERIC_IDENTITY} and EventSource {@link EventSource#REQUEST_CONTENT}</li>
	 *     <li> EventType {@link EventType#EDGE_IDENTITY} and EventSource {@link EventSource#REQUEST_IDENTITY}</li>
	 *     <li> EventType {@link EventType#EDGE_IDENTITY} and EventSource {@link EventSource#UPDATE_IDENTITY}</li>
	 *     <li> EventType {@link EventType#EDGE_IDENTITY} and EventSource {@link EventSource#REMOVE_IDENTITY}</li>
	 *     <li> EventType {@link EventType#EDGE_IDENTITY} and EventSource {@link EventSource#REQUEST_CONTENT}</li>
	 *     <li> EventType {@link EventType#GENERIC_IDENTITY} and EventSource {@link EventSource#REQUEST_RESET}</li>
	 *     <li> EventType {@link EventType#HUB} and EventSource {@link EventSource#SHARED_STATE}</li>
	 * </ul>
	 * </p>
	 */
	@Override
	protected void onRegistered() {
		super.onRegistered();

		// GENERIC_IDENTITY event listeners
		getApi()
			.registerEventListener(EventType.GENERIC_IDENTITY, EventSource.REQUEST_CONTENT, this::handleRequestContent);

		getApi().registerEventListener(EventType.GENERIC_IDENTITY, EventSource.REQUEST_RESET, this::handleRequestReset);

		// EDGE_IDENTITY event listeners
		getApi()
			.registerEventListener(EventType.EDGE_IDENTITY, EventSource.REQUEST_IDENTITY, this::handleRequestIdentity);

		getApi()
			.registerEventListener(EventType.EDGE_IDENTITY, EventSource.UPDATE_IDENTITY, this::handleUpdateIdentities);

		getApi()
			.registerEventListener(EventType.EDGE_IDENTITY, EventSource.REMOVE_IDENTITY, this::handleRemoveIdentity);

		// HUB shared state event listener
		getApi().registerEventListener(EventType.HUB, EventSource.SHARED_STATE, this::handleIdentityDirectECIDUpdate);
	}

	@Override
	public boolean readyForEvent(@NonNull Event event) {
		if (!state.bootupIfReady(sharedStateHandle)) return false;

		// Get url variables request depends on Configuration shared state
		// Wait for configuration state to be set before processing such an event.
		if (EventUtils.isGetUrlVariablesRequestEvent(event)) {
			final SharedStateResult configurationStateResult = sharedStateHandle.getSharedState(
				IdentityConstants.SharedState.Configuration.NAME,
				event
			);

			return (configurationStateResult != null && configurationStateResult.getStatus() == SharedStateStatus.SET);
		}

		return true;
	}

	/**
	 * Handles events requesting for identifiers. Dispatches a response event containing requested identifiers.
	 * @param event the identity request event
	 */
	void handleRequestIdentity(@NonNull final Event event) {
		if (EventUtils.isGetUrlVariablesRequestEvent(event)) {
			handleUrlVariablesRequest(event);
		} else {
			handleGetIdentifiersRequest(event);
		}
	}

	/**
	 * Handles events requesting for formatted and encoded identifiers url for hybrid apps.
	 *
	 * @param event the identity request {@link Event}
	 */
	void handleUrlVariablesRequest(@NonNull final Event event) {
		final SharedStateResult configSharedStateResult = sharedStateHandle.getSharedState(
			IdentityConstants.SharedState.Configuration.NAME,
			event
		);

		final Map<String, Object> configurationState = configSharedStateResult != null
			? configSharedStateResult.getValue()
			: null;

		final String orgId = EventUtils.getOrgId(configurationState);

		if (StringUtils.isNullOrEmpty(orgId)) {
			handleUrlVariableResponse(
				event,
				null,
				"Cannot process getUrlVariables request Identity event, Experience Cloud Org ID not found in configuration."
			);
			return;
		}

		final ECID ecid = state.getIdentityProperties().getECID();
		final String ecidString = ecid != null ? ecid.toString() : null;

		if (StringUtils.isNullOrEmpty(ecidString)) {
			handleUrlVariableResponse(
				event,
				null,
				"Cannot process getUrlVariables request Identity event, ECID not found."
			);
			return;
		}

		final String urlVariablesString = URLUtils.generateURLVariablesPayload(
			String.valueOf(TimeUtils.getUnixTimeInSeconds()),
			ecidString,
			orgId
		);

		handleUrlVariableResponse(event, urlVariablesString);
	}

	/**
	 * Handles response event after processing the url variables request.
	 *
	 * @param event the identity request {@link Event}
	 * @param urlVariables {@link String} representing the urlVariables encoded string
	 */
	private void handleUrlVariableResponse(@NonNull final Event event, final String urlVariables) {
		handleUrlVariableResponse(event, urlVariables, null);
	}

	/**
	 * Handles response event after processing the url variables request.
	 *
	 * @param event the identity request {@link Event}
	 * @param urlVariables {@link String} representing the urlVariables encoded string
	 * @param errorMsg {@link String} representing error encountered while generating the urlVariables string
	 */
	private void handleUrlVariableResponse(
		@NonNull final Event event,
		final String urlVariables,
		final String errorMsg
	) {
		Event responseEvent = new Event.Builder(
			IdentityConstants.EventNames.IDENTITY_RESPONSE_URL_VARIABLES,
			EventType.EDGE_IDENTITY,
			EventSource.RESPONSE_IDENTITY
		)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(IdentityConstants.EventDataKeys.URL_VARIABLES, urlVariables);
					}
				}
			)
			.inResponseToEvent(event)
			.build();

		if (StringUtils.isNullOrEmpty(urlVariables) && !StringUtils.isNullOrEmpty(errorMsg)) {
			Log.warning(LOG_TAG, LOG_SOURCE, errorMsg);
		}

		getApi().dispatch(responseEvent);
	}

	/**
	 * Handles update identity requests to add/update customer identifiers.
	 *
	 * @param event the edge update identity {@link Event}
	 */
	void handleUpdateIdentities(@NonNull final Event event) {
		final Map<String, Object> eventData = event.getEventData();

		if (eventData == null) {
			Log.trace(LOG_TAG, LOG_SOURCE, "Cannot update identifiers, event data is null.");
			return;
		}

		final IdentityMap map = IdentityMap.fromXDMMap(eventData);

		if (map == null) {
			Log.debug(
				LOG_TAG,
				LOG_SOURCE,
				"Failed to update identifiers as no identifiers were found in the event data."
			);
			return;
		}

		state.updateCustomerIdentifiers(map);
		shareIdentityXDMSharedState(event);
	}

	/**
	 * Handles remove identity requests to remove customer identifiers.
	 *
	 * @param event the edge remove identity request {@link Event}
	 */
	void handleRemoveIdentity(@NonNull final Event event) {
		final Map<String, Object> eventData = event.getEventData();

		if (eventData == null) {
			Log.trace(LOG_TAG, LOG_SOURCE, "Cannot remove identifiers, event data is null.");
			return;
		}

		final IdentityMap map = IdentityMap.fromXDMMap(eventData);

		if (map == null) {
			Log.debug(
				LOG_TAG,
				LOG_SOURCE,
				"Failed to remove identifiers as no identifiers were found in the event data."
			);
			return;
		}

		state.removeCustomerIdentifiers(map);
		shareIdentityXDMSharedState(event);
	}

	/**
	 * Handles events requesting for identifiers. Dispatches response event containing the identifiers. Called by listener registered with event hub.
	 *
	 * @param event the identity request {@link Event}
	 */
	private void handleGetIdentifiersRequest(@NonNull final Event event) {
		final Map<String, Object> xdmData = state.getIdentityProperties().toXDMData(false);
		final Event responseEvent = new Event.Builder(
			IdentityConstants.EventNames.IDENTITY_RESPONSE_CONTENT_ONE_TIME,
			EventType.EDGE_IDENTITY,
			EventSource.RESPONSE_IDENTITY
		)
			.setEventData(xdmData)
			.inResponseToEvent(event)
			.build();

		getApi().dispatch(responseEvent);
	}

	/**
	 * Handles Edge Identity request reset events.
	 *
	 * @param event the identity request reset {@link Event}
	 */
	void handleRequestReset(@NonNull final Event event) {
		state.resetIdentifiers();
		shareIdentityXDMSharedState(event);

		// dispatch reset complete event
		final Event responseEvent = new Event.Builder(
			IdentityConstants.EventNames.RESET_IDENTITIES_COMPLETE,
			EventType.EDGE_IDENTITY,
			EventSource.RESET_COMPLETE
		)
			.inResponseToEvent(event)
			.build();

		getApi().dispatch(responseEvent);
	}

	/**
	 * Handles ECID sync between Edge Identity and Identity Direct, usually called when Identity Direct's shared state is updated.
	 *
	 * @param event the shared state update {@link Event}
	 */
	void handleIdentityDirectECIDUpdate(@NonNull final Event event) {
		if (!EventUtils.isSharedStateUpdateFor(IdentityConstants.SharedState.IdentityDirect.NAME, event)) {
			return;
		}

		final SharedStateResult identitySharedStateResult = sharedStateHandle.getSharedState(
			IdentityConstants.SharedState.IdentityDirect.NAME,
			event
		);

		final Map<String, Object> identityState = (identitySharedStateResult != null)
			? identitySharedStateResult.getValue()
			: null;

		if (identityState == null) {
			return;
		}

		final ECID legacyEcid = EventUtils.getECID(identityState);

		if (state.updateLegacyExperienceCloudId(legacyEcid)) {
			shareIdentityXDMSharedState(event);
		}
	}

	/**
	 * Handles events to set the advertising identifier. Called by listener registered with event hub.
	 *
	 * @param event the {@link Event} containing advertising identifier data
	 */
	void handleRequestContent(@NonNull final Event event) {
		if (!EventUtils.isAdIdEvent(event)) {
			return;
		}
		// Doesn't need event dispatcher because MobileCore can be called directly
		state.updateAdvertisingIdentifier(event, sharedStateHandle);
	}

	/**
	 * Fetches the latest Identity properties and shares the XDMSharedState.
	 *
	 * @param event the {@link Event} that triggered the XDM shared state change
	 */
	private void shareIdentityXDMSharedState(final Event event) {
		sharedStateHandle.createXDMSharedState(state.getIdentityProperties().toXDMData(false), event);
	}
}
