## Getting started

The Adobe Experience Platform Identity for Edge Network extension has the following peer dependency, which must be installed prior to installing the identity extension:
- [Mobile Core](https://aep-sdks.gitbook.io/docs/foundation-extensions/mobile-core)

## Add the AEP Identity extension to your app

### Download and import the Identity extension

> :information_source: The following instructions are for configuring an application using Adobe Experience Platform Edge mobile extensions. If an application will include both Edge Network and Adobe Solution extensions, both the Identity for Edge Network and Identity for Experience Cloud ID Service extensions are required. Find more details in the [Frequently Asked Questions](https://aep-sdks.gitbook.io/docs/foundation-extensions/identity-for-edge-network/identity-faq) page.


### Java

1. Add the Mobile Core and Edge extensions to your project using the app's Gradle file.

   ```java
   implementation 'com.adobe.marketing.mobile:core:1.+'
   implementation 'com.adobe.marketing.mobile:edge:1.+'
   implementation 'com.adobe.marketing.mobile:edgeidentity:1.+'
   ```

2. Import the Mobile Core and Edge extensions in your Application class.

   ```java
    import com.adobe.marketing.mobile.MobileCore;
    import com.adobe.marketing.mobile.Edge;
    import com.adobe.marketing.mobile.edge.identity.Identity;
   ```

3. Register the Identity for Edge Extension with MobileCore:

### Java

```java
public class MobileApp extends Application {

    @Override
    public void onCreate() {
      super.onCreate();
      MobileCore.setApplication(this);
      try {
        Edge.registerExtension();
        Identity.registerExtension();
        // register other extensions
        MobileCore.start(new AdobeCallback () {
            @Override
            public void call(Object o) {
                MobileCore.configureWithAppID("yourAppId");
            }
        });      

      } catch (Exception e) {
        ...
      }


    }
}
```
