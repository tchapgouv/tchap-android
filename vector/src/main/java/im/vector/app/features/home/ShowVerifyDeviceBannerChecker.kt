/*
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import org.matrix.android.sdk.api.MatrixPatterns.getServerName
import java.security.MessageDigest
import javax.inject.Inject

class ShowVerifyDeviceBannerChecker @Inject constructor() {
    companion object {
        private val EXCLUDED_HASHED_DOMAINS = setOf(
                "be2ca6fbe45d25e254c69f3033cf04025c3a3b12f1f1ac41e0cd53a604b3953d",
                "2264334ed43c7ad4a012b038272f77fa0310398c57deb6245655f2a01d9ded3c"
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun canShowVerifyDeviceBanner(userId: String): Boolean {
        val sha256 = MessageDigest.getInstance("SHA-256")
        val hashedDomain = userId.getServerName()
                .split(".")
                .takeLast(3)
                .joinToString(".")
                .let { sha256.digest(it.toByteArray()).toHexString() }
        return hashedDomain !in EXCLUDED_HASHED_DOMAINS
    }
}
