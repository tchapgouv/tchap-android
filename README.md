# Tchap Android

Tchap Android is an Android Matrix Client provided by [DINUM](https://tchap.numerique.gouv.fr/). The app can be run on every Android devices with Android OS Lollipop and more (API 21).

[<img src="resources/img/google-play-badge.png" alt="Get it on Google Play" height="60">](https://play.google.com/store/apps/details?id=fr.gouv.tchap.a)

The integrity of APKs obtained from github can be [verified](https://developer.android.com/studio/command-line/apksigner#usage-verify) using the signing certificate fingerprints:
```
SHA-256: 2799b5dc1c4ee23127bffdad325db7096f5d0b4e3856f0000305e23f61f991ac
SHA-1: 48d2a6cb6a779fc8fa3b75cd56a55cc706886205
```

# New Android SDK

Tchap is based on [Element](https://github.com/element-hq/element-android) with a new Android [SDK](https://github.com/matrix-org/matrix-android-sdk2) fully written in Kotlin. In order to make the early development as fast as possible, Tchap and the new SDK currently share the same git repository.

# Releases to app stores

There is some delay between when a release is created and when it appears in the app stores (Google Play Store). Here are some of the reasons:

* Not all versioned releases that appear on GitHub are considered stable. Each release is first considered beta: this continues for at least two days. If the release is stable (no serious issues or crashes are reported), then it is released as a production release in Google Play Store.
* Each release on the Google Play Store undergoes review by Google before it comes out. This can take an unpredictable amount of time. In some cases it has taken several weeks.

If you would like to receive releases more quickly (bearing in mind that they may not be stable) you have a number of options:

1. [Sign up to receive beta releases](https://play.google.com/apps/testing/fr.gouv.tchap.a) via the Google Play Store.
2. Install a [release APK](https://github.com/tchapgouv/tchap-android/releases) directly - download the relevant .apk file. Note: These are not the store versions, so you may have to uninstall the previous Tchap version before. Take care to properly logout and export your encrypted keys before.

## Contributing

Please refer to [CONTRIBUTING.md](https://github.com/tchapgouv/tchap-android/blob/develop/CONTRIBUTING.md) if you want to contribute on the Tchap Android project!

Come chat with the community in the dedicated Matrix [room](https://matrix.to/#/#element-android:matrix.org).

Also [this documentation](./docs/_developer_onboarding.md) can hopefully help developers to start working on the project.

## Triaging issues

Issues are triaged by community members and the Android App Team, following the [triage process](https://github.com/element-hq/element-meta/wiki/Triage-process).

We use [issue labels](https://github.com/element-hq/element-meta/wiki/Issue-labelling) to sort all incoming issues.

## Copyright and License

Copyright (c) 2018 - 2025 New Vector Ltd

This software is dual licensed by New Vector Ltd (Element). It can be used either:

(1) for free under the terms of the GNU Affero General Public License (as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version); OR

(2) under the terms of a paid-for Element Commercial License agreement between you and Element (the terms of which may vary depending on what you and Element have agreed to).

Unless required by applicable law or agreed to in writing, software distributed under the Licenses is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licenses for the specific language governing permissions and limitations under the Licenses.
