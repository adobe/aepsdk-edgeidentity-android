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
import androidx.annotation.Nullable;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Identity is used to clearly distinguish people that are interacting with digital experiences.
 * An {@code IdentityItem} is intended to be included in an instance of {@link IdentityMap}.
 *
 * @see <a href="https://github.com/adobe/xdm/blob/master/docs/reference/datatypes/identityitem.schema.md">Identity Item Schema</a>
 */
public final class IdentityItem {

	private static final String LOG_SOURCE = "IdentityItem";

	private final String id;
	private final AuthenticatedState authenticatedState;
	private final boolean primary;

	/**
	 * Creates a new {@link IdentityItem}.
	 * An {@code IdentityItem} should not have an empty or null {@code id} value. An {@link IdentityMap}
	 * will reject {@code IdentityItem}s with null or empty identifiers.
	 *
	 *
	 * @param id                 id for the item; should not be null
	 * @param authenticatedState {@link AuthenticatedState} for the item; if none is provided {@link AuthenticatedState#AMBIGUOUS} is used as default
	 * @param primary            primary flag for the item
	 * @throws IllegalArgumentException if {@code id} is null
	 */
	public IdentityItem(
		@NonNull final String id,
		@Nullable final AuthenticatedState authenticatedState,
		final boolean primary
	) {
		if (id == null) {
			throw new IllegalArgumentException("id must be non-null");
		}

		this.id = id;
		this.authenticatedState = authenticatedState != null ? authenticatedState : AuthenticatedState.AMBIGUOUS;
		this.primary = primary;
	}

	/**
	 * Creates a new {@link IdentityItem} with default values
	 * {@code authenticatedState) is set to AMBIGUOUS
	 * (@code primary} is set to false
	 * An {@code IdentityItem} should not have an empty or null {@code id} value. An {@link IdentityMap}
	 * will reject {@code IdentityItem}s with null or empty identifiers.
	 *
	 * @param id the id for this {@link IdentityItem}; should not be null
	 * @throws IllegalArgumentException if {@code id} is null
	 */
	public IdentityItem(@NonNull final String id) {
		this(id, AuthenticatedState.AMBIGUOUS, false);
	}

	/**
	 * Creates a copy of item.
	 *
	 * @param item A {@link IdentityItem} to be copied; should not be null
	 */
	public IdentityItem(@NonNull final IdentityItem item) {
		this(item.id, item.authenticatedState, item.primary);
	}

	/**
	 * Identity of the consumer in the related namespace.
	 *
	 * @return The id for this identity item
	 */
	@NonNull
	public String getId() {
		return id;
	}

	/**
	 * The state this identity is authenticated.
	 *
	 * @return Current {@link AuthenticatedState} for this item
	 */
	@NonNull
	public AuthenticatedState getAuthenticatedState() {
		return authenticatedState;
	}

	/**
	 * Indicates if this identity is the preferred identity.
	 * Is used as a hint to help systems better organize how identities are queried.
	 *
	 * @return true if this item is primary, false otherwise
	 */
	public boolean isPrimary() {
		return primary;
	}

	@NonNull
	@Override
	public String toString() {
		// format:off
        return "{"
                + "\"" + IdentityConstants.XDMKeys.ID + "\": \"" + id + "\", "
                + "\"" + IdentityConstants.XDMKeys.AUTHENTICATED_STATE + "\": \"" + (authenticatedState == null ? "null" :
                authenticatedState.getName()) + "\", "
                + "\"" + IdentityConstants.XDMKeys.PRIMARY + "\": " + primary
                + "}";
        // format:on
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		IdentityItem that = (IdentityItem) o;
		return id.equalsIgnoreCase(that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	// ========================================================================================
	// package protected methods
	// ========================================================================================

	/**
	 * Converts this object into a map representation
	 *
	 * @return this object in a map representation
	 */
	Map<String, Object> toObjectMap() {
		Map<String, Object> map = new HashMap<>();

		if (id != null) {
			map.put(IdentityConstants.XDMKeys.ID, id);
		}

		if (authenticatedState != null) {
			map.put(IdentityConstants.XDMKeys.AUTHENTICATED_STATE, authenticatedState.getName());
		} else {
			map.put(IdentityConstants.XDMKeys.AUTHENTICATED_STATE, AuthenticatedState.AMBIGUOUS.getName());
		}

		map.put(IdentityConstants.XDMKeys.PRIMARY, primary);
		return map;
	}

	/**
	 * Creates an {@link IdentityItem} from the data
	 *
	 * @param data the data representing an {@link IdentityItem}
	 * @return an initialized {@link IdentityItem} based on the data, null if data is invalid
	 */
	static IdentityItem fromData(final Map<String, Object> data) {
		if (data == null) {
			return null;
		}

		try {
			final String id = DataReader.getString(data, IdentityConstants.XDMKeys.ID);

			final AuthenticatedState authenticatedState = AuthenticatedState.fromString(
				DataReader.optString(
					data,
					IdentityConstants.XDMKeys.AUTHENTICATED_STATE,
					AuthenticatedState.AMBIGUOUS.getName()
				)
			);

			final boolean primary = DataReader.optBoolean(data, IdentityConstants.XDMKeys.PRIMARY, false);

			return new IdentityItem(id, authenticatedState, primary);
		} catch (final DataReaderException e) {
			Log.debug(LOG_TAG, LOG_SOURCE, "Failed to create IdentityItem from data.");
		} catch (final IllegalArgumentException e) {
			Log.debug(
				LOG_TAG,
				LOG_SOURCE,
				"Failed to create IdentityItem from data as 'id' is null. %s",
				e.getLocalizedMessage()
			);
		}

		return null;
	}
}
