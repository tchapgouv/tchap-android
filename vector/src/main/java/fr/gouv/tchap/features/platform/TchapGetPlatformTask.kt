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

package fr.gouv.tchap.features.platform

import android.content.Context
import fr.gouv.tchap.android.sdk.internal.services.threepidplatformdiscover.model.Platform
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.platform.ViewModelTask
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.api.session.identity.ThreePid
import timber.log.Timber
import javax.inject.Inject

sealed class GetPlatformResult {
    data class Success(val platform: Platform) : GetPlatformResult()
    data class Failure(val throwable: Throwable) : GetPlatformResult()
}

data class Params(
        val email: String
)

/**
 * Get the Tchap platform configuration (HS/IS) for the provided email address.
 */
class TchapGetPlatformTask @Inject constructor(
        private val context: Context,
        private val activeSessionHolder: ActiveSessionHolder,
        private val matrix: Matrix
) : ViewModelTask<Params, GetPlatformResult> {

    override suspend fun execute(params: Params): GetPlatformResult {
        Timber.d("## TchapGetPlatformTask [${params.email}]")

        // Prepare the list of the known IS urls in order to run over the list until to get an answer.
        val idServerUrls = buildIdServerUrls()
        if (idServerUrls.isEmpty()) {
            return GetPlatformResult.Failure(Failure.ServerError(MatrixError(
                    code = MatrixError.M_UNKNOWN,
                    message = "No host"
            ), 403))
        }

        var failure: Throwable? = null
        idServerUrls.onEach { url ->
            try {
                val platform = matrix.threePidPlatformDiscoverService().getPlatform(url, ThreePid.Email(params.email))
                Timber.d("## TchapGetPlatformTask succeeded (${platform.hs})")
                return GetPlatformResult.Success(platform)
            } catch (throwable: Throwable) {
                Timber.e(throwable, "## TchapGetPlatformTask failed ")
                failure = throwable
            }
        }

        return GetPlatformResult.Failure(failure ?: Failure.Unknown(null))
    }

    private fun buildIdServerUrls(): Set<String> {
        val idServerUrls = mutableSetOf<String>()

        // Consider first the current identity server if any.
        val currentIdServerUrl = activeSessionHolder.getSafeActiveSession()?.identityService()?.getCurrentIdentityServerUrl()
        if (currentIdServerUrl != null) {
            idServerUrls.add(currentIdServerUrl.removeSuffix("/"))
        }

        // add randomly the preferred known ISes plus the other known ISes
        idServerUrls += context.resources.getStringArray(R.array.preferred_identity_server_names).toList().shuffled()
                .plus(context.resources.getStringArray(R.array.identity_server_names).toList().shuffled())
                .map { host -> context.getString(R.string.server_url_prefix) + host.removeSuffix("/") }

        return idServerUrls
    }
}
