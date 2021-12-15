/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.createdirect

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import fr.gouv.tchap.core.utils.TchapUtils
import fr.gouv.tchap.features.platform.GetPlatformResult
import fr.gouv.tchap.features.platform.Params
import fr.gouv.tchap.features.platform.TchapGetPlatformTask
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.userdirectory.PendingSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.user.model.User
import timber.log.Timber

class CreateDirectRoomViewModel @AssistedInject constructor(@Assisted
                                                            initialState: CreateDirectRoomViewState,
                                                            val session: Session,
                                                            private val directRoomHelper: DirectRoomHelper,
                                                            private val getPlatformTask: TchapGetPlatformTask) :
        VectorViewModel<CreateDirectRoomViewState, CreateDirectRoomAction, CreateDirectRoomViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<CreateDirectRoomViewModel, CreateDirectRoomViewState> {
        override fun create(initialState: CreateDirectRoomViewState): CreateDirectRoomViewModel
    }

    companion object : MavericksViewModelFactory<CreateDirectRoomViewModel, CreateDirectRoomViewState> by hiltMavericksViewModelFactory()

    override fun handle(action: CreateDirectRoomAction) {
        when (action) {
            is CreateDirectRoomAction.CreateRoomAndInviteSelectedUsers -> onSubmitInvitees(action)
            is CreateDirectRoomAction.InviteByEmail                    -> handleIndividualInviteByEmail(action.email)
            is CreateDirectRoomAction.CreateDirectMessageByUserId      -> handleCreateDirectMessageByUserId(action.userId)
        }.exhaustive
    }

    /**
     * If users already have a DM room then navigate to it instead of creating a new room.
     */
    private fun onSubmitInvitees(action: CreateDirectRoomAction.CreateRoomAndInviteSelectedUsers) {
        // Tchap: All the user invite and DM creation process has been reworked
        // Tchap: - multi-selection is forbidden, DM are restricted to 1:1
        // Tchap: - invites by email might expire for external accounts so we have to cancel pending invites to send a new ones
        // Tchap: - invites by msisdn are not supported yet
        val selection = action.selections.singleOrNull() ?: return
        setState { copy(isLoading = true) }
        when (selection) {
            // User already exists, so we can create or retrieve the DM with him
            is PendingSelection.UserPendingSelection     -> handleCreateDirectMessageByUserId(selection.user.userId)
            // User is unknown, so we have to invite him before creating the DM
            is PendingSelection.ThreePidPendingSelection -> {
                if (selection.threePid is ThreePid.Email) {
                    handleIndividualInviteByEmail(selection.threePid.email)
                } else {
                    setState { copy(isLoading = false) }
                    _viewEvents.post(CreateDirectRoomViewEvents.Failure(Throwable("Invite by Msisdn is not supported yet.")))
                }
            }
        }
    }

    private fun handleCreateDirectMessageByUserId(userId: String) {
        setState { copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            // Create or retrieve the DM and notify UI about the result
            runCatching { directRoomHelper.ensureDMExists(userId) }.fold(
                    {
                        setState { copy(isLoading = false) }
                        _viewEvents.post(CreateDirectRoomViewEvents.OpenDirectChat(it))
                    },
                    {
                        setState { copy(isLoading = false) }
                        _viewEvents.post(CreateDirectRoomViewEvents.Failure(it))
                    }
            )
        }
    }

    private fun handleIndividualInviteByEmail(email: String) {
        setState { copy(isLoading = true) }
        val existingRoom = session.getExistingDirectRoomWithUser(email)
        viewModelScope.launch(Dispatchers.IO) {
            // Start the invite process by checking whether a Tchap account has been created for this email.
            val userId = tryOrNull { session.identityService().lookUp(listOf(ThreePid.Email(email))) }
                    ?.find { it.threePid.value == email }
                    ?.matrixId

            // Email matches with an existing account, notify the UI with the resulting user
            if (userId != null) {
                setState { copy(isLoading = false) }
                val user = tryOrNull { session.resolveUser(userId) } ?: User(userId, TchapUtils.computeDisplayNameFromUserId(userId), null)
                _viewEvents.post(CreateDirectRoomViewEvents.UserDiscovered(user))
            }
            // Email does not match with an existing account, try to invite him before creating the DM
            else {
                val homeServer = (getPlatformTask.execute(Params(email)) as? GetPlatformResult.Success)?.platform?.hs
                // Email does not match with a known homeserver, cannot send the invite
                if (homeServer.isNullOrEmpty()) {
                    handleUnauthorizedEmail(existingRoom, email)
                }
                // Invite the user by email and create the DM
                else {
                    handleCreateDirectMessageByEmail(existingRoom, email, TchapUtils.isExternalTchapServer(homeServer))
                }
            }
        }
    }

    private fun handleUnauthorizedEmail(existingRoom: String?, email: String) {
        setState { copy(isLoading = false) }
        // There is no existing room, so notify the UI that the provided email is unauthorized
        if (existingRoom.isNullOrEmpty()) {
            _viewEvents.post(CreateDirectRoomViewEvents.InviteUnauthorizedEmail(email))
        }
        // A DM already exists with the provided email, so ignore the error and notify the user that the invite has been already sent
        else {
            _viewEvents.post(CreateDirectRoomViewEvents.InviteAlreadySent(email))
        }
    }

    private fun handleCreateDirectMessageByEmail(existingRoom: String?, email: String, isExternalEmail: Boolean) {
        setState { copy(isLoading = true) }

        // A DM already exists and the email is not external, so notify the user that the invite has been already sent
        if (existingRoom?.isNotEmpty() == true && !isExternalEmail) {
            setState { copy(isLoading = false) }
            _viewEvents.post(CreateDirectRoomViewEvents.InviteAlreadySent(email))
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                // There is already a discussion with this email but the email is bound to the external instance (for which the invites may expire).
                // We have to re-invite the NoTchapUser by cancelling the pending invite and sending a new one.
                // We don't have a way for the moment to check if the invite has expired or not...
                if (existingRoom?.isNotEmpty() == true && isExternalEmail) {
                    revokePendingInviteAndLeave(existingRoom)
                }

                val roomParams = CreateRoomParams().apply {
                    invite3pids.add(ThreePid.Email(email))
                    setDirectMessage()
                }

                // Create the DM and notify UI about the result
                runCatching { session.createRoom(roomParams) }.fold(
                        {
                            setState { copy(isLoading = false) }
                            _viewEvents.post(CreateDirectRoomViewEvents.InviteSent)
                        },
                        {
                            setState { copy(isLoading = false) }
                            _viewEvents.post(CreateDirectRoomViewEvents.Failure(it))
                        }
                )
            }
        }
    }

    private suspend fun revokePendingInviteAndLeave(roomId: String) {
        session.getRoom(roomId)?.let { room ->
            val token = room.getStateEvent(EventType.STATE_ROOM_THIRD_PARTY_INVITE)?.stateKey

            try {
                if (!token.isNullOrEmpty()) {
                    room.sendStateEvent(
                            eventType = EventType.STATE_ROOM_THIRD_PARTY_INVITE,
                            stateKey = token,
                            body = emptyMap()
                    )
                } else {
                    Timber.d("unable to revoke invite (no pending invite)")
                }

                room.leave()
            } catch (failure: Throwable) {
                setState { copy(isLoading = false) }
                _viewEvents.post(CreateDirectRoomViewEvents.Failure(failure))
            }
        }
    }
}
