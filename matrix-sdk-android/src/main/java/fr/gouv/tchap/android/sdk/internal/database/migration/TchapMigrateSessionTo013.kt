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

package fr.gouv.tchap.android.sdk.internal.database.migration

import fr.gouv.tchap.android.sdk.api.session.events.model.TchapEventType
import fr.gouv.tchap.android.sdk.api.session.room.model.RoomAccessRulesContent
import io.realm.DynamicRealm
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntityFields
import org.matrix.android.sdk.internal.database.model.EventEntityFields
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.util.database.RealmMigrator

internal class TchapMigrateSessionTo013(realm: DynamicRealm) : RealmMigrator(realm, 13) {

    override fun doMigrate(realm: DynamicRealm) {
        val accessRulesContentAdapter = MoshiProvider.providesMoshi().adapter(RoomAccessRulesContent::class.java)
        realm.schema.get("RoomSummaryEntity")
                ?.addField(RoomSummaryEntityFields.ACCESS_RULES_STR, String::class.java)
                ?.transform { obj ->
                    val accessRulesEvent = realm.where("CurrentStateEventEntity")
                            .equalTo(CurrentStateEventEntityFields.ROOM_ID, obj.getString(RoomSummaryEntityFields.ROOM_ID))
                            .equalTo(CurrentStateEventEntityFields.TYPE, TchapEventType.STATE_ROOM_ACCESS_RULES)
                            .findFirst()

                    val roomAccessRules = accessRulesEvent?.getObject(CurrentStateEventEntityFields.ROOT.`$`)
                            ?.getString(EventEntityFields.CONTENT)?.let {
                                accessRulesContentAdapter.fromJson(it)?.accessRules
                            }

                    obj.setString(RoomSummaryEntityFields.ACCESS_RULES_STR, roomAccessRules?.name)
                }
    }
}
