/*
 * Copyright 2018 New Vector Ltd
 * Copyright 2018 DINSIC
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
package fr.gouv.tchap.core.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

/**
 * Class converted from Java to Kotlin
 * TODO : Fix code which wasn't well converted
 */
class HexagonMaskView : AppCompatImageView {
    private var hexagonPath: Path? = null
    private var width = 0f
    private var height = 0f
    private var borderPaint: Paint? = null
    private var borderRatio = 0

    constructor(
            context: Context?
    ) : super(context!!) {
        init()
    }

    constructor(
            context: Context?,
            attrs: AttributeSet?
    ) : super(context!!, attrs) {
        init()
    }

    constructor(
            context: Context?,
            attrs: AttributeSet?,
            defStyleAttr: Int
    ) : super(context!!, attrs, defStyleAttr) {
        init()
    }

    /**
     * Define the border settings
     *
     * @param color the border color (Color.LTGRAY by default).
     * @param ratio the ratio of the border width to the radius
     * of the hexagon (value between 0 and 100, default value: 3)
     */
    fun setBorderSettings(color: Int, ratio: Int) {
        var ratio2 = ratio

        borderPaint?.color = color

        if (ratio < 0) {
            ratio2 = 0
        } else if (ratio > 100) {
            ratio2 = 100
        }
        if (borderRatio != ratio) {
            borderRatio = ratio2
            // The hexagon path must be updated
            calculatePath()
        } else {
            invalidate()
        }
    }

    private fun init() {
        hexagonPath = Path()
        borderPaint = Paint()
        borderPaint!!.isAntiAlias = true
        borderPaint!!.style = Paint.Style.STROKE
        borderPaint!!.color = Color.LTGRAY
        borderRatio = 3
    }

    private fun calculatePath() {
        // Compute the radius of the hexagon, and the border width
        val radius = height / 2
        val borderWidth = radius * borderRatio / 100
        borderPaint!!.strokeWidth = borderWidth

        // Define the hexagon path by placing it in the middle of the border.
        val pathRadius = radius - borderWidth / 2
        val triangleHeight = (Math.sqrt(3.0) * pathRadius / 2).toFloat()
        val centerX = width / 2
        val centerY = height / 2
        hexagonPath!!.reset()
        hexagonPath!!.moveTo(centerX, centerY + pathRadius)
        hexagonPath!!.lineTo(centerX - triangleHeight, centerY + pathRadius / 2)
        hexagonPath!!.lineTo(centerX - triangleHeight, centerY - pathRadius / 2)
        hexagonPath!!.lineTo(centerX, centerY - pathRadius)
        hexagonPath!!.lineTo(centerX + triangleHeight, centerY - pathRadius / 2)
        hexagonPath!!.lineTo(centerX + triangleHeight, centerY + pathRadius / 2)
        hexagonPath!!.lineTo(centerX, centerY + pathRadius)
        // Add again the first segment to get the right display of the border.
        hexagonPath!!.lineTo(centerX - triangleHeight, centerY + pathRadius / 2)
        invalidate()
    }

    public override fun onDraw(c: Canvas) {
        // Apply a clip to draw the bitmap inside an hexagon shape
        c.save()
        c.clipPath(hexagonPath!!)
        super.onDraw(c)
        // Restore the canvas context
        c.restore()
        // Draw the border
        c.drawPath(hexagonPath!!, borderPaint!!)
    }

    // getting the view size
    public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        width = this.measuredWidth.toFloat()
        height = this.measuredHeight.toFloat()
        calculatePath()
    }
}