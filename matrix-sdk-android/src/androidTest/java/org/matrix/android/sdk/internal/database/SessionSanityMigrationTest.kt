/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.database

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.realm.Realm
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.matrix.android.sdk.internal.database.model.SessionRealmModule
import org.matrix.android.sdk.internal.util.Normalizer

@RunWith(AndroidJUnit4::class)
class SessionSanityMigrationTest {

    @get:Rule val configurationFactory = TestRealmConfigurationFactory()

    lateinit var context: Context
    var realm: Realm? = null

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
    }

    @After
    fun tearDown() {
        realm?.close()
    }

    // Tchap: use custom realm database
    @Test
    fun sessionDatabaseShouldMigrateGracefully() {
        val realmName = "tchap_session_41.realm"
        val migration = RealmSessionStoreMigration(Normalizer())
        val realmConfiguration = configurationFactory.createConfiguration(
                realmName,
                "2948ff8106ad80ca8aeba7ef59775075258d8805f9f6eb306add6c3097154bf5c99bbc965931a29e4512f0b3981d3a562c4c86b860846bac2312e1ab61026762",
                SessionRealmModule(),
                migration.schemaVersion,
                migration
        )
        configurationFactory.copyRealmFromAssets(context, realmName, realmName)

        realm = Realm.getInstance(realmConfiguration)
    }
}
