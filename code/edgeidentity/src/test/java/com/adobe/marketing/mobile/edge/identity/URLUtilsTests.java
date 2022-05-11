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

import org.junit.Assert;
import org.junit.Ignore;
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
		String actual = URLUtils.generateURLVariablesPayload("TEST_TS", "TEST_ECID", "TEST@ORGID");
		assertEquals("adobe_mc=TS%3DTEST_TS%7CMCMID%3DTEST_ECID%7CMCORGID%3DTEST_ORGID", actual);
	}

	// UrlEncode tests
	@Test
	public void urlEncodeWithNoEncodedNeeded() {
		assertEquals("thisisateststring", URLUtils.urlEncode("thisisateststring"));
	}

	@Test
	public void urlEncodeStartsWithHyphen() {
		assertEquals("test-test", URLUtils.urlEncode("test-test"));
	}

	@Test
	public void urlEncodeStartsWithAtTheRate() {
		assertEquals("test%40Org", URLUtils.urlEncode("test@Org"));
	}

	@Test
	@Ignore
	public void urlEncodeWithSpaces() {
		assertEquals("this%20is%20a%20test%20string", URLUtils.urlEncode("this is a test string"));
	}

	@Test
	@Ignore
	public void urlEncodeStartsWithSpace() {
		assertEquals("%20afterspace", URLUtils.urlEncode(" afterspace"));
	}

	@Test
	public void urlEncodeOnlyUnicode() {
		assertEquals("%E7%BD%91", URLUtils.urlEncode("网"));
	}

	@Test
	public void urlEncodeStartsWithUnicode() {
		assertEquals("%E7%BD%91test", URLUtils.urlEncode("网test"));
	}

	@Test
	public void urlEncodeEndsWithUnicode() {
		assertEquals("test%E7%BD%91", URLUtils.urlEncode("test网"));
	}

	@Test
	public void urlEncodeBlankString() {
		assertEquals("", URLUtils.urlEncode(""));
	}

	@Test
	@Ignore
	public void urlEncodeDeathString() {
		assertEquals(
			"~%21%40%23%24%25%5E%26%2A%28%29-%2B%3D%7C%7D%7B%5D%5B%5C%2F.%3C%2C%3E",
			URLUtils.urlEncode("~!@#$%^&*()-+=|}{][\\/.<,>")
		);
	}

	@Test
	public void testURLEncodeNull() {
		Assert.assertNull(URLUtils.urlEncode(null));
	}
}
