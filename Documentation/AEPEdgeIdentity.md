# Getting Started

# Test App
## GAID Implementation
 In Android, users are automatically opted-in to ad ID tracking
They can choose to opt out of tracking in Android Settings at the device(?) level (not sure if
individual app level permissions are possible)
thus developers using ad ID should get ad ID from the API each time it is used, as permissions can change
- note: users can see their ad ID value in the settings page for Ads
- note: unlike iOS, the permission is more passive, where the user has to seek out the option to turn
off tracking, and no permissions prompt is shown on first launch, etc.


Accessibility of ad ID through AdvertisingIdClient APIs:
All devices that support Google Play Services follow the device level opt-in/out status, regardless of the app’s target SDK level.
This is enforced starting from April 1, 2022.
Prior to this, it was only applied to devices running Android 12.

Required manifest permissions to use ad ID (normal level permission):
This is only required for Android 13+ (Apps with target API level set to 33 (Android 13))
Conversely, for apps with target API level set to 32 (Android 12L) or older, this permission is not needed.
If it is not declared when required, you will get an all-zero ad ID.
In the test app's case, the current targetSdkVersion is 30, so the permission is not required.

The permission is:
<uses-permission android:name="com.google.android.gms.permission.AD_ID"/>
To prevent permission merging:
<uses-permission android:name="com.google.android.gms.permission.AD_ID" tools:node="remove"/>

note: some SDKs may already include this permission in their manifest; ex: Google Mobile Ads SDK (play-services-ads)
consider merging manifest files: https://developer.android.com/studio/build/manage-manifests#merge-manifests

Based on simulator testing:
- the opt-in/out status change or resetting the ad ID value does not affect app lifecycle;
that is, unlike iOS the app is not terminated with value or opt-in/out status changes
    - thus, the guidance for only accessing the ad ID through the API and not caching the value;
    permissions for ad tracking and/or the value of the ID itself may be changed at any time
    - practically, it may be cumbersome to detect changes at arbitrary points in the logic throughout the app;
    it may be helpful to use:
        - a getter helper for ad ID that detects and handles changes in value or opt-in/out
        - using app lifecycle foreground event to check for ad ID value or opt-in/out changes
- opt-out does not seem to cause the admob SDK to return an all-zeros ad ID (based on testing with emulator)


Google Mobile Ads Lite SDK
https://developers.google.com/admob/android/lite-sdk
API reference: https://developers.google.com/android/reference/com/google/android/gms/ads/identifier/AdvertisingIdClient
AdMob requires an application ID (free sample id given by Google for testing purposes) specified in the AndroidManifest.xml
for the app to run, when the SDK is included in the build (otherwise the app will crash).
However, the SDK doesn't have to be initialized in order to use the ad ID fetching flow:
https://developers.google.com/admob/android/quick-start#import_the_mobile_ads_sdk

AndroidX Ads SDK
https://developer.android.com/jetpack/androidx/releases/ads#1.0.0-alpha04
API reference: https://developer.android.com/reference/androidx/ads/identifier/AdvertisingIdClient
Doesn't work (also see commented implementation below):
https://stackoverflow.com/questions/59217195/how-do-i-use-or-implement-an-android-advertising-id-provider
Note: the last time the AndroidX SDK was updated was January 22, 2020: Version 1.0.0-alpha04

## Example AndroidX Ads Implementation
Note that AndroidX Ads SDK doesn't seem to work; isAdvertisingIdProviderAvailable never returns true even with valid applicationContext
```kotlin
if (AdvertisingIdClient.isAdvertisingIdProviderAvailable(context.applicationContext)) {
    val advertisingIdInfoListenableFuture = AdvertisingIdClient.getAdvertisingIdInfo(context.applicationContext)
    addCallback(advertisingIdInfoListenableFuture,
            object : FutureCallback<AdvertisingIdInfo> {
                override fun onSuccess(adInfo: AdvertisingIdInfo?) {
                    if (adInfo == null) {
                        return
                    }
                    val id = adInfo.id
                    val providerPackageName = adInfo.providerPackageName
                    val isLimitTrackingEnabled = adInfo.isLimitAdTrackingEnabled
                    Log.d("Custom_Identity_Fragment", "id: $id, providerPackageName: $providerPackageName, isLimitTrackingEnabled: $isLimitTrackingEnabled")
                }

                override fun onFailure(t: Throwable) {
                    Log.e("Custom_Identity_Fragment", "Failed to connect to Advertising ID provider: $t")
                    // Try to connect to the Advertising ID provider again, or fall
                    // back to an ads solution that doesn't require using the
                    // Advertising ID library.
                }
            },
            Executors.newSingleThreadExecutor()
    )
} else {
    Log.d("Custom_Identity_Fragment", "The Advertising ID client library is unavailable.")
    // The Advertising ID client library is unavailable. Use a different
    // library to perform any required ads use cases.
}
```