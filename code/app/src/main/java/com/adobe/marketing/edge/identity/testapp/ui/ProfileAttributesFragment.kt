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

package com.adobe.marketing.edge.identity.testapp.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.adobe.marketing.edge.identity.testapp.R
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.ProfileAttributes
import com.adobe.marketing.mobile.edge.identity.DeviceAttributes
import com.adobe.marketing.mobile.edge.identity.Identity
import java.util.TimeZone

class ProfileAttributesFragment : Fragment() {

    companion object {
        private const val LOG_TAG = "ProfileAttributes"
        private const val PUSH_TOKEN_ONLY = "dummy-fcm-token-only-001"
        private const val PUSH_TOKEN_WITH_TIMEZONE = "dummy-fcm-token-timezone-002"
        private const val PUSH_TOKEN_WITH_JAPAN_TIMEZONE = "dummy-fcm-token-japan-timezone-003"
        private const val JAPAN_TIMEZONE = "Asia/Tokyo"
        private const val PROFILE_TIMEZONE = "America/New_York"
        private const val FORWARDED_PUSH_PLATFORM = "fcm"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_profile_attributes, container, false)
        val timezoneField = root.findViewById<EditText>(R.id.edit_timezone)

        root.findViewById<Button>(R.id.btn_sync_timezone).setOnClickListener {
            val input = timezoneField.text.toString().trim()
            val tz = if (input.isNotEmpty()) TimeZone.getTimeZone(input) else TimeZone.getDefault()
            Log.d(LOG_TAG, "Syncing timezone: ${tz.id}")
            MobileCore.updateProfileAttributes(
                ProfileAttributes.Builder().setTimeZone(tz).build()
            )
        }

        root.findViewById<Button>(R.id.btn_sync_push_token).setOnClickListener {
            syncDeviceAttributes(PUSH_TOKEN_ONLY, null)
        }

        root.findViewById<Button>(R.id.btn_sync_push_token_timezone).setOnClickListener {
            syncDeviceAttributes(PUSH_TOKEN_WITH_TIMEZONE, PROFILE_TIMEZONE)
        }

        root.findViewById<Button>(R.id.btn_sync_push_token_japan_timezone).setOnClickListener {
            syncDeviceAttributes(PUSH_TOKEN_WITH_JAPAN_TIMEZONE, JAPAN_TIMEZONE)
        }

        root.findViewById<Button>(R.id.btn_sync_japan_timezone_bypass).setOnClickListener {
            syncTimezoneBypassConsent(JAPAN_TIMEZONE)
        }

        root.findViewById<Button>(R.id.btn_sync_new_york_timezone_bypass).setOnClickListener {
            syncTimezoneBypassConsent(PROFILE_TIMEZONE)
        }

        return root
    }

    private fun syncTimezoneBypassConsent(timeZone: String) {
        val attributes = DeviceAttributes.Builder()
            .setTimeZone(timeZone)
            .build()

        Log.i(
            LOG_TAG,
            "Calling updateDeviceAttributesBypassConsent with timeZone=$timeZone and no push token"
        )
        Identity.updateDeviceAttributesBypassConsent(attributes)
    }

    private fun syncDeviceAttributes(pushToken: String, timeZone: String?) {
        val attributes = DeviceAttributes.Builder()
            .setPushToken(pushToken)
            .apply {
                if (timeZone != null) {
                    setTimeZone(timeZone)
                }
            }
            .build()

        Log.i(
            LOG_TAG,
            "Calling updateDeviceAttributesBypassConsent with pushToken=$pushToken, " +
                "timeZone=${timeZone ?: "<unset>"}; app.id=${requireContext().packageName} " +
                "is supplied by the SDK from the app package, and the bundled rule forwards " +
                "app.platform=$FORWARDED_PUSH_PLATFORM"
        )
        Identity.updateDeviceAttributesBypassConsent(attributes)
    }
}
