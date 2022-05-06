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

package com.adobe.marketing.edge.identity.app

import android.app.Application
import com.adobe.marketing.mobile.Assurance
import com.adobe.marketing.mobile.Edge
import com.adobe.marketing.mobile.LoggingMode
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.edge.consent.Consent
import com.adobe.marketing.mobile.edge.identity.Identity

class EdgeIdentityApplication : Application() {
    // Add your app environment id to configure the SDK from your Data Collection tag property
    private var APP_ENVIRONMENT_ID: String = "yourAppId"

    override fun onCreate() {
        super.onCreate()

        // register AEP SDK extensions
        MobileCore.setApplication(this)
        MobileCore.setLogLevel(LoggingMode.VERBOSE)

        Consent.registerExtension()
        Identity.registerExtension()
        Edge.registerExtension()
        Assurance.registerExtension()

        MobileCore.start {
            MobileCore.configureWithAppID(APP_ENVIRONMENT_ID)
        }
    }
}
