/*
 * Copyright (c) 2023 Tchap
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

package fr.gouv.tchap.android.sdk.api.auth.login

import org.matrix.android.sdk.api.auth.LoginType
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.internal.auth.AuthAPI
import org.matrix.android.sdk.internal.auth.PendingSessionStore
import org.matrix.android.sdk.internal.auth.SessionCreator
import org.matrix.android.sdk.internal.auth.data.PasswordLoginParams
import org.matrix.android.sdk.internal.auth.data.ThreePidMedium
import org.matrix.android.sdk.internal.auth.db.PendingSessionData
import org.matrix.android.sdk.internal.auth.login.DefaultLoginWizard
import org.matrix.android.sdk.internal.network.executeRequest

internal class TchapLoginWizard(
        private val authAPI: AuthAPI,
        private val sessionCreator: SessionCreator,
        private val pendingSessionStore: PendingSessionStore
) : DefaultLoginWizard(authAPI, sessionCreator, pendingSessionStore) {

    private var pendingSessionData: PendingSessionData = pendingSessionStore.getPendingSessionData() ?: error("Pending session data should exist here")

    override suspend fun login(
            login: String,
            password: String,
            initialDeviceName: String,
            deviceId: String?
    ): Session {
        val loginParams = PasswordLoginParams.thirdPartyIdentifier(
                    medium = ThreePidMedium.EMAIL,
                    address = login,
                    password = password,
                    deviceDisplayName = initialDeviceName,
                    deviceId = deviceId
            )
        val credentials = executeRequest(null) {
            authAPI.login(loginParams)
        }

        return sessionCreator.createSession(credentials, pendingSessionData.homeServerConnectionConfig, LoginType.PASSWORD)
    }
}
