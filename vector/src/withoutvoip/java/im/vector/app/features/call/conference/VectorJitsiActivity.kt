/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.call.conference

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import com.airbnb.mvrx.Mavericks
import im.vector.app.databinding.ActivityJitsiBinding
import kotlinx.parcelize.Parcelize

class VectorJitsiActivity : Activity() {

    @Parcelize
    data class Args(
            val roomId: String,
            val widgetId: String,
            val enableVideo: Boolean
    ) : Parcelable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityJitsiBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    companion object {
        fun newIntent(context: Context, roomId: String, widgetId: String, enableVideo: Boolean): Intent {
            return Intent(context, VectorJitsiActivity::class.java).apply {
                putExtra(Mavericks.KEY_ARG, Args(roomId, widgetId, enableVideo))
            }
        }
    }
}
