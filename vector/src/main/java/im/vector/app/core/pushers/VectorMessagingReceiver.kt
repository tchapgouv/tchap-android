/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.core.pushers

import android.content.Context
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.services.GuardServiceStarter
import im.vector.app.features.settings.BackgroundSyncMode
import im.vector.app.features.settings.VectorPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.matrix.android.sdk.api.logger.LoggerTag
import org.unifiedpush.android.connector.MessagingReceiver
import timber.log.Timber
import javax.inject.Inject

private val loggerTag = LoggerTag("Push", LoggerTag.SYNC)

/**
 * Hilt injection happen at super.onReceive().
 */
@AndroidEntryPoint
class VectorMessagingReceiver : MessagingReceiver() {
    @Inject lateinit var pushersManager: PushersManager
    @Inject lateinit var activeSessionHolder: ActiveSessionHolder
    @Inject lateinit var vectorPreferences: VectorPreferences
    @Inject lateinit var vectorMessagingHelper: VectorMessagingHelper
    @Inject lateinit var guardServiceStarter: GuardServiceStarter
    @Inject lateinit var unifiedPushStore: UnifiedPushStore
    @Inject lateinit var unifiedPushHelper: UnifiedPushHelper

    private val coroutineScope = CoroutineScope(SupervisorJob())

    /**
     * Called when message is received.
     *
     * @param context the Android context
     * @param message the message
     * @param instance connection, for multi-account
     */
    override fun onMessage(context: Context, message: ByteArray, instance: String) {
        vectorMessagingHelper.onMessage(String(message))
    }

    override fun onNewEndpoint(context: Context, endpoint: String, instance: String) {
        Timber.tag(loggerTag.value).i("onNewEndpoint: adding $endpoint")
        if (vectorPreferences.areNotificationEnabledForDevice() && activeSessionHolder.hasActiveSession()) {
            // If the endpoint has changed
            // or the gateway has changed
            if (unifiedPushStore.getEndpointOrToken() != endpoint) {
                unifiedPushStore.storeUpEndpoint(endpoint)
                coroutineScope.launch {
                    unifiedPushHelper.storeCustomOrDefaultGateway(endpoint) {
                        unifiedPushStore.getPushGateway()?.let {
                            pushersManager.enqueueRegisterPusher(endpoint, it)
                        }
                    }
                }
            } else {
                Timber.tag(loggerTag.value).i("onNewEndpoint: skipped")
            }
        }
        val mode = BackgroundSyncMode.FDROID_BACKGROUND_SYNC_MODE_DISABLED
        vectorPreferences.setFdroidSyncBackgroundMode(mode)
        guardServiceStarter.stop()
    }

    override fun onRegistrationFailed(context: Context, instance: String) {
        Toast.makeText(context, "Push service registration failed", Toast.LENGTH_SHORT).show()
        val mode = BackgroundSyncMode.FDROID_BACKGROUND_SYNC_MODE_FOR_REALTIME
        vectorPreferences.setFdroidSyncBackgroundMode(mode)
        guardServiceStarter.start()
    }

    override fun onUnregistered(context: Context, instance: String) {
        Timber.tag(loggerTag.value).d("Unifiedpush: Unregistered")
        val mode = BackgroundSyncMode.FDROID_BACKGROUND_SYNC_MODE_FOR_REALTIME
        vectorPreferences.setFdroidSyncBackgroundMode(mode)
        guardServiceStarter.start()
        runBlocking {
            try {
                pushersManager.unregisterPusher(unifiedPushStore.getEndpointOrToken().orEmpty())
            } catch (e: Exception) {
                Timber.tag(loggerTag.value).d("Probably unregistering a non existing pusher")
            }
        }
    }
}
