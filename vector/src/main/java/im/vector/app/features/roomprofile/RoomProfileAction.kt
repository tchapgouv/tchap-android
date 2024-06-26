/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package im.vector.app.features.roomprofile

import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState

sealed class RoomProfileAction : VectorViewModelAction {
    object EnableEncryption : RoomProfileAction()
    object LeaveRoom : RoomProfileAction()
    data class ChangeRoomNotificationState(val notificationState: RoomNotificationState) : RoomProfileAction()
    object ShareRoomProfile : RoomProfileAction()
    object CreateShortcut : RoomProfileAction()
    object RestoreEncryptionState : RoomProfileAction()
    // TCHAP force to false to deactivate "Never send messages to unverified devices in room"
//    data class SetEncryptToVerifiedDeviceOnly(val enabled: Boolean) : RoomProfileAction()
}
