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
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.SharedStateResolution;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.util.StringUtils;
import java.util.HashMap;
import java.util.Map;

class IdentityExtension extends Extension {

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
	 *     <li> EventType {@link IdentityConstants.EventType#GENERIC_IDENTITY} and EventSource {@link IdentityConstants.EventSource#REQUEST_CONTENT}</li>
	 *     <li> EventType {@link IdentityConstants.EventType#EDGE_IDENTITY} and EventSource {@link IdentityConstants.EventSource#REQUEST_IDENTITY}</li>
	 *     <li> EventType {@link IdentityConstants.EventType#EDGE_IDENTITY} and EventSource {@link IdentityConstants.EventSource#UPDATE_IDENTITY}</li>
	 *     <li> EventType {@link IdentityConstants.EventType#EDGE_IDENTITY} and EventSource {@link IdentityConstants.EventSource#REMOVE_IDENTITY}</li>
	 *     <li> EventType {@link IdentityConstants.EventType#EDGE_IDENTITY} and EventSource {@link IdentityConstants.EventSource#REQUEST_CONTENT}</li>
	 *     <li> EventType {@link IdentityConstants.EventType#GENERIC_IDENTITY} and EventSource {@link IdentityConstants.EventSource#REQUEST_RESET}</li>
	 *     <li> EventType {@link IdentityConstants.EventType#HUB} and EventSource {@link IdentityConstants.EventSource#SHARED_STATE}</li>
	 * </ul>
	 * </p>
	 */
	@Override
	protected void onRegistered() {
		// GENERIC_IDENTITY event listeners
		getApi()
			.registerEventListener(
				IdentityConstants.EventType.GENERIC_IDENTITY,
				IdentityConstants.EventSource.REQUEST_CONTENT,
				this::handleEvent
			);

		getApi()
			.registerEventListener(
				IdentityConstants.EventType.GENERIC_IDENTITY,
				IdentityConstants.EventSource.REQUEST_RESET,
				this::handleEvent
			);

		// EDGE_IDENTITY event listeners
		getApi()
			.registerEventListener(
				IdentityConstants.EventType.EDGE_IDENTITY,
				IdentityConstants.EventSource.REQUEST_IDENTITY,
				this::handleEvent
			);

		getApi()
			.registerEventListener(
				IdentityConstants.EventType.EDGE_IDENTITY,
				IdentityConstants.EventSource.UPDATE_IDENTITY,
				this::handleEvent
			);

		getApi()
			.registerEventListener(
				IdentityConstants.EventType.EDGE_IDENTITY,
				IdentityConstants.EventSource.REMOVE_IDENTITY,
				this::handleEvent
			);

		// HUB shared state event listener
		getApi()
			.registerEventListener(
				IdentityConstants.EventType.HUB,
				IdentityConstants.EventSource.SHARED_STATE,
				this::handleEvent
			);
	}

	@Override
	public boolean readyForEvent(@NonNull Event event) {
		// Check if we are already booted
		if (state.hasBooted()) return true;

		// Attempt to boot
		return state.bootupIfReady(sharedStateHandle);
	}

	/**
	 * Responsible for handling the incoming events that this extension is registered for.
	 */
	@VisibleForTesting
	void handleEvent(@NonNull final Event event) {
		if (EventUtils.isRequestIdentityEvent(event)) {
			if (EventUtils.isGetUrlVariablesRequestEvent(event)) {
				handleUrlVariablesRequest(event);
			} else {
				handleIdentityRequest(event);
			}
		} else if (EventUtils.isRequestContentEvent(event)) {
			handleRequestContent(event);
		} else if (EventUtils.isUpdateIdentityEvent(event)) {
			handleUpdateIdentities(event);
		} else if (EventUtils.isRemoveIdentityEvent(event)) {
			handleRemoveIdentity(event);
		} else if (EventUtils.isRequestResetEvent(event)) {
			handleRequestReset(event);
		} else if (EventUtils.isSharedStateUpdateFor(IdentityConstants.SharedState.IdentityDirect.NAME, event)) {
			handleIdentityDirectECIDUpdate(event);
		}
	}

	/**
	 * Handles events requesting for formatted and encoded identifiers url for hybrid apps.
	 *
	 * @param event the identity request {@link Event}
	 */
	private void handleUrlVariablesRequest(final Event event) {
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
				"IdentityExtension - Cannot process getUrlVariables request Identity event, Experience Cloud Org ID not found in configuration."
			);
			return;
		}

		final ECID ecid = state.getIdentityProperties().getECID();
		final String ecidString = ecid != null ? ecid.toString() : null;

		if (StringUtils.isNullOrEmpty(ecidString)) {
			handleUrlVariableResponse(
				event,
				null,
				"IdentityExtension - Cannot process getUrlVariables request Identity event, ECID not found."
			);
			return;
		}

		final String urlVariablesString = URLUtils.generateURLVariablesPayload(
			String.valueOf(Utils.getUnixTimeInSeconds()),
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
	private void handleUrlVariableResponse(final Event event, final String urlVariables) {
		handleUrlVariableResponse(event, urlVariables, null);
	}

	/**
	 * Handles response event after processing the url variables request.
	 *
	 * @param event the identity request {@link Event}
	 * @param urlVariables {@link String} representing the urlVariables encoded string
	 * @param errorMsg {@link String} representing error encountered while generating the urlVariables string
	 */
	private void handleUrlVariableResponse(final Event event, final String urlVariables, final String errorMsg) {
		Event responseEvent = new Event.Builder(
			IdentityConstants.EventNames.IDENTITY_RESPONSE_URL_VARIABLES,
			IdentityConstants.EventType.EDGE_IDENTITY,
			IdentityConstants.EventSource.RESPONSE_IDENTITY
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
			MobileCore.log(LoggingMode.WARNING, LOG_TAG, errorMsg);
		}

		getApi().dispatch(responseEvent);
	}

	/**
	 * Handles update identity requests to add/update customer identifiers.
	 *
	 * @param event the edge update identity {@link Event}
	 */
	private void handleUpdateIdentities(final Event event) {
		final Map<String, Object> eventData = event.getEventData(); // do not need to null check on eventData, as they are done on listeners
		final IdentityMap map = IdentityMap.fromXDMMap(eventData);

		if (map == null) {
			MobileCore.log(
				LoggingMode.DEBUG,
				LOG_TAG,
				"IdentityExtension - Failed to update identifiers as no identifiers were found in the event data."
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
	private void handleRemoveIdentity(final Event event) {
		final Map<String, Object> eventData = event.getEventData(); // do not need to null check on eventData, as they are done on listeners
		final IdentityMap map = IdentityMap.fromXDMMap(eventData);

		if (map == null) {
			MobileCore.log(
				LoggingMode.DEBUG,
				LOG_TAG,
				"IdentityExtension - Failed to remove identifiers as no identifiers were found in the event data."
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
	private void handleIdentityRequest(final Event event) {
		Map<String, Object> xdmData = state.getIdentityProperties().toXDMData(false);
		Event responseEvent = new Event.Builder(
			IdentityConstants.EventNames.IDENTITY_RESPONSE_CONTENT_ONE_TIME,
			IdentityConstants.EventType.EDGE_IDENTITY,
			IdentityConstants.EventSource.RESPONSE_IDENTITY
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
	private void handleRequestReset(final Event event) {
		state.resetIdentifiers();
		shareIdentityXDMSharedState(event);

		// dispatch reset complete event
		final Event responseEvent = new Event.Builder(
			IdentityConstants.EventNames.RESET_IDENTITIES_COMPLETE,
			IdentityConstants.EventType.EDGE_IDENTITY,
			IdentityConstants.EventSource.RESET_COMPLETE
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
	private void handleIdentityDirectECIDUpdate(final Event event) {
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
	private void handleRequestContent(final Event event) {
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
