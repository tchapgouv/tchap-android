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

package fr.gouv.tchap.android.sdk.internal.session.users

import fr.gouv.tchap.android.sdk.api.session.userinfo.model.TchapUserInfo
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface TchapGetUsersInfoTask : Task<TchapGetUsersInfoParams, Map<String, TchapUserInfo>>

internal class TchapDefaultGetUsersInfoTask @Inject constructor(
        private val usersAPI: TchapUserInfoAPI
) : TchapGetUsersInfoTask {

    override suspend fun execute(params: TchapGetUsersInfoParams): Map<String, TchapUserInfo> {
        val result = executeRequest(null) {
            usersAPI.getUsersInfo(params)
        }

        return result.mapValues {
            TchapUserInfo(
                    expired = it.value.expired,
                    deactivated = it.value.deactivated
            )
        }
    }
}
