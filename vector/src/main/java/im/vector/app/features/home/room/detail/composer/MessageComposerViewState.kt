/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.home.room.detail.composer

import com.airbnb.mvrx.MavericksState
import im.vector.app.BuildConfig
import im.vector.app.features.home.room.detail.RoomDetailArgs
import im.vector.app.features.home.room.detail.composer.voice.VoiceMessageRecorderView
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

/**
 * Describes the current send mode:
 * REGULAR: sends the text as a regular message
 * QUOTE: User is currently quoting a message
 * EDIT: User is currently editing an existing message
 *
 * Depending on the state the bottom toolbar will change (icons/preview/actions...)
 */
sealed interface SendMode {
    data class Regular(
            val text: String,
            val fromSharing: Boolean,
            // This is necessary for forcing refresh on selectSubscribe
            private val ts: Long = System.currentTimeMillis()
    ) : SendMode

    data class Quote(val timelineEvent: TimelineEvent, val text: String) : SendMode
    data class Edit(val timelineEvent: TimelineEvent, val text: String) : SendMode
    data class Reply(val timelineEvent: TimelineEvent, val text: String) : SendMode
    data class Voice(val text: String) : SendMode
}

// Tchap: Add an enum state to disable the sending message:
// - In a direct room if the recipient left the room.
// - If the powerLevel doesn't authorize the sending message action.
enum class TchapCanSendMessageState {
    AUTHORIZED,
    PERMISSION_DENIED,
    EMPTY_DM
}

data class MessageComposerViewState(
        val roomId: String,
        val canSendMessage: TchapCanSendMessageState = TchapCanSendMessageState.AUTHORIZED,
        val isSendButtonVisible: Boolean = false,
        val sendMode: SendMode = SendMode.Regular("", false),
        val voiceRecordingUiState: VoiceMessageRecorderView.RecordingUiState = VoiceMessageRecorderView.RecordingUiState.Idle
) : MavericksState {

    val isVoiceRecording = when (voiceRecordingUiState) {
        VoiceMessageRecorderView.RecordingUiState.Idle      -> false
        is VoiceMessageRecorderView.RecordingUiState.Locked,
        VoiceMessageRecorderView.RecordingUiState.Draft,
        is VoiceMessageRecorderView.RecordingUiState.Recording -> true
    }

    val isVoiceMessageIdle = !isVoiceRecording

    val isComposerVisible = (canSendMessage == TchapCanSendMessageState.AUTHORIZED) && !isVoiceRecording
    val isVoiceMessageRecorderVisible = (canSendMessage == TchapCanSendMessageState.AUTHORIZED) && !isSendButtonVisible && BuildConfig.SHOW_VOICE_RECORDER

    @Suppress("UNUSED") // needed by mavericks
    constructor(args: RoomDetailArgs) : this(roomId = args.roomId)
}
