/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomdirectory

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Incomplete
import com.airbnb.mvrx.Success
import fr.gouv.tchap.core.utils.TchapUtils
import im.vector.app.core.epoxy.errorWithRetryItem
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.epoxy.noResultItem
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.AvatarRenderer
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.MatrixPatterns
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.roomdirectory.PublicRoom
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class PublicRoomsController @Inject constructor(
        private val stringProvider: StringProvider,
        private val avatarRenderer: AvatarRenderer,
        private val errorFormatter: ErrorFormatter
) : TypedEpoxyController<PublicRoomsViewState>() {

    var callback: Callback? = null

    override fun buildModels(viewState: PublicRoomsViewState) {
        val host = this
        val publicRooms = viewState.publicRooms

        val unknownRoomItem = viewState.buildUnknownRoomIfNeeded()

        val noResult = publicRooms.isEmpty() && viewState.asyncPublicRoomsRequest is Success && unknownRoomItem == null
        if (noResult) {
            // No result
            noResultItem {
                id("noResult")
                text(host.stringProvider.getString(CommonStrings.no_result_placeholder))
            }
        } else {
            publicRooms.toSortedMap(compareByDescending<PublicRoom> { it.numJoinedMembers }
                    .thenBy { TchapUtils.getHomeServerDisplayNameFromMXIdentifier(it.roomId) }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.toMatrixItem().displayName.toString() }
                    .thenBy { it.roomId })
                    .forEach {
                        buildPublicRoom(it.key, it.value, viewState)
                    }

            unknownRoomItem?.addTo(this)

            if ((viewState.hasMore && viewState.asyncPublicRoomsRequest is Success) ||
                    viewState.asyncPublicRoomsRequest is Incomplete) {
                loadingItem {
                    id("loading")
                }
            }
        }

        if (viewState.asyncPublicRoomsRequest is Fail) {
            errorWithRetryItem {
                id("error")
                text(host.errorFormatter.toHumanReadable(viewState.asyncPublicRoomsRequest.error))
            }
        }
    }

    private fun buildPublicRoom(publicRoom: PublicRoom, roomDirectoryData: RoomDirectoryData, viewState: PublicRoomsViewState) {
        val host = this
        publicRoomItem {
            avatarRenderer(host.avatarRenderer)
            id(publicRoom.roomId)
            matrixItem(publicRoom.toMatrixItem())
            roomAlias(publicRoom.getPrimaryAlias())
            roomTopic(publicRoom.topic)
            roomDomain(TchapUtils.getHomeServerDisplayNameFromMXIdentifier(publicRoom.roomId))
            nbOfMembers(publicRoom.numJoinedMembers)

            val roomChangeMembership = viewState.changeMembershipStates[publicRoom.roomId] ?: ChangeMembershipState.Unknown
            val isJoined = viewState.joinedRoomsIds.contains(publicRoom.roomId) || roomChangeMembership is ChangeMembershipState.Joined
            val joinState = when {
                isJoined -> JoinState.JOINED
                roomChangeMembership is ChangeMembershipState.Joining -> JoinState.JOINING
                roomChangeMembership is ChangeMembershipState.FailedJoining -> JoinState.JOINING_ERROR
                else -> JoinState.NOT_JOINED
            }
            joinState(joinState)

            joinListener {
                host.callback?.onPublicRoomJoin(publicRoom)
            }
            globalListener {
                host.callback?.onPublicRoomClicked(publicRoom, roomDirectoryData, joinState)
            }
        }
    }

    private fun PublicRoomsViewState.buildUnknownRoomIfNeeded(): UnknownRoomItem? {
        val roomIdOrAlias = currentFilter.trim()
        val isAlias = MatrixPatterns.isRoomAlias(roomIdOrAlias) && !publicRooms.any { it.key.canonicalAlias == roomIdOrAlias }
        val isRoomId = !isAlias && MatrixPatterns.isRoomId(roomIdOrAlias) && !publicRooms.any { it.key.roomId == roomIdOrAlias }
        val roomItem = when {
            isAlias -> MatrixItem.RoomAliasItem(roomIdOrAlias, roomIdOrAlias)
            isRoomId -> MatrixItem.RoomItem(roomIdOrAlias)
            else -> null
        }
        val host = this@PublicRoomsController
        return roomItem?.let {
            UnknownRoomItem_().apply {
                id(roomIdOrAlias)
                matrixItem(it)
                avatarRenderer(host.avatarRenderer)
                globalListener {
                    host.callback?.onUnknownRoomClicked(roomIdOrAlias)
                }
            }
        }
    }

    interface Callback {
        fun onUnknownRoomClicked(roomIdOrAlias: String)
        fun onPublicRoomClicked(publicRoom: PublicRoom, roomDirectoryData: RoomDirectoryData, joinState: JoinState)
        fun onPublicRoomJoin(publicRoom: PublicRoom)
    }
}
