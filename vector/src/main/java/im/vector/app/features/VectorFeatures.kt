/*
 * Copyright (c) 2021 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features

import im.vector.app.config.Config
import im.vector.app.config.OnboardingVariant
import im.vector.app.core.resources.AppNameProvider
import im.vector.app.core.resources.BooleanProvider
import im.vector.app.core.resources.StringArrayProvider
import im.vector.app.features.settings.VectorPreferences
import javax.inject.Inject

interface VectorFeatures {

    fun tchapIsVisioSupported(homeServerUrl: String): Boolean
    fun tchapIsCrossSigningEnabled(): Boolean
    fun tchapIsKeyBackupEnabled(): Boolean
    fun tchapIsThreadEnabled(): Boolean
    fun tchapIsLabsVisible(domain: String): Boolean
    fun tchapIsSecureBackupRequired(): Boolean
    fun onboardingVariant(): OnboardingVariant
    fun isOnboardingAlreadyHaveAccountSplashEnabled(): Boolean
    fun isOnboardingSplashCarouselEnabled(): Boolean
    fun isOnboardingUseCaseEnabled(): Boolean
    fun isOnboardingPersonalizeEnabled(): Boolean
    fun isOnboardingCombinedRegisterEnabled(): Boolean
    fun isOnboardingCombinedLoginEnabled(): Boolean
    fun allowExternalUnifiedPushDistributors(): Boolean
    fun isScreenSharingEnabled(): Boolean
    fun isLocationSharingEnabled(): Boolean
    fun forceUsageOfOpusEncoder(): Boolean

    /**
     * This is only to enable if the labs flag should be visible and effective.
     * If on the client-side you want functionality that should be enabled with the new layout,
     * use [VectorPreferences.isNewAppLayoutEnabled] instead.
     */
    fun isNewAppLayoutFeatureEnabled(): Boolean
    fun isVoiceBroadcastEnabled(): Boolean
    fun isUnverifiedSessionsAlertEnabled(): Boolean
}

class DefaultVectorFeatures @Inject constructor(
        private val appNameProvider: AppNameProvider,
        private val stringArrayProvider: StringArrayProvider,
        private val booleanProvider: BooleanProvider
) : VectorFeatures {
    override fun tchapIsVisioSupported(homeServerUrl: String) = booleanProvider.getBoolean(im.vector.app.config.R.bool.tchap_is_visio_supported) &&
        stringArrayProvider.getStringArray(im.vector.app.config.R.array.tchap_is_visio_supported_homeservers).let { homeServerUrls ->
            homeServerUrls.isEmpty() || homeServerUrls.any { homeServerUrl.contains(it) }
        }
    override fun tchapIsCrossSigningEnabled() = booleanProvider.getBoolean(im.vector.app.config.R.bool.tchap_is_cross_signing_enabled)
    override fun tchapIsKeyBackupEnabled() = booleanProvider.getBoolean(im.vector.app.config.R.bool.tchap_is_key_backup_enabled)
    override fun tchapIsThreadEnabled() = booleanProvider.getBoolean(im.vector.app.config.R.bool.tchap_is_thread_enabled)
    override fun tchapIsLabsVisible(domain: String) = booleanProvider.getBoolean(im.vector.app.config.R.bool.settings_root_labs_visible) ||
            domain == appNameProvider.getAppName()
    override fun tchapIsSecureBackupRequired() = booleanProvider.getBoolean(im.vector.app.config.R.bool.tchap_is_secure_backup_required)
    override fun onboardingVariant() = Config.ONBOARDING_VARIANT
    override fun isOnboardingAlreadyHaveAccountSplashEnabled() = true
    override fun isOnboardingSplashCarouselEnabled() = false // TCHAP no carousel
    override fun isOnboardingUseCaseEnabled() = true
    override fun isOnboardingPersonalizeEnabled() = false // TCHAP no personalization
    override fun isOnboardingCombinedRegisterEnabled() = true
    override fun isOnboardingCombinedLoginEnabled() = true
    override fun allowExternalUnifiedPushDistributors(): Boolean = Config.ALLOW_EXTERNAL_UNIFIED_PUSH_DISTRIBUTORS
    override fun isScreenSharingEnabled(): Boolean = true
    override fun isLocationSharingEnabled() = Config.ENABLE_LOCATION_SHARING
    override fun forceUsageOfOpusEncoder(): Boolean = false
    override fun isNewAppLayoutFeatureEnabled(): Boolean = true
    override fun isVoiceBroadcastEnabled(): Boolean = true
    override fun isUnverifiedSessionsAlertEnabled(): Boolean = true
}
