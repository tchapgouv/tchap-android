/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding.ftueauth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.associateContentStateWith
import im.vector.app.core.extensions.clearErrorOnChange
import im.vector.app.core.extensions.content
import im.vector.app.core.extensions.editText
import im.vector.app.core.extensions.hidePassword
import im.vector.app.core.extensions.setOnImeDoneListener
import im.vector.app.databinding.FragmentFtueResetPasswordInputBinding
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewState
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.failure.isMissingEmailVerification

@AndroidEntryPoint
class FtueAuthResetPasswordEntryFragment :
        AbstractFtueAuthFragment<FragmentFtueResetPasswordInputBinding>() {

    private val tchap = Tchap()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtueResetPasswordInputBinding {
        return FragmentFtueResetPasswordInputBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        views.newPasswordInput.associateContentStateWith(button = views.newPasswordSubmit)
        views.newPasswordInput.setOnImeDoneListener { tchap.resetPassword() }
        views.newPasswordInput.clearErrorOnChange(viewLifecycleOwner)
        views.newPasswordSubmit.debouncedClicks { tchap.resetPassword() }
    }

    private fun resetPassword() {
        viewModel.handle(
                OnboardingAction.ConfirmNewPassword(
                        newPassword = views.newPasswordInput.content(),
                        signOutAllDevices = views.entrySignOutAll.isChecked
                )
        )
    }

    override fun onError(throwable: Throwable) {
        when {
            throwable.isMissingEmailVerification() -> super.onError(throwable)
            else -> {
                views.newPasswordInput.error = errorFormatter.toHumanReadable(throwable)
            }
        }
    }

    override fun updateWithState(state: OnboardingViewState) {
        views.signedOutAllGroup.isVisible = state.resetState.supportsLogoutAllDevices

        if (state.isLoading) {
            views.newPasswordInput.editText().hidePassword()
        }
    }

    override fun resetViewModel() {
        viewModel.handle(OnboardingAction.ResetResetPassword)
    }

    private inner class Tchap {

        private var showWarning: Boolean = true

        // TCHAP Show warning once before changing the password
        fun resetPassword() {
            if (showWarning) {
                showWarning = false
                MaterialAlertDialogBuilder(requireActivity())
                        .setTitle(CommonStrings.login_reset_password_warning_title)
                        .setMessage(CommonStrings.login_reset_password_warning_content)
                        .setPositiveButton(CommonStrings.login_reset_password_warning_submit) { _, _ ->
                            this@FtueAuthResetPasswordEntryFragment.resetPassword()
                        }
                        .setNegativeButton(CommonStrings.action_cancel, null)
                        .show()
            } else {
                this@FtueAuthResetPasswordEntryFragment.resetPassword()
            }
        }
    }
}
