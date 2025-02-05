/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding.ftueauth

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.resources.BuildMeta
import im.vector.app.core.utils.openUrlInExternalBrowser
import im.vector.app.databinding.FragmentTchapWelcomeBinding
import im.vector.app.features.VectorFeatures
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingFlow
import im.vector.app.features.settings.VectorPreferences
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

/**
 * In this screen, the user is viewing an introduction to what he can do with this application.
 */
@AndroidEntryPoint
class FtueAuthSplashFragment :
        AbstractFtueAuthFragment<FragmentTchapWelcomeBinding>() {

    @Inject lateinit var vectorPreferences: VectorPreferences
    @Inject lateinit var vectorFeatures: VectorFeatures
    @Inject lateinit var buildMeta: BuildMeta

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTchapWelcomeBinding {
        return FragmentTchapWelcomeBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        // TCHAP Login with SSO
        val isAlreadyHaveAccountEnabled = vectorFeatures.isOnboardingAlreadyHaveAccountSplashEnabled()
        views.loginSplashSSO.apply {
            val spannable = SpannableString(getString(CommonStrings.login_social_signin_with, TCHAP_SSO_PROVIDER))
            spannable.setSpan(StyleSpan(Typeface.BOLD), spannable.length - TCHAP_SSO_PROVIDER.length, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            text = spannable
            isVisible = isAlreadyHaveAccountEnabled && vectorFeatures.tchapIsSSOEnabled()
            debouncedClicks { alreadyHaveAnAccountWithSSO() }
        }
        views.loginSplashSSOHelp.apply {
            text = getString(CommonStrings.tchap_connection_sso_help, TCHAP_SSO_PROVIDER)
            isVisible = isAlreadyHaveAccountEnabled && vectorFeatures.tchapIsSSOEnabled()
            debouncedClicks { openUrlInExternalBrowser(requireContext(), TCHAP_SSO_URL) }
        }
        views.loginSplashSubmit.apply {
            setText(if (isAlreadyHaveAccountEnabled) CommonStrings.login_splash_create_account else CommonStrings.login_splash_submit)
            debouncedClicks { splashSubmit(isAlreadyHaveAccountEnabled) }
        }
        views.loginSplashAlreadyHaveAccount.apply {
            isVisible = isAlreadyHaveAccountEnabled
            debouncedClicks { alreadyHaveAnAccount() }
        }

        if (buildMeta.isDebug || vectorPreferences.developerMode()) {
            views.loginSplashVersion.isVisible = true
            @SuppressLint("SetTextI18n")
            views.loginSplashVersion.text = "Version : ${buildMeta.versionName}\n" +
                    "Branch: ${buildMeta.gitBranchName} ${buildMeta.gitRevision}"
            views.loginSplashVersion.debouncedClicks { navigator.openDebug(requireContext()) }
        }
    }

    /** TCHAP Login with SSO. */
    private fun alreadyHaveAnAccountWithSSO() {
        viewModel.handle(OnboardingAction.SplashAction.OnIAlreadyHaveAnAccount(onboardingFlow = OnboardingFlow.TchapSignInWithSSO))
    }

    private fun splashSubmit(isAlreadyHaveAccountEnabled: Boolean) {
        val getStartedFlow = if (isAlreadyHaveAccountEnabled) OnboardingFlow.SignUp else OnboardingFlow.SignInSignUp
        viewModel.handle(OnboardingAction.SplashAction.OnGetStarted(onboardingFlow = getStartedFlow))
    }

    private fun alreadyHaveAnAccount() {
        viewModel.handle(OnboardingAction.SplashAction.OnIAlreadyHaveAnAccount(onboardingFlow = OnboardingFlow.SignIn))
    }

    override fun resetViewModel() {
        // Nothing to do
    }
}
