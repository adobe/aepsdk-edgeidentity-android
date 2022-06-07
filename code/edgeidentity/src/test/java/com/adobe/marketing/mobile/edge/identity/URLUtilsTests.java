/*
  Copyright 2022 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.edge.identity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class URLUtilsTests {

	@Test
	public void test_generateURLVariablesPayload_emptyValuesPassed_returnsStringWithURLPrefixOnly() {
		String actual = URLUtils.generateURLVariablesPayload("", "", "");
		assertEquals("adobe_mc=null", actual);
	}

	@Test
	public void test_generateURLVariablesPayload_nullValuesPassed_returnsStringWithURLPrefixOnly() {
		String actual = URLUtils.generateURLVariablesPayload(null, null, null);
		assertEquals("adobe_mc=null", actual);
	}

	@Test
	public void test_generateURLVariablesPayload_validStringValuesPassed_returnsStringWith_TS_ECID_ORGID() {
		String actual = URLUtils.generateURLVariablesPayload("TEST_TS", "TEST_ECID", "Adobe-Test@OrgId");
		assertEquals("adobe_mc=TS%3DTEST_TS%7CMCMID%3DTEST_ECID%7CMCORGID%3DAdobe-Test%40OrgId", actual);
	}
}
