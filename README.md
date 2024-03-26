# Adobe Experience Platform Edge Identity Mobile Extension
[![Maven Central](https://img.shields.io/maven-central/v/com.adobe.marketing.mobile/edgeidentity.svg?logo=android&logoColor=white&label=edgeidentity)](https://mvnrepository.com/artifact/com.adobe.marketing.mobile/edgeidentity)

## About this project

The Adobe Experience Platform Identity for Edge Network mobile extension enables identity management from your mobile app when using the [Adobe Experience Platform Mobile SDK](https://developer.adobe.com/client-sdks/documentation) and the Edge Network extension.


### Installation

Integrate the Identity for Edge Network mobile extension into your app by following the [getting started guide](Documentation/getting-started.md).

### Development

**Open the project**

To open and run the project, open the `code/settings.gradle` file in Android Studio.

**Run the test app**

To configure and run the test app for this project, follow the [getting started guide for the test app](Documentation/getting-started-test-app.md).

**Development on M1 Macs**

If you are seeing any build failures when running the project for the first time on your M1 machine, check out the [troubleshooting guides](Documentation/troubleshooting-guide.md).

#### Code Format

This project uses the code formatting tools [Spotless](https://github.com/diffplug/spotless/tree/main/plugin-gradle) with [Prettier](https://prettier.io/) and [ktlint](https://github.com/pinterest/ktlint). Formatting is applied when the project is built from Gradle and is checked when changes are submitted to the CI build system.

Prettier requires [Node version](https://nodejs.org/en/download/releases/) 10+

To enable the Git pre-commit hook to apply code formatting on each commit, run the following to update the project's git config `core.hooksPath`:
```
make init
```

## Related Projects

| Project                                                      | Description                                                  |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| [Core extensions](https://github.com/adobe/aepsdk-core-android) | The Mobile Core represents the foundation of the Adobe Experience Platform mobile SDK.               |
| [Edge Network extension](https://github.com/adobe/aepsdk-edge-android) | The Edge Network extension allows you to send data to the Adobe Edge Network from a mobile application.               |
| [Consent for Edge Network extension](https://github.com/adobe/aepsdk-edgeconsent-android) | The Consent extension enables consent preferences collection from your mobile app when using the AEP SDK and the Edge Network extension.              |
| [Assurance extension](https://github.com/adobe/aepsdk-assurance-android) | The Assurance extension enables validation workflows for your SDK implementation.                |

## Documentation

Additional documentation for usage and SDK architecture can be found under the [Documentation](Documentation) directory.

## Contributing

Contributions are welcomed! Read the [Contributing Guide](./.github/CONTRIBUTING.md) for more information.

## Licensing

This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.

