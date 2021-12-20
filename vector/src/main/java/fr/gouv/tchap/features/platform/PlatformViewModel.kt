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

package fr.gouv.tchap.features.platform

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.launch

class PlatformViewModel @AssistedInject constructor(
        @Assisted initialState: PlatformViewState,
        private val platformViewModelTask: TchapGetPlatformTask
) : VectorViewModel<PlatformViewState, PlatformAction, PlatformViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<PlatformViewModel, PlatformViewState> {
        override fun create(initialState: PlatformViewState): PlatformViewModel
    }

    companion object : MavericksViewModelFactory<PlatformViewModel, PlatformViewState> by hiltMavericksViewModelFactory()

    private fun handleDiscoverTchapPlatform(action: PlatformAction.DiscoverTchapPlatform) {
        _viewEvents.post(PlatformViewEvents.Loading)
        viewModelScope.launch {
            when (val result = platformViewModelTask.execute(Params(action.email))) {
                is GetPlatformResult.Failure -> _viewEvents.post(PlatformViewEvents.Failure(result.throwable))
                is GetPlatformResult.Success -> _viewEvents.post(PlatformViewEvents.Success(result.platform))
            }
        }
    }

    override fun handle(action: PlatformAction) {
        when (action) {
            is PlatformAction.DiscoverTchapPlatform -> handleDiscoverTchapPlatform(action)
        }.exhaustive
    }
}
