# Adobe Experience Platform Edge Identity Mobile Extension


## About this project

The Adobe Experience Platform Edge Identity is a mobile extension for the [Adobe Experience Platform SDK](https://github.com/Adobe-Marketing-Cloud/acp-sdks) and requires the `MobileCore` extension. This extension enables handling of user identity data from a mobile app when using the AEP Mobile SDK and the Edge Network extension.


### Installation

Integrate the Edge Identity extension into your app by including the following in your gradle file's `dependencies`:

```gradle
implementation 'com.adobe.marketing.mobile:edgeidentity:1.+'
implementation 'com.adobe.marketing.mobile:edge:1.+'
implementation 'com.adobe.marketing.mobile:core:1.+'
implementation 'com.adobe.marketing.mobile:edgeconsent:1.+' // Recommended when using the setAdvertisingIdentifier API
```

### Development

**Open the project**

To open and run the project, open the `code/settings.gradle` file in Android Studio.

**Data Collection mobile property prerequisites**

The test app needs to be configured with the following edge extensions before it can be used:
- Mobile Core (installed by default)
- [Edge](https://aep-sdks.gitbook.io/docs/foundation-extensions/experience-platform-extension)
- [Edge Identity](https://aep-sdks.gitbook.io/docs/foundation-extensions/identity-for-edge-network)
- [Edge Consent](https://aep-sdks.gitbook.io/docs/foundation-extensions/consent-for-edge-network) (recommended when using the setAdvertisingIdentifier API)

**Run demo application**

1. In the test app, set your `ENVIRONMENT_FILE_ID` in `EdgeIdentityApplication.kt`.
2. Select the `app` runnable with the desired emulator and run the program.

> **Note**
> To enable GAID related advertising identifier features, follow the [documentation](Documentation/README.md#advertising-identifier) for the required setup steps.

**View the platform events with Assurance**

Configure a new Assurance session by setting the Base URL to `testapp://main` and launch Assurance in the demo app by running the following command in your terminal:

```bash
$ adb shell am start -W -a  android.intent.action.VIEW -d "testapp://main?adb_validation_sessionid=ADD_YOUR_SESSION_ID_HERE" com.adobe.marketing.mobile.testapp
```

Note: replace ADD_YOUR_SESSION_ID_HERE with your Assurance session identifier.

Once the connection is established and the events list starts getting populated, you can filter the events for this extension by typing `Edge Identity` in the `Search Events` search box.

**Development on M1 Macs**  
M1 Macs may run into errors during the build process, specifically finding the npm installation directory. 

```
Execution failed for task ':spotlessInternalRegisterDependencies'.

Can't automatically determine npm executable and none was specifically supplied!

Spotless tries to find your npm executable automatically. It looks for npm in the following places:
- An executable referenced by the java system property 'npm.exec' - if such a system property exists.
- The environment variable 'NVM_BIN' - if such an environment variable exists.
- The environment variable 'NVM_SYMLINK' - if such an environment variable exists.
- The environment variable 'NODE_PATH' - if such an environment variable exists.
- In your 'PATH' environment variable

If autodiscovery fails for your system, try to set one of the environment variables correctly or
try setting the system property 'npm.exec' in the build process to override autodiscovery.
```

To address this: 
- Update Android Studio to the latest version (minimum version Bumblebee Patch 1 should address this issue) 
- Update the Android Gradle Plugin to the latest version (7.x.x as of this writing)  

If that does not address the issue, try installing node using the installer and not through homebrew: https://nodejs.org/en/download/ 

Please make sure that these build configuration changes are kept local; any build process dependencies (ex: Gradle version, packages) that are updated in this process should **not** be included in any PRs that are not specifically for updating the project's build configuration.

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

## Documentation

Additional documentation for usage and SDK architecture can be found under the [Documentation](Documentation) directory.

## Contributing

Contributions are welcomed! Read the [Contributing Guide](./.github/CONTRIBUTING.md) for more information.

## Licensing

This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.

