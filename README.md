<<<<<<< HEAD
# Tchap Android
=======
[![Buildkite](https://badge.buildkite.com/ad0065c1b70f557cd3b1d3d68f9c2154010f83c4d6f71706a9.svg?branch=develop)](https://buildkite.com/matrix-dot-org/element-android/builds?branch=develop)
[![Weblate](https://translate.element.io/widgets/element-android/-/svg-badge.svg)](https://translate.element.io/engage/element-android/?utm_source=widget)
[![Element Android Matrix room #element-android:matrix.org](https://img.shields.io/matrix/element-android:matrix.org.svg?label=%23element-android:matrix.org&logo=matrix&server_fqdn=matrix.org)](https://matrix.to/#/#element-android:matrix.org)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=vector-im_element-android&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=vector-im_element-android)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=vector-im_element-android&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=vector-im_element-android)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=vector-im_element-android&metric=bugs)](https://sonarcloud.io/summary/new_code?id=vector-im_element-android)
>>>>>>> v1.4.27-RC2

Tchap Android v2 is an Android Matrix client. The app can be run on every Android devices with Android OS Lollipop and more (API 21).

[<img src="resources/img/google-play-badge.png" alt="Get it on Google Play" height="60">](https://play.google.com/store/apps/details?id=fr.gouv.tchap.a)

# New Android SDK

Tchap is based on [Element](https://github.com/vector-im/element-android) with a new Android [SDK](https://github.com/matrix-org/matrix-android-sdk2) fully written in Kotlin. In order to make the early development as fast as possible, Tchap and the new SDK currently share the same git repository.

# Releases to app stores

There is some delay between when a release is created and when it appears in the app stores (Google Play Store). Here are some of the reasons:

* Not all versioned releases that appear on GitHub are considered stable. Each release is first considered beta: this continues for at least two days. If the release is stable (no serious issues or crashes are reported), then it is released as a production release in Google Play Store.
* Each release on the Google Play Store undergoes review by Google before it comes out. This can take an unpredictable amount of time. In some cases it has taken several weeks.

If you would like to receive releases more quickly (bearing in mind that they may not be stable) you have a number of options:

1. [Sign up to receive beta releases](https://play.google.com/apps/testing/fr.gouv.tchap.a) via the Google Play Store.
2. Install a [release APK](https://github.com/tchapgouv/tchap-android/releases) directly - download the relevant .apk file. Note: These are not the store versions, so you may have to uninstall the previous Tchap version before. Take care to properly logout and export your encrypted keys before.

## Contributing

Please refer to [CONTRIBUTING.md](https://github.com/tchapgouv/tchap-android-v2/blob/develop/CONTRIBUTING.md) if you want to contribute on the Tchap Android project!
