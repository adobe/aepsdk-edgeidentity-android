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
import java.util.TimeZone

class ProfileAttributesFragment : Fragment() {

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
            Log.d("ProfileAttributes", "Syncing timezone: ${tz.id}")
            MobileCore.updateProfileAttributes(
                ProfileAttributes.Builder().setTimeZone(tz).build()
            )
        }

        return root
    }
}
