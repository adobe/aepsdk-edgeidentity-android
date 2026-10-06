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

package com.adobe.marketing.edge.identity.testapp

import android.app.Application
import com.adobe.marketing.mobile.Assurance
import com.adobe.marketing.mobile.Edge
import com.adobe.marketing.mobile.LoggingMode
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.edge.consent.Consent
import com.adobe.marketing.mobile.edge.identity.Identity

class EdgeIdentityApplication : Application() {
    // Datastream ID (edge.configId) from the "Luma Mobile App - vinamra" datastream, not a Launch/Tags Environment File ID
    private var EDGE_CONFIG_ID: String = "8f56dfea-5290-4cf7-8ea2-9e5f0fb5d051"

    override fun onCreate() {
        super.onCreate()

        // register AEP SDK extensions
        MobileCore.setApplication(this)
        MobileCore.setLogLevel(LoggingMode.VERBOSE)
        MobileCore.registerExtensions(
            listOf(Edge.EXTENSION, Identity.EXTENSION, Consent.EXTENSION, Assurance.EXTENSION)
        ) {
            MobileCore.updateConfiguration(
                mapOf(
                    "edge.configId" to EDGE_CONFIG_ID,
                    "consent.default" to mapOf("consents" to mapOf("collect" to mapOf("val" to "y")))
                )
            )
        }
    }
}
