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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.adobe.marketing.edge.identity.app.R
import com.adobe.marketing.edge.identity.app.model.SharedViewModel
import com.adobe.marketing.mobile.*
import kotlin.random.Random

class MultipleIdentityFragment : Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val sharedViewModel by activityViewModels<SharedViewModel>()

        val root = inflater.inflate(R.layout.fragment_multiple_identity, container, false)

        // Setup radio button for Edge Identity which attaches observes to change click and text label
        val edgeRegisteredRadioButton = root.findViewById<RadioButton>(R.id.radio_edge_identity_registered)
        edgeRegisteredRadioButton.isChecked = sharedViewModel.isEdgeIdentityRegistered.value ?: true
        edgeRegisteredRadioButton.text = sharedViewModel.edgeIdentityRegisteredText.value
        sharedViewModel.isEdgeIdentityRegistered.observe(viewLifecycleOwner, Observer {
            edgeRegisteredRadioButton.isChecked = it
        })
        sharedViewModel.edgeIdentityRegisteredText.observe(viewLifecycleOwner, Observer {
            edgeRegisteredRadioButton.text = it
        })

        // Setup radio button for direct Identity which attaches observes to change click and text label
        val directRegisteredRadioButton = root.findViewById<RadioButton>(R.id.radio_direct_identity_registered)
        directRegisteredRadioButton.isChecked = sharedViewModel.isDirectIdentityRegistered.value ?: false
        directRegisteredRadioButton.text = sharedViewModel.directIdentityRegisteredText.value
        sharedViewModel.isDirectIdentityRegistered.observe(viewLifecycleOwner, Observer {
            directRegisteredRadioButton.isChecked = it
        })
        sharedViewModel.directIdentityRegisteredText.observe(viewLifecycleOwner, Observer {
            directRegisteredRadioButton.text = it
        })
        directRegisteredRadioButton.setOnClickListener {
            // There is no API to unregister an extension, so only handle registration case
            if (sharedViewModel.isDirectIdentityRegistered.value == false) {
                com.adobe.marketing.mobile.Identity.registerExtension()
                sharedViewModel.toggleDirectIdentityRegistration()
            }
        }

        // Send a sample event to Edge
        root.findViewById<Button>(R.id.btn_edge_send_event).setOnClickListener {
            val event = ExperienceEvent.Builder()
            event.setXdmSchema(mapOf("eventType" to "com.adobe.edge.identity"))
            Edge.sendEvent(event.build(), null)
        }

        // Trigger state change button, to cause direct Identity state change event
        val triggerStateChangeButton = root.findViewById<Button>(R.id.btn_direct_trigger_state_change)
        triggerStateChangeButton.setOnClickListener {
            // If the identifier is the same, Identity won't change state
            val randomId = Random.nextInt().toString()
            com.adobe.marketing.mobile.Identity.syncIdentifier("idType", randomId, VisitorID.AuthenticationState.AUTHENTICATED)
        }

        // Clear Edge Identity persistence
        val edgeClearPersistence = root.findViewById<Button>(R.id.btn_edge_clear_persistence)
        edgeClearPersistence.setOnClickListener {
            val sharedPreferences = activity?.getSharedPreferences("com.adobe.edge.identity", Context.MODE_PRIVATE) ?: return@setOnClickListener
            with (sharedPreferences.edit()) {
                clear()
                commit()
            }
        }

        // Clear direct Identity persistence
        val directClearPersistence = root.findViewById<Button>(R.id.btn_direct_clear_persistence)
        directClearPersistence.setOnClickListener {
            val sharedPreferences = activity?.getSharedPreferences("visitorIDServiceDataStore", Context.MODE_PRIVATE) ?: return@setOnClickListener
            with (sharedPreferences.edit()) {
                clear()
                commit()
            }
        }

        // Set privacy status to Opt-In
        val privacyOptInButton = root.findViewById<Button>(R.id.btn_set_privacy_optin)
        privacyOptInButton.setOnClickListener {
            MobileCore.setPrivacyStatus(MobilePrivacyStatus.OPT_IN)
        }

        // Set privacy status to Opt-Out
        val privacyOptOutButton = root.findViewById<Button>(R.id.btn_set_privacy_optout)
        privacyOptOutButton.setOnClickListener {
            MobileCore.setPrivacyStatus(MobilePrivacyStatus.OPT_OUT)
        }

        return root
    }
}
