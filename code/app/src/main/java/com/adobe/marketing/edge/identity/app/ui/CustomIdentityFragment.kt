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
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.adobe.marketing.edge.identity.app.R
import com.adobe.marketing.edge.identity.app.model.SharedViewModel
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.edge.consent.Consent
import com.adobe.marketing.mobile.edge.identity.AuthenticatedState
import com.adobe.marketing.mobile.edge.identity.Identity
import com.adobe.marketing.mobile.edge.identity.IdentityItem
import com.adobe.marketing.mobile.edge.identity.IdentityMap
/* Ad ID implementation (pt. 4/5)
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
/* Ad ID implementation (pt. 4/5) */*/

private const val LOG_TAG = "Custom_Identity_Fragment"
private const val ZERO_ADVERTISING_ID = "00000000-0000-0000-0000-000000000000"

class CustomIdentityFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val sharedViewModel by activityViewModels<SharedViewModel>()

        val root = inflater.inflate(R.layout.fragment_custom_identity, container, false)

        val adIdEditText = root.findViewById<EditText>(R.id.text_ad_id)
        adIdEditText.setText(sharedViewModel.adId.value)
        adIdEditText.doAfterTextChanged {
            sharedViewModel.setAdId(it.toString())
        }

        val identifierEditText = root.findViewById<EditText>(R.id.text_identifier)
        identifierEditText.setText(sharedViewModel.identifier.value)
        identifierEditText.doAfterTextChanged {
            sharedViewModel.setIdentifier(it.toString())
        }

        val namespaceEditText = root.findViewById<EditText>(R.id.text_namespace)
        namespaceEditText.setText(sharedViewModel.namespace.value)
        namespaceEditText.doAfterTextChanged {
            sharedViewModel.setNamespace(it.toString())
        }

        val isPrimaryCheckbox = root.findViewById<CheckBox>(R.id.checkbox_is_primary)
        isPrimaryCheckbox.isChecked = sharedViewModel.isPrimary.value ?: false
        isPrimaryCheckbox.setOnCheckedChangeListener { _, isChecked ->
            sharedViewModel.setIsPrimary(isChecked)
        }

        val authenticatedRadioGroup = root.findViewById<RadioGroup>(R.id.radio_group_authenticated)
        sharedViewModel.authenticatedStateId.value?.let { checkedId ->
            authenticatedRadioGroup.findViewById<RadioButton>(checkedId).isChecked = true
        }
        authenticatedRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            val changedRadioButton: RadioButton = group.findViewById(checkedId)
            if (changedRadioButton.isChecked) {
                sharedViewModel.setAuthenticatedState(AuthenticatedState.fromString(changedRadioButton.text.toString()))
                sharedViewModel.setAuthenticatedStateId(checkedId)
            }
        }

        root.findViewById<Button>(R.id.btn_update_identities).setOnClickListener {
            val identifier: String? = sharedViewModel.identifier.value
            val namespace: String? = sharedViewModel.namespace.value
            val authenticatedState: AuthenticatedState? = sharedViewModel.authenticatedState.value
            val isPrimary: Boolean = sharedViewModel.isPrimary.value ?: false

            if (identifier != null && namespace != null) {
                val item = IdentityItem(identifier, authenticatedState, isPrimary)
                val map = IdentityMap()
                map.addItem(item, namespace)
                Identity.updateIdentities(map)
            }
        }

        root.findViewById<Button>(R.id.btn_remove_identities).setOnClickListener {
            val identifier: String? = sharedViewModel.identifier.value
            val namespace: String? = sharedViewModel.namespace.value
            val authenticatedState: AuthenticatedState? = sharedViewModel.authenticatedState.value
            val isPrimary: Boolean = sharedViewModel.isPrimary.value ?: false

            if (identifier != null && namespace != null) {
                val item = IdentityItem(identifier, authenticatedState, isPrimary)
                Identity.removeIdentity(item, namespace)
            }
        }

        // Advertising identifier features

        // Button for Set Ad ID behavior
        // Sets the advertising identifier set in the corresponding textfield using the MobileCore API
        root.findViewById<Button>(R.id.btn_set_ad_id).setOnClickListener {
            val adId: String? = sharedViewModel.adId.value
            MobileCore.setAdvertisingIdentifier(adId)
        }

        // Default hint for how to enable ad ID features; overwritten by actual implementation when ad ID features are enabled.
        root.findViewById<Button>(R.id.btn_get_gaid).setOnClickListener {
            Log.d(LOG_TAG, "For complete instructions on how to enable ad ID features, please see ./Documentation/advertising-identifier.md")
        }

        /* Ad ID implementation (pt. 5/5)
        // For details on implementation tips and differences between Google Play Services Ads vs AndroidX Ads,
        // please see the project Documentation -> AEPEdgeIdentity.md : Android Test App -> Testing tips with Android advertising identifier
        root.findViewById<Button>(R.id.btn_get_gaid).setOnClickListener {
            val context = context
            Log.d(LOG_TAG, "context: $context, appContext: ${context?.applicationContext}")
            if (context != null) {
                // Create IO (background) coroutine scope to fetch ad ID value
                val scope = CoroutineScope(Dispatchers.IO).launch {
                    val adID = sharedViewModel.getGAID(context.applicationContext)
                    Log.d(LOG_TAG, "Sending ad ID value: $adID to MobileCore.setAdvertisingIdentifier")
                    MobileCore.setAdvertisingIdentifier(adID)
                }
            }
        }
        /* Ad ID implementation (pt. 5/5) */*/

        root.findViewById<Button>(R.id.btn_set_ad_id_empty_string).setOnClickListener {
            Log.d(LOG_TAG, "Setting advertising identifier to: \"\"")
            MobileCore.setAdvertisingIdentifier("")
        }

        root.findViewById<Button>(R.id.btn_set_ad_id_null).setOnClickListener {
            Log.d(LOG_TAG, "Setting advertising identifier to: null")
            MobileCore.setAdvertisingIdentifier(null)
        }

        root.findViewById<Button>(R.id.btn_set_ad_id_all_zeros).setOnClickListener {
            Log.d(LOG_TAG, "Setting advertising identifier to: $ZERO_ADVERTISING_ID")
            MobileCore.setAdvertisingIdentifier(ZERO_ADVERTISING_ID)
        }

        root.findViewById<Button>(R.id.btn_get_consents).setOnClickListener {
            Consent.getConsents { consents ->
                Log.d(LOG_TAG, "Got Consents: $consents")
            }
        }

        return root
    }
}
