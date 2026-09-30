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

/** A real v4 SQLite file (APK 0.0.3) with messages and logs: nothing is lost, new columns are null. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class MigrationV4V5Test {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DietaBotDatabase::class.java.canonicalName!!,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test fun additiveMigrationKeepsChatAndLogs_newColumnsNull() {
        val name = "migration_v4_v5.db"
        helper.createDatabase(name, 4).apply {
            execSQL("INSERT INTO profile (id,ceilingMode,kcalSame,kcalWeekday,kcalWeekend,kcalDays,eat,pct,onboardingDone,firstDay,slotMode) VALUES (1,'same',2000,2000,2300,'[2000,2000,2000,2000,2000,2000,2000]','partial',50,1,'2026-09-25','same')")
            execSQL("INSERT INTO meal_slot (id,name,minutesFromMidnight,sortOrder,days) VALUES (8,'Café',450,0,127)")
            execSQL("INSERT INTO meal_log (id,date,window,text,kcal,p,stable,slotId,carbs,fat,source) VALUES (7,'2026-09-29','','2 ovos',380,22,1,8,4,16,'user')")
            execSQL("INSERT INTO chat_message (id,date,role,text,createdAtEpochMs) VALUES (1,'2026-09-29','user','2 ovos',1000)")
            execSQL("INSERT INTO chat_message (id,date,role,text,createdAtEpochMs,estimateKcal,estimateP,estimateC,estimateG,estimateConfidence,estimateSlotId,estimateQuestion,estimateItems) VALUES (2,'2026-09-29','assistant','estimativa',1001,380,22,4,16,'medium',8,'porção?','ovo')")
            execSQL("INSERT INTO chat_message (id,date,role,text,createdAtEpochMs,estimateKcal,estimateSlotId) VALUES (3,'2026-09-29','logged','Café',1002,380,8)")
            execSQL("INSERT INTO day_digest (date,seq,text,createdAtEpochMs) VALUES ('2026-09-29',1,'memória do dia',1000)")
            close()
        }
        helper.runMigrationsAndValidate(name, 5, true, MIGRATION_4_5).apply {
            query("SELECT id,role,text,estimateKcal,estimateQuestion,estimateItems,estimateMealText,intent FROM chat_message ORDER BY id").use {
                assertThat(it.count).isEqualTo(3)
                it.moveToPosition(1)
                assertThat(it.getString(1)).isEqualTo("assistant")
                assertThat(it.getString(2)).isEqualTo("estimativa")
                assertThat(it.getInt(3)).isEqualTo(380)
                assertThat(it.getString(4)).isEqualTo("porção?")
                assertThat(it.getString(5)).isEqualTo("ovo")
                assertThat(it.isNull(6)).isTrue()
                assertThat(it.isNull(7)).isTrue()
            }
            query("SELECT slotId,text,kcal,carbs,fat FROM meal_log").use {
                it.moveToFirst()
                assertThat(it.getLong(0)).isEqualTo(8)
                assertThat(it.getString(1)).isEqualTo("2 ovos")
                assertThat(it.getInt(2)).isEqualTo(380)
            }
            query("SELECT text FROM day_digest").use { it.moveToFirst(); assertThat(it.getString(0)).isEqualTo("memória do dia") }
            query("SELECT eat,pct FROM profile").use { it.moveToFirst(); assertThat(it.getString(0)).isEqualTo("partial") }
            query("PRAGMA foreign_key_check").use { assertThat(it.count).isEqualTo(0) }
            close()
        }
    }
}
