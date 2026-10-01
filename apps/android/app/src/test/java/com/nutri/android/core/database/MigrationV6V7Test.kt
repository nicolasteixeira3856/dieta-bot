package com.nutri.android.core.database

import android.app.Application
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A v6 file (A28) with Chat rows: nothing changes but the newest-first index (A32). */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class MigrationV6V7Test {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DietaBotDatabase::class.java.canonicalName!!,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test fun indexOnlyMigrationKeepsChat() {
        val name = "migration_v6_v7.db"
        helper.createDatabase(name, 6).apply {
            execSQL("INSERT INTO chat_message (id,date,role,text,createdAtEpochMs,memoryUpdated) VALUES (1,'2026-09-29','user','2 ovos',1000,0)")
            execSQL("INSERT INTO chat_message (id,date,role,text,createdAtEpochMs,estimateKcal,intent,pendingMemory,memoryUsedKinds,memoryUpdated) VALUES (2,'2026-09-29','assistant','estimativa',1001,380,'log','[]','permanent',1)")
            close()
        }
        helper.runMigrationsAndValidate(name, 7, true, MIGRATION_6_7).apply {
            query("SELECT id,role,text,estimateKcal,intent,pendingMemory,memoryUsedKinds,memoryUpdated FROM chat_message ORDER BY createdAtEpochMs DESC, id DESC").use {
                assertThat(it.count).isEqualTo(2)
                it.moveToFirst()
                assertThat(it.getLong(0)).isEqualTo(2)
                assertThat(it.getString(1)).isEqualTo("assistant")
                assertThat(it.getInt(3)).isEqualTo(380)
                assertThat(it.getString(4)).isEqualTo("log")
                assertThat(it.getString(5)).isEqualTo("[]")
                assertThat(it.getString(6)).isEqualTo("permanent")
                assertThat(it.getInt(7)).isEqualTo(1)
            }
            query("PRAGMA index_list(`chat_message`)").use {
                val names = buildList { while (it.moveToNext()) add(it.getString(it.getColumnIndexOrThrow("name"))) }
                assertThat(names).contains("index_chat_message_createdAtEpochMs_id")
            }
            close()
        }
    }
}
