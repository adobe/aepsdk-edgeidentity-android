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

import com.adobe.marketing.mobile.Event;
import java.util.Map;

/**
 * Handles a single profile attribute end-to-end: it claims a key on the incoming
 * {@code PROFILE_ATTRIBUTE} event, owns its dedup + persistence, and reports its
 * contribution to the outgoing collated {@code profile.updateAttributes} Edge payload and
 * to the profile-attributes shared state.
 *
 * <p>Each attribute (timezone today; locale, push identifier, ... in the future) is one handler
 * registered in {@link ProfileAttributeHandlers}. The collector layer in {@link IdentityState}
 * iterates every registered handler and stitches their contributions into a single Edge event
 * and into the shared state.
 */
interface ProfileAttributeHandler {
	/**
	 * Returns the key on the incoming {@code PROFILE_ATTRIBUTE} event data that this handler
	 * claims (e.g. {@code "timeZone"}). The collector uses this purely as an input filter:
	 * {@link #collectFromEvent(Event)} is invoked only when this key is present in the event
	 * data.
	 *
	 * <p>This is independent of the keys this handler emits in its outgoing payload, which the
	 * handler controls directly via the maps returned from {@link #collectFromEvent(Event)} and
	 * {@link #collectFromStorage()}.
	 *
	 * @return the event-data key this handler reads
	 */
	String getAttributeKey();

	/**
	 * Reads this handler's value from the update {@code event}, dedups against persistence, and
	 * on change writes the new value to persistence before returning the contribution to add to
	 * the outgoing collated {@code profile.updateAttributes} Edge payload. Returns {@code null}
	 * (or an empty map) when nothing should be contributed (key absent, value empty, or
	 * unchanged).
	 *
	 * <p>Persistence is written <b>before</b> the caller dispatches so that a crash or process
	 * death between persist and dispatch leaves the pending value in storage; the next sync will
	 * pick it up via dedup.
	 *
	 * @param event the profile attributes update event
	 * @return the contribution to merge into the outgoing payload, or {@code null}
	 */
	Map<String, Object> collectFromEvent(Event event);

	/**
	 * Returns this handler's persisted contribution as it should appear in the profile-attributes
	 * shared state, or {@code null} (or an empty map) when nothing is stored.
	 *
	 * @return the persisted contribution, or {@code null}
	 */
	Map<String, Object> collectFromStorage();
}
