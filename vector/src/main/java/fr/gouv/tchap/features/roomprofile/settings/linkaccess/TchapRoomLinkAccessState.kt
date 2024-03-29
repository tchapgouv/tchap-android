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

package fr.gouv.tchap.features.roomprofile.settings.linkaccess

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Uninitialized
import im.vector.app.core.ui.bottomsheet.BottomSheetGenericState
import im.vector.app.features.roomprofile.RoomProfileArgs
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import org.matrix.android.sdk.api.session.room.model.RoomSummary

data class TchapRoomLinkAccessState(
        val roomId: String,
        val currentRoomJoinRules: RoomJoinRules = RoomJoinRules.INVITE,
        val roomSummary: Async<RoomSummary> = Uninitialized,
        val canonicalAlias: String? = null,
        val alternativeAliases: List<String> = emptyList(),
        val isLoading: Boolean = false,
        val canChangeLinkAccess: Boolean = false
) : BottomSheetGenericState() {

    val isLinkAccessEnabled: Boolean
        get() = currentRoomJoinRules != RoomJoinRules.INVITE

    constructor(args: RoomProfileArgs) : this(
            roomId = args.roomId
    )
}
