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
 */

package im.vector.app.features.roomdirectory.createroom

import android.net.Uri
import fr.gouv.tchap.core.utils.TchapRoomType
import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules

sealed class CreateRoomAction : VectorViewModelAction {
    data class SetAvatar(val imageUri: Uri?) : CreateRoomAction()
    data class SetName(val name: String) : CreateRoomAction()
    data class SetTopic(val topic: String) : CreateRoomAction()
    data class SetVisibility(val rule: RoomJoinRules) : CreateRoomAction()
    data class SetTchapRoomType(val roomType: TchapRoomType) : CreateRoomAction()
    data class SetRoomAliasLocalPart(val aliasLocalPart: String) : CreateRoomAction()
    data class SetIsEncrypted(val isEncrypted: Boolean) : CreateRoomAction()

    object ToggleShowAdvanced : CreateRoomAction()
    data class DisableFederation(val disableFederation: Boolean) : CreateRoomAction()

    object Create : CreateRoomAction()
    object Reset : CreateRoomAction()
}
