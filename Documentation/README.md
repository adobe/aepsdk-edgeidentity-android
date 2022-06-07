# Advertising identifier

## Configuration
To enable advertising identifier features in the sample app, follow these steps:
1. Update the value for key `gms_ads_app_id` located in the `secrets.xml` at [aepsdk-edgeidentity-android/code/app/src/main/res/values](../code/app/src/main/res/values/secrets.xml) with a valid Google AdMob app ID.
    - See Google's [quick start reference](https://developers.google.com/admob/android/quick-start) on how to get your AdMob app ID. See step 3 of the [Configure your app](https://developers.google.com/admob/android/quick-start#import_the_mobile_ads_sdk) section for a free public test app ID from Google.
    - Any real key values in the `secrets.xml` file should **not** be committed to the repository.
2. By default, the ad ID features are commented out in the sample app. To enable these features, uncomment the implemention code using [find and replace all](https://www.jetbrains.com/help/idea/finding-and-replacing-text-in-project.html#replace_search_string_in_project) to replace all instances of:
```java
/* Ad ID implementation
```
with:
```java
//* Ad ID implementation
```
Each code block has a pair of block comments wrapped around it to enable this behavior:
```java
/* Ad ID implementation (pt. 1/4)
<commented implementation code...>
/* Ad ID implementation (pt. 1/4) */
```

After replacement it will become:
```java
//* Ad ID implementation (pt. 1/4)
<active implementation code!>
//* Ad ID implementation (pt. 1/4) */
```

For convenience, these are the default find and replace shortcuts in Android Studio:  
[<img src="./assets/find-and-replace-shortcuts.png" alt="Default shortcuts for find and replace" width="500"/>](./assets/find-and-replace-shortcuts.png)  

The shortcut should open a window that looks like the following:
[<img src="./assets/find-and-replace-all-example.png" alt="Example of find and replace" width="500"/>](./assets/find-and-replace-all-example.png)  
There should be 5 pairs of special comment blocks (10 total matches) across two files:
`app/build.gradle`, `CustomIdentityFragment.kt`, and `SharedViewModel.kt` 

3. With the implementation code and gradle files uncommented with new dependencies, sync the project with the Gradle file changes using: File -> Sync Project with Gradle Files

[<img src="./assets/sync-project-gradle-example.png" alt="Example of find and replace" width="500"/>](./assets/sync-project-gradle-example.png)  

The app should now be properly configured to use advertising identifier features.

To **disable** these features, follow these steps:
1. [Find and replace](https://www.jetbrains.com/help/idea/finding-and-replacing-text-in-project.html#replace_search_string_in_project) all instances of:
```java
//* Ad ID implementation
```
with:
```java
/* Ad ID implementation
```
2. Sync Project with Gradle files using: File -> Sync Project with Gradle Files