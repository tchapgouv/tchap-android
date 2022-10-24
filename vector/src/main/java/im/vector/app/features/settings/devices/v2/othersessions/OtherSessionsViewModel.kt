/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.settings.devices.v2.othersessions

import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.auth.PendingAuthHandler
import im.vector.app.features.settings.devices.v2.GetDeviceFullInfoListUseCase
import im.vector.app.features.settings.devices.v2.RefreshDevicesUseCase
import im.vector.app.features.settings.devices.v2.VectorSessionsListViewModel
import im.vector.app.features.settings.devices.v2.filter.DeviceManagerFilterType
import im.vector.app.features.settings.devices.v2.signout.InterceptSignoutFlowResponseUseCase
import im.vector.app.features.settings.devices.v2.signout.SignoutSessionResult
import im.vector.app.features.settings.devices.v2.signout.SignoutSessionsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.session.uia.DefaultBaseAuth
import timber.log.Timber
import javax.net.ssl.HttpsURLConnection
import kotlin.coroutines.Continuation

class OtherSessionsViewModel @AssistedInject constructor(
        @Assisted private val initialState: OtherSessionsViewState,
        activeSessionHolder: ActiveSessionHolder,
        private val stringProvider: StringProvider,
        private val getDeviceFullInfoListUseCase: GetDeviceFullInfoListUseCase,
        private val signoutSessionsUseCase: SignoutSessionsUseCase,
        private val interceptSignoutFlowResponseUseCase: InterceptSignoutFlowResponseUseCase,
        private val pendingAuthHandler: PendingAuthHandler,
        refreshDevicesUseCase: RefreshDevicesUseCase
) : VectorSessionsListViewModel<OtherSessionsViewState, OtherSessionsAction, OtherSessionsViewEvents>(
        initialState, activeSessionHolder, refreshDevicesUseCase
) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<OtherSessionsViewModel, OtherSessionsViewState> {
        override fun create(initialState: OtherSessionsViewState): OtherSessionsViewModel
    }

    companion object : MavericksViewModelFactory<OtherSessionsViewModel, OtherSessionsViewState> by hiltMavericksViewModelFactory()

    private var observeDevicesJob: Job? = null

    init {
        observeDevices(initialState.currentFilter)
    }

    private fun observeDevices(currentFilter: DeviceManagerFilterType) {
        observeDevicesJob?.cancel()
        observeDevicesJob = getDeviceFullInfoListUseCase.execute(
                filterType = currentFilter,
                excludeCurrentDevice = initialState.excludeCurrentDevice
        )
                .execute { async ->
                    copy(
                            devices = async,
                    )
                }
    }

    // TODO update unit tests
    override fun handle(action: OtherSessionsAction) {
        when (action) {
            is OtherSessionsAction.PasswordAuthDone -> handlePasswordAuthDone(action)
            OtherSessionsAction.ReAuthCancelled -> handleReAuthCancelled()
            OtherSessionsAction.SsoAuthDone -> handleSsoAuthDone()
            is OtherSessionsAction.FilterDevices -> handleFilterDevices(action)
            OtherSessionsAction.DisableSelectMode -> handleDisableSelectMode()
            is OtherSessionsAction.EnableSelectMode -> handleEnableSelectMode(action.deviceId)
            is OtherSessionsAction.ToggleSelectionForDevice -> handleToggleSelectionForDevice(action.deviceId)
            OtherSessionsAction.DeselectAll -> handleDeselectAll()
            OtherSessionsAction.SelectAll -> handleSelectAll()
            OtherSessionsAction.MultiSignout -> handleMultiSignout()
        }
    }

    private fun handleFilterDevices(action: OtherSessionsAction.FilterDevices) {
        setState {
            copy(
                    currentFilter = action.filterType
            )
        }
        observeDevices(action.filterType)
    }

    private fun handleDisableSelectMode() {
        setSelectionForAllDevices(isSelected = false, enableSelectMode = false)
    }

    private fun handleEnableSelectMode(deviceId: String?) {
        toggleSelectionForDevice(deviceId, enableSelectMode = true)
    }

    private fun handleToggleSelectionForDevice(deviceId: String) = withState { state ->
        toggleSelectionForDevice(deviceId, enableSelectMode = state.isSelectModeEnabled)
    }

    private fun toggleSelectionForDevice(deviceId: String?, enableSelectMode: Boolean) = withState { state ->
        val updatedDevices = if (state.devices is Success) {
            val devices = state.devices.invoke().toMutableList()
            val indexToUpdate = devices.indexOfFirst { it.deviceInfo.deviceId == deviceId }
            if (indexToUpdate >= 0) {
                val currentInfo = devices[indexToUpdate]
                val updatedInfo = currentInfo.copy(isSelected = !currentInfo.isSelected)
                devices[indexToUpdate] = updatedInfo
            }
            Success(devices)
        } else {
            state.devices
        }

        setState {
            copy(
                    devices = updatedDevices,
                    isSelectModeEnabled = enableSelectMode
            )
        }
    }

    private fun handleSelectAll() = withState { state ->
        setSelectionForAllDevices(isSelected = true, enableSelectMode = state.isSelectModeEnabled)
    }

    private fun handleDeselectAll() = withState { state ->
        setSelectionForAllDevices(isSelected = false, enableSelectMode = state.isSelectModeEnabled)
    }

    private fun setSelectionForAllDevices(isSelected: Boolean, enableSelectMode: Boolean) = withState { state ->
        val updatedDevices = if (state.devices is Success) {
            val updatedDevices = state.devices.invoke().map { it.copy(isSelected = isSelected) }
            Success(updatedDevices)
        } else {
            state.devices
        }

        setState {
            copy(
                    devices = updatedDevices,
                    isSelectModeEnabled = enableSelectMode
            )
        }
    }

    private fun handleMultiSignout() = withState { state ->
        viewModelScope.launch {
            setLoading(true)
            val deviceIds = getDeviceIdsToSignout(state)
            if (deviceIds.isEmpty()) {
                return@launch
            }
            val signoutResult = signout(deviceIds)
            setLoading(false)

            if (signoutResult.isSuccess) {
                onSignoutSuccess()
            } else {
                when (val failure = signoutResult.exceptionOrNull()) {
                    null -> onSignoutSuccess()
                    else -> onSignoutFailure(failure)
                }
            }
        }
    }

    private fun getDeviceIdsToSignout(state: OtherSessionsViewState): List<String> {
        return if (state.isSelectModeEnabled) {
            state.devices()?.filter { it.isSelected }.orEmpty()
        } else {
            state.devices().orEmpty()
        }.mapNotNull { it.deviceInfo.deviceId }
    }

    private suspend fun signout(deviceIds: List<String>) = signoutSessionsUseCase.execute(deviceIds, object : UserInteractiveAuthInterceptor {
        override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
            when (val result = interceptSignoutFlowResponseUseCase.execute(flowResponse, errCode, promise)) {
                is SignoutSessionResult.ReAuthNeeded -> onReAuthNeeded(result)
                is SignoutSessionResult.Completed -> Unit
            }
        }
    })

    private fun onReAuthNeeded(reAuthNeeded: SignoutSessionResult.ReAuthNeeded) {
        Timber.d("onReAuthNeeded")
        pendingAuthHandler.pendingAuth = DefaultBaseAuth(session = reAuthNeeded.flowResponse.session)
        pendingAuthHandler.uiaContinuation = reAuthNeeded.uiaContinuation
        _viewEvents.post(OtherSessionsViewEvents.RequestReAuth(reAuthNeeded.flowResponse, reAuthNeeded.errCode))
    }

    private fun setLoading(isLoading: Boolean) {
        setState { copy(isLoading = isLoading) }
    }

    private fun onSignoutSuccess() {
        Timber.d("signout success")
        refreshDeviceList()
        _viewEvents.post(OtherSessionsViewEvents.SignoutSuccess)
    }

    private fun onSignoutFailure(failure: Throwable) {
        Timber.e("signout failure", failure)
        val failureMessage = if (failure is Failure.OtherServerError && failure.httpCode == HttpsURLConnection.HTTP_UNAUTHORIZED) {
            stringProvider.getString(R.string.authentication_error)
        } else {
            stringProvider.getString(R.string.matrix_error)
        }
        _viewEvents.post(OtherSessionsViewEvents.SignoutError(Exception(failureMessage)))
    }

    private fun handleSsoAuthDone() {
        pendingAuthHandler.ssoAuthDone()
    }

    private fun handlePasswordAuthDone(action: OtherSessionsAction.PasswordAuthDone) {
        pendingAuthHandler.passwordAuthDone(action.password)
    }

    private fun handleReAuthCancelled() {
        pendingAuthHandler.reAuthCancelled()
    }
}
