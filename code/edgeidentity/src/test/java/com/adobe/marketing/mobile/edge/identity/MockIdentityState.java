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

import java.util.ArrayList;
import java.util.List;

/**
 * Partial mock class for {@link IdentityState} to be used for testing
 */
class MockIdentityState extends IdentityState {

	MockIdentityState(final IdentityProperties identityProperties) {
		super(identityProperties);
	}

	int updateCustomerIdentifiersCalledTimes = 0;
	List<IdentityMap> updateCustomerIdentifiersParams = new ArrayList<>();
	@Override
	void updateCustomerIdentifiers(final IdentityMap map) {
		updateCustomerIdentifiersCalledTimes++;
		updateCustomerIdentifiersParams.add(map);
	}

	int removeCustomerIdentifiersCalledTimes = 0;
	List<IdentityMap> removeCustomerIdentifiersParams = new ArrayList<>();
	@Override
	void removeCustomerIdentifiers(final IdentityMap map) {
		removeCustomerIdentifiersCalledTimes++;
		removeCustomerIdentifiersParams.add(map);
	}

	boolean hasBooted = false;
	@Override
	boolean hasBooted() {
		return hasBooted;
	}
}
