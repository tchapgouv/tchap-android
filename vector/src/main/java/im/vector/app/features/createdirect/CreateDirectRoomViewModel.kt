/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.createdirect

import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import fr.gouv.tchap.core.utils.TchapUtils
import fr.gouv.tchap.features.platform.GetPlatformResult
import fr.gouv.tchap.features.platform.Params
import fr.gouv.tchap.features.platform.TchapGetPlatformTask
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.mvrx.runCatchingToAsync
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.analytics.plan.CreatedRoom
import im.vector.app.features.raw.wellknown.getElementWellknown
import im.vector.app.features.raw.wellknown.isE2EByDefault
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.userdirectory.PendingSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.getUserOrDefault
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.permalinks.PermalinkData
import org.matrix.android.sdk.api.session.permalinks.PermalinkParser
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.user.model.User
import timber.log.Timber

class CreateDirectRoomViewModel @AssistedInject constructor(
        @Assisted initialState: CreateDirectRoomViewState,
        private val directRoomHelper: DirectRoomHelper,
        private val getPlatformTask: TchapGetPlatformTask,
        private val rawService: RawService,
        private val vectorPreferences: VectorPreferences,
        val session: Session,
        val analyticsTracker: AnalyticsTracker,
) :
        VectorViewModel<CreateDirectRoomViewState, CreateDirectRoomAction, CreateDirectRoomViewEvents>(initialState) {

    private val tchap = Tchap()

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<CreateDirectRoomViewModel, CreateDirectRoomViewState> {
        override fun create(initialState: CreateDirectRoomViewState): CreateDirectRoomViewModel
    }

    companion object : MavericksViewModelFactory<CreateDirectRoomViewModel, CreateDirectRoomViewState> by hiltMavericksViewModelFactory()

    override fun handle(action: CreateDirectRoomAction) {
        when (action) {
            is CreateDirectRoomAction.InviteByEmail -> tchap.handleIndividualInviteByEmail(action.email)
            is CreateDirectRoomAction.CreateDirectMessageByUserId -> tchap.handleCreateDirectMessageByUserId(action.userId)
            is CreateDirectRoomAction.PrepareRoomWithSelectedUsers -> tchap.onSubmitInvitees(action.selections)
            is CreateDirectRoomAction.CreateRoomAndInviteSelectedUsers -> onCreateRoomWithInvitees()
            is CreateDirectRoomAction.QrScannedAction -> onCodeParsed(action)
        }
    }

    private fun onCodeParsed(action: CreateDirectRoomAction.QrScannedAction) {
        val mxid = (PermalinkParser.parse(action.result) as? PermalinkData.UserLink)?.userId

        if (mxid === null) {
            _viewEvents.post(CreateDirectRoomViewEvents.InvalidCode)
        } else {
            // The following assumes MXIDs are case insensitive
            if (mxid.equals(other = session.myUserId, ignoreCase = true)) {
                _viewEvents.post(CreateDirectRoomViewEvents.DmSelf)
            } else {
                // Try to get user from known users and fall back to creating a User object from MXID
                val qrInvitee = session.getUserOrDefault(mxid)
                tchap.onSubmitInvitees(setOf(PendingSelection.UserPendingSelection(qrInvitee)))
            }
        }
    }

    /**
     * If users already have a DM room then navigate to it instead of creating a new room.
     */
    // TCHAP unused, replaced by Tchap.onSubmitInvitees
    private fun onSubmitInvitees(selections: Set<PendingSelection>) {
        val existingRoomId = selections.singleOrNull()?.getMxId()?.let { userId ->
            session.roomService().getExistingDirectRoomWithUser(userId)
        }
        if (existingRoomId != null) {
            // Do not create a new DM, just tell that the creation is successful by passing the existing roomId
            setState { copy(createAndInviteState = Success(existingRoomId)) }
        } else {
            createLocalRoomWithSelectedUsers(selections)
        }
    }

    private fun onCreateRoomWithInvitees() {
        // Create the DM
        withState { createLocalRoomWithSelectedUsers(it.pendingSelections) }
    }

    private fun createLocalRoomWithSelectedUsers(selections: Set<PendingSelection>) {
        setState { copy(createAndInviteState = Loading()) }

        viewModelScope.launch(Dispatchers.IO) {
            val adminE2EByDefault = rawService.getElementWellknown(session.sessionParams)
                    ?.isE2EByDefault()
                    ?: true

            val roomParams = CreateRoomParams()
                    .apply {
                        selections.forEach {
                            when (it) {
                                is PendingSelection.UserPendingSelection -> invitedUserIds.add(it.user.userId)
                                is PendingSelection.ThreePidPendingSelection -> invite3pids.add(it.threePid)
                            }
                        }
                        setDirectMessage()
                        enableEncryptionIfInvitedUsersSupportIt = adminE2EByDefault
                    }

            val result = runCatchingToAsync {
                if (vectorPreferences.isDeferredDmEnabled() && roomParams.invite3pids.isEmpty()) {
                    session.roomService().createLocalRoom(roomParams)
                } else {
                    analyticsTracker.capture(CreatedRoom(isDM = roomParams.isDirect.orFalse()))
                    session.roomService().createRoom(roomParams)
                }
            }

            setState {
                copy(
                        createAndInviteState = result
                )
            }
        }
    }

    /**
     * Container to isolate Tchap specific methods, should help to reduce the conflicts when rebasing against Element.
     */
    private inner class Tchap {
        /**
         * If users already have a DM room then navigate to it instead of creating a new room.
         */
        fun onSubmitInvitees(selections: Set<PendingSelection>) {
            // TCHAP All the user invite and DM creation process has been reworked
            // TCHAP - multi-selection is forbidden, DM are restricted to 1:1
            // TCHAP - invites by email might expire for external accounts so we have to cancel pending invites to send a new ones
            // TCHAP - invites by msisdn are not supported yet
            val selection = selections.singleOrNull() ?: return
            setState { copy(isLoading = true) }
            when (selection) {
                // User already exists, so we can create or retrieve the DM with him
                is PendingSelection.UserPendingSelection -> handleCreateDirectMessageByUserId(selection.user.userId)
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

        fun handleCreateDirectMessageByUserId(userId: String) {
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
                        })
            }
        }

        fun handleIndividualInviteByEmail(email: String) {
            setState { copy(isLoading = true) }
            val existingRoom = session.roomService().getExistingDirectRoomWithUser(email)
            viewModelScope.launch(Dispatchers.IO) {
                // Start the invite process by checking whether a Tchap account has been created for this email.
                val userId = tryOrNull { session.identityService().lookUp(listOf(ThreePid.Email(email))) }
                        ?.find { it.threePid.value == email }
                        ?.matrixId

                // Email matches with an existing account, notify the UI with the resulting user
                if (userId != null) {
                    setState { copy(isLoading = false) }
                    val user = tryOrNull { session.userService().resolveUser(userId) } ?: User(userId, TchapUtils.computeDisplayNameFromUserId(userId), null)
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

        fun handleUnauthorizedEmail(existingRoom: String?, email: String) {
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

        fun handleCreateDirectMessageByEmail(existingRoom: String?, email: String, isExternalEmail: Boolean) {
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
                    runCatching { session.roomService().createRoom(roomParams) }.fold(
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

        suspend fun revokePendingInviteAndLeave(roomId: String) {
            session.getRoom(roomId)?.let { room ->
                val token = room.stateService().getStateEvent(EventType.STATE_ROOM_THIRD_PARTY_INVITE, QueryStringValue.IsNotNull)?.stateKey

                try {
                    if (!token.isNullOrEmpty()) {
                        room.stateService().sendStateEvent(
                                eventType = EventType.STATE_ROOM_THIRD_PARTY_INVITE,
                                stateKey = token,
                                body = emptyMap()
                        )
                    } else {
                        Timber.d("unable to revoke invite (no pending invite)")
                    }

                    session.roomService().leaveRoom(roomId)
                } catch (failure: Throwable) {
                    setState { copy(isLoading = false) }
                    _viewEvents.post(CreateDirectRoomViewEvents.Failure(failure))
                }
            }
        }
    }
}
