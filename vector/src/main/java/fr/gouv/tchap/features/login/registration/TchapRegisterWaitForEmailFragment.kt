/*
 * Copyright 2019 New Vector Ltd
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

package fr.gouv.tchap.features.login.registration

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.args
import fr.gouv.tchap.features.login.TchapAbstractLoginFragment
import fr.gouv.tchap.features.login.TchapLoginAction
import fr.gouv.tchap.features.login.TchapLoginViewEvents
import im.vector.app.R
import im.vector.app.databinding.FragmentTchapRegisterWaitForEmailBinding
import im.vector.app.features.login.SignMode
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.failure.is401
import javax.inject.Inject

@Parcelize
data class TchapRegisterWaitForEmailFragmentArgument(
        val email: String
) : Parcelable

/**
 * In this screen, the user is asked to check his emails
 */
class TchapRegisterWaitForEmailFragment @Inject constructor() : TchapAbstractLoginFragment<FragmentTchapRegisterWaitForEmailBinding>() {

    private val params: TchapRegisterWaitForEmailFragmentArgument by args()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTchapRegisterWaitForEmailBinding {
        return FragmentTchapRegisterWaitForEmailBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar(views.toolbar)
        views.toolbar.setTitle(R.string.tchap_register_title)

        setupViews()
    }

    override fun onResume() {
        super.onResume()

        loginViewModel.handle(TchapLoginAction.CheckIfEmailHasBeenValidated(0))
    }

    override fun onPause() {
        super.onPause()

        loginViewModel.handle(TchapLoginAction.StopEmailValidationCheck)
    }

    private fun setupViews() {
        views.fragmentTchapRegisterWaitForEmailEmail.text = params.email
        views.fragmentTchapRegisterWaitForEmailLoginButton.setOnClickListener { signIn() }
        views.fragmentTchapRegisterWaitForEmailBack.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    override fun onError(throwable: Throwable) {
        if (throwable.is401()) {
            // Try again, with a delay
            loginViewModel.handle(TchapLoginAction.CheckIfEmailHasBeenValidated(10_000))
        } else {
            super.onError(throwable)
        }
    }

    override fun resetViewModel() {
        loginViewModel.handle(TchapLoginAction.ResetLogin)
    }

    private fun signIn() {
        loginViewModel.handle(TchapLoginAction.PostViewEvent(TchapLoginViewEvents.OnLoginEmailValidated))
    }
}
