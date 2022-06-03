# Adobe Experience Platform Identity for Edge Network Extension - Android

## Prerequisites

Refer to the [Getting Started Guide](getting-started.md)

## API reference

| APIs                                           |
| ---------------------------------------------- |
| [setAdvertisingIdentifier](#setAdvertisingIdentifier) |

------

### setAdvertisingIdentifier

When this API is called with a valid advertising identifier, the Identity for Edge Network extension includes the advertising identifier in the XDM Identity Map using the _GAID_ namespace. If the API is called with the empty string (`""`), `null`, or the nil UUID values, the GAID is removed from the XDM Identity Map (if previously set).

The GAID is preserved between app upgrades, is saved and restored during the standard application backup process, and is removed at uninstall.

> **Warning**  
> In order to enable collection of the user's current advertising tracking authorization selection for the provided advertising identifier, you need to install and register the [AEPEdgeConsent](https://aep-sdks.gitbook.io/docs/foundation-extensions/consent-for-edge-network) extension and update the [AEPEdge](https://aep-sdks.gitbook.io/docs/foundation-extensions/experience-platform-extension) dependency to minimum 1.4.1.

> **Note**  
> These examples require Google Play Services to be configured in your mobile application, and use the Google Mobile Ads Lite SDK. For instructions on how to import the SDK and configure your `ApplicationManifest.xml` file, see [Google Mobile Ads Lite SDK setup](https://developers.google.com/admob/android/lite-sdk).

> **Note**  
> These are just implementation examples. For more information about advertising identifiers and how to handle them correctly in your mobile application, see [Google Play Services documentation about AdvertisingIdClient](https://developers.google.com/android/reference/com/google/android/gms/ads/identifier/AdvertisingIdClient).

#### Java

##### Syntax
```java
public static void setAdvertisingIdentifier(final String advertisingIdentifier);
```

##### Example
```java
...
@Override
public void onResume() {
    super.onResume();
    ...
    new Thread(new Runnable() {
        @Override
        public void run() {
            String advertisingIdentifier = null;

            try {
                AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(getApplicationContext());
                if (adInfo != null) {
                    if (!adInfo.isLimitAdTrackingEnabled()) {
                        advertisingIdentifier = adInfo.getId();
                    } else {
                        MobileCore.log(LoggingMode.DEBUG, "ExampleActivity", "Limit Ad Tracking is enabled by the user, cannot process the advertising identifier");
                    }
                }

            } catch (IOException e) {
                // Unrecoverable error connecting to Google Play services (e.g.,
                // the old version of the service doesn't support getting AdvertisingId).
                MobileCore.log(LoggingMode.DEBUG, "ExampleActivity", "IOException while retrieving the advertising identifier " + e.getLocalizedMessage());
            } catch (GooglePlayServicesNotAvailableException e) {
                // Google Play services is not available entirely.
                MobileCore.log(LoggingMode.DEBUG, "ExampleActivity", "GooglePlayServicesNotAvailableException while retrieving the advertising identifier " + e.getLocalizedMessage());
            } catch (GooglePlayServicesRepairableException e) {
                // Google Play services is not installed, up-to-date, or enabled.
                MobileCore.log(LoggingMode.DEBUG, "ExampleActivity", "GooglePlayServicesRepairableException while retrieving the advertising identifier " + e.getLocalizedMessage());
            }

            MobileCore.setAdvertisingIdentifier(advertisingIdentifier);
        }
    }).start();
}
```

#### Kotlin

##### Syntax
```kotlin
public fun setAdvertisingIdentifier(advertisingIdentifier: String)
```
##### Example
<details>
  <summary><code>import ...</code></summary>
  
```kotlin
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import java.io.IOException
```
</details>

```kotlin
suspend fun getGAID(applicationContext: Context): String {
    var adID = ""
    try {
        val idInfo = AdvertisingIdClient.getAdvertisingIdInfo(applicationContext)
        if (idInfo.isLimitAdTrackingEnabled) {
            Log.d("ExampleActivity", "Limit Ad Tracking is enabled by the user, setting ad ID to \"\"")
            return adID
        }
        Log.d("ExampleActivity", "Limit Ad Tracking disabled; ad ID value: ${idInfo.id}")
        adID = idInfo.id
    } catch (e: GooglePlayServicesNotAvailableException) {
        Log.d("ExampleActivity", "GooglePlayServicesNotAvailableException while retrieving the advertising identifier ${e.localizedMessage}")
    } catch (e: GooglePlayServicesRepairableException) {
        Log.d("ExampleActivity", "GooglePlayServicesRepairableException while retrieving the advertising identifier ${e.localizedMessage}")
    } catch (e: IOException) {
        Log.d("ExampleActivity", "IOException while retrieving the advertising identifier ${e.localizedMessage}")
    }
    Log.d("ExampleActivity", "Returning ad ID value: $adID")
    return adID
}
```
Call site:
<details>
  <summary><code>import ...</code></summary>
  
```kotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
```
</details>

```kotlin
 // Create background coroutine scope to fetch ad ID value
val scope = CoroutineScope(Dispatchers.IO).launch {
    val adID = sharedViewModel.getGAID(context.applicationContext)
    Log.d("ExampleActivity", "Sending ad ID value: $adID to MobileCore.setAdvertisingIdentifier")
    MobileCore.setAdvertisingIdentifier(adID)
}
```