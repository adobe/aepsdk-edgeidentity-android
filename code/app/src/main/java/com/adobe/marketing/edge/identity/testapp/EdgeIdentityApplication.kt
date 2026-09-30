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
import android.net.Uri
import android.util.Log
import com.adobe.marketing.mobile.AdobeCallbackWithError
import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.EventSource
import com.adobe.marketing.mobile.EventType
import com.adobe.marketing.mobile.LoggingMode
import com.adobe.marketing.mobile.MobileCore

class EdgeIdentityApplication : Application() {
    companion object {
        private const val LOG_TAG = "EdgeIdentityApplication"
        private const val CONFIGURATION_RETRIEVE_DATA = "config.getData"
        private const val LAUNCH_APP_ID =
            "staging/1b50a869c4a2/72557653d422/launch-51bcfc552b32"
    }

    override fun onCreate() {
        super.onCreate()

        MobileCore.setLogLevel(LoggingMode.VERBOSE)
        // Load configuration and rules through this Launch environment; no root bundled assets are used.
        MobileCore.initialize(this, LAUNCH_APP_ID) {
            logActiveConfiguration()
        }
    }

    private fun logActiveConfiguration() {
        val event = Event.Builder(
            "Get Active Configuration",
            EventType.CONFIGURATION,
            EventSource.REQUEST_CONTENT
        ).setEventData(mapOf(CONFIGURATION_RETRIEVE_DATA to true)).build()

        MobileCore.dispatchEventWithResponseCallback(
            event,
            5000,
            object : AdobeCallbackWithError<Event> {
                override fun call(response: Event) {
                    val configuration = response.eventData.orEmpty()
                    val rulesUrl = configuration["rules.url"] as? String
                    val launchEnvironmentId = rulesUrl
                        ?.let { Uri.parse(it).pathSegments.getOrNull(2) }
                        ?: "not set"
                    val buildEnvironment = configuration["build.environment"] ?: "not set"
                    val edgeEnvironment = configuration["edge.environment"] ?: "not set"
                    val edgeConfigId = configuration["edge.configId"] ?: "not set"

                    Log.i(
                        LOG_TAG,
                        "Active configuration shared state: build.environment=$buildEnvironment, " +
                            "Launch environmentId=$launchEnvironmentId, " +
                            "edge.environment=$edgeEnvironment, edge.configId=$edgeConfigId"
                    )
                }

                override fun fail(error: com.adobe.marketing.mobile.AdobeError) {
                    Log.e(LOG_TAG, "Failed to retrieve active configuration shared state: $error")
                }
            }
        )
    }
}
