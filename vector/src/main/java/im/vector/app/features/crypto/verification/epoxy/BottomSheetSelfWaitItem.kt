/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.crypto.verification.epoxy

import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel

/**
 * A action for bottom sheet.
 */
@EpoxyModelClass
abstract class BottomSheetSelfWaitItem : VectorEpoxyModel<BottomSheetSelfWaitItem.Holder>(R.layout.item_verification_wait) {

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.otherMxClientCaption.isVisible = false // There are no other mx clients in Tchap
    }

    class Holder : VectorEpoxyHolder() {
        val otherMxClientCaption by bind<TextView>(R.id.otherMxClientCaption)
    }
}
