/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.features.settings

import android.content.Context
import android.os.Bundle
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.singletonEntryPoint
import im.vector.app.core.preference.VectorPreference
import im.vector.app.core.utils.FirstThrottler
import im.vector.app.core.utils.openUrlInChromeCustomTab
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.navigation.Navigator
import java.util.Calendar

@AndroidEntryPoint
class VectorSettingsRootFragment :
        VectorSettingsBaseFragment() {

    override var titleRes: Int = R.string.title_activity_settings
    override val preferenceXmlRes = R.xml.vector_settings_root

    private val firstThrottler = FirstThrottler(1000)

    private lateinit var navigator: Navigator

    override fun onAttach(context: Context) {
        val singletonEntryPoint = context.singletonEntryPoint()
        navigator = singletonEntryPoint.navigator()
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsScreenName = MobileScreen.ScreenName.Settings
    }

    override fun bindPref() {
        tintIcons()

        // Tchap: Manage new FAQ entry
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_HELP_PREFERENCE_KEY)!!
                .onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (firstThrottler.canHandle() is FirstThrottler.CanHandlerResult.Yes) {
                openUrlInChromeCustomTab(requireContext(), null, VectorSettingsUrls.HELP)
            }
            false
        }

        // Tchap: Manage Christmas entry
        if (Calendar.getInstance().before(Calendar.getInstance().apply { set(2024, 1, 10) })) {
            findPreference<VectorPreference>(VectorPreferences.TCHAP_SETTINGS_CHRISTMAS_PREFERENCE_KEY)!!.let {
                it.isVisible = true
                it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    if (firstThrottler.canHandle() is FirstThrottler.CanHandlerResult.Yes) {
                        navigator.openRoom(requireContext(), "!cDKdQyXHeWBEaKDWWV:agent.dinum.tchap.gouv.fr", null)
                    }
                    false
                }
            }
        }
    }

    private fun tintIcons() {
        for (i in 0 until preferenceScreen.preferenceCount) {
            (preferenceScreen.getPreference(i) as? VectorPreference)?.let { it.tintIcon = true }
        }
    }
}
