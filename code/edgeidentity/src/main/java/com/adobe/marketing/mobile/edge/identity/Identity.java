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

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionError;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;

import java.util.List;

import static com.adobe.marketing.mobile.edge.identity.IdentityConstants.LOG_TAG;

/**
 * Defines the public APIs for the AEP Edge Identity extension.
 */
public class Identity {

	private Identity() {}

	/**
	 * Returns the version of the {@link Identity} extension
	 *
	 * @return The version as {@code String}
	 */
	public static String extensionVersion() {
		return IdentityConstants.EXTENSION_VERSION;
	}

	/**
	 * Registers the extension with the Mobile SDK. This method should be called only once in your application class.
	 */
	public static void registerExtension() {
		MobileCore.registerExtension(IdentityExtension.class, new ExtensionErrorCallback<ExtensionError>() {
			@Override
			public void error(ExtensionError extensionError) {
				MobileCore.log(LoggingMode.ERROR, LOG_TAG,
							   "Identity - There was an error registering the Edge Identity extension: " + extensionError.getErrorName());
			}
		});
	}

	/**
	 * Returns the Experience Cloud ID. An empty string is returned if the Experience Cloud ID was previously cleared.
	 *
	 * @param callback {@link AdobeCallback} of {@code String} invoked with the Experience Cloud ID
	 *                 If an {@link AdobeCallbackWithError} is provided, an {@link AdobeError} can be returned in the
	 *                 eventuality of any error that occurred while getting the Experience Cloud ID
	 */
	public static void getExperienceCloudId(final AdobeCallback<String> callback) {
		if (callback == null) {
			MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
						   "Identity - Unexpected null callback, provide a callback to retrieve current ECID.");
			return;
		}

		final Event event = new Event.Builder(IdentityConstants.EventNames.IDENTITY_REQUEST_IDENTITY_ECID,
											  IdentityConstants.EventType.EDGE_IDENTITY,
											  IdentityConstants.EventSource.REQUEST_IDENTITY).build();

		final ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
			@Override
			public void error(final ExtensionError extensionError) {
				returnError(callback, extensionError);
				MobileCore.log(LoggingMode.DEBUG, LOG_TAG, String.format("Identity - Failed to dispatch %s event: Error : %s.",
							   IdentityConstants.EventNames.IDENTITY_REQUEST_IDENTITY_ECID,
							   extensionError.getErrorName()));
			}
		};

		MobileCore.dispatchEventWithResponseCallback(event, new AdobeCallback<Event>() {
			@Override
			public void call(Event responseEvent) {
				if (responseEvent == null || responseEvent.getEventData() == null) {
					returnError(callback, AdobeError.UNEXPECTED_ERROR);
					return;
				}

				final IdentityMap identityMap = IdentityMap.fromXDMMap(responseEvent.getEventData());

				if (identityMap == null) {
					MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
								   "Identity - Failed to read IdentityMap from response event, invoking error callback with AdobeError.UNEXPECTED_ERROR");
					returnError(callback, AdobeError.UNEXPECTED_ERROR);
					return;
				}

				final List<IdentityItem> ecidItems = identityMap.getIdentityItemsForNamespace(IdentityConstants.Namespaces.ECID);

				if (ecidItems == null || ecidItems.isEmpty() || ecidItems.get(0).getId() == null) {
					callback.call("");
				} else {
					callback.call(ecidItems.get(0).getId());
				}

			}
		}, errorCallback);
	}

	/**
	 * Updates the currently known {@link IdentityMap} within the SDK.
	 * The Identity extension will merge the received identifiers with the previously saved one in an additive manner,
	 * no identifiers will be removed using this API.
	 * Identifiers which have an empty {@code id} or empty {@code namespace} are not allowed and are ignored.
	 *
	 * @param identityMap The identifiers to add or update.
	 */
	public static void updateIdentities(final IdentityMap identityMap) {
		if (identityMap == null || identityMap.isEmpty()) {
			MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Identity - Unable to updateIdentities, IdentityMap is null or empty");
			return;
		}

		final ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
			@Override
			public void error(final ExtensionError extensionError) {
				MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
							   String.format("Identity - Update Identities API. Failed to dispatch %s event: Error : %s.",
											 IdentityConstants.EventNames.UPDATE_IDENTITIES,
											 extensionError.getErrorName()));
			}
		};


		final Event updateIdentitiesEvent = new Event.Builder(IdentityConstants.EventNames.UPDATE_IDENTITIES,
				IdentityConstants.EventType.EDGE_IDENTITY,
				IdentityConstants.EventSource.UPDATE_IDENTITY).setEventData(identityMap.asXDMMap(false)).build();
		MobileCore.dispatchEvent(updateIdentitiesEvent, errorCallback);
	}

	/**
	 * Removes the identity from the stored client-side {@link IdentityMap}. The Identity extension will stop sending this identifier.
	 * This does not clear the identifier from the User Profile Graph.
	 *
	 * @param item the {@link IdentityItem} to remove.
	 * @param namespace The namespace of the identity to remove.
	 */
	public static void removeIdentity(final IdentityItem item, final String namespace) {
		if (Utils.isNullOrEmpty(namespace)) {
			MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Identity - Unable to removeIdentity, namespace is null or empty");
			return;
		}

		if (item == null) {
			MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Identity - Unable to removeIdentity, IdentityItem is null");
			return;
		}

		IdentityMap identityMap = new IdentityMap();
		identityMap.addItem(item, namespace);

		final ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
			@Override
			public void error(final ExtensionError extensionError) {
				MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
							   String.format("Identity - removeIdentity API. Failed to dispatch %s event: Error : %s.",
											 IdentityConstants.EventNames.REMOVE_IDENTITIES,
											 extensionError.getErrorName()));
			}
		};


		final Event removeIdentitiesEvent = new Event.Builder(IdentityConstants.EventNames.REMOVE_IDENTITIES,
				IdentityConstants.EventType.EDGE_IDENTITY,
				IdentityConstants.EventSource.REMOVE_IDENTITY).setEventData(identityMap.asXDMMap(false)).build();
		MobileCore.dispatchEvent(removeIdentitiesEvent, errorCallback);
	}

	/**
	 * Returns all identifiers, including customer identifiers which were previously added.
	 *
	 * @param callback {@link AdobeCallback} invoked with the current {@link IdentityMap}
	 *                 If an {@link AdobeCallbackWithError} is provided, an {@link AdobeError} can be returned in the
	 *                 eventuality of any error that occurred while getting the stored identities.
	 */
	public static void getIdentities(final AdobeCallback<IdentityMap> callback) {
		if (callback == null) {
			MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
						   "Identity - Unexpected null callback, provide a callback to retrieve current IdentityMap.");
			return;
		}

		final Event event = new Event.Builder(IdentityConstants.EventNames.REQUEST_IDENTITIES,
											  IdentityConstants.EventType.EDGE_IDENTITY,
											  IdentityConstants.EventSource.REQUEST_IDENTITY).build();

		final ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
			@Override
			public void error(final ExtensionError extensionError) {
				returnError(callback, extensionError);
				MobileCore.log(LoggingMode.DEBUG, LOG_TAG, String.format("Identity - Failed to dispatch %s event: Error : %s.",
							   IdentityConstants.EventNames.REQUEST_IDENTITIES,
							   extensionError.getErrorName()));
			}
		};

		MobileCore.dispatchEventWithResponseCallback(event, new AdobeCallback<Event>() {
			@Override
			public void call(Event responseEvent) {
				if (responseEvent == null || responseEvent.getEventData() == null) {
					returnError(callback, AdobeError.UNEXPECTED_ERROR);
					return;
				}

				final IdentityMap identityMap = IdentityMap.fromXDMMap(responseEvent.getEventData());

				if (identityMap == null) {
					MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
								   "Identity - Failed to read IdentityMap from response event, invoking error callback with AdobeError.UNEXPECTED_ERROR");
					returnError(callback, AdobeError.UNEXPECTED_ERROR);
					return;
				}

				callback.call(identityMap);
			}
		}, errorCallback);
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

		final AdobeCallbackWithError<T> adobeCallbackWithError = callback instanceof AdobeCallbackWithError ?
				(AdobeCallbackWithError<T>) callback : null;

		if (adobeCallbackWithError != null) {
			adobeCallbackWithError.fail(error);
		}
	}
}
