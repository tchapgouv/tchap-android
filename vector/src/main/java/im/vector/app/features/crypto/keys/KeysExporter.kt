/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.keys

import android.content.Context
import android.net.Uri
import im.vector.app.core.dispatchers.CoroutineDispatchers
import im.vector.app.core.extensions.safeOpenOutputStream
import im.vector.app.core.resources.StringProvider
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

class KeysExporter @Inject constructor(
        private val authenticationService: AuthenticationService,
        private val stringProvider: StringProvider,
        private val session: Session,
        private val context: Context,
        private val dispatchers: CoroutineDispatchers
) {
    /**
     * Export keys and write them to the provided uri.
     */
    suspend fun export(password: String, uri: Uri) {
        withContext(dispatchers.io) {
            checkPasswordPolicy(password)
            val data = session.cryptoService().exportRoomKeys(password)
            context.safeOpenOutputStream(uri)
                    ?.use { it.write(data) }
                    ?: throw IllegalStateException("Unable to open file for writing")
            verifyExportedKeysOutputFileSize(uri, expectedSize = data.size.toLong())
        }
    }

    private fun verifyExportedKeysOutputFileSize(uri: Uri, expectedSize: Long) {
        val output = context.contentResolver.openFileDescriptor(uri, "r", null)
        when {
            output == null -> throw IllegalStateException("Exported file not found")
            output.statSize != expectedSize -> {
                val exception = UnexpectedExportKeysFileSizeException(
                        expectedFileSize = expectedSize,
                        actualFileSize = output.statSize
                )
                output.close()
                throw exception
            }
        }
    }

    // TCHAP add policy on the password to export keys
    private suspend fun checkPasswordPolicy(password: String) {
        val passwordPolicy = tryOrNull { authenticationService.getPasswordPolicy(session.sessionParams.homeServerConnectionConfig) }
        val isValid = passwordPolicy?.let { policy ->
            val minLengthValid = policy.minLength?.let { minLength -> password.length >= minLength } ?: true
            val hasDigit = policy.requireDigit == null || password.any { it.isDigit() }
            val hasLowercase = policy.requireLowercase == null || password.any { it.isLowerCase() }
            val hasUppercase = policy.requireUppercase == null || password.any { it.isUpperCase() }
            val hasSymbol = policy.requireSymbol == null || password.any { !it.isLetterOrDigit() }

            minLengthValid && hasDigit && hasLowercase && hasUppercase && hasSymbol
        } ?: true

        if (!isValid) {
            throw Failure.ServerError(
                    error = MatrixError(
                            code = MatrixError.M_WEAK_PASSWORD,
                            message = stringProvider.getString(CommonStrings.tchap_password_weak_pwd_error)
                    ),
                    httpCode = 400
            )
        }
    }
}

class UnexpectedExportKeysFileSizeException(expectedFileSize: Long, actualFileSize: Long) : IllegalStateException(
        "Exported Keys file has unexpected file size, got: $actualFileSize but expected: $expectedFileSize"
)
