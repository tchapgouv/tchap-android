/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.push.fcm

import androidx.fragment.app.Fragment
import im.vector.app.core.pushers.UnifiedPushHelper
import im.vector.app.features.VectorFeatures
import im.vector.app.features.push.NotificationTroubleshootTestManagerFactory
import im.vector.app.features.settings.troubleshoot.NotificationTroubleshootTestManager
import im.vector.app.features.settings.troubleshoot.TestAccountSettings
import im.vector.app.features.settings.troubleshoot.TestAvailableUnifiedPushDistributors
import im.vector.app.features.settings.troubleshoot.TestCurrentUnifiedPushDistributor
import im.vector.app.features.settings.troubleshoot.TestDeviceSettings
import im.vector.app.features.settings.troubleshoot.TestEndpointAsTokenRegistration
import im.vector.app.features.settings.troubleshoot.TestNotification
import im.vector.app.features.settings.troubleshoot.TestPushRulesSettings
import im.vector.app.features.settings.troubleshoot.TestSystemSettings
import im.vector.app.features.settings.troubleshoot.TestUnifiedPushEndpoint
import im.vector.app.features.settings.troubleshoot.TestUnifiedPushGateway
import im.vector.app.gplay.features.settings.troubleshoot.TestFirebaseToken
import im.vector.app.gplay.features.settings.troubleshoot.TestPlayServices
import im.vector.app.gplay.features.settings.troubleshoot.TestTokenRegistration
import javax.inject.Inject

class GoogleNotificationTroubleshootTestManagerFactory @Inject constructor(
        private val unifiedPushHelper: UnifiedPushHelper,
        private val testSystemSettings: TestSystemSettings,
        private val testAccountSettings: TestAccountSettings,
        private val testDeviceSettings: TestDeviceSettings,
        private val testPushRulesSettings: TestPushRulesSettings,
        private val testPlayServices: TestPlayServices,
        private val testFirebaseToken: TestFirebaseToken,
        private val testTokenRegistration: TestTokenRegistration,
        private val testCurrentUnifiedPushDistributor: TestCurrentUnifiedPushDistributor,
        private val testUnifiedPushGateway: TestUnifiedPushGateway,
        private val testUnifiedPushEndpoint: TestUnifiedPushEndpoint,
        private val testAvailableUnifiedPushDistributors: TestAvailableUnifiedPushDistributors,
        private val testEndpointAsTokenRegistration: TestEndpointAsTokenRegistration,
        // private val testPushFromPushGateway: TestPushFromPushGateway, // TCHAP remove
        private val testNotification: TestNotification,
        private val vectorFeatures: VectorFeatures,
) : NotificationTroubleshootTestManagerFactory {

    override fun create(fragment: Fragment): NotificationTroubleshootTestManager {
        val mgr = NotificationTroubleshootTestManager(fragment)
        mgr.addTest(testSystemSettings)
        mgr.addTest(testAccountSettings)
        mgr.addTest(testDeviceSettings)
        mgr.addTest(testPushRulesSettings)
        if (vectorFeatures.allowExternalUnifiedPushDistributors()) {
            // FIXME Tchap:  Hide this verification, we can't call tchap server.
//            mgr.addTest(testAvailableUnifiedPushDistributors)
//            mgr.addTest(testCurrentUnifiedPushDistributor)
        }
        if (unifiedPushHelper.isEmbeddedDistributor()) {
            mgr.addTest(testPlayServices)
            mgr.addTest(testFirebaseToken)
            mgr.addTest(testTokenRegistration)
        } else {
            mgr.addTest(testUnifiedPushGateway)
            mgr.addTest(testUnifiedPushEndpoint)
            mgr.addTest(testEndpointAsTokenRegistration)
        }
       // mgr.addTest(testPushFromPushGateway) // TCHAP remove
        mgr.addTest(testNotification)
        return mgr
    }
}
