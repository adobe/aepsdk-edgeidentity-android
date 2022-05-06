# Adobe Experience Platform Edge Identity Mobile Extension


## About this project

The Adobe Experience Platform Edge Identity is a mobile extension for the [Adobe Experience Platform SDK](https://github.com/Adobe-Marketing-Cloud/acp-sdks) and requires the `MobileCore` extension. This extension enables handling of user identity data from a mobile app when using the AEP Mobile SDK and the Edge Network extension.


### Installation

Integrate the Edge Identity extension into your app by including the following in your gradle file's `dependencies`:

```
implementation 'com.adobe.marketing.mobile:edgeidentity:1.+'
implementation 'com.adobe.marketing.mobile:edge:1.+'
implementation 'com.adobe.marketing.mobile:core:1.+'
```

### Development

**Open the project**

To open and run the project, open the `code/settings.gradle` file in Android Studio

**Run demo application**

The demo app is required to have an AdMob app ID setup.

Once you opened the project in Android Studio (see above), browse to `app/res/value/secrets.xml` .  Follow the information to setup and enter an AdMob app ID.

Add App Environment ID to `app/.../EdgeIdentityApplication.kt`.   

Select the `app` runnable with your favorite simulator and run the program.

**View the platform events with Assurance**

Configure a new Assurance session by setting the Base URL to `testapp://main` and launch Assurance in the demo app by running the following command in your terminal:

```bash
$ adb shell am start -W -a  android.intent.action.VIEW -d "testapp://main?adb_validation_sessionid=ADD_YOUR_SESSION_ID_HERE" com.adobe.marketing.mobile.testapp
```

Note: replace ADD_YOUR_SESSION_ID_HERE with your Assurance session identifier.

Once the connection is established and the events list starts getting populated, you can filter the events for this extension by typing `Edge Identity` in the `Search Events` search box.

### Code Format

This project uses the code formatting tools [Spotless](https://github.com/diffplug/spotless/tree/main/plugin-gradle) with [Prettier](https://prettier.io/) and [ktlint](https://github.com/pinterest/ktlint). Formatting is applied when the project is built from Gradle and is checked when changes are submitted to the CI build system.

Prettier requires [Node version](https://nodejs.org/en/download/releases/) 10+

To enable the Git pre-commit hook to apply code formatting on each commit, run the following to update the project's git config `core.hooksPath`:
```
make init
```

## Related Projects

| Project                                                      | Description                                                  |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| [AEP SDK Sample App for Android](https://github.com/adobe/aepsdk-sample-app-android) | Contains Android sample app for the AEP SDK.                 |

## Contributing

Contributions are welcomed! Read the [Contributing Guide](./.github/CONTRIBUTING.md) for more information.

## Licensing

This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.

