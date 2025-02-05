/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.createdirect

import im.vector.app.core.platform.VectorViewEvents
import org.matrix.android.sdk.api.session.user.model.User

sealed class CreateDirectRoomViewEvents : VectorViewEvents {
    data class UserDiscovered(val user: User) : CreateDirectRoomViewEvents()
    data class InviteUnauthorizedEmail(val email: String) : CreateDirectRoomViewEvents()
    data class InviteAlreadySent(val email: String) : CreateDirectRoomViewEvents()
    object InviteSent : CreateDirectRoomViewEvents()
    data class OpenDirectChat(val roomId: String) : CreateDirectRoomViewEvents()
    data class Failure(val throwable: Throwable) : CreateDirectRoomViewEvents()
    object InvalidCode : CreateDirectRoomViewEvents()
    object DmSelf : CreateDirectRoomViewEvents()
}
