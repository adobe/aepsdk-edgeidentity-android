# Adobe Experience Platform Identity Edge Mobile Extension


## About this project

The AEP Identity Edge Mobile Extension is an extension for the [Adobe Experience Platform SDK](https://github.com/Adobe-Marketing-Cloud/acp-sdks) and requires the `MobileCore` extension.


### Installation

Integrate the Identity Edge extension into your app by including the following in your gradle file's `dependencies`:

```
implementation 'com.adobe.marketing.mobile:identityedge:1.+'
implementation 'com.adobe.marketing.mobile:edge:1.+'
implementation 'com.adobe.marketing.mobile:sdk-core:1.+'
```

### Development

**Open the project**

To open and run the project, open the `code/settings.gradle` file in Android Studio

**Run demo application**

Once you opened the project in Android Studio (see above), select the `app` runnable and your favorite simulator and run the program.

**View the platform events with Assurance**

Configure a new Assurance session by setting the Base URL to `testapp://main` and launch Assurance in the demo app by running the following command in your terminal:

```bash
$ adb shell am start -W -a  android.intent.action.VIEW -d "testapp://main?adb_validation_sessionid=ADD_YOUR_SESSION_ID_HERE" com.adobe.marketing.mobile.identitytestapp
```

Note: replace ADD_YOUR_SESSION_ID_HERE with your Assurance session identifier.

Once the connection is established and the events list starts getting populated, you can filter the Identity extension events by typing `AEP` in the `Search Events` search box. See full list of available events [here](https://aep-sdks.gitbook.io/docs/beta/experience-platform-extension/experience-platform-debugging#event-types-handled-by-the-aep-mobile-extension).



## Related Projects

| Project                                                      | Description                                                  |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| [AEP SDK Sample App for Android](https://github.com/adobe/aepsdk-sample-app-android) | Contains Android sample app for the AEP SDK.                 |

## Contributing

Contributions are welcomed! Read the [Contributing Guide](./.github/CONTRIBUTING.md) for more information.

## Licensing

This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.

