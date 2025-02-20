/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.verification.self

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.epoxy.OnModelBuildFinishedListener
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.giveAccessibilityFocus
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.PERMISSIONS_FOR_TAKING_PHOTO
import im.vector.app.core.utils.checkPermissions
import im.vector.app.core.utils.onPermissionDeniedDialog
import im.vector.app.core.utils.openUrlInChromeCustomTab
import im.vector.app.core.utils.registerForPermissionsResult
import im.vector.app.databinding.BottomSheetVerificationChildFragmentBinding
import im.vector.app.features.crypto.verification.VerificationAction
import im.vector.app.features.qrcode.QrCodeScannerActivity
import im.vector.app.features.settings.VectorSettingsUrls
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.crypto.verification.EVerificationState
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SelfVerificationFragment : VectorBaseFragment<BottomSheetVerificationChildFragmentBinding>(),
        SelfVerificationController.InteractionListener {

    @Inject lateinit var controller: SelfVerificationController

    private var requestAccessibilityFocus: Boolean = false
    private val modelBuildListener: OnModelBuildFinishedListener = OnModelBuildFinishedListener {
        if (requestAccessibilityFocus) {
            // Do not use giveAccessibilityFocusOnce() here.
            views.bottomSheetVerificationRecyclerView.layoutManager?.findViewByPosition(0)?.giveAccessibilityFocus()
            requestAccessibilityFocus = false
            // Note: it does not work when the recycler view is displayed for the first time, because findViewByPosition(0) is null
        }
    }

    private val viewModel by parentFragmentViewModel(SelfVerificationViewModel::class)

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetVerificationChildFragmentBinding {
        return BottomSheetVerificationChildFragmentBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    override fun onDestroyView() {
        views.bottomSheetVerificationRecyclerView.cleanup()
        controller.removeModelBuildListener(modelBuildListener)
        controller.listener = null
        super.onDestroyView()
    }

    private fun setupRecyclerView() {
        views.bottomSheetVerificationRecyclerView.configureWith(controller, hasFixedSize = false, disableItemAnimation = true)
        controller.addModelBuildListener(modelBuildListener)
        controller.listener = this
    }

    override fun invalidate() = withState(viewModel) { state ->
//        Timber.w("VALR: invalidate with State: ${state.pendingRequest}")
        if (state.isNewScreen()) {
            requestAccessibilityFocus = true
        }
        controller.update(state)
    }

    override fun onClickHelp() {
        openUrlInChromeCustomTab(requireContext(), null, TCHAP_FAQ_VERIFICATION_URL)
    }

    override fun onClickRecoverFromPassphrase() {
        viewModel.handle(VerificationAction.VerifyFromPassphrase)
    }

    override fun onClickSkip() {
        viewModel.handle(VerificationAction.SkipVerification)
    }

    override fun onClickResetSecurity() {
        viewModel.handle(VerificationAction.ForgotResetAll)
    }

    override fun onDoneFrom4S() {
        viewModel.handle(VerificationAction.GotItConclusion(true))
    }

    override fun keysNotIn4S() {
        viewModel.handle(VerificationAction.FailedToGetKeysFrom4S)
    }

    override fun confirmCancelRequest(confirm: Boolean) {
    }

    override fun wasNotMe() {
        // we just want to cancel and open device settings
        viewModel.handle(VerificationAction.SelfVerificationWasNotMe)
    }

    override fun onClickOnVerificationStart() {
        viewModel.handle(VerificationAction.RequestSelfVerification)
    }

    override fun onDone(b: Boolean) {
        viewModel.handle(VerificationAction.GotItConclusion(b))
    }

    override fun onDoNotMatchButtonTapped() {
        viewModel.handle(VerificationAction.SASDoNotMatchAction)
    }

    override fun onMatchButtonTapped() {
        viewModel.handle(VerificationAction.SASMatchAction)
    }

    private val openCameraActivityResultLauncher = registerForPermissionsResult { allGranted, deniedPermanently ->
        if (allGranted) {
            doOpenQRCodeScanner()
        } else if (deniedPermanently) {
            activity?.onPermissionDeniedDialog(CommonStrings.denied_permission_camera)
        }
    }

    override fun openCamera() {
        if (checkPermissions(PERMISSIONS_FOR_TAKING_PHOTO, requireActivity(), openCameraActivityResultLauncher)) {
            doOpenQRCodeScanner()
        }
    }

    private fun doOpenQRCodeScanner() {
        QrCodeScannerActivity.startForResult(requireActivity(), scanActivityResultLauncher)
    }

    private val scanActivityResultLauncher = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val scannedQrCode = QrCodeScannerActivity.getResultText(activityResult.data)
            val wasQrCode = QrCodeScannerActivity.getResultIsQrCode(activityResult.data)

            if (wasQrCode && !scannedQrCode.isNullOrBlank()) {
                onRemoteQrCodeScanned(scannedQrCode)
            } else {
                Timber.w("It was not a QR code, or empty result")
            }
        }
    }

    private fun onRemoteQrCodeScanned(remoteQrCode: String) = withState(viewModel) { state ->
        viewModel.handle(
                VerificationAction.RemoteQrCodeScanned(
                        state.pendingRequest.invoke()?.otherUserId.orEmpty(),
                        state.pendingRequest.invoke()?.transactionId.orEmpty(),
                        remoteQrCode
                )
        )
    }

    override fun doVerifyBySas() {
        viewModel.handle(VerificationAction.StartSASVerification)
    }

    override fun onUserDeniesQrCodeScanned() {
        viewModel.handle(VerificationAction.OtherUserDidNotScanned)
    }

    override fun onUserConfirmsQrCodeScanned() {
        viewModel.handle(VerificationAction.OtherUserScannedSuccessfully)
    }

    override fun acceptRequest() {
        viewModel.handle(VerificationAction.ReadyPendingVerification)
    }

    override fun declineRequest() {
        viewModel.handle(VerificationAction.CancelPendingVerification)
    }

    private var currentScreenIndex = -1

    private fun SelfVerificationViewState.isNewScreen(): Boolean {
        val newIndex = toScreenIndex()
        if (currentScreenIndex == newIndex) {
            return false
        }
        currentScreenIndex = newIndex
        return true
    }

    private fun SelfVerificationViewState.toScreenIndex(): Int {
        return if (activeAction !is UserAction.None) {
            when (activeAction) {
                UserAction.ConfirmCancel -> 30
                UserAction.None -> 31
            }
        } else {
            when (pendingRequest) {
                is Fail -> 0
                is Loading -> 1
                is Success -> when (pendingRequest.invoke().state) {
                    EVerificationState.WaitingForReady -> 10
                    EVerificationState.Requested -> 11
                    EVerificationState.Ready -> 12
                    EVerificationState.Started -> 13
                    EVerificationState.WeStarted -> 14
                    EVerificationState.WaitingForDone -> 15
                    EVerificationState.Done -> 16
                    EVerificationState.Cancelled -> 17
                    EVerificationState.HandledByOtherSession -> 18
                }
                Uninitialized -> 2
            }
        }
    }

    companion object {
        private const val TCHAP_FAQ_VERIFICATION_URL =
                "${VectorSettingsUrls.HELP}/fr/article/comment-verifier-un-nouvel-appareil-sur-tchap-xm0b0y/"
    }
}
