## Adobe Experience Platform Edge Identity Mobile Extension

The Adobe Experience Platform Identity for Edge Network mobile extension enables identity management from your mobile app when using the Adobe Experience Platform Mobile SDK and the Edge Network extension.

## Configure the Identity extension in Data Collection UI
1. Log into [Adobe Experience Platform Data Collection](https://experience.adobe.com/data-collection).
2. From **Tags**, locate or search for your Tag mobile property.
3. In your mobile property, select **Extensions** tab.
4. On the **Catalog** tab, locate or search for the **Identity** extension, and select **Install**.
5. There are no configuration settings for **Identity**.
6. Follow the publishing process to update SDK configuration.

## Add the Identity extension to your app

The Adobe Experience Platform Identity for Edge Network extension depends on the following extensions:

- [Mobile Core](https://github.com/adobe/aepsdk-core-android)
- [Edge Network](https://github.com/adobe/aepsdk-edge-android) (required for handling requests to Adobe Edge Network)

### Download and import the Identity extension

> **Note** The following instructions are for configuring an application using Adobe Experience Platform Edge mobile extensions. If an application will include both Edge Network and Adobe Solution extensions, both the Identity for Edge Network and Identity for Experience Cloud ID Service extensions are required. Find more details in the [Frequently Asked Questions](frequently-asked-questions.md) page.

1. Add the Mobile Core, Edge, and Edge Identity extensions to your project using the app's Gradle file:
  
```gradle
implementation 'com.adobe.marketing.mobile:core:2.+'
implementation 'com.adobe.marketing.mobile:edge:2.+'
implementation 'com.adobe.marketing.mobile:edgeidentity:2.+'
   ```
> **Warning**
> Using dynamic dependency versions is not recommended for production apps. Refer to this [page](https://github.com/adobe/aepsdk-core-android/blob/main/Documentation/MobileCore/gradle-dependencies.md) for managing gradle dependencies.

2. Import the libraries:
#### Java
```java
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.Edge;
import com.adobe.marketing.mobile.edge.identity.Identity;
```
#### Kotlin
```kotlin
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.Edge
import com.adobe.marketing.mobile.edge.identity.Identity
```


### Register the Identity for Edge Extension with MobileCore

#### Java

```java
public class MobileApp extends Application {
    // Set up the preferred Environment File ID from your mobile property configured in Data Collection UI
    private final String ENVIRONMENT_FILE_ID = "";

    @Override
    public void onCreate() {
      super.onCreate();
      MobileCore.setApplication(this);
      MobileCore.configureWithAppID(ENVIRONMENT_FILE_ID);

      // Register Adobe Experience Platform SDK extensions
      MobileCore.registerExtensions(
         Arrays.asList(Edge.EXTENSION, Identity.EXTENSION),
         o -> Log.debug("MobileApp", "MobileApp", "Adobe Experience Platform Mobile SDK initialized.")
       );
    }
}
```
#### Kotlin

```kotlin
class MobileApp : Application() {
    // Set up the preferred Environment File ID from your mobile property configured in Data Collection UI
    private var ENVIRONMENT_FILE_ID: String = ""
    override fun onCreate() {
        super.onCreate()
        MobileCore.setApplication(this)
        MobileCore.configureWithAppID(ENVIRONMENT_FILE_ID)
        // Register Adobe Experience Platform SDK extensions
        MobileCore.registerExtensions(
            listOf(Edge.EXTENSION, Identity.EXTENSION)
        ) {
            Log.debug("MobileApp", "MobileApp", "Adobe Experience Platform Mobile SDK initialized.")
        }
    }
}
```