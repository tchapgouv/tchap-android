# Contributing to Tchap Android

<!--- TOC -->

* [Android Studio settings](#android-studio-settings)
  * [Template](#template)
* [Compilation](#compilation)
* [I want to submit a PR to fix an issue](#i-want-to-submit-a-pr-to-fix-an-issue)
  * [Kotlin](#kotlin)
  * [Tchap changes](#tchap-changes)
  * [Changelog](#changelog)
  * [Code quality](#code-quality)
    * [Internal tool](#internal-tool)
    * [ktlint](#ktlint)
    * [knit](#knit)
    * [lint](#lint)
  * [Unit tests](#unit-tests)
  * [Tests](#tests)
  * [Internationalisation](#internationalisation)
  * [Accessibility](#accessibility)
  * [Layout](#layout)
  * [Authors](#authors)
* [Thanks](#thanks)

<!--- END -->

## Android Studio settings

Please set the "hard wrap" setting of Android Studio to 160 chars, this is the setting we use internally to format the source code (Menu `Settings/Editor/Code Style` then `Hard wrap at`).
Please ensure that you're using the project formatting rules (which are in the project at .idea/codeStyles/), and format the file before committing them.

### Template

An Android Studio template has been added to the project to help creating all files needed when adding a new screen to the application. Fragment, ViewModel, Activity, etc.

To install the template (to be done only once):
- Go to folder `./tools/template`.
- Mac OSX: Run the script `./configure.sh`.

   Linux: Run `ANDROID_STUDIO=/path/to/android-studio ./configure`
    - e.g. `ANDROID_STUDIO=/usr/local/android-studio ./configure`

- Restart Android Studio.

To create a new screen:
- First create a new package in your code.
- Then right click on the package, and select `New/New Vector/Element Feature`.
- Follow the Wizard, especially replace `Main` by something more relevant to your feature.
- Click on `Finish`.
- Remaining steps are described as TODO in the generated files, or will be pointed out by the compiler, or at runtime :)

Note that if the templates are modified, the only things to do is to restart Android Studio for the change to take effect.

## Compilation

For now, the Matrix SDK and the Tchap application are in the same project. So there is no specific thing to do, this project should compile without any special action.

See [docs/rust_crypto_integration.md](./docs/rust_crypto_integration.md#testing-with-a-local-rust-aar) for notes on building against a custom version of the Rust `matrix-sdk-crypto`.

## I want to submit a PR to fix an issue

Please have a look in the [dedicated documentation](./docs/pull_request.md) about pull request.

Please check if a corresponding issue exists. If yes, please let us know in a comment that you're working on it.
If an issue does not exist yet, it may be relevant to open a new issue and let us know that you're implementing it.

### Kotlin

This project is full Kotlin. Please do not write Java classes.

### Tchap changes

Please add a comment upon each Tchap specific change compared to the Element codebase.  The comment must be prefixed by `Tchap: ` to help developers
identifying this kind of changes and simplifying conflict resolution during the rebase against Element.
Also, add an explanation about why the changes are necessary for Tchap or why the behaviour is different from Element.

Please consider staying aligned as much as possible with the Element codebase, if a change or an improvement can be relevant for Element, prefer to add a
contribution in the [Element repository](https://github.com/element-hq/element-android).

### Changelog

Please create at least one file under ./changelog.d containing details about your change. Towncrier will be used when preparing the release.

Towncrier says to use the PR number for the filename, but the issue number is also fine.

Supported filename extensions are:

- ``.feature``: Signifying a new feature in Tchap Android.
- ``.improvements``: Signifying a feature improvement in Tchap Android.
- ``.bugfix``: Signifying a bug fix.
- ``.wip``: Signifying a work in progress change, typically a component of a larger feature which will be enabled once all tasks are complete.
- ``.doc``: Signifying a documentation improvement.
- ``.sdk``: Signifying a change to the Matrix SDK, this could be an addition, deprecation or removal of a public API.
- ``.misc``: Any other changes.

See https://github.com/twisted/towncrier#news-fragments if you need more details.

### Code quality

Make sure the following commands execute without any error:

#### Internal tool

<pre>
./tools/check/check_code_quality.sh
</pre>

#### ktlint

<pre>
./gradlew ktlintCheck --continue
</pre>

Note that you can run

<pre>
./gradlew ktlintFormat
</pre>

For ktlint to fix some detected errors for you (you still have to check and commit the fix of course)

#### knit

[knit](https://github.com/Kotlin/kotlinx-knit) is a tool which checks markdown files on the project. Also it generates/updates the table of content (toc) of the markdown files.

So everytime the toc should be updated, just run
<pre>
./gradlew knit
</pre>

and commit the changes.

The CI will check that markdown files are up to date by running

<pre>
./gradlew knitCheck
</pre>

#### lint

<pre>
./gradlew lintGplayBtchapWithpinningRelease
./gradlew lintFdroidBtchapWithoutpinningRelease
</pre>

### Unit tests

Make sure the following commands execute without any error:

<pre>
./gradlew testGplayBtchapWithoutpinningReleaseUnitTest
</pre>

### Tests

Tchap is currently supported on Android Lollipop (API 21+): please test your change on an Android device (or Android emulator) running with API 21. Many issues can happen (including crashes) on older devices.
Also, if possible, please test your change on a real device. Testing on Android emulator may not be sufficient.

You should consider adding Unit tests with your PR, and also integration tests (AndroidTest). Please refer to [this document](./docs/integration_tests.md) to install and run the integration test environment.

### Internationalisation

When adding new string resources, please only add new entries in the 2 files: `value/strings_tchap.xml` and `values-fr/strings_tchap.xml`.

Do not hesitate to use plurals when appropriate.

### Accessibility

Please consider accessibility as an important point. As a minimum requirement, in layout XML files please use attributes such as `android:contentDescription` and `android:importantForAccessibility`, and test with a screen reader if it's working well. You can add new string resources, dedicated to accessibility, in this case, please prefix theirs id with `a11y_`.

For instance, when updating the image `src` of an ImageView, please also consider updating its `contentDescription`. A good example is a play pause button.

### Layout

Also please check that the colors are ok for all the current themes of Tchap. Please use `?attr` instead of `@color` to reference colors in the layout. You can check this in the layout editor preview by selecting all the main themes (`AppTheme.Status`, `AppTheme.Dark`, etc.).

### Authors

Feel free to add an entry in file AUTHORS.md

## Thanks

Thanks for contributing to Tchap projects!
