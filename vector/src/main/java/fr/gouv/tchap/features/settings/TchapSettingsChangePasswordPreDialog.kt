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

package fr.gouv.tchap.features.settings

import android.app.Activity
import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.core.dialogs.ExportKeysDialog
import im.vector.app.core.extensions.queryExportKeys
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.resources.BuildMeta
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

class TchapSettingsChangePasswordPreDialog(
        private val session: Session
) : DialogFragment() {

    @Inject lateinit var buildMeta: BuildMeta

    var listener: InteractionListener? = null

    private val manualExportKeysActivityResultLauncher = registerStartForActivityResult {
        if (it.resultCode == Activity.RESULT_OK) {
            val uri = it.data?.data
            if (uri != null) {
                activity?.let { activity ->
                    ExportKeysDialog().show(activity, object : ExportKeysDialog.ExportKeyDialogListener {
                        override fun onPassphrase(passphrase: String) {
                            listener?.exportKeys(passphrase, uri)
                        }
                    })
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = MaterialAlertDialogBuilder(requireActivity())
                .setCancelable(false)
                .setTitle(CommonStrings.dialog_title_warning)
                .setMessage(CommonStrings.tchap_settings_change_pwd_caution)
                .setPositiveButton(CommonStrings.settings_change_password) { _, _ -> listener?.changePassword() }
                .setNeutralButton(CommonStrings.encryption_export_e2e_room_keys, null)
                .setNegativeButton(CommonStrings.action_cancel, null)
                .create()

        dialog.setOnShowListener {
            val exportKeysButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            exportKeysButton.setOnClickListener {
                queryExportKeys(session.myUserId, buildMeta.applicationName, manualExportKeysActivityResultLauncher)
            }
        }

        return dialog
    }

    fun showLoadingView(isLoading: Boolean) {
        (dialog as? AlertDialog)?.apply {
            getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = !isLoading
            getButton(AlertDialog.BUTTON_NEGATIVE)?.isEnabled = !isLoading
            getButton(AlertDialog.BUTTON_NEUTRAL)?.isEnabled = !isLoading
        }
    }

    interface InteractionListener {
        fun changePassword()
        fun exportKeys(passphrase: String, uri: Uri)
    }
}
