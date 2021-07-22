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

package im.vector.app.features.home

import im.vector.app.core.platform.VectorViewEvents

sealed class HomeDetailViewEvents : VectorViewEvents {
    object Loading : HomeDetailViewEvents()
    object CallStarted : HomeDetailViewEvents()
    data class FailToCall(val failure: Throwable) : HomeDetailViewEvents()
    data class InviteIgnoredForDiscoveredUser(val userId: String) : HomeDetailViewEvents()
    data class InviteIgnoredForUnauthorizedEmail(val email: String) : HomeDetailViewEvents()
    data class InviteIgnoredForExistingRoom(val email: String) : HomeDetailViewEvents()
    object InviteNoTchapUserByEmail : HomeDetailViewEvents()
    data class GetPlatform(val email: String) : HomeDetailViewEvents()
    data class OpenDirectChat(val roomId: String) : HomeDetailViewEvents()
    data class Failure(val throwable: Throwable) : HomeDetailViewEvents()
}
