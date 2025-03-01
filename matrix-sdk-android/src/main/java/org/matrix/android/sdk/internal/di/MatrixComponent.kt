/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.di

import android.content.Context
import android.content.res.Resources
import com.squareup.moshi.Moshi
import dagger.BindsInstance
import dagger.Component
import fr.gouv.tchap.android.sdk.api.services.threepidplatformdiscover.ThreePidPlatformDiscoverService
import okhttp3.OkHttpClient
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.HomeServerHistoryService
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.securestorage.SecureStorageModule
import org.matrix.android.sdk.api.securestorage.SecureStorageService
import org.matrix.android.sdk.api.settings.LightweightSettingsStorage
import org.matrix.android.sdk.internal.SessionManager
import org.matrix.android.sdk.internal.auth.AuthModule
import org.matrix.android.sdk.internal.auth.SessionParamsStore
import org.matrix.android.sdk.internal.debug.DebugModule
import org.matrix.android.sdk.internal.raw.RawModule
import org.matrix.android.sdk.internal.session.MockHttpInterceptor
import org.matrix.android.sdk.internal.session.TestInterceptor
import org.matrix.android.sdk.internal.settings.SettingsModule
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.util.BackgroundDetectionObserver
import org.matrix.android.sdk.internal.util.system.SystemModule
import org.matrix.android.sdk.internal.worker.MatrixWorkerFactory
import java.io.File

@Component(
        modules = [
            MatrixModule::class,
            NetworkModule::class,
            AuthModule::class,
            RawModule::class,
            DebugModule::class,
            SettingsModule::class,
            SystemModule::class,
            NoOpTestModule::class,
            SecureStorageModule::class,
        ]
)
@MatrixScope
internal interface MatrixComponent {

    fun matrixCoroutineDispatchers(): MatrixCoroutineDispatchers

    fun moshi(): Moshi

    @Unauthenticated
    fun okHttpClient(): OkHttpClient

    @MockHttpInterceptor
    fun testInterceptor(): TestInterceptor?

    fun authenticationService(): AuthenticationService

    fun threePidPlatformDiscoverService(): ThreePidPlatformDiscoverService

    fun rawService(): RawService

    fun lightweightSettingsStorage(): LightweightSettingsStorage

    fun homeServerHistoryService(): HomeServerHistoryService

    fun context(): Context

    fun matrixConfiguration(): MatrixConfiguration

    fun resources(): Resources

    @CacheDirectory
    fun cacheDir(): File

    fun taskExecutor(): TaskExecutor

    fun sessionParamsStore(): SessionParamsStore

    fun backgroundDetectionObserver(): BackgroundDetectionObserver

    fun sessionManager(): SessionManager

    fun secureStorageService(): SecureStorageService

    fun matrixWorkerFactory(): MatrixWorkerFactory

    fun inject(matrix: Matrix)

    @Component.Factory
    interface Factory {
        fun create(
                @BindsInstance context: Context,
                @BindsInstance matrixConfiguration: MatrixConfiguration
        ): MatrixComponent
    }
}
