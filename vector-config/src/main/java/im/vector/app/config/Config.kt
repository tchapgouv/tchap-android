/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.config

import kotlin.time.Duration.Companion.days

/**
 * Set of flags to configure the application.
 */
object Config {

    // TCHAP The spaces feature is hidden, show all rooms in the home
    const val SHOW_SPACES = false
    const val SPACES_SHOW_ALL_IN_HOME = !SHOW_SPACES

    const val SHOW_VOICE_RECORDER = true

    /**
     * Flag to allow external UnifiedPush distributors to be chosen by the user.
     *
     * Set to true to allow any available external UnifiedPush distributor to be chosen by the user.
     * - For Gplay variant it means that FCM will be used by default, but user can choose another UnifiedPush distributor;
     * - For F-Droid variant, it means that background polling will be used by default, but user can choose another UnifiedPush distributor.
     *
     * Set to false to prevent usage of external UnifiedPush distributors.
     * - For Gplay variant it means that only FCM will be used;
     * - For F-Droid variant, it means that only background polling will be available to the user.
     *
     * *Note*: When the app is already installed on users' phone:
     * - Changing the value from `false` to `true` will let the user be able to select an external UnifiedPush distributor;
     * - Changing the value from `true` to `false` will force the app to return to the background sync / Firebase Push.
     */
    const val ALLOW_EXTERNAL_UNIFIED_PUSH_DISTRIBUTORS = false // TCHAP Disable UnifiedPush (use Firebase/background sync)

    const val ENABLE_LOCATION_SHARING = true
    const val LOCATION_MAP_TILER_KEY = "" // TCHAP No key for map is needed

    /**
     * Whether to read the `io.element.functional_members` state event
     * and exclude any service members when computing a room's name and avatar.
     */
    const val SUPPORT_FUNCTIONAL_MEMBERS = true

    /**
     * The maximum length of voice messages in milliseconds.
     */
    const val VOICE_MESSAGE_LIMIT_MS = 120_000L

    /**
     * The strategy for sharing device keys.
     */
    val KEY_SHARING_STRATEGY = KeySharingStrategy.WhenTyping

    /**
     * The onboarding flow.
     */
    val ONBOARDING_VARIANT = OnboardingVariant.FTUE_AUTH

    /**
     * If set, MSC3086 asserted identity messages sent on VoIP calls will cause the call to appear in the room corresponding to the asserted identity.
     * This *must* only be set in trusted environments.
     */
    const val HANDLE_CALL_ASSERTED_IDENTITY_EVENTS = false

    const val LOW_PRIVACY_LOG_ENABLE = false
    const val ENABLE_STRICT_MODE_LOGS = false

    /**
     * The analytics configuration to use for the Debug build type.
     * Can be disabled by providing Analytics.Disabled
     */
    val DEBUG_ANALYTICS_CONFIG = Analytics.Enabled(
            postHogHost = "https://us.i.posthog.com", // TCHAP dev posthog
            postHogApiKey = "phc_eQOeaQiaIxdX9kaQmqYTD7RJLyFubYmGYKUI9czqqQD", // TCHAP dev posthog
            policyLink = "https://tchap.numerique.gouv.fr/politique-de-confidentialite",
            sentryDSN = "",
            sentryEnvironment = "DEBUG"
    )

    /**
     * The analytics configuration to use for the Release build type.
     * Can be disabled by providing Analytics.Disabled
     */
    val RELEASE_ANALYTICS_CONFIG = Analytics.Enabled(
            postHogHost = "https://posthogdev.tchap.incubateur.net", // TCHAP prod posthog
            postHogApiKey = "phc_FFa4pkvmuWjF9nZOMmYJWUXMibuYnCnPyf3DqPGZs4L", // TCHAP prod posthog
            policyLink = "https://tchap.numerique.gouv.fr/politique-de-confidentialite",
            sentryDSN = "",
            sentryEnvironment = "RELEASE"
    )

    /**
     * The analytics configuration to use for the Nightly build type.
     * Can be disabled by providing Analytics.Disabled
     */
    val NIGHTLY_ANALYTICS_CONFIG = Analytics.Disabled // TCHAP No analytics
    val RELEASE_R_ANALYTICS_CONFIG = Analytics.Disabled // TCHAP No analytics
    val ER_NIGHTLY_ANALYTICS_CONFIG = Analytics.Disabled // TCHAP No analytics
    val ER_DEBUG_ANALYTICS_CONFIG = Analytics.Disabled // TCHAP No analytics

    val SHOW_UNVERIFIED_SESSIONS_ALERT_AFTER_MILLIS = 7.days.inWholeMilliseconds // 1 Week

    /**
     * Sunsetting the application.
     * Fork maintainers can use this to inform users about their new application if any. Note that you probably also want
     * to replace the resource `replacement_app_icon` too.
     */
    val sunsetConfig: SunsetConfig = SunsetConfig.Enabled(
            learnMoreLink = "https://element.io/app-for-productivity",
            replacementApplicationName = "Element X",
            replacementApplicationId = "io.element.android.x",
    )
}
