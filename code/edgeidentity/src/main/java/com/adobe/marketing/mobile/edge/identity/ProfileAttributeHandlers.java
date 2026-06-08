/*
  Copyright 2025 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.edge.identity;

import java.util.List;

/**
 * Central registry of every {@link ProfileAttributeHandler} the Edge Identity extension knows
 * about. Adding a new attribute is a one-line addition here plus one new handler file; the
 * collector layer in {@link IdentityState} is attribute-agnostic and needs no changes.
 */
final class ProfileAttributeHandlers {

	private ProfileAttributeHandlers() {}

	/**
	 * Returns the full list of registered handlers, each bound to {@code store} for persistence.
	 *
	 * @param store the shared profile-attributes store every handler reads/writes through
	 * @return an immutable list of registered {@link ProfileAttributeHandler}s
	 */
	static List<ProfileAttributeHandler> all(final ProfileAttributeStore store) {
		return List.of(new TimeZoneAttributeHandler(store));
	}
}
