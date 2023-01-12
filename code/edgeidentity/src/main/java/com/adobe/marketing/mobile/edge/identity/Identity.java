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
import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.StringUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines the public APIs for the AEP Edge Identity extension.
 */
public class Identity {

	public static final Class<? extends Extension> EXTENSION = IdentityExtension.class;
	private static final long CALLBACK_TIMEOUT_MILLIS = 500L;

	private static final String LOG_SOURCE = "Identity";

	private Identity() {}

	/**
	 * Returns the version of the {@link Identity} extension
	 *
	 * @return The version as {@code String}
	 */
	@NonNull
	public static String extensionVersion() {
		return IdentityConstants.EXTENSION_VERSION;
	}

	/**
	 * Registers the extension with the Mobile SDK. This method should be called only once in your application class.
	 *
	 * @deprecated as of 2.0.0, use {@link MobileCore#registerExtensions(List, AdobeCallback)} with {@link Identity#EXTENSION} instead.
	 */
	@Deprecated
	@SuppressWarnings("deprecation")
	public static void registerExtension() {
		MobileCore.registerExtension(
			IdentityExtension.class,
			extensionError ->
				Log.error(
					LOG_TAG,
					LOG_SOURCE,
					"There was an error registering the Edge Identity extension: " + extensionError.getErrorName()
				)
		);
	}

	/**
	 * Returns the Experience Cloud ID. An empty string is returned if the Experience Cloud ID was previously cleared.
	 *
	 * @param callback {@link AdobeCallback} of {@code String} invoked with the Experience Cloud ID
	 *                 If an {@link AdobeCallbackWithError} is provided, an {@link AdobeError} can be returned in the
	 *                 eventuality of any error that occurred while getting the Experience Cloud ID
	 */
	public static void getExperienceCloudId(@NonNull final AdobeCallback<String> callback) {
		if (callback == null) {
			Log.debug(LOG_TAG, LOG_SOURCE, "Unexpected null callback, provide a callback to retrieve current ECID.");
			return;
		}

		final Event event = new Event.Builder(
			IdentityConstants.EventNames.IDENTITY_REQUEST_IDENTITY_ECID,
			EventType.EDGE_IDENTITY,
			EventSource.REQUEST_IDENTITY
		)
			.build();

		final AdobeCallbackWithError<Event> callbackWithError = new AdobeCallbackWithError<Event>() {
			@Override
			public void call(final Event responseEvent) {
				if (responseEvent == null || responseEvent.getEventData() == null) {
					returnError(callback, AdobeError.UNEXPECTED_ERROR);
					return;
				}

				final IdentityMap identityMap = IdentityMap.fromXDMMap(responseEvent.getEventData());

				if (identityMap == null) {
					Log.debug(
						LOG_TAG,
						LOG_SOURCE,
						"Failed to read IdentityMap from response event, invoking error callback with AdobeError.UNEXPECTED_ERROR"
					);
					returnError(callback, AdobeError.UNEXPECTED_ERROR);
					return;
				}

				final List<IdentityItem> ecidItems = identityMap.getIdentityItemsForNamespace(
					IdentityConstants.Namespaces.ECID
				);

				if (ecidItems == null || ecidItems.isEmpty() || ecidItems.get(0).getId() == null) {
					callback.call("");
				} else {
					callback.call(ecidItems.get(0).getId());
				}
			}

			@Override
			public void fail(final AdobeError adobeError) {
				returnError(callback, adobeError);
				Log.debug(
					LOG_TAG,
					LOG_SOURCE,
					String.format(
						"Failed to dispatch %s event: Error : %s.",
						IdentityConstants.EventNames.IDENTITY_REQUEST_IDENTITY_ECID,
						adobeError.getErrorName()
					)
				);
			}
		};

		MobileCore.dispatchEventWithResponseCallback(event, CALLBACK_TIMEOUT_MILLIS, callbackWithError);
	}

	/**
	 * Returns the identifiers in URL query parameter format for consumption in hybrid mobile applications.
	 * There is no leading &amp; or ? punctuation as the caller is responsible for placing the variables in their resulting URL in the correct locations.
	 * If an error occurs while retrieving the URL variables, the AdobeCallbackWithError is called with a null value and AdobeError instance.
	 * If AdobeCallback is provided then callback is not called in case of error.
	 * Otherwise, the encoded string is returned, for ex: "adobe_mc=TS%3DTIMESTAMP_VALUE%7CMCMID%3DYOUR_ECID%7CMCORGID%3D9YOUR_EXPERIENCE_CLOUD_ID"
	 * The {@code adobe_mc} attribute is an URL encoded list that contains:
	 *	<ul>
	 *		<li>TS: a timestamp taken when the request was made</li>
	 * 		<li>MCID: Experience Cloud ID (ECID)</li>
	 * 		<li>MCORGID: Experience Cloud Org ID</li>
	 * 	</ul>
	 *
	 * @param callback {@link AdobeCallback} of {@code String} invoked with a value containing the identifiers in query parameter format.
	 *     	           If an {@link AdobeCallbackWithError} is provided, an {@link AdobeError} can be returned in the
	 *	               eventuality of any error that occurred while getting the identifiers query string
	 */
	public static void getUrlVariables(@NonNull final AdobeCallback<String> callback) {
		if (callback == null) {
			Log.debug(
				LOG_TAG,
				LOG_SOURCE,
				"Unexpected null callback, provide a callback to retrieve current visitor identifiers (URLVariables) query string."
			);
			return;
		}

		final Event event = new Event.Builder(
			IdentityConstants.EventNames.IDENTITY_REQUEST_URL_VARIABLES,
			EventType.EDGE_IDENTITY,
			EventSource.REQUEST_IDENTITY
		)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(IdentityConstants.EventDataKeys.URL_VARIABLES, true);
					}
				}
			)
			.build();

		final AdobeCallbackWithError<Event> callbackWithError = new AdobeCallbackWithError<Event>() {
			@Override
			public void call(final Event responseEvent) {
				if (responseEvent == null || responseEvent.getEventData() == null) {
					returnError(callback, AdobeError.UNEXPECTED_ERROR);
					return;
				}

				final Map<String, Object> data = responseEvent.getEventData();
				final String urlVariableString = DataReader.optString(
					data,
					IdentityConstants.EventDataKeys.URL_VARIABLES,
					null
				);

				if (urlVariableString == null) {
					returnError(callback, AdobeError.UNEXPECTED_ERROR);
					return;
				}
				callback.call(urlVariableString);
			}

			@Override
			public void fail(final AdobeError adobeError) {
				returnError(callback, adobeError);
				Log.debug(
					LOG_TAG,
					LOG_SOURCE,
					String.format(
						"Failed to dispatch %s event: Error : %s.",
						IdentityConstants.EventNames.IDENTITY_REQUEST_URL_VARIABLES,
						adobeError.getErrorName()
					)
				);
			}
		};

		MobileCore.dispatchEventWithResponseCallback(event, CALLBACK_TIMEOUT_MILLIS, callbackWithError);
	}

	/**
	 * Updates the currently known {@link IdentityMap} within the SDK.
	 * The Identity extension will merge the received identifiers with the previously saved one in an additive manner,
	 * no identifiers will be removed using this API.
	 * Identifiers which have an empty {@code id} or empty {@code namespace} are not allowed and are ignored.
	 *
	 * @param identityMap The identifiers to add or update.
	 */
	public static void updateIdentities(@NonNull final IdentityMap identityMap) {
		if (identityMap == null || identityMap.isEmpty()) {
			Log.debug(LOG_TAG, LOG_SOURCE, "Unable to updateIdentities, IdentityMap is null or empty");
			return;
		}

		final Event updateIdentitiesEvent = new Event.Builder(
			IdentityConstants.EventNames.UPDATE_IDENTITIES,
			EventType.EDGE_IDENTITY,
			EventSource.UPDATE_IDENTITY
		)
			.setEventData(identityMap.asXDMMap(false))
			.build();

		MobileCore.dispatchEvent(updateIdentitiesEvent);
	}

	/**
	 * Removes the identity from the stored client-side {@link IdentityMap}. The Identity extension will stop sending this identifier.
	 * This does not clear the identifier from the User Profile Graph.
	 *
	 * @param item      the {@link IdentityItem} to remove.
	 * @param namespace The namespace of the identity to remove.
	 */
	public static void removeIdentity(@NonNull final IdentityItem item, @NonNull final String namespace) {
		if (StringUtils.isNullOrEmpty(namespace)) {
			Log.debug(LOG_TAG, LOG_SOURCE, "Unable to removeIdentity, namespace is null or empty");
			return;
		}

		if (item == null) {
			Log.debug(LOG_TAG, LOG_SOURCE, "Unable to removeIdentity, IdentityItem is null");
			return;
		}

		IdentityMap identityMap = new IdentityMap();
		identityMap.addItem(item, namespace);

		final Event removeIdentitiesEvent = new Event.Builder(
			IdentityConstants.EventNames.REMOVE_IDENTITIES,
			EventType.EDGE_IDENTITY,
			EventSource.REMOVE_IDENTITY
		)
			.setEventData(identityMap.asXDMMap(false))
			.build();
		MobileCore.dispatchEvent(removeIdentitiesEvent);
	}

	/**
	 * Returns all identifiers, including customer identifiers which were previously added.
	 *
	 * @param callback {@link AdobeCallback} invoked with the current {@link IdentityMap}
	 *                 If an {@link AdobeCallbackWithError} is provided, an {@link AdobeError} can be returned in the
	 *                 eventuality of any error that occurred while getting the stored identities.
	 */
	public static void getIdentities(@NonNull final AdobeCallback<IdentityMap> callback) {
		if (callback == null) {
			Log.debug(
				LOG_TAG,
				LOG_SOURCE,
				"Unexpected null callback, provide a callback to retrieve current IdentityMap."
			);
			return;
		}

		final Event event = new Event.Builder(
			IdentityConstants.EventNames.REQUEST_IDENTITIES,
			EventType.EDGE_IDENTITY,
			EventSource.REQUEST_IDENTITY
		)
			.build();

		final AdobeCallbackWithError<Event> callbackWithError = new AdobeCallbackWithError<Event>() {
			@Override
			public void call(final Event responseEvent) {
				if (responseEvent == null || responseEvent.getEventData() == null) {
					returnError(callback, AdobeError.UNEXPECTED_ERROR);
					return;
				}

				final IdentityMap identityMap = IdentityMap.fromXDMMap(responseEvent.getEventData());

				if (identityMap == null) {
					Log.debug(
						LOG_TAG,
						LOG_SOURCE,
						"Failed to read IdentityMap from response event, invoking error callback with AdobeError.UNEXPECTED_ERROR"
					);
					returnError(callback, AdobeError.UNEXPECTED_ERROR);
					return;
				}

				callback.call(identityMap);
			}

			@Override
			public void fail(final AdobeError adobeError) {
				returnError(callback, adobeError);
				Log.debug(
					LOG_TAG,
					LOG_SOURCE,
					String.format(
						"Failed to dispatch %s event: Error : %s.",
						IdentityConstants.EventNames.REQUEST_IDENTITIES,
						adobeError.getErrorName()
					)
				);
			}
		};

		MobileCore.dispatchEventWithResponseCallback(event, CALLBACK_TIMEOUT_MILLIS, callbackWithError);
	}

	/**
	 * When an {@link AdobeCallbackWithError} is provided, the fail method will be called with provided {@link AdobeError}.
	 *
	 * @param callback should not be null, should be instance of {@code AdobeCallbackWithError}
	 * @param error    the {@code AdobeError} returned back in the callback
	 */
	private static <T> void returnError(final AdobeCallback<T> callback, final AdobeError error) {
		if (callback == null) {
			return;
		}

		final AdobeCallbackWithError<T> adobeCallbackWithError = callback instanceof AdobeCallbackWithError
			? (AdobeCallbackWithError<T>) callback
			: null;

		if (adobeCallbackWithError != null) {
			adobeCallbackWithError.fail(error);
		}
	}
}
