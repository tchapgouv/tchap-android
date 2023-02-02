<<<<<<< HEAD
# Tchap Android

Tchap Android v2 is an Android Matrix client. The app can be run on every Android devices with Android OS Lollipop and more (API 21).
=======
[![Latest build](https://github.com/vector-im/element-android/actions/workflows/build.yml/badge.svg?query=branch%3Adevelop)](https://github.com/vector-im/element-android/actions/workflows/build.yml?query=branch%3Adevelop)
[![Weblate](https://translate.element.io/widgets/element-android/-/svg-badge.svg)](https://translate.element.io/engage/element-android/?utm_source=widget)
[![Element Android Matrix room #element-android:matrix.org](https://img.shields.io/matrix/element-android:matrix.org.svg?label=%23element-android:matrix.org&logo=matrix&server_fqdn=matrix.org)](https://matrix.to/#/#element-android:matrix.org)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=vector-im_element-android&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=vector-im_element-android)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=vector-im_element-android&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=vector-im_element-android)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=vector-im_element-android&metric=bugs)](https://sonarcloud.io/summary/new_code?id=vector-im_element-android)

# Element Android

Element Android is an Android Matrix Client provided by [Element](https://element.io/). The app can be run on every Android devices with Android OS Lollipop and more (API 21).

It is a total rewrite of [Riot-Android](https://github.com/vector-im/riot-android) with a new user experience.

[<img src="resources/img/google-play-badge.png" alt="Get it on Google Play" height="60">](https://play.google.com/store/apps/details?id=im.vector.app)
[<img src="resources/img/f-droid-badge.png" alt="Get it on F-Droid" height="60">](https://f-droid.org/app/im.vector.app)

Build of develop branch: [![GitHub Action](https://github.com/vector-im/element-android/actions/workflows/build.yml/badge.svg?query=branch%3Adevelop)](https://github.com/vector-im/element-android/actions/workflows/build.yml?query=branch%3Adevelop) Nightly test status: [![allScreensTest](https://github.com/vector-im/element-android/actions/workflows/nightly.yml/badge.svg)](https://github.com/vector-im/element-android/actions/workflows/nightly.yml)
>>>>>>> v1.5.11

[<img src="resources/img/google-play-badge.png" alt="Get it on Google Play" height="60">](https://play.google.com/store/apps/details?id=fr.gouv.tchap.a)

# New Android SDK

Tchap is based on [Element](https://github.com/vector-im/element-android) with a new Android [SDK](https://github.com/matrix-org/matrix-android-sdk2) fully written in Kotlin. In order to make the early development as fast as possible, Tchap and the new SDK currently share the same git repository.

# Releases to app stores

There is some delay between when a release is created and when it appears in the app stores (Google Play Store). Here are some of the reasons:

* Not all versioned releases that appear on GitHub are considered stable. Each release is first considered beta: this continues for at least two days. If the release is stable (no serious issues or crashes are reported), then it is released as a production release in Google Play Store.
* Each release on the Google Play Store undergoes review by Google before it comes out. This can take an unpredictable amount of time. In some cases it has taken several weeks.

If you would like to receive releases more quickly (bearing in mind that they may not be stable) you have a number of options:

<<<<<<< HEAD
1. [Sign up to receive beta releases](https://play.google.com/apps/testing/fr.gouv.tchap.a) via the Google Play Store.
2. Install a [release APK](https://github.com/tchapgouv/tchap-android/releases) directly - download the relevant .apk file. Note: These are not the store versions, so you may have to uninstall the previous Tchap version before. Take care to properly logout and export your encrypted keys before.
=======
1. [Sign up to receive beta releases](https://play.google.com/apps/testing/im.vector.app) via the Google Play Store.
2. Install a [release APK](https://github.com/vector-im/element-android/releases) directly - download the relevant .apk file and allow installing from untrusted sources in your device settings.  Note: these releases are the Google Play version, which depend on some Google services.  If you prefer to avoid that, try the latest dev builds, and choose the F-Droid version.
3. If you're really brave, install the [very latest dev build](https://github.com/vector-im/element-android/actions/workflows/build.yml?query=branch%3Adevelop) - pick a build, then click on `Summary` to download the APKs from there: `vector-Fdroid-debug` and `vector-Gplay-debug` contains the APK for the desired store. Each file contains 5 APKs. 4 APKs for every supported specific architecture of device. In doubt you can install the `universal` APK.
>>>>>>> v1.5.11

## Contributing

Please refer to [CONTRIBUTING.md](https://github.com/tchapgouv/tchap-android-v2/blob/develop/CONTRIBUTING.md) if you want to contribute on the Tchap Android project!

Come chat with the community in the dedicated Matrix [room](https://matrix.to/#/#element-android:matrix.org).

Also [this documentation](./docs/_developer_onboarding.md) can hopefully help developers to start working on the project.

## Triaging issues

Issues are triaged by community members and the Android App Team, following the [triage process](https://github.com/vector-im/element-meta/wiki/Triage-process).

We use [issue labels](https://github.com/vector-im/element-meta/wiki/Issue-labelling) to sort all incoming issues.

>>>>>>> v1.5.2
