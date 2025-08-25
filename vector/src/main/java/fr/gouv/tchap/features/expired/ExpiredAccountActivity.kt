/*
 * Copyright 2021 New Vector Ltd
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

package fr.gouv.tchap.features.expired

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.core.view.isVisible
import com.airbnb.mvrx.viewModel
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityTchapExpiredBinding
import im.vector.app.features.MainActivity
import im.vector.app.features.MainActivityArgs
import im.vector.lib.strings.CommonStrings
import timber.log.Timber
import javax.inject.Inject

/**
 * In this screen, the user is viewing a message informing that his account has expired.
 */
@AndroidEntryPoint
class ExpiredAccountActivity : VectorBaseActivity<ActivityTchapExpiredBinding>(), ExpiredAccountViewModel.Factory {

    @Inject lateinit var expiredAccountFactory: ExpiredAccountViewModel.Factory

    private val viewModel: ExpiredAccountViewModel by viewModel()

    override fun getBinding() = ActivityTchapExpiredBinding.inflate(layoutInflater)

    override val rootView: View
        get() = views.expiredAccount

    override fun create(initialState: ExpiredAccountViewState): ExpiredAccountViewModel {
        return expiredAccountFactory.create(initialState)
    }

    override fun initUiAndData() {
        with(views) {
            resumeButton.debouncedClicks {
                MainActivity.restartApp(this@ExpiredAccountActivity, MainActivityArgs(clearCache = true))
            }
            renewalEmailButton.debouncedClicks {
                viewModel.handle(ExpiredAccountAction.RequestSendingRenewalEmail)
            }
        }
        viewModel.onEach { renderState(it) }
    }

    override fun handleExpiredAccount() {
        // No op here
        Timber.w("Ignoring expired account global error")
    }

    private fun renderState(state: ExpiredAccountViewState) {
        with(views) {
            if (state.isRenewalEmailSent) {
                titleView.setText(CommonStrings.tchap_expired_account_on_new_sent_email_title)
                msgView.setText(CommonStrings.tchap_expired_account_on_new_sent_email_msg)
                renewalEmailButton.isVisible = false
            } else {
                titleView.setText(CommonStrings.tchap_expired_account_title)
                msgView.setText(CommonStrings.tchap_expired_account_msg)
                renewalEmailButton.isVisible = true
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, ExpiredAccountActivity::class.java)
        }
    }
}
