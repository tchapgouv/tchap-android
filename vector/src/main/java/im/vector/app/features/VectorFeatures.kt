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

import im.vector.app.R
import im.vector.app.config.Config
import im.vector.app.config.OnboardingVariant
import im.vector.app.core.resources.BooleanProvider
import javax.inject.Inject

interface VectorFeatures {

    fun isVoipSupported(): Boolean
    fun isCrossSigningEnabled(): Boolean
    fun isKeyBackupEnabled(): Boolean
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
    fun shouldStartDmOnFirstMessage(): Boolean
    fun isNewAppLayoutEnabled(): Boolean
}

class DefaultVectorFeatures @Inject constructor(
        private val booleanProvider: BooleanProvider
) : VectorFeatures {
    override fun isVoipSupported() = booleanProvider.getBoolean(R.bool.tchap_is_voip_supported)
    override fun isCrossSigningEnabled() = booleanProvider.getBoolean(R.bool.tchap_is_cross_signing_enabled)
    override fun isKeyBackupEnabled() = booleanProvider.getBoolean(R.bool.tchap_is_key_backup_enabled)
    override fun onboardingVariant() = Config.ONBOARDING_VARIANT
    override fun isOnboardingAlreadyHaveAccountSplashEnabled() = true
    override fun isOnboardingSplashCarouselEnabled() = true
    override fun isOnboardingUseCaseEnabled() = true
    override fun isOnboardingPersonalizeEnabled() = true
    override fun isOnboardingCombinedRegisterEnabled() = true
    override fun isOnboardingCombinedLoginEnabled() = true
    override fun allowExternalUnifiedPushDistributors(): Boolean = Config.ALLOW_EXTERNAL_UNIFIED_PUSH_DISTRIBUTORS
    override fun isScreenSharingEnabled(): Boolean = true
    override fun isLocationSharingEnabled() = Config.ENABLE_LOCATION_SHARING
    override fun forceUsageOfOpusEncoder(): Boolean = false
    override fun shouldStartDmOnFirstMessage(): Boolean = false
    override fun isNewAppLayoutEnabled(): Boolean = false
}
