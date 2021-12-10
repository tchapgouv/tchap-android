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

import android.content.res.ColorStateList
import android.graphics.Paint
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.onClick
import im.vector.app.features.home.room.detail.timeline.helper.ContentDownloadStateTrackerBinder
import im.vector.app.features.home.room.detail.timeline.helper.ContentScannerStateTracker
import im.vector.app.features.home.room.detail.timeline.helper.ContentUploadStateTrackerBinder
import im.vector.app.features.themes.ThemeUtils
import me.gujun.android.span.span
import org.matrix.android.sdk.internal.crypto.attachments.ElementToDecrypt

@EpoxyModelClass(layout = R.layout.item_timeline_event_base)
abstract class MessageFileItem : AbsMessageItem<MessageFileItem.Holder>() {

    @EpoxyAttribute
    var filename: CharSequence = ""

    @EpoxyAttribute
    var mxcUrl: String = ""

    @EpoxyAttribute
    @DrawableRes
    var iconRes: Int = 0

//    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
//    var clickListener: ClickListener? = null

    @EpoxyAttribute
    var izLocalFile = false

    @EpoxyAttribute
    var izDownloaded = false

    @EpoxyAttribute
    lateinit var contentUploadStateTrackerBinder: ContentUploadStateTrackerBinder

    @EpoxyAttribute
    lateinit var contentDownloadStateTrackerBinder: ContentDownloadStateTrackerBinder

    @EpoxyAttribute
    var contentScannerStateTracker: ContentScannerStateTracker? = null

    @EpoxyAttribute
    var encryptedFileInfo: ElementToDecrypt? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        renderSendState(holder.fileLayout, holder.filenameView)
        if (!attributes.informationData.sendState.hasFailed()) {
            contentUploadStateTrackerBinder.bind(attributes.informationData.eventId, izLocalFile, holder.progressLayout)
        } else {
            holder.fileImageView.setImageResource(R.drawable.ic_cross)
            holder.progressLayout.isVisible = false
        }

        holder.clearAvState()
        contentScannerStateTracker?.bind(attributes.informationData.eventId, mxcUrl, encryptedFileInfo, holder)

        holder.filenameView.text = filename
        if (attributes.informationData.sendState.isSending()) {
            holder.fileImageView.setImageResource(iconRes)
        } else {
            if (izDownloaded) {
                holder.fileImageView.setImageResource(iconRes)
                holder.fileDownloadProgress.progress = 100
            } else {
                contentDownloadStateTrackerBinder.bind(mxcUrl, holder)
                holder.fileImageView.setImageResource(R.drawable.ic_download)
                holder.fileDownloadProgress.progress = 0
            }
        }
//        holder.view.setOnClickListener(clickListener)

        holder.filenameView.onClick(attributes.itemClickListener)
        holder.filenameView.setOnLongClickListener(attributes.itemLongClickListener)
        holder.fileImageWrapper.onClick(attributes.itemClickListener)
        holder.fileImageWrapper.setOnLongClickListener(attributes.itemLongClickListener)
        holder.filenameView.paintFlags = (holder.filenameView.paintFlags or Paint.UNDERLINE_TEXT_FLAG)
    }

    override fun unbind(holder: Holder) {
        super.unbind(holder)
        contentUploadStateTrackerBinder.unbind(attributes.informationData.eventId)
        contentDownloadStateTrackerBinder.unbind(mxcUrl)
        contentScannerStateTracker?.unBind(attributes.informationData.eventId)
    }

    override fun getViewType() = STUB_ID

    class Holder : AbsMessageItem.Holder(STUB_ID), ScannableHolder {
        val progressLayout by bind<ViewGroup>(R.id.messageFileUploadProgressLayout)
        val fileLayout by bind<ViewGroup>(R.id.messageFileLayout)
        val fileImageView by bind<ImageView>(R.id.messageFileIconView)
        val fileImageWrapper by bind<ViewGroup>(R.id.messageFileImageView)
        val fileDownloadProgress by bind<ProgressBar>(R.id.messageFileProgressbar)
        val filenameView by bind<TextView>(R.id.messageFilenameView)

        private val messageFileAvText by bind<TextView>(R.id.messageFileAvText)

        fun clearAvState() {
            fileImageWrapper.background = ContextCompat.getDrawable(view.context, R.drawable.rounded_rect_shape_8)
            fileImageView.setImageResource(R.drawable.ic_attachment)
            messageFileAvText.isVisible = false
        }

        override fun mediaScanResult(clean: Boolean) {
            if (clean) {
                fileDownloadProgress.isVisible = true
                fileImageWrapper.background = ContextCompat.getDrawable(view.context, R.drawable.rounded_rect_shape_8)
                fileImageView.setImageResource(R.drawable.ic_paperclip)
                fileImageView.imageTintList = ColorStateList.valueOf(ThemeUtils.getColor(view.context, R.attr.vctr_notice_secondary))

                messageFileAvText.text = view.context.getText(R.string.antivirus_clean)
                messageFileAvText.isVisible = true
                messageFileAvText.setCompoundDrawablesWithIntrinsicBounds(
                        ContextCompat.getDrawable(view.context, R.drawable.ic_av_checked),
                        null,
                        null,
                        null
                )
            } else {
                // disable click on infected files
                fileImageView.setOnClickListener(null)
                fileImageView.setOnLongClickListener(null)
                filenameView.setOnClickListener(null)
                filenameView.setOnLongClickListener(null)

                fileImageWrapper.background = ContextCompat.getDrawable(view.context, R.drawable.rounded_rect_shape_8)
                fileDownloadProgress.isVisible = false
                fileImageView.setImageResource(R.drawable.ic_tchap_danger)
                fileImageView.imageTintList = null

                messageFileAvText.text = span(view.context.getText(R.string.antivirus_infected)) {
                    textColor = ThemeUtils.getColor(view.context, R.attr.colorError)
                }
                messageFileAvText.isVisible = true
                messageFileAvText.setCompoundDrawables(null, null, null, null)
                filenameView.text = view.context.getString(R.string.tchap_scan_media_untrusted_content_message, filenameView.text)
            }
        }

        override fun mediaScanInProgress() {
            fileImageView.background = ContextCompat.getDrawable(view.context, R.drawable.rounded_rect_shape_8)
            fileImageView.setImageResource(R.drawable.ic_paperclip)

            messageFileAvText.text = span(view.context.getText(R.string.antivirus_in_progress)) {
                textColor = ThemeUtils.getColor(view.context, R.attr.vctr_notice_secondary)
            }
            messageFileAvText.isVisible = true
            messageFileAvText.setCompoundDrawables(null, null, null, null)
        }
    }

    companion object {
        private const val STUB_ID = R.id.messageContentFileStub
    }
}
