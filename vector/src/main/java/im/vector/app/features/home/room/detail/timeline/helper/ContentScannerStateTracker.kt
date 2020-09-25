/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.features.home.room.detail.timeline.helper

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.home.room.detail.timeline.item.ScannableHolder
import org.matrix.android.sdk.api.MatrixUrls.isMxcUrl
import org.matrix.android.sdk.api.session.contentscanner.ScanState
import org.matrix.android.sdk.api.session.contentscanner.ScanStatusInfo
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.crypto.attachments.ElementToDecrypt
import timber.log.Timber
import javax.inject.Inject

class ContentScannerStateTracker @Inject constructor(private val activeSessionHolder: ActiveSessionHolder) {

    private val lifecycleOwner: LifecycleOwner = LifecycleOwner { lifecycleRegistry }
    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(lifecycleOwner)

    private val trackedStatus = mutableMapOf<String, LiveData<Optional<ScanStatusInfo>>>()

    fun bind(eventId: String, mxcURL: String?, encryptedFileInfo: ElementToDecrypt?, holder: ScannableHolder) {
        activeSessionHolder.getSafeActiveSession()?.let { session ->
            if (!session.contentScannerService().isScannerEnabled()) return
            if (!lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                lifecycleRegistry.currentState = Lifecycle.State.STARTED
            }

            val ld = if (mxcURL?.isMxcUrl() == true) {
                session.contentScannerService().getLiveStatusForFile(mxcURL, true, encryptedFileInfo)
            } else {
                return@let
            }

            updateStateOnBind(holder, session.contentScannerService().getCachedScanResultForFile(mxcURL))

            Transformations.distinctUntilChanged(ld)
                    .observe(lifecycleOwner, Observer {
                        val scanStatusInfo = it.getOrNull()
                        Timber.v("SCAN STATUS ${scanStatusInfo?.state} for url $mxcURL")
                        when (scanStatusInfo?.state) {
                            ScanState.INFECTED    -> {
                                holder.mediaScanResult(false)
                            }
                            ScanState.TRUSTED     -> {
                                holder.mediaScanResult(true)
                            }
                            ScanState.UNKNOWN,
                            ScanState.IN_PROGRESS -> {
                                holder.mediaScanInProgress()
                            }
                        }
                    })
            trackedStatus[eventId] = ld
        }
    }

    private fun updateStateOnBind(holder: ScannableHolder, scanStatus: ScanStatusInfo?) {
        when (scanStatus?.state) {
            ScanState.INFECTED    -> {
                holder.mediaScanResult(false)
            }
            ScanState.TRUSTED     -> {
                holder.mediaScanResult(true)
            }
            ScanState.UNKNOWN,
            ScanState.IN_PROGRESS -> {
                holder.mediaScanInProgress()
            }
        }
    }

    fun unBind(eventId: String) {
        trackedStatus.remove(eventId)?.removeObservers(lifecycleOwner)
    }

    fun clear() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
}
