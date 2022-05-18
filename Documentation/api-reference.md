# Adobe Experience Platform Identity for Edge Network Extension - Android

## Prerequisites

Refer to the [Getting Started Guide](getting-started.md)

## API reference

| APIs                                           |
| ---------------------------------------------- |
| [extensionVersion](#extensionVersion)          |
| [getExperienceCloudId](#getExperienceCloudId)  |
| [getIdentities](#getIdentities)                |
| [getUrlVariables](#getUrlVariables)            |
| [registerExtension](#registerExtension)        |
| [removeIdentity](#removeIdentity)              |
| [resetIdentities](#resetIdentities)            |
| [updateIdentities](#updateIdentities)          |

------

### extensionVersion

The extensionVersion() API returns the version of the Identity for Edge Network extension.

#### Java

##### Syntax
```java
public static String extensionVersion()
```

##### Example
```java
String extensionVersion = Identity.extensionVersion();
```
------

### getExperienceCloudId

This API retrieves the Experience Cloud ID (ECID) that was generated when the app was initially launched. This ID is preserved between app upgrades, is saved and restored during the standard application backup process, and is removed at uninstall.

> The ECID value is returned via the `AdobeCallback`. When `AdobeCallbackWithError` is provided to this API, the timeout value is 500ms. If the operation times out or an unexpected error occurs, the `fail` method is called with the appropriate `AdobeError`.

#### Java

##### Syntax
```java
public static void getExperienceCloudId(final AdobeCallback<String> callback);
```

* _callback_ is invoked after the ECID is available. The callback may be invoked on a different thread.

##### Example
```java
Identity.getExperienceCloudId(new AdobeCallback<String>() {    
    @Override    
    public void call(String id) {        
         //Handle the ID returned here    
    }
});
```

------

### getIdentities

Get all the identities in the Identity for Edge Network extension, including customer identifiers which were previously added.

> When `AdobeCallbackWithError` is provided, and you are fetching the identities from the Mobile SDK, the timeout value is 500ms. If the operation times out or an unexpected error occurs, the `fail` method is called with the appropriate `AdobeError`.

#### Java

##### Syntax
```java
public static void getIdentities(final AdobeCallback<IdentityMap> callback);
```
* _callback_ is invoked after the identities are available. The return format is an instance of [IdentityMap](api-reference.md#identitymap). The callback may be invoked on a different thread.

##### Example
```java
Identity.getIdentities(new AdobeCallback<IdentityMap>() {    
    @Override    
    public void call(IdentityMap identityMap) {        
         //Handle the IdentityMap returned here    
    }
});
```

------

### getUrlVariables
> :information_source: This API is available with version 1.1.0 and above.

This API returns the identifiers in URL query parameter format for consumption in **hybrid mobile applications**. There is no leading & or ? punctuation as the caller is responsible for placing the variables in their resulting URL in the correct locations. If an error occurs while retrieving the URL variables, the callback handler will be called with a null value. Otherwise, the encoded string is returned, for example: `"adobe_mc=TS%3DTIMESTAMP_VALUE%7CMCMID%3DYOUR_ECID%7CMCORGID%3D9YOUR_EXPERIENCE_CLOUD_ID"`

* The `adobe_mc` attribute is an URL encoded list that contains:
  * `MCMID` - Experience Cloud ID \(ECID\)
  * `MCORGID` - Experience Cloud Org ID
  * `TS` - A timestamp taken when this request was made

> :memo: When `AdobeCallbackWithError` is provided, and you are fetching the url variables from the Mobile SDK, the timeout value is 500ms. If the operation times out or an unexpected error occurs, the `fail` method is called with the appropriate `AdobeError`.

#### Java

##### Syntax
```java
public static void getUrlVariables(final AdobeCallback<String> callback);
```
* _callback_ has an NSString value that contains the visitor identifiers as a query string after the service request is complete.

##### Example
```java
Identity.getUrlVariables(new AdobeCallback<String>() {    
    @Override    
    public void call(String urlVariablesString) {        
        //handle the URL query parameter string here
        //For example, open the URL in a webView  
        WebView webView;
        webView = (WebView)findViewById(R.id.your_webview); // initialize with your webView
        webview.loadUrl("https://example.com?" + urlVariablesString);
    }
});
```

------

### registerExtension

Registers the Identity for Edge Network extension with the Mobile Core extension.

> :information_source: If your use-case covers both Edge Network and Adobe Experience Cloud Solutions extensions, you need to register Identity for Edge Network and Identity for Experience Cloud Identity Service from Mobile Core extensions. For more details, see the [frequently asked questions](https://aep-sdks.gitbook.io/docs/foundation-extensions/identity-for-edge-network/identity-faq#q-i-am-using-aep-edge-and-adobe-solutions-extensions-which-identity-extension-should-i-install-and-register).

#### Java

##### Syntax
```java
public static void registerExtension()
```

##### Example
```java
import com.adobe.marketing.mobile.edge.identity.Identity

...
Identity.registerExtension();
```

------

### removeIdentity

Remove the identity from the stored client-side [IdentityMap](#identitymap). The Identity extension will stop sending the identifier to the Edge Network. Using this API does not remove the identifier from the server-side User Profile Graph or Identity Graph.

Identities with an empty _id_ or _namespace_ are not allowed and are ignored.

Removing identities using a reserved namespace is not allowed using this API. The reserved namespaces are:

* ECID
* IDFA
* GAID

#### Java

##### Syntax
```java
public static void removeIdentity(final IdentityItem item, final String namespace);
```

##### Example
```java
IdentityItem item = new IdentityItem("user@example.com");
Identity.removeIdentity(item, "Email");
```

------

### resetIdentities

Clears all identities stored in the Identity extension and generates a new Experience Cloud ID (ECID). Using this API does not remove the identifiers from the server-side User Profile Graph or Identity Graph.

This is a destructive action, since once an ECID is removed it cannot be reused. The new ECID generated by this API can increase metrics like unique visitors when a new user profile is created.

Some example use cases for this API are:
* During debugging, to see how new ECIDs (and other identifiers paired with it) behave with existing rules and metrics.
* A last-resort reset for when an ECID should no longer be used.

This API is not recommended for:
* Resetting a user's consent and privacy settings; see [Privacy and GDPR](https://aep-sdks.gitbook.io/docs/resources/privacy-and-gdpr).
* Removing existing custom identifiers; use the [`removeIdentity`](#removeidentity) API instead.
* Removing a previously synced advertising identifier after the advertising tracking settings were changed by the user; use the [`setAdvertisingIdentifier`](https://aep-sdks.gitbook.io/docs/foundation-extensions/mobile-core/identity/identity-api-reference#setadvertisingidentifier) API instead.

> :warning: The Identity for Edge Network extension does not read the Mobile SDK's privacy status, and therefore setting the SDK's privacy status to opt-out will not automatically clear the identities from the Identity for Edge Network extension. See [`MobileCore.resetIdentities`](https://aep-sdks.gitbook.io/docs/foundation-extensions/mobile-core/mobile-core-api-reference#resetidentities) for more details.

------

### updateIdentities

Update the currently known identities within the SDK. The Identity extension will merge the received identifiers with the previously saved ones in an additive manner, no identities are removed from this API.

Identities with an empty _id_ or _namespace_ are not allowed and are ignored.

Updating identities using a reserved namespace is not allowed using this API. The reserved namespaces are:

* ECID
* IDFA
* GAID

#### Java

##### Syntax
```java
public static void updateIdentities(final IdentityMap identityMap);
```

##### Example
```java
IdentityItem item = new IdentityItem("user@example.com");
IdentityMap identityMap = new IdentityMap();
identityMap.addItem(item, "Email")
Identity.updateIdentities(identityMap);
```

------

## Public Classes

### IdentityMap

Defines a map containing a set of end user identities, keyed on either namespace integration code or the namespace ID of the identity. The values of the map are an array, meaning that more than one identity of each namespace may be carried.

The format of the IdentityMap class is defined by the [XDM Identity Map Schema](https://github.com/adobe/xdm/blob/master/docs/reference/mixins/shared/identitymap.schema.md).

For more information, please read an overview of the [AEP Identity Service](https://experienceleague.adobe.com/docs/experience-platform/identity/home.html).

```text
"identityMap" : {
    "Email" : [
      {
        "id" : "user@example.com",
        "authenticatedState" : "authenticated",
        "primary" : false
      }
    ],
    "Phone" : [
      {
        "id" : "1234567890",
        "authenticatedState" : "ambiguous",
        "primary" : false
      },
      {
        "id" : "5557891234",
        "authenticatedState" : "ambiguous",
        "primary" : false
      }
    ],
    "ECID" : [
      {
        "id" : "44809014977647551167356491107014304096",
        "authenticatedState" : "ambiguous",
        "primary" : true
      }
    ]
  }
```

**Example**

```java
// Construct
IdentityMap identityMap = new IdentityMap();

// Add an item
IdentityItem item = new IdentityItem("user@example.com");
identityMap.addItem(item, "Email");

// Remove an item
IdentityItem item = new IdentityItem("user@example.com");
identityMap.removeItem(item, "Email");

// Get a list of items for a given namespace
List<IdentityItem> items = identityMap.getIdentityItemsForNamespace("Email");

// Get a list of all namespaces used in current IdentityMap
List<String> namespaces = identityMap.getNamespaces();

// Check if IdentityMap has no identities
boolean hasNotIdentities = identityMap.isEmpty();
```

------

### IdentityItem

Defines an identity to be included in an [IdentityMap](#identitymap).

The format of the IdentityItem class is defined by the [XDM Identity Item Schema](https://github.com/adobe/xdm/blob/master/docs/reference/datatypes/identityitem.schema.md).

**Example**

```java
// Construct
IdentityItem item = new IdentityItem("identifier");

IdentityItem item = new IdentityItem("identifier", AuthenticatedState.AUTHENTICATED, false);


// Getters
String id = item.getId();

AuthenticatedState state = item.getAuthenticatedState();

boolean primary = item.isPrimary();
```

------

### AuthenticatedState

Defines the state an [Identity Item](api-reference.md#identityitem) is authenticated for.

The possible authenticated states are:

* Ambiguous - the state is ambiguous or not defined
* Authenticated - the user is identified by a login or similar action
* LoggedOut - the user was identified by a login action at a previous time, but is not logged in now

**Syntax**

```java
public enum AuthenticatedState {
    AMBIGUOUS("ambiguous"),
    AUTHENTICATED("authenticated"),
    LOGGED_OUT("loggedOut");
}
```
