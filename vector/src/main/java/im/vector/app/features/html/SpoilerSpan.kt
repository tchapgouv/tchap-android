/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.html

import android.graphics.Color
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import im.vector.app.core.resources.ColorProvider

class SpoilerSpan(private val colorProvider: ColorProvider) : ClickableSpan() {

    override fun onClick(widget: View) {
        isHidden = !isHidden
        widget.invalidate()
    }

    private var isHidden = true

    override fun updateDrawState(tp: TextPaint) {
        if (isHidden) {
            tp.bgColor = colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_spoiler_background_color)
            tp.color = Color.TRANSPARENT
        } else {
            tp.bgColor = colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_markdown_block_background_color)
            tp.color = colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_primary)
        }
    }
}
