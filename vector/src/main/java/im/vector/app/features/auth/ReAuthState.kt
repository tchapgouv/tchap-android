/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.auth

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import org.matrix.android.sdk.api.session.identity.ThreePid

data class ReAuthState(
        val title: String? = null,
        val session: String? = null,
        val flowType: String? = null,
        val ssoFallbackPageWasShown: Boolean = false,
        val lastErrorCode: String? = null,
        val resultKeyStoreAlias: String = "",
        val threePids: Async<List<ThreePid>> = Uninitialized,
) : MavericksState {
    constructor(args: ReAuthActivity.Args) : this(
            args.title,
            args.session,
            args.flowType,
            lastErrorCode = args.lastErrorCode,
            resultKeyStoreAlias = args.resultKeyStoreAlias
    )

    constructor() : this(null, null)
}
