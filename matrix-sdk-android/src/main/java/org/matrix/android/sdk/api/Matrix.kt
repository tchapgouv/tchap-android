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

package org.matrix.android.sdk.api

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import com.zhuinden.monarchy.Monarchy
import fr.gouv.tchap.android.sdk.api.services.threepidplatformdiscover.ThreePidPlatformDiscoverService
import org.matrix.android.sdk.BuildConfig
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.HomeServerHistoryService
import org.matrix.android.sdk.api.debug.DebugService
import org.matrix.android.sdk.api.network.ApiInterceptorListener
import org.matrix.android.sdk.api.network.ApiPath
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.securestorage.SecureStorageService
import org.matrix.android.sdk.api.settings.LightweightSettingsStorage
import org.matrix.android.sdk.internal.SessionManager
import org.matrix.android.sdk.internal.di.DaggerMatrixComponent
import org.matrix.android.sdk.internal.network.ApiInterceptor
import org.matrix.android.sdk.internal.network.UserAgentHolder
import org.matrix.android.sdk.internal.util.BackgroundDetectionObserver
import org.matrix.android.sdk.internal.worker.MatrixWorkerFactory
import java.util.concurrent.Executors
import javax.inject.Inject

/**
 * This is the main entry point to the matrix sdk.
 * <br/>
 *
 * The constructor creates a new instance of Matrix, it's recommended to manage this instance as a singleton.
 *
 * @param context the application context
 * @param matrixConfiguration global configuration that will be used for every [org.matrix.android.sdk.api.session.Session]
 */
class Matrix(context: Context, matrixConfiguration: MatrixConfiguration) {

    @Inject internal lateinit var authenticationService: AuthenticationService
    @Inject internal lateinit var threePidPlatformDiscoverService: ThreePidPlatformDiscoverService
    @Inject internal lateinit var rawService: RawService
    @Inject internal lateinit var debugService: DebugService
    @Inject internal lateinit var userAgentHolder: UserAgentHolder
    @Inject internal lateinit var backgroundDetectionObserver: BackgroundDetectionObserver
    @Inject internal lateinit var sessionManager: SessionManager
    @Inject internal lateinit var homeServerHistoryService: HomeServerHistoryService
    @Inject internal lateinit var apiInterceptor: ApiInterceptor
    @Inject internal lateinit var matrixWorkerFactory: MatrixWorkerFactory
    @Inject internal lateinit var lightweightSettingsStorage: LightweightSettingsStorage
    @Inject internal lateinit var secureStorageService: SecureStorageService

    private val uiHandler = Handler(Looper.getMainLooper())

    init {
        val appContext = context.applicationContext
        Monarchy.init(appContext)
        DaggerMatrixComponent.factory().create(appContext, matrixConfiguration).inject(this)
        if (appContext !is Configuration.Provider) {
            val configuration = Configuration.Builder()
                    .setExecutor(Executors.newCachedThreadPool())
                    .setWorkerFactory(matrixWorkerFactory)
                    .build()
            WorkManager.initialize(appContext, configuration)
        }
        uiHandler.post {
            ProcessLifecycleOwner.get().lifecycle.addObserver(backgroundDetectionObserver)
        }
    }

    /**
     * Return the User Agent used for any request that the SDK is making to the homeserver.
     * There is no way to change the user agent at the moment.
     */
    fun getUserAgent() = userAgentHolder.userAgent

    /**
     * Return the AuthenticationService.
     */
    fun authenticationService() = authenticationService

    fun threePidPlatformDiscoverService() = threePidPlatformDiscoverService

    /**
     * Return the RawService.
     */
    fun rawService() = rawService

    /**
     * Return the DebugService.
     */
    fun debugService() = debugService

    /**
     * Return the LightweightSettingsStorage.
     */
    fun lightweightSettingsStorage() = lightweightSettingsStorage

    /**
     * Return the HomeServerHistoryService.
     */
    fun homeServerHistoryService() = homeServerHistoryService

    /**
     * Returns the SecureStorageService used to encrypt and decrypt sensitive data.
     */
    fun secureStorageService(): SecureStorageService = secureStorageService

    /**
     * Get the worker factory. The returned value has to be provided to `WorkConfiguration.Builder()`.
     */
    fun getWorkerFactory(): WorkerFactory = matrixWorkerFactory

    /**
     * Register an API interceptor, to be able to be notified when the specified API got a response.
     */
    fun registerApiInterceptorListener(path: ApiPath, listener: ApiInterceptorListener) {
        apiInterceptor.addListener(path, listener)
    }

    /**
     * Un-register an API interceptor.
     */
    fun unregisterApiInterceptorListener(path: ApiPath, listener: ApiInterceptorListener) {
        apiInterceptor.removeListener(path, listener)
    }

    companion object {
        /**
         * @return a String with details about the Matrix SDK version.
         */
        fun getSdkVersion(): String {
            return BuildConfig.SDK_VERSION + " (" + BuildConfig.GIT_SDK_REVISION + ")"
        }

        fun getCryptoVersion(longFormat: Boolean): String {
            val version = org.matrix.rustcomponents.sdk.crypto.version()
            val gitHash = org.matrix.rustcomponents.sdk.crypto.versionInfo().gitSha
            val vodozemac = org.matrix.rustcomponents.sdk.crypto.vodozemacVersion()
            return if (longFormat) "Rust SDK $version ($gitHash), Vodozemac $vodozemac" else version
        }
    }
}
