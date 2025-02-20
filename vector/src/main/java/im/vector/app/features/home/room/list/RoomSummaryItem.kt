/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list

import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.amulyakhare.textdrawable.TextDrawable
import fr.gouv.tchap.core.utils.TchapRoomType
import fr.gouv.tchap.core.utils.TchapUtils
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.ui.views.PresenceStateImageView
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.RoomListDisplayMode
import im.vector.app.features.themes.ThemeUtils
import im.vector.lib.core.utils.epoxy.charsequence.EpoxyCharSequence
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.session.presence.model.UserPresence
import org.matrix.android.sdk.api.util.MatrixItem

@EpoxyModelClass
abstract class RoomSummaryItem : VectorEpoxyModel<RoomSummaryItem.Holder>(R.layout.item_tchap_room) {

    @EpoxyAttribute
    lateinit var typingMessage: String

    @EpoxyAttribute
    lateinit var avatarRenderer: AvatarRenderer

    @EpoxyAttribute
    lateinit var matrixItem: MatrixItem

    @EpoxyAttribute
    var displayMode: RoomListDisplayMode = RoomListDisplayMode.PEOPLE

    @EpoxyAttribute
    lateinit var subtitle: String

    @EpoxyAttribute
    lateinit var lastFormattedEvent: EpoxyCharSequence

    @EpoxyAttribute
    lateinit var lastEventTime: String

    @EpoxyAttribute
    var encryptionTrustLevel: RoomEncryptionTrustLevel? = null

    @EpoxyAttribute
    var userPresence: UserPresence? = null

    @EpoxyAttribute
    var showPresence: Boolean = false

    @EpoxyAttribute
    var izPublic: Boolean = false

    @EpoxyAttribute
    var unreadNotificationCount: Int = 0

    @EpoxyAttribute
    var hasUnreadMessage: Boolean = false

    @EpoxyAttribute
    var hasDraft: Boolean = false

    @EpoxyAttribute
    var showHighlighted: Boolean = false

    @EpoxyAttribute
    var hasFailedSending: Boolean = false

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var itemLongClickListener: View.OnLongClickListener? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var itemClickListener: ClickListener? = null

    @EpoxyAttribute
    var showSelected: Boolean = false

    // TCHAP items
    @EpoxyAttribute
    lateinit var roomType: TchapRoomType

    @EpoxyAttribute
    var useSingleLineForLastEvent: Boolean = false

    override fun bind(holder: Holder) {
        super.bind(holder)

        renderDisplayMode(holder)
        holder.rootView.onClick(itemClickListener)
        holder.rootView.setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            itemLongClickListener?.onLongClick(it) ?: false
        }
        // TCHAP remove domain from the display name
        holder.titleView.text = TchapUtils.getRoomNameFromDisplayName(matrixItem.getBestName(), roomType)

        holder.unreadCounterBadgeView.render(UnreadCounterBadgeView.State.Count(unreadNotificationCount, showHighlighted))
        holder.unreadIndentIndicator.isVisible = hasUnreadMessage
        holder.draftView.isVisible = hasDraft
        avatarRenderer.render(matrixItem, holder.avatarImageView)
        // TCHAP deleted in Tchap layout
//        holder.roomAvatarDecorationImageView.render(encryptionTrustLevel)
//        holder.roomAvatarPublicDecorationImageView.isVisible = izPublic
        // TCHAP Set invisible instead of gone to keep view size
        holder.roomAvatarFailSendingImageView.isInvisible = !hasFailedSending
        renderSelection(holder, showSelected)
        holder.roomAvatarPresenceImageView.render(showPresence, userPresence)

        // TCHAP items
        renderTchapRoomType(holder)

        if (useSingleLineForLastEvent) {
            holder.subtitleView.setLines(1)
        }
    }

    private fun renderDisplayMode(holder: Holder) = when (displayMode) {
        RoomListDisplayMode.ROOMS,
        RoomListDisplayMode.PEOPLE,
        RoomListDisplayMode.NOTIFICATIONS -> renderForDefaultDisplayMode(holder)
        RoomListDisplayMode.FILTERED -> renderForFilteredDisplayMode(holder)
    }

    private fun renderForDefaultDisplayMode(holder: Holder) {
        holder.subtitleView.text = lastFormattedEvent.charSequence
        holder.lastEventTimeView.text = lastEventTime
        holder.typingView.setTextOrHide(typingMessage)
        holder.subtitleView.isInvisible = holder.typingView.isVisible
    }

    private fun renderForFilteredDisplayMode(holder: Holder) {
        holder.subtitleView.text = subtitle
    }

    override fun unbind(holder: Holder) {
        holder.rootView.setOnClickListener(null)
        holder.rootView.setOnLongClickListener(null)
        avatarRenderer.clear(holder.avatarImageView)
        super.unbind(holder)
    }

    private fun renderSelection(holder: Holder, isSelected: Boolean) {
        if (isSelected) {
            holder.avatarCheckedImageView.visibility = View.VISIBLE
            val backgroundColor = ThemeUtils.getColor(holder.view.context, com.google.android.material.R.attr.colorPrimary)
            val backgroundDrawable = TextDrawable.builder().buildRound("", backgroundColor)
            holder.avatarImageView.setImageDrawable(backgroundDrawable)
        } else {
            holder.avatarCheckedImageView.visibility = View.GONE
            avatarRenderer.render(matrixItem, holder.avatarImageView)
        }
    }

    private fun renderTchapRoomType(holder: Holder) {
        with(holder) {
            when (roomType) {
                TchapRoomType.EXTERNAL -> {
                    domainNameView.text = view.context.getString(CommonStrings.tchap_room_extern_room_type)
                    domainNameView.setTextColor(ContextCompat.getColor(view.context, im.vector.lib.ui.styles.R.color.tchap_room_external))
                }
                TchapRoomType.FORUM -> {
                    domainNameView.text = view.context.getString(CommonStrings.tchap_room_forum_type)
                    domainNameView.setTextColor(ContextCompat.getColor(view.context, im.vector.lib.ui.styles.R.color.tchap_room_forum))
                }
                else -> {
                    domainNameView.text = null
                }
            }

            val drawable = if (roomType == TchapRoomType.FORUM) ContextCompat.getDrawable(view.context, R.drawable.ic_tchap_forum) else null
            avatarRoomTypeImageView.setImageDrawable(drawable)
        }
    }

    class Holder : VectorEpoxyHolder() {
        val titleView by bind<TextView>(R.id.roomNameView)
        val unreadCounterBadgeView by bind<UnreadCounterBadgeView>(R.id.roomUnreadCounterBadgeView)
        val unreadIndentIndicator by bind<View>(R.id.roomUnreadIndicator)
        val subtitleView by bind<TextView>(R.id.subtitleView)
        val typingView by bind<TextView>(R.id.roomTypingView)
        val draftView by bind<ImageView>(R.id.roomDraftBadge)
        val lastEventTimeView by bind<TextView>(R.id.roomLastEventTimeView)
        val avatarCheckedImageView by bind<ImageView>(R.id.roomAvatarCheckedImageView)
        val avatarImageView by bind<ImageView>(R.id.roomAvatarImageView)

        // TCHAP deleted in Tchap layout
        // val roomAvatarDecorationImageView by bind<ShieldImageView>(R.id.roomAvatarDecorationImageView)
        // val roomAvatarPublicDecorationImageView by bind<ImageView>(R.id.roomAvatarPublicDecorationImageView)
        val roomAvatarFailSendingImageView by bind<ImageView>(R.id.roomAvatarFailSendingImageView)
        val roomAvatarPresenceImageView by bind<PresenceStateImageView>(R.id.roomAvatarPresenceImageView)
        val rootView by bind<ConstraintLayout>(R.id.itemRoomLayout)

        // TCHAP items
        val domainNameView by bind<TextView>(R.id.tchapRoomDomainNameView)
        val avatarRoomTypeImageView by bind<ImageView>(R.id.tchapRoomTypeImageView)
    }
}
