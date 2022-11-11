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

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.SharedStateResult;
import java.util.Map;

/**
 * Callback for streamlining  Shared State operations (within and outside the extension class)
 */
interface SharedStateCallback {
	/**
	 * Fetches the Shared State for the provided {@code event} from the specified {@code stateOwner}.
	 *
	 * @param stateOwner Shared state owner name
	 * @param event      current event for which to fetch the shared state; if null is passed, the latest shared state will be returned
	 * @return a {@code SharedStateResult} at the event; null if an error occurred
	 */
	SharedStateResult getSharedState(final String stateOwner, final Event event);

	/**
	 * Creates an XDM Shared State for the provided {@code event} with the specified {@code state}.
	 *
	 * @param state data to be set as XDM Shared State
	 * @param event current event for which to set the shared state; if null is passed, the next shared state version will be set
	 */
	void createXDMSharedState(final Map<String, Object> state, final Event event);
}
