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

package com.adobe.marketing.edge.identity.app.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.adobe.marketing.edge.identity.app.R
import com.adobe.marketing.edge.identity.app.model.SharedViewModel
import com.adobe.marketing.mobile.MobileCore
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class GetIdentityFragment : Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val sharedViewModel by activityViewModels<SharedViewModel>()
        val root = inflater.inflate(R.layout.fragment_get_identity, container, false)

        val ecidTextView = root.findViewById<TextView>(R.id.text_ecid)
        sharedViewModel.ecidText.observe(viewLifecycleOwner, Observer {
            ecidTextView.text = it
        })

        val ecidLegacyTextView = root.findViewById<TextView>(R.id.text_legacy_ecid)
        sharedViewModel.ecidLegacyText.observe(viewLifecycleOwner, Observer {
            ecidLegacyTextView.text = it
        })

        val identitiesTextView = root.findViewById<TextView>(R.id.text_identities)
        sharedViewModel.identitiesText.observe(viewLifecycleOwner, Observer {
            identitiesTextView.text = it
        })

        root.findViewById<Button>(R.id.btn_get_ecid).setOnClickListener {
            val latch = CountDownLatch(2)
            var resultPrimary: String? = null
            var resultSecondary: String? = null

            com.adobe.marketing.mobile.edge.identity.Identity.getExperienceCloudId { ecid ->
                Log.d("Home", "Got ECID: $ecid")
                resultPrimary = ecid
                latch.countDown()
            }

            com.adobe.marketing.mobile.Identity.getExperienceCloudId { ecid ->
                Log.d("Home", "Got Legacy ECID: $ecid")
                resultSecondary = ecid
                latch.countDown()
            }

            latch.await(1, TimeUnit.SECONDS)

            sharedViewModel.setEcidValue(if (resultPrimary != null) "ecid: $resultPrimary" else "ecid: not found")
            sharedViewModel.setEcidLegacyValue(if (resultSecondary != null) "legacy: $resultSecondary" else "")
        }

        root.findViewById<Button>(R.id.btn_get_identities).setOnClickListener {
            val latch = CountDownLatch(1)
            var result: String? = null
            com.adobe.marketing.mobile.edge.identity.Identity.getIdentities { identities ->
                Log.d("Home", "Got Identities: $identities")
                val jsonObject = JSONObject(identities.toString())
                result = jsonObject.toString(2)
                latch.countDown()
            }

            latch.await(2, TimeUnit.SECONDS)

            sharedViewModel.setIdentitiesValue(result ?: "")
        }

        root.findViewById<Button>(R.id.btn_reset_identities).setOnClickListener {
            MobileCore.resetIdentities()
        }

        return root
    }
}
