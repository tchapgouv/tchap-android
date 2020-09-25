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

package im.vector.app.features.home.room.detail.timeline.item

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.files.LocalFilesHelper
import im.vector.app.core.glide.GlideApp
import im.vector.app.features.home.room.detail.timeline.helper.ContentScannerStateTracker
import im.vector.app.features.home.room.detail.timeline.helper.ContentUploadStateTrackerBinder
import im.vector.app.features.media.ImageContentRenderer
import im.vector.app.features.themes.ThemeUtils
import me.gujun.android.span.span
import org.matrix.android.sdk.api.extensions.tryOrNull

@EpoxyModelClass(layout = R.layout.item_timeline_event_base)
abstract class MessageImageVideoItem : AbsMessageItem<MessageImageVideoItem.Holder>() {

    @EpoxyAttribute
    lateinit var mediaData: ImageContentRenderer.Data

    @EpoxyAttribute
    var playable: Boolean = false

    @EpoxyAttribute
    var mode = ImageContentRenderer.Mode.THUMBNAIL

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var clickListener: ClickListener? = null

    @EpoxyAttribute
    lateinit var imageContentRenderer: ImageContentRenderer

    @EpoxyAttribute
    lateinit var contentUploadStateTrackerBinder: ContentUploadStateTrackerBinder

    @EpoxyAttribute
    var contentScannerStateTracker: ContentScannerStateTracker? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        imageContentRenderer.render(mediaData, mode, holder.imageView)
        if (!attributes.informationData.sendState.hasFailed()) {
            contentUploadStateTrackerBinder.bind(
                    attributes.informationData.eventId,
                    LocalFilesHelper(holder.view.context).isLocalFile(mediaData.url),
                    holder.progressLayout
            )
        } else {
            holder.progressLayout.isVisible = false
        }

        holder.resetAV()
        contentScannerStateTracker?.bind(mediaData.eventId, mediaData.url, mediaData.elementToDecrypt, holder)

        holder.imageView.onClick(clickListener)
        holder.imageView.setOnLongClickListener(attributes.itemLongClickListener)
        ViewCompat.setTransitionName(holder.imageView, "imagePreview_${id()}")
        holder.mediaContentView.onClick(attributes.itemClickListener)
        holder.mediaContentView.setOnLongClickListener(attributes.itemLongClickListener)
        holder.playContentView.visibility = if (playable) View.VISIBLE else View.GONE
    }

    override fun unbind(holder: Holder) {
        tryOrNull { GlideApp.with(holder.imageView).clear(holder.imageView) }
        imageContentRenderer.clear(holder.imageView)
        contentUploadStateTrackerBinder.unbind(attributes.informationData.eventId)
        contentScannerStateTracker?.unBind(mediaData.eventId)
        holder.imageView.setOnClickListener(null)
        holder.imageView.setOnLongClickListener(null)
        super.unbind(holder)
    }

    override fun getViewType() = STUB_ID

    class Holder : AbsMessageItem.Holder(STUB_ID), ScannableHolder {
        val progressLayout by bind<ViewGroup>(R.id.messageMediaUploadProgressLayout)
        val imageView by bind<ImageView>(R.id.messageThumbnailView)
        val playContentView by bind<ImageView>(R.id.messageMediaPlayView)
        val mediaContentView by bind<ViewGroup>(R.id.messageContentMedia)
        val failedToSendIndicator by bind<ImageView>(R.id.messageFailToSendIndicator)

        val avInfectedIndicator by bind<ViewGroup>(R.id.messageMediaInfectedIcon)
        val messageMediaAvText by bind<TextView>(R.id.messageMediaAvText)

        fun resetAV() {
            avInfectedIndicator.isVisible = false
            messageMediaAvText.isVisible = false
            messageMediaAvText.setCompoundDrawables(null, null, null, null)
        }

        override fun mediaScanResult(clean: Boolean) {
            if (clean) {
                avInfectedIndicator.isVisible = false
                messageMediaAvText.text = view.context.getText(R.string.antivirus_clean)
                messageMediaAvText.isVisible = true
                messageMediaAvText.setCompoundDrawablesWithIntrinsicBounds(
                        ContextCompat.getDrawable(view.context, R.drawable.ic_av_checked),
                        null,
                        null,
                        null
                )
            } else {
                // disable click on infected files
                imageView.setOnClickListener(null)
                imageView.setOnLongClickListener(null)
                mediaContentView.setOnClickListener(null)
                mediaContentView.setOnLongClickListener(null)

                avInfectedIndicator.isVisible = true
                messageMediaAvText.text = span(view.context.getText(R.string.antivirus_infected)) {
                    textColor = ThemeUtils.getColor(view.context, R.attr.colorError)
                }
                messageMediaAvText.isVisible = true
                messageMediaAvText.setCompoundDrawables(null, null, null, null)
            }
        }

        override fun mediaScanInProgress() {
            avInfectedIndicator.isVisible = false
            messageMediaAvText.text = span(view.context.getText(R.string.antivirus_in_progress)) {
                textColor = ThemeUtils.getColor(view.context, R.attr.vctr_notice_secondary)
            }
            messageMediaAvText.isVisible = true
            messageMediaAvText.setCompoundDrawables(null, null, null, null)
        }
    }

    companion object {
        private const val STUB_ID = R.id.messageContentMediaStub
    }
}
