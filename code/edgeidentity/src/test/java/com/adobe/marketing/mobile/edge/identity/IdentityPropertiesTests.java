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

import static com.adobe.marketing.mobile.edge.identity.IdentityTestUtil.TestECIDItem;
import static com.adobe.marketing.mobile.edge.identity.IdentityTestUtil.TestItem;
import static com.adobe.marketing.mobile.edge.identity.IdentityTestUtil.createXDMIdentityMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;
import com.adobe.marketing.mobile.util.JSONAsserts;
import java.util.Map;
import org.junit.Test;

public class IdentityPropertiesTests {

	// ======================================================================================================================
	// Tests for method : toXDMData(final boolean allowEmpty)
	// ======================================================================================================================

	@Test
	public void test_toXDMData_AllowEmpty_True() throws DataReaderException {
		// setup
		IdentityProperties props = new IdentityProperties();

		// test
		Map<String, Object> xdmMap = props.toXDMData(true);

		// verify
		Map<String, Object> identityMap = DataReader.getTypedMap(
			Object.class,
			xdmMap,
			IdentityConstants.XDMKeys.IDENTITY_MAP
		);
		assertNotNull(identityMap);
		assertTrue(identityMap.isEmpty());
	}

	@Test
	public void test_toXDMData_AllowEmpty_False() {
		// setup
		IdentityProperties props = new IdentityProperties();

		// test
		Map<String, Object> xdmMap = props.toXDMData(false);

		// verify
		assertNull(xdmMap.get(IdentityConstants.XDMKeys.IDENTITY_MAP));
	}

	@Test
	public void test_toXDMData_Full() {
		// setup
		IdentityProperties props = new IdentityProperties();
		props.setECID(new ECID());
		props.setECIDSecondary(new ECID());
		props.setAdId("test-ad-id");

		// test
		Map<String, Object> xdmData = props.toXDMData(false);

		String expected =
			"{" +
			"  \"identityMap\": {" +
			"    \"ECID\": [" +
			"      {" +
			"        \"id\": \"" +
			props.getECID().toString() +
			"\"," +
			"        \"authenticatedState\": \"ambiguous\"," +
			"        \"primary\": false" +
			"      }," +
			"      {" +
			"        \"id\": \"" +
			props.getECIDSecondary().toString() +
			"\"," +
			"        \"authenticatedState\": \"ambiguous\"," +
			"        \"primary\": false" +
			"      }" +
			"    ]," +
			"    \"GAID\": [" +
			"      {" +
			"        \"id\": \"test-ad-id\"," +
			"        \"authenticatedState\": \"ambiguous\"," +
			"        \"primary\": false" +
			"      }" +
			"    ]" +
			"  }" +
			"}";

		JSONAsserts.assertEquals(expected, xdmData);
	}

	@Test
	public void test_toXDMData_OnlyPrimaryECID() {
		// setup
		IdentityProperties props = new IdentityProperties();
		props.setECID(new ECID());

		// verify
		String expected =
			"{" +
			"  \"identityMap\": {" +
			"    \"ECID\": [" +
			"      {" +
			"        \"id\": \"" +
			props.getECID().toString() +
			"\"," +
			"        \"authenticatedState\": \"ambiguous\"," +
			"        \"primary\": false" +
			"      }" +
			"    ]" +
			"  }" +
			"}";

		JSONAsserts.assertEquals(expected, props.toXDMData(false));
	}

	@Test
	public void test_toXDMData_OnlySecondaryECID() {
		// should not set secondary ECID if primary not set
		// setup
		IdentityProperties props = new IdentityProperties();
		props.setECIDSecondary(new ECID());

		// test and verify, can't have secondary ECID without primary ECID
		Map<String, Object> xdmMap = props.toXDMData(false);
		JSONAsserts.assertExactMatch("{}", xdmMap);
	}

	@Test
	public void text_toXDMData_OnlyAdId() {
		// setup
		IdentityProperties props = new IdentityProperties();
		props.setAdId("test-ad-id");

		// test
		Map<String, Object> xdmMap = props.toXDMData(false);

		// verify
		String expected =
			"{" +
			"  \"identityMap\": {" +
			"    \"GAID\": [" +
			"      {" +
			"        \"authenticatedState\": \"ambiguous\"," +
			"        \"id\": \"test-ad-id\"," +
			"        \"primary\": false" +
			"      }" +
			"    ]" +
			"  }" +
			"}";

		JSONAsserts.assertExactMatch(expected, xdmMap);
		assertEquals("test-ad-id", props.getAdId());
	}

	@Test
	public void text_toXDMData_whenEmptyAdId_thenNoValue() {
		// setup
		IdentityProperties props = new IdentityProperties();
		props.setAdId("");

		// test
		Map<String, Object> xdmMap = props.toXDMData(false);

		// verify
		JSONAsserts.assertEquals("{}", xdmMap);
	}

	// ======================================================================================================================
	// Tests for constructor : IdentityProperties(final Map<String, Object> xdmData)
	// ======================================================================================================================

	@Test
	public void testConstruct_FromXDMData_LoadingDataFromPersistence() {
		// setup
		Map<String, Object> persistedIdentifiers = createXDMIdentityMap(
			new TestItem("UserId", "secretID"),
			new TestItem("PushId", "token"),
			new TestItem("GAID", "test-ad-id"),
			new TestECIDItem("primaryECID"),
			new TestECIDItem("secondaryECID")
		);

		// test
		IdentityProperties props = new IdentityProperties(persistedIdentifiers);

		// verify
		String expected =
			"{" +
			"  \"identityMap\": {" +
			"    \"GAID\": [" +
			"      {" +
			"        \"id\": \"test-ad-id\"," +
			"        \"authenticatedState\": \"ambiguous\"," +
			"        \"primary\": false" +
			"      }" +
			"    ]," +
			"    \"UserId\": [" +
			"      {" +
			"        \"id\": \"secretID\"," +
			"        \"authenticatedState\": \"ambiguous\"," +
			"        \"primary\": false" +
			"      }" +
			"    ]," +
			"    \"ECID\": [" +
			"      {" +
			"        \"id\": \"primaryECID\"," +
			"        \"authenticatedState\": \"ambiguous\"," +
			"        \"primary\": false" +
			"      }," +
			"      {" +
			"        \"id\": \"secondaryECID\"," +
			"        \"authenticatedState\": \"ambiguous\"," +
			"        \"primary\": false" +
			"      }" +
			"    ]," +
			"    \"PushId\": [" +
			"      {" +
			"        \"id\": \"token\"," +
			"        \"authenticatedState\": \"ambiguous\"," +
			"        \"primary\": false" +
			"      }" +
			"    ]" +
			"  }" +
			"}";

		JSONAsserts.assertEquals(expected, props.toXDMData(false));
		assertEquals("primaryECID", props.getECID().toString());
		assertEquals("secondaryECID", props.getECIDSecondary().toString());
	}

	@Test
	public void testConstruct_FromXDMData_NothingFromPersistence() {
		// test
		IdentityProperties props = new IdentityProperties(null);

		// verify
		JSONAsserts.assertEquals("{}", props.toXDMData(false));
	}

	@Test
	public void testConstruct_FromXDMData_removesInvalidIdentityItems() {
		// setup, Items with null or empty ids are invalid
		Map<String, Object> persistedIdentifiers = createXDMIdentityMap(
			new TestItem("UserId", "secretID"),
			new TestItem("InvalidEmpty", ""),
			new TestItem("InvalidNull", null),
			new TestECIDItem("primaryECID"),
			new TestECIDItem("secondaryECID")
		);

		// test
		IdentityProperties props = new IdentityProperties(persistedIdentifiers);

		// verify
		String expected =
			"{" +
			"  \"identityMap\": {" +
			"    \"UserId\": [" +
			"      {" +
			"        \"id\": \"secretID\"," +
			"        \"authenticatedState\": \"ambiguous\"," +
			"        \"primary\": false" +
			"      }" +
			"    ]," +
			"    \"ECID\": [" +
			"      {" +
			"        \"id\": \"primaryECID\"," +
			"        \"authenticatedState\": \"ambiguous\"," +
			"        \"primary\": false" +
			"      }," +
			"      {" +
			"        \"id\": \"secondaryECID\"," +
			"        \"authenticatedState\": \"ambiguous\"," +
			"        \"primary\": false" +
			"      }" +
			"    ]" +
			"  }" +
			"}";

		JSONAsserts.assertEquals(expected, props.toXDMData(false));
		assertEquals("primaryECID", props.getECID().toString());
		assertEquals("secondaryECID", props.getECIDSecondary().toString());
	}

	// ======================================================================================================================
	// Tests for method : setECID(final ECID newEcid)
	// ======================================================================================================================

	@Test
	public void test_setECID_WillReplaceTheOldECID() {
		// setup
		IdentityProperties props = new IdentityProperties();

		// test 1
		props.setECID(new ECID("primary"));

		// verify
		String expected1 =
			"{" +
			"  \"identityMap\": {" +
			"    \"ECID\": [" +
			"      {" +
			"        \"id\": \"primary\"," +
			"        \"authenticatedState\": \"ambiguous\"," +
			"        \"primary\": false" +
			"      }" +
			"    ]" +
			"  }" +
			"}";

		JSONAsserts.assertEquals(expected1, props.toXDMData(false));
		assertEquals("primary", props.getECID().toString());

		// test 2 - call setECID again to replace the old one
		props.setECID(new ECID("primaryAgain"));

		// verify
		String expected2 =
			"{" +
			"  \"identityMap\": {" +
			"    \"ECID\": [" +
			"      {" +
			"        \"id\": \"primaryAgain\"," +
			"        \"authenticatedState\": \"ambiguous\"," +
			"        \"primary\": false" +
			"      }" +
			"    ]" +
			"  }" +
			"}";

		JSONAsserts.assertEquals(expected2, props.toXDMData(false));
		assertEquals("primaryAgain", props.getECID().toString());
	}

	@Test
	public void test_setECID_NullRemovesFromIdentityMap() {
		// setup
		IdentityProperties props = new IdentityProperties();

		// test 1 - set a valid ECID and then to null
		props.setECID(new ECID("primary"));
		props.setECID(null);

		// verify
		JSONAsserts.assertEquals("{}", props.toXDMData(false));
		assertNull(props.getECID());
	}

	// ======================================================================================================================
	// Tests for method : setECIDSecondary(final ECID newEcid)
	// ======================================================================================================================

	@Test
	public void test_setECIDSecondary_WillReplaceTheOldECID() {
		// setup
		IdentityProperties props = new IdentityProperties();

		// test 1
		props.setECID(new ECID("primary"));
		props.setECIDSecondary(new ECID("secondary"));

		// verify
		String expected1 =
			"{" +
			"  \"identityMap\": {" +
			"    \"ECID\": [" +
			"      {" +
			"        \"id\": \"primary\"," +
			"        \"authenticatedState\": \"ambiguous\"," +
			"        \"primary\": false" +
			"      }," +
			"      {" +
			"        \"id\": \"secondary\"," +
			"        \"authenticatedState\": \"ambiguous\"," +
			"        \"primary\": false" +
			"      }" +
			"    ]" +
			"  }" +
			"}";

		JSONAsserts.assertEquals(expected1, props.toXDMData(false));
		assertEquals("secondary", props.getECIDSecondary().toString());

		// test 2 - call setECIDSecondary again to replace the old one
		props.setECIDSecondary(new ECID("secondaryAgain"));

		// verify
		String expected2 =
			"{" +
			"  \"identityMap\": {" +
			"    \"ECID\": [" +
			"      {" +
			"        \"id\": \"primary\"," +
			"        \"authenticatedState\": \"ambiguous\"," +
			"        \"primary\": false" +
			"      }," +
			"      {" +
			"        \"id\": \"secondaryAgain\"," +
			"        \"authenticatedState\": \"ambiguous\"," +
			"        \"primary\": false" +
			"      }" +
			"    ]" +
			"  }" +
			"}";

		JSONAsserts.assertEquals(expected2, props.toXDMData(false));
		assertEquals("secondaryAgain", props.getECIDSecondary().toString());
	}

	@Test
	public void test_setECIDSecondary_NullRemovesFromIdentityMap() {
		// setup
		IdentityProperties props = new IdentityProperties(
			createXDMIdentityMap(new TestECIDItem("primary"), new TestECIDItem("secondary"))
		);

		String expected =
			"{" +
			"  \"identityMap\": {" +
			"    \"ECID\": [" +
			"      {" +
			"        \"id\": \"primary\"," +
			"        \"authenticatedState\": \"ambiguous\"," +
			"        \"primary\": false" +
			"      }," +
			"      {" +
			"        \"id\": \"secondary\"," +
			"        \"authenticatedState\": \"ambiguous\"," +
			"        \"primary\": false" +
			"      }" +
			"    ]" +
			"  }" +
			"}";

		JSONAsserts.assertEquals(expected, props.toXDMData(false));

		props.setECIDSecondary(null);

		// test
		String expected2 =
			"{" +
			"  \"identityMap\": {" +
			"    \"ECID\": [" +
			"      {" +
			"        \"id\": \"primary\"," +
			"        \"authenticatedState\": \"ambiguous\"," +
			"        \"primary\": false" +
			"      }" +
			"    ]" +
			"  }" +
			"}";

		JSONAsserts.assertEquals(expected2, props.toXDMData(false));
		assertNull(props.getECIDSecondary());
	}

	@Test
	public void test_clearPrimaryECID_alsoClearsSecondaryECID() {
		// setup
		IdentityProperties props = new IdentityProperties(
			createXDMIdentityMap(new TestECIDItem("primary"), new TestECIDItem("secondary"))
		);

		// test
		props.setECID(null);

		// verify
		JSONAsserts.assertEquals("{}", props.toXDMData(false));
		assertNull(props.getECIDSecondary());
	}

	@Test
	public void test_setPrimaryECIDPreservesSecondaryECID() {
		// setup
		IdentityProperties props = new IdentityProperties(
			createXDMIdentityMap(new TestECIDItem("primary"), new TestECIDItem("secondary"))
		);

		// test
		props.setECID(new ECID("primaryAgain"));

		// verify
		String expected =
			"{" +
			"  \"identityMap\": {" +
			"    \"ECID\": [" +
			"      {" +
			"        \"id\": \"primaryAgain\"," +
			"        \"authenticatedState\": \"ambiguous\"," +
			"        \"primary\": false" +
			"      }," +
			"      {" +
			"        \"id\": \"secondary\"," +
			"        \"authenticatedState\": \"ambiguous\"," +
			"        \"primary\": false" +
			"      }" +
			"    ]" +
			"  }" +
			"}";

		JSONAsserts.assertEquals(expected, props.toXDMData(false));
	}

	@Test
	public void test_primaryECIDIsAlwaysTheFirstElement() {
		// setup
		IdentityProperties props = new IdentityProperties();

		// test
		props.setECID(new ECID("primary"));
		props.setECIDSecondary(new ECID("secondary"));
		props.setECID(new ECID("primaryAgain"));

		// verify
		String expected =
			"{" +
			"  \"identityMap\": {" +
			"    \"ECID\": [" +
			"      {" +
			"        \"id\": \"primaryAgain\"," +
			"        \"authenticatedState\": \"ambiguous\"," +
			"        \"primary\": false" +
			"      }," +
			"      {" +
			"        \"id\": \"secondary\"," +
			"        \"authenticatedState\": \"ambiguous\"," +
			"        \"primary\": false" +
			"      }" +
			"    ]" +
			"  }" +
			"}";

		JSONAsserts.assertEquals(expected, props.toXDMData(false));
	}

	// =============================================================================================
	// Tests for setAdId() getAdId()
	// =============================================================================================
	@Test
	public void test_getsetAdId_whenValid_thenValid() {
		// Setup
		IdentityProperties props = new IdentityProperties();
		props.setAdId("adId");

		// Test
		final String advertisingIdentifier = props.getAdId();

		// Verify
		assertEquals("adId", advertisingIdentifier);
	}

	@Test
	public void test_getsetAdId_whenNull_thenNull() {
		// Setup
		IdentityProperties props = new IdentityProperties();
		props.setAdId(null);

		// Test
		final String advertisingIdentifier = props.getAdId();

		// Verify
		assertNull(advertisingIdentifier);
	}

	@Test
	public void test_getsetAdId_whenEmpty_thenNull() {
		// Setup
		IdentityProperties props = new IdentityProperties();
		props.setAdId("");

		// Test
		final String advertisingIdentifier = props.getAdId();

		// Verify
		assertNull(advertisingIdentifier);
	}

	// ======================================================================================================================
	// Tests for updateCustomerIdentifiers()
	// ======================================================================================================================

	@Test
	public void test_updateCustomerIdentifiers_validProperties() {
		// Setup
		IdentityProperties props = new IdentityProperties();
		props.setECID(new ECID("internalECID"));

		// Test
		final Map<String, Object> customerIdentifierUpdate = createXDMIdentityMap(
			new IdentityTestUtil.TestItem("UserId", "somevalue")
		);
		props.updateCustomerIdentifiers(IdentityMap.fromXDMMap(customerIdentifierUpdate));

		// Verify
		final Map<String, Object> expectedProperties = createXDMIdentityMap(
			new IdentityTestUtil.TestItem("ECID", "internalECID"),
			new IdentityTestUtil.TestItem("UserId", "somevalue")
		);
		assertEquals(expectedProperties, props.toXDMData(false));
	}

	@Test
	public void test_updateCustomerIdentifiers_doesNotUpdateReservedNamespace() {
		// Setup
		IdentityProperties props = new IdentityProperties();
		props.setECID(new ECID("internalECID"));

		// Test
		final Map<String, Object> customerIdentifierUpdate = createXDMIdentityMap(
			new IdentityTestUtil.TestItem("ECID", "somevalue"),
			new IdentityTestUtil.TestItem("GAID", "somevalue"),
			new IdentityTestUtil.TestItem("IDFA", "somevalue"),
			new IdentityTestUtil.TestItem("IdFA", "somevalue"),
			new IdentityTestUtil.TestItem("gaid", "somevalue"),
			new IdentityTestUtil.TestItem("UserId", "somevalue")
		);
		props.updateCustomerIdentifiers(IdentityMap.fromXDMMap(customerIdentifierUpdate));

		// Verify
		final Map<String, Object> expectedProperties = createXDMIdentityMap(
			new IdentityTestUtil.TestItem("ECID", "internalECID"),
			new IdentityTestUtil.TestItem("UserId", "somevalue")
		);
		assertEquals(expectedProperties, props.toXDMData(false));
	}

	@Test
	public void test_updateCustomerIdentifiers_storesAllIdentifiersCaseSensitively() {
		// Setup
		IdentityProperties props = new IdentityProperties();
		props.setECID(new ECID("internalECID"));

		// Test
		final Map<String, Object> customerIdentifierUpdate = createXDMIdentityMap(
			new IdentityTestUtil.TestItem("caseSensitive", "somevalue"),
			new IdentityTestUtil.TestItem("CASESENSITIVE", "SOMEVALUE")
		);
		props.updateCustomerIdentifiers(IdentityMap.fromXDMMap(customerIdentifierUpdate));

		// Verify
		final Map<String, Object> expectedProperties = createXDMIdentityMap(
			new IdentityTestUtil.TestItem("ECID", "internalECID"),
			new IdentityTestUtil.TestItem("caseSensitive", "somevalue"),
			new IdentityTestUtil.TestItem("CASESENSITIVE", "SOMEVALUE")
		);
		assertEquals(expectedProperties, props.toXDMData(false));
	}

	// ======================================================================================================================
	// Tests for removeCustomerIdentifiers()
	// ======================================================================================================================

	@Test
	public void test_removeCustomerIdentifiers_removesIdentifiers() {
		// Setup
		IdentityProperties props = new IdentityProperties();
		props.setECID(new ECID("internalECID"));
		final Map<String, Object> customerIdentifierUpdate = createXDMIdentityMap(
			new IdentityTestUtil.TestItem("UserId", "secretID"),
			new IdentityTestUtil.TestItem("PushId", "token")
		);
		props.updateCustomerIdentifiers(IdentityMap.fromXDMMap(customerIdentifierUpdate));

		// test
		final Map<String, Object> removedIdentityXDM = createXDMIdentityMap(
			new IdentityTestUtil.TestItem("UserId", "secretID")
		);
		props.removeCustomerIdentifiers(IdentityMap.fromXDMMap(removedIdentityXDM));

		// Verify
		final Map<String, Object> expectedProperties = createXDMIdentityMap(
			new IdentityTestUtil.TestItem("ECID", "internalECID"),
			new IdentityTestUtil.TestItem("PushId", "token")
		);
		assertEquals(expectedProperties, props.toXDMData(false));
	}

	@Test
	public void test_removeCustomerIdentifiers_doesNotRemoveReservedNamespaces() {
		// Setup
		IdentityProperties props = new IdentityProperties();
		final ECID initialECID = new ECID();
		props.setECID(initialECID);
		props.setAdId("initialADID");

		// test
		final Map<String, Object> removedIdentityXDM = createXDMIdentityMap(
			new IdentityTestUtil.TestItem("GAID", "initialECID"),
			new IdentityTestUtil.TestItem("ECID", initialECID.toString())
		);
		props.removeCustomerIdentifiers(IdentityMap.fromXDMMap(removedIdentityXDM));

		// Verify
		IdentityProperties expectedProperties = new IdentityProperties();
		expectedProperties.setECID(initialECID);
		expectedProperties.setAdId("initialADID");

		assertEquals(expectedProperties.toXDMData(false), props.toXDMData(false));
	}

	@Test
	public void test_removeCustomerIdentifiers_removesCaseSensitively() {
		// Setup
		IdentityProperties props = new IdentityProperties();
		props.setECID(new ECID("internalECID"));
		final Map<String, Object> customerIdentifierUpdate = createXDMIdentityMap(
			new IdentityTestUtil.TestItem("caseSensitive", "somevalue"),
			new IdentityTestUtil.TestItem("CASESENSITIVE", "SOMEVALUE")
		);
		props.updateCustomerIdentifiers(IdentityMap.fromXDMMap(customerIdentifierUpdate));

		// check that initial contents are expected
		final Map<String, Object> expectedProperties = createXDMIdentityMap(
			new IdentityTestUtil.TestItem("ECID", "internalECID"),
			new IdentityTestUtil.TestItem("caseSensitive", "somevalue"),
			new IdentityTestUtil.TestItem("CASESENSITIVE", "SOMEVALUE")
		);
		assertEquals(expectedProperties, props.toXDMData(false));

		// test
		final Map<String, Object> removedIdentityXDM = createXDMIdentityMap(
			new IdentityTestUtil.TestItem("caseSensitive", "somevalue")
		);

		props.removeCustomerIdentifiers(IdentityMap.fromXDMMap(removedIdentityXDM));

		// verify
		final Map<String, Object> expectedIdentityXDM = createXDMIdentityMap(
			new IdentityTestUtil.TestItem("ECID", "internalECID"),
			new IdentityTestUtil.TestItem("CASESENSITIVE", "SOMEVALUE")
		);

		assertEquals(expectedIdentityXDM, props.toXDMData(false));
	}
}
