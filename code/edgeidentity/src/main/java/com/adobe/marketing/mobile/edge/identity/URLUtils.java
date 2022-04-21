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

import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;
import java.io.UnsupportedEncodingException;

public class URLUtils {

	static final String LOG_TAG = "URLUtils";

	// lookup tables used by urlEncode
	private static final String[] encodedChars = new String[] {
		"%00",
		"%01",
		"%02",
		"%03",
		"%04",
		"%05",
		"%06",
		"%07",
		"%08",
		"%09",
		"%0A",
		"%0B",
		"%0C",
		"%0D",
		"%0E",
		"%0F",
		"%10",
		"%11",
		"%12",
		"%13",
		"%14",
		"%15",
		"%16",
		"%17",
		"%18",
		"%19",
		"%1A",
		"%1B",
		"%1C",
		"%1D",
		"%1E",
		"%1F",
		"%20",
		"%21",
		"%22",
		"%23",
		"%24",
		"%25",
		"%26",
		"%27",
		"%28",
		"%29",
		"%2A",
		"%2B",
		"%2C",
		"-",
		".",
		"%2F",
		"0",
		"1",
		"2",
		"3",
		"4",
		"5",
		"6",
		"7",
		"8",
		"9",
		"%3A",
		"%3B",
		"%3C",
		"%3D",
		"%3E",
		"%3F",
		"%40",
		"A",
		"B",
		"C",
		"D",
		"E",
		"F",
		"G",
		"H",
		"I",
		"J",
		"K",
		"L",
		"M",
		"N",
		"O",
		"P",
		"Q",
		"R",
		"S",
		"T",
		"U",
		"V",
		"W",
		"X",
		"Y",
		"Z",
		"%5B",
		"%5C",
		"%5D",
		"%5E",
		"_",
		"%60",
		"a",
		"b",
		"c",
		"d",
		"e",
		"f",
		"g",
		"h",
		"i",
		"j",
		"k",
		"l",
		"m",
		"n",
		"o",
		"p",
		"q",
		"r",
		"s",
		"t",
		"u",
		"v",
		"w",
		"x",
		"y",
		"z",
		"%7B",
		"%7C",
		"%7D",
		"~",
		"%7F",
		"%80",
		"%81",
		"%82",
		"%83",
		"%84",
		"%85",
		"%86",
		"%87",
		"%88",
		"%89",
		"%8A",
		"%8B",
		"%8C",
		"%8D",
		"%8E",
		"%8F",
		"%90",
		"%91",
		"%92",
		"%93",
		"%94",
		"%95",
		"%96",
		"%97",
		"%98",
		"%99",
		"%9A",
		"%9B",
		"%9C",
		"%9D",
		"%9E",
		"%9F",
		"%A0",
		"%A1",
		"%A2",
		"%A3",
		"%A4",
		"%A5",
		"%A6",
		"%A7",
		"%A8",
		"%A9",
		"%AA",
		"%AB",
		"%AC",
		"%AD",
		"%AE",
		"%AF",
		"%B0",
		"%B1",
		"%B2",
		"%B3",
		"%B4",
		"%B5",
		"%B6",
		"%B7",
		"%B8",
		"%B9",
		"%BA",
		"%BB",
		"%BC",
		"%BD",
		"%BE",
		"%BF",
		"%C0",
		"%C1",
		"%C2",
		"%C3",
		"%C4",
		"%C5",
		"%C6",
		"%C7",
		"%C8",
		"%C9",
		"%CA",
		"%CB",
		"%CC",
		"%CD",
		"%CE",
		"%CF",
		"%D0",
		"%D1",
		"%D2",
		"%D3",
		"%D4",
		"%D5",
		"%D6",
		"%D7",
		"%D8",
		"%D9",
		"%DA",
		"%DB",
		"%DC",
		"%DD",
		"%DE",
		"%DF",
		"%E0",
		"%E1",
		"%E2",
		"%E3",
		"%E4",
		"%E5",
		"%E6",
		"%E7",
		"%E8",
		"%E9",
		"%EA",
		"%EB",
		"%EC",
		"%ED",
		"%EE",
		"%EF",
		"%F0",
		"%F1",
		"%F2",
		"%F3",
		"%F4",
		"%F5",
		"%F6",
		"%F7",
		"%F8",
		"%F9",
		"%FA",
		"%FB",
		"%FC",
		"%FD",
		"%FE",
		"%FF",
	};

	private static final int ALL_BITS_ENABLED = 0xFF;
	private static final boolean[] utf8Mask = new boolean[] {
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		true,
		true,
		false,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		false,
		false,
		false,
		false,
		true,
		false,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		true,
		false,
		false,
		false,
		true,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
		false,
	};

	/**
	 * Helper function to generate url variables in format acceptable by the AEP web SDKs
	 *
	 * @param ts timestamp {@link String} denoting time when url variables request was made
	 * @param ecid Experience Cloud identifier {@link String} generated by the SDK
	 * @param orgId Experience Cloud Org identifier {@link String} set in the configuration
	 * @return {@link String} formatted with the visitor id payload
	 */
	static String generateURLVariablesPayload(final String ts, final String ecid, final String orgId) {
		final StringBuilder urlFragment = new StringBuilder();

		// construct the adobe_mc string

		// append timestamp
		String theIdString = appendKVPToVisitorIdString(null, IdentityConstants.UrlKeys.TS, ts);

		// append ecid
		theIdString = appendKVPToVisitorIdString(theIdString, IdentityConstants.UrlKeys.EXPERIENCE_CLOUD_ID, ecid);

		// add Experience Cloud Org ID
		theIdString = appendKVPToVisitorIdString(theIdString, IdentityConstants.UrlKeys.EXPERIENCE_CLOUD_ORG_ID, orgId);

		// after the adobe_mc string is created, encode the idString before adding it to the url
		urlFragment.append(IdentityConstants.UrlKeys.PAYLOAD);
		urlFragment.append("=");

		String encodedIdString = urlEncode(theIdString);
		if (encodedIdString != null) {
			urlFragment.append(encodedIdString);
		}

		return urlFragment.toString();
	}

	/**
	 * Takes in a key-value pair and appends it to the source string
	 * <p>
	 * This method <b>does not</b> URL encode the provided {@code value} on the resulting string.
	 * If encoding is needed, make sure that the values are encoded before being passed into this function.
	 *
	 * @param originalString {@link String} to append the key and value to
	 * @param key key to append
	 * @param value value to append
	 *
	 * @return a new string with the key and value appended, or {@code originalString}
	 *         if {@code key} or {@code value} are null or empty
	 */
	static String appendKVPToVisitorIdString(final String originalString, final String key, final String value) {
		// quickly return original string if key or value are empty
		if (Utils.isNullOrEmpty(key) || Utils.isNullOrEmpty(value)) {
			return originalString;
		}

		// get the value for the new variable
		final String newUrlVariable = String.format("%s=%s", key, value);

		// if the original string is not empty, we need to append a pipe before we return
		if (Utils.isNullOrEmpty(originalString)) {
			return newUrlVariable;
		} else {
			return String.format("%s|%s", originalString, newUrlVariable);
		}
	}

	/**
	 * Encodes an URL given as {@code String}.
	 *
	 * @param unencodedString nullable {@link String} value to be encoded
	 * @return the encoded {@code String}
	 */
	static String urlEncode(final String unencodedString) {
		// bail fast
		if (unencodedString == null) {
			return null;
		}

		try {
			final byte[] stringBytes = unencodedString.getBytes("UTF-8");
			final int len = stringBytes.length;
			int curIndex = 0;

			// iterate looking for any characters that don't match our "safe" mask
			while (curIndex < len && utf8Mask[stringBytes[curIndex] & ALL_BITS_ENABLED]) {
				curIndex++;
			}

			// if our iterator got all the way to the end of the string, no unsafe characters existed
			// and it's safe to return the original value that was passed in
			if (curIndex == len) {
				return unencodedString;
			}

			// if we get here we know there's at least one character we need to encode
			final StringBuilder encodedString = new StringBuilder(stringBytes.length << 1);

			// if i > than 1 then we have some characters we can just "paste" in
			if (curIndex > 0) {
				encodedString.append(new String(stringBytes, 0, curIndex, "UTF-8"));
			}

			// rip through the rest of the string character by character
			for (; curIndex < len; curIndex++) {
				encodedString.append(encodedChars[stringBytes[curIndex] & ALL_BITS_ENABLED]);
			}

			// return the completed string
			return encodedString.toString();
		} catch (UnsupportedEncodingException e) {
			MobileCore.log(
				LoggingMode.DEBUG,
				LOG_TAG,
				String.format("Failed to url encode string %s (%s)", unencodedString, e)
			);
			return null;
		}
	}
}
