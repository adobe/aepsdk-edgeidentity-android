/* ******************************************************************************
 * ADOBE CONFIDENTIAL
 *  ___________________
 *
 *  Copyright 2021 Adobe
 *  All Rights Reserved.
 *
 *  NOTICE: All information contained herein is, and remains
 *  the property of Adobe and its suppliers, if any. The intellectual
 *  and technical concepts contained herein are proprietary to Adobe
 *  and its suppliers and are protected by all applicable intellectual
 *  property laws, including trade secret and copyright laws.
 *  Dissemination of this information or reproduction of this material
 *  is strictly forbidden unless prior written permission is obtained
 *  from Adobe.
 ******************************************************************************/

package com.adobe.marketing.mobile.testApp;

import android.app.Application;
import android.util.Log;

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.Edge;
import com.adobe.marketing.mobile.InvalidInitException;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.Signal;
import com.adobe.marketing.mobile.edge.identity.Identity;

public class TestApplication extends Application {
    private static final String LOG_TAG = "EdgeIdentityTestApp";

    // TODO: fill in your Launch environment ID here
    private final String LAUNCH_ENVIRONMENT_ID = "";

    @Override
    public void onCreate() {
        super.onCreate();
        MobileCore.setApplication(this);

        MobileCore.setLogLevel(LoggingMode.VERBOSE);

		/* Launch generates a unique environment ID that the SDK uses to retrieve your
		configuration. This ID is generated when an app configuration is created and published to
		a given environment. It is strongly recommended to configure the SDK with the Launch
		environment ID.
		*/
        MobileCore.configureWithAppID(LAUNCH_ENVIRONMENT_ID);

        // register Adobe core extensions
        try {
            Signal.registerExtension();
            Identity.registerExtension();
            Edge.registerExtension();
            Assurance.registerExtension();
        } catch (InvalidInitException e) {
            e.printStackTrace();
        }

        // once all the extensions are registered, call MobileCore.start(...) to start processing the events
        MobileCore.start(new AdobeCallback() {
            @Override
            public void call(final Object o) {
                Log.d(LOG_TAG, "Mobile SDK was initialized");
            }
        });
    }

}
