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

package fr.gouv.tchap.core.data.room

import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.state.isPublic

object RoomUtils {

    /**
     * FIXME: This method is not used yet and will be moved later
     */
    fun getRoomType(room: Room): RoomTchapType {
        val isEncrypted = room.roomSummary()?.isEncrypted ?: false
        val isDirect = room.roomSummary()?.isDirect ?: false
        val isPublic = room.isPublic()

        // FIXME : Change roomAccessRule with real implementation
        val roomAccessRule = RoomAccessRules.RESTRICTED

        return when {
            isDirect    -> RoomTchapType.DIRECT
            isEncrypted -> when (roomAccessRule) {
                RoomAccessRules.RESTRICTED   -> RoomTchapType.PRIVATE
                RoomAccessRules.UNRESTRICTED -> RoomTchapType.EXTERNAL
            }
            isPublic    -> RoomTchapType.FORUM
            else        -> RoomTchapType.UNKNOWN
        }
    }
}
