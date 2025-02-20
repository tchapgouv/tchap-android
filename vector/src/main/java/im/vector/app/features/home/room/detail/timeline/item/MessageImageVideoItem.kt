/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.files.LocalFilesHelper
import im.vector.app.core.glide.GlideApp
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.home.room.detail.timeline.helper.ContentScannerStateTracker
import im.vector.app.features.home.room.detail.timeline.helper.ContentUploadStateTrackerBinder
import im.vector.app.features.home.room.detail.timeline.style.TimelineMessageLayout
import im.vector.app.features.home.room.detail.timeline.style.granularRoundedCorners
import im.vector.app.features.media.ImageContentRenderer
import im.vector.app.features.themes.ThemeUtils
import im.vector.lib.strings.CommonStrings
import me.gujun.android.span.span
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.room.model.message.MessageType

@EpoxyModelClass
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
        val messageLayout = baseAttributes.informationData.messageLayout
        val dimensionConverter = DimensionConverter(holder.view.resources)
        val imageCornerTransformation = if (messageLayout is TimelineMessageLayout.Bubble) {
            messageLayout.cornersRadius.granularRoundedCorners()
        } else {
            RoundedCorners(dimensionConverter.dpToPx(8))
        }
        imageContentRenderer.render(mediaData, mode, holder.imageView, imageCornerTransformation)
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

        val isImageMessage = attributes.informationData.messageType in listOf(MessageType.MSGTYPE_IMAGE, MessageType.MSGTYPE_STICKER_LOCAL)
        val autoplayAnimatedImages = attributes.autoplayAnimatedImages

        holder.playContentView.visibility = if (playable && isImageMessage && autoplayAnimatedImages) {
            View.GONE
        } else if (playable) {
            View.VISIBLE
        } else {
            View.GONE
        }
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

    override fun getViewStubId() = STUB_ID

    class Holder : AbsMessageItem.Holder(STUB_ID), ScannableHolder {
        val progressLayout by bind<ViewGroup>(R.id.messageMediaUploadProgressLayout)
        val imageView by bind<ImageView>(R.id.messageThumbnailView)
        val playContentView by bind<ImageView>(R.id.messageMediaPlayView)
        val mediaContentView by bind<ViewGroup>(R.id.messageContentMedia)
        val failedToSendIndicator by bind<ImageView>(R.id.messageFailToSendIndicator)

        val avInfectedIndicator by bind<ImageView>(R.id.messageMediaInfectedIcon)
        val messageMediaAvText by bind<TextView>(R.id.messageMediaAvText)

        fun resetAV() {
            avInfectedIndicator.isVisible = false
            messageMediaAvText.isVisible = false
            messageMediaAvText.setCompoundDrawables(null, null, null, null)
        }

        override fun mediaScanResult(clean: Boolean) {
            if (clean) {
                avInfectedIndicator.isVisible = false
                messageMediaAvText.text = view.context.getText(CommonStrings.antivirus_clean)
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
                messageMediaAvText.text = span(view.context.getText(CommonStrings.antivirus_infected)) {
                    textColor = ThemeUtils.getColor(view.context, com.google.android.material.R.attr.colorError)
                }
                messageMediaAvText.isVisible = true
                messageMediaAvText.setCompoundDrawables(null, null, null, null)
            }
        }

        override fun mediaScanInProgress() {
            avInfectedIndicator.isVisible = false
            messageMediaAvText.text = span(view.context.getText(CommonStrings.antivirus_in_progress)) {
                textColor = ThemeUtils.getColor(view.context, im.vector.lib.ui.styles.R.attr.vctr_notice_secondary)
            }
            messageMediaAvText.isVisible = true
            messageMediaAvText.setCompoundDrawables(null, null, null, null)
        }
    }

    companion object {
        private val STUB_ID = R.id.messageContentMediaStub
    }
}
