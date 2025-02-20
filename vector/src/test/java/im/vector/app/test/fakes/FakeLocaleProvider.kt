/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.core.resources.LocaleProvider
import io.mockk.every
import io.mockk.mockk
import java.util.Locale

class FakeLocaleProvider : LocaleProvider by mockk() {

    fun givenCurrent(locale: Locale) {
        every { current() } returns locale
    }
}
