/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding.ftueauth

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.autofill.HintConstants
import androidx.core.text.isDigitsOnly
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.hidePassword
import im.vector.app.core.extensions.setLeftDrawable
import im.vector.app.core.extensions.toReducedUrl
import im.vector.app.core.resources.BuildMeta
import im.vector.app.core.utils.openUrlInExternalBrowser
import im.vector.app.databinding.FragmentLoginBinding
import im.vector.app.features.VectorFeatures
import im.vector.app.features.login.LoginMode
import im.vector.app.features.login.SSORedirectRouterActivity
import im.vector.app.features.login.ServerType
import im.vector.app.features.login.SignMode
import im.vector.app.features.login.SocialLoginButtonsView
import im.vector.app.features.login.render
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingFlow
import im.vector.app.features.onboarding.OnboardingViewEvents
import im.vector.app.features.onboarding.OnboardingViewState
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.auth.SSOAction
import org.matrix.android.sdk.api.extensions.isEmail
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.failure.isInvalidPassword
import org.matrix.android.sdk.api.failure.isInvalidUsername
import org.matrix.android.sdk.api.failure.isLoginEmailUnknown
import org.matrix.android.sdk.api.failure.isRegistrationDisabled
import org.matrix.android.sdk.api.failure.isUsernameInUse
import org.matrix.android.sdk.api.failure.isWeakPassword
import reactivecircus.flowbinding.android.widget.textChanges
import javax.inject.Inject

/**
 * In this screen:
 * In signin mode:
 * - the user is asked for login (or email) and password to sign in to a homeserver.
 * - He also can reset his password
 * In signup mode:
 * - the user is asked for login and password
 */
@AndroidEntryPoint
class FtueAuthLoginFragment :
        AbstractSSOFtueAuthFragment<FragmentLoginBinding>() {

    @Inject lateinit var buildMeta: BuildMeta
    @Inject lateinit var vectorFeatures: VectorFeatures

    private val tchap = Tchap()

    private var isSignupMode = false

    // Temporary patch for https://github.com/element-hq/riotX-android/issues/1410,
    // waiting for https://github.com/matrix-org/synapse/issues/7576
    private var isNumericOnlyUserIdForbidden = false

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoginBinding {
        return FragmentLoginBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSubmitButton()
        setupForgottenPasswordButton()

        views.passwordField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }

    private fun setupForgottenPasswordButton() {
        views.forgetPasswordButton.setOnClickListener { forgetPasswordClicked() }
    }

    private fun setupAutoFill(state: OnboardingViewState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (state.signMode) {
                SignMode.Unknown -> error("developer error")
                SignMode.TchapSignUp,
                SignMode.SignUp -> {
                    views.loginField.setAutofillHints(HintConstants.AUTOFILL_HINT_NEW_USERNAME)
                    views.passwordField.setAutofillHints(HintConstants.AUTOFILL_HINT_NEW_PASSWORD)
                }
                SignMode.TchapSignInWithSSO,
                SignMode.TchapSignIn -> {
                    views.loginField.setAutofillHints(HintConstants.AUTOFILL_HINT_EMAIL_ADDRESS)
                    views.passwordField.setAutofillHints(HintConstants.AUTOFILL_HINT_NEW_PASSWORD)
                }
                SignMode.SignIn,
                SignMode.SignInWithMatrixId -> {
                    views.loginField.setAutofillHints(HintConstants.AUTOFILL_HINT_USERNAME)
                    views.passwordField.setAutofillHints(HintConstants.AUTOFILL_HINT_PASSWORD)
                }
            }
        }
    }

    private fun ssoMode(state: OnboardingViewState) = when (state.signMode) {
        SignMode.Unknown -> error("developer error")
        SignMode.TchapSignUp,
        SignMode.SignUp -> SocialLoginButtonsView.Mode.MODE_SIGN_UP
        SignMode.TchapSignInWithSSO,
        SignMode.TchapSignIn,
        SignMode.SignIn,
        SignMode.SignInWithMatrixId -> SocialLoginButtonsView.Mode.MODE_SIGN_IN
    }

    private fun submit() {
        withState(viewModel) { state ->
            cleanupUi()

            val login = views.loginField.text.toString()
            val password = views.passwordField.text.toString()

            // This can be called by the IME action, so deal with empty cases
            var error = 0
            // TCHAP custom error policy
            if (login.isEmpty() || !login.isEmail()) {
                views.loginFieldTil.error = getString(CommonStrings.auth_invalid_email)
                error++
            }
            if (isSignupMode && isNumericOnlyUserIdForbidden && login.isDigitsOnly()) {
                views.loginFieldTil.error = getString(CommonStrings.error_forbidden_digits_only_username)
                error++
            }
            if (password.isEmpty() && state.signMode != SignMode.TchapSignInWithSSO) {
                views.passwordFieldTil.error = getString(
                        if (isSignupMode) {
                            CommonStrings.error_empty_field_choose_password
                        } else {
                            CommonStrings.error_empty_field_your_password
                        }
                )
                error++
            }

            // TCHAP password confirmation
            if (state.signMode == SignMode.TchapSignUp && password != views.tchapPasswordConfirmationField.text.toString()) {
                views.passwordFieldTil.error = getString(CommonStrings.tchap_auth_password_dont_match)
                error++
            }

            if (error == 0) {
                if (state.signMode != SignMode.TchapSignInWithSSO) {
                    val initialDeviceName = getString(CommonStrings.login_default_session_public_name)
                    viewModel.handle(state.signMode.toAuthenticateAction(login, password, initialDeviceName))
                } else {
                    viewModel.handle(OnboardingAction.LoginWithSSO(login))
                }
            }
        }
    }

    private fun cleanupUi() {
        views.loginSubmit.hideKeyboard()
        views.loginFieldTil.error = null
        views.passwordFieldTil.error = null
        views.tchapPasswordConfirmationFieldTil.error = null
    }

    private fun setupUi(state: OnboardingViewState) {
        views.loginFieldTil.hint = getString(
                when (state.signMode) {
                    SignMode.Unknown -> error("developer error")
                    SignMode.SignUp -> CommonStrings.login_signup_username_hint
                    SignMode.SignIn -> CommonStrings.login_signin_username_hint
                    SignMode.TchapSignUp,
                    SignMode.TchapSignInWithSSO,
                    SignMode.TchapSignIn -> CommonStrings.tchap_connection_email
                    SignMode.SignInWithMatrixId -> CommonStrings.login_signin_matrix_id_hint
                }
        )

        // Handle direct signin first
        if (state.signMode == SignMode.SignInWithMatrixId) {
            views.loginServerIcon.isVisible = false
            views.loginTitle.text = getString(CommonStrings.login_signin_matrix_id_title)
            views.loginNotice.text = getString(CommonStrings.login_signin_matrix_id_notice)
            views.loginPasswordNotice.isVisible = true
        } else {
            val resId = when (state.signMode) {
                SignMode.TchapSignUp,
                SignMode.SignUp -> CommonStrings.login_signup_to
                SignMode.TchapSignIn -> CommonStrings.login_connect_to
                SignMode.SignIn -> CommonStrings.login_connect_to
                SignMode.TchapSignInWithSSO -> CommonStrings.login_social_signin_with
                else -> error("developer error")
            }

            when (state.serverType) {
                ServerType.MatrixOrg -> {
                    views.loginServerIcon.isVisible = true
                    views.loginServerIcon.setImageResource(R.drawable.ic_logo_matrix_org)
                    views.loginTitle.text = getString(resId, state.selectedHomeserver.userFacingUrl.toReducedUrl())
                    views.loginNotice.text = getString(CommonStrings.login_server_matrix_org_text)
                }
                ServerType.EMS -> {
                    views.loginServerIcon.isVisible = true
                    views.loginServerIcon.setImageResource(R.drawable.ic_logo_element_matrix_services)
                    views.loginTitle.text = getString(resId, "Element Matrix Services")
                    views.loginNotice.text = getString(CommonStrings.login_server_modular_text)
                }
                ServerType.Other -> {
                    views.loginServerIcon.isVisible = false
                    views.loginTitle.text = getString(resId, state.selectedHomeserver.userFacingUrl.toReducedUrl())
                    views.loginNotice.text = getString(CommonStrings.login_server_other_text)
                }
                ServerType.Unknown -> {
                    // TCHAP Hide views if empty
                    views.loginServerIcon.isVisible = false
                    views.loginNotice.isVisible = false
                    if (state.signMode == SignMode.TchapSignInWithSSO) {
                        views.loginTitle.text = getString(resId, TCHAP_SSO_PROVIDER)
                    } else {
                        views.loginTitle.text = getString(resId, buildMeta.applicationName)
                    }
                }
            }
            views.loginPasswordNotice.isVisible = false

            if (state.selectedHomeserver.preferredLoginMode is LoginMode.SsoAndPassword) {
                views.loginSocialLoginButtons.render(state.selectedHomeserver.preferredLoginMode, ssoMode(state)) { provider ->
                    viewModel.fetchSsoUrl(
                            redirectUrl = SSORedirectRouterActivity.VECTOR_REDIRECT_URL,
                            deviceId = state.deviceId,
                            provider = provider,
                            action = if (state.signMode == SignMode.SignUp) SSOAction.REGISTER else SSOAction.LOGIN
                    )
                            ?.let { openInCustomTab(it) }
                }
            } else {
                views.loginSocialLoginButtons.ssoIdentityProviders = null
            }
        }
    }

    private fun setupButtons(state: OnboardingViewState) {
        views.forgetPasswordButton.isVisible = state.signMode == SignMode.SignIn || state.signMode == SignMode.TchapSignIn

        views.loginSubmit.text = getString(
                when (state.signMode) {
                    SignMode.Unknown -> error("developer error")
                    SignMode.TchapSignUp,
                    SignMode.SignUp -> CommonStrings.login_signup_submit
                    SignMode.TchapSignInWithSSO -> CommonStrings.login_signin_sso
                    SignMode.TchapSignIn,
                    SignMode.SignIn,
                    SignMode.SignInWithMatrixId -> CommonStrings.login_signin
                }, TCHAP_SSO_PROVIDER
        )
    }

    private fun setupSubmitButton() = withState(viewModel) { state ->
        views.loginSubmit.setOnClickListener { submit() }
        combine(
                views.loginField.textChanges().map { it.trim().isNotEmpty() },
                views.passwordField.textChanges().map { it.isNotEmpty() }
        ) { isLoginNotEmpty, isPasswordNotEmpty ->
            (isLoginNotEmpty && isPasswordNotEmpty) || state.signMode == SignMode.TchapSignInWithSSO && views.loginField.text?.isEmail().orFalse()
        }
                .onEach {
                    views.loginFieldTil.error = null
                    views.passwordFieldTil.error = null
                    views.loginSubmit.isEnabled = it
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun forgetPasswordClicked() {
        viewModel.handle(OnboardingAction.PostViewEvent(OnboardingViewEvents.OnForgetPasswordClicked))
    }

    override fun resetViewModel() {
        viewModel.handle(OnboardingAction.ResetAuthenticationAttempt)
    }

    override fun onError(throwable: Throwable) {
        // Trick to display the error without text.
        views.loginFieldTil.error = " "
        when {
            throwable.isUsernameInUse() || throwable.isInvalidUsername() -> {
                views.loginFieldTil.error = errorFormatter.toHumanReadable(throwable)
            }
            throwable.isLoginEmailUnknown() -> {
                views.loginFieldTil.error = getString(CommonStrings.login_login_with_email_error)
            }
            throwable.isInvalidPassword() && spaceInPassword() -> {
                views.passwordFieldTil.error = getString(CommonStrings.auth_invalid_login_param_space_in_password)
            }
            throwable.isWeakPassword() || throwable.isInvalidPassword() -> {
                views.passwordFieldTil.error = errorFormatter.toHumanReadable(throwable)
            }
            isSignupMode && throwable.isRegistrationDisabled() -> {
                MaterialAlertDialogBuilder(requireActivity())
                        .setTitle(CommonStrings.dialog_title_error)
                        .setMessage(getString(CommonStrings.login_registration_disabled))
                        .setPositiveButton(CommonStrings.ok, null)
                        .show()
            }
            else -> {
                super.onError(throwable)
            }
        }
    }

    override fun updateWithState(state: OnboardingViewState) {
        isSignupMode = state.signMode == SignMode.SignUp || state.signMode == SignMode.TchapSignUp
        isNumericOnlyUserIdForbidden = state.serverType == ServerType.MatrixOrg

        tchap.setupUi(state)
        setupAutoFill(state)
        setupButtons(state)
        tchap.tryLoginSSO(state)

        if (state.isLoading) {
            // Ensure password is hidden
            views.passwordField.hidePassword()
        }
    }

    /**
     * Detect if password ends or starts with spaces.
     */
    private fun spaceInPassword() = views.passwordField.text.toString().let { it.trim() != it }

    private inner class Tchap {

        fun setupUi(state: OnboardingViewState) {
            this@FtueAuthLoginFragment.setupUi(state) // call "super" method

            val isSignUpMode = state.signMode == SignMode.TchapSignUp
            views.loginFieldTil.isHelperTextEnabled = isSignUpMode
            views.passwordFieldTil.isHelperTextEnabled = isSignUpMode
            views.tchapPasswordConfirmationFieldTil.isVisible = isSignUpMode
            views.loginSocialLoginContainer.isVisible = isSignUpMode && vectorFeatures.tchapIsSSOEnabled()

            when(state.signMode) {
                SignMode.TchapSignUp -> {
                    views.loginSSOSubmit.text = getString(CommonStrings.login_signin_sso, TCHAP_SSO_PROVIDER)
                    views.loginSSOSubmit.debouncedClicks {
                        viewModel.handle(
                                OnboardingAction.SplashAction.OnIAlreadyHaveAnAccount(
                                        onboardingFlow = OnboardingFlow.TchapSignInWithSSO
                                )
                        )
                    }
                }
                SignMode.TchapSignInWithSSO -> {
                    views.loginSubmit.setLeftDrawable(im.vector.lib.ui.styles.R.drawable.ic_tchap_proconnect)
                    views.loginSSOHelp.text = getString(CommonStrings.tchap_connection_sso_help, TCHAP_SSO_PROVIDER)
                    views.loginSSODescription.text = getString(CommonStrings.tchap_connection_sso_description, TCHAP_SSO_PROVIDER)
                    views.loginSSOHelp.debouncedClicks { openUrlInExternalBrowser(requireContext(), TCHAP_SSO_URL) }
                    views.loginSSODescription.debouncedClicks { openUrlInExternalBrowser(requireContext(), TCHAP_SSO_FAQ_URL) }
                    views.passwordFieldTil.isVisible = false
                    views.loginSSOHelp.isVisible = true
                    views.loginSSODescription.isVisible = true
                }
                else -> {
                    views.passwordField.imeOptions = EditorInfo.IME_ACTION_DONE
                    views.loginSSOHelp.isVisible = false
                }
            }
        }

        fun tryLoginSSO(state: OnboardingViewState) {
            if (state.signMode != SignMode.TchapSignInWithSSO) return
            if (views.loginField.text.isNullOrEmpty()) return
            if (state.selectedHomeserver.upstreamUrl.isNullOrEmpty()) return
            if (views.loginSocialLoginButtons.ssoIdentityProviders.isNullOrEmpty()) {
                views.loginFieldTil.error = getString(CommonStrings.tchap_auth_sso_inactive, TCHAP_SSO_PROVIDER)
                viewModel.handle(OnboardingAction.ResetHomeServerUrl)
                return
            }

            views.loginSocialLoginButtons.ssoIdentityProviders?.first().let {
                viewModel.fetchSsoUrl(
                        redirectUrl = SSORedirectRouterActivity.VECTOR_REDIRECT_URL,
                        deviceId = state.deviceId,
                        provider = it,
                        action = SSOAction.LOGIN
                )
                        ?.let { url -> openInCustomTab(url) }

                views.loginField.text?.clear()
            }
        }
    }
}
