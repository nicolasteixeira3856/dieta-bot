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

/** A v5 file (A27) with Chat rows and logs: nothing is lost, the memory columns start empty (A28). */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class MigrationV5V6Test {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DietaBotDatabase::class.java.canonicalName!!,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test fun additiveMigrationKeepsChat_memoryColumnsNullAndZero() {
        val name = "migration_v5_v6.db"
        helper.createDatabase(name, 5).apply {
            execSQL("INSERT INTO profile (id,ceilingMode,kcalSame,kcalWeekday,kcalWeekend,kcalDays,eat,pct,onboardingDone,firstDay,slotMode) VALUES (1,'same',2000,2000,2300,'[2000,2000,2000,2000,2000,2000,2000]','partial',50,1,'2026-09-25','same')")
            execSQL("INSERT INTO meal_slot (id,name,minutesFromMidnight,sortOrder,days) VALUES (8,'Café',450,0,127)")
            execSQL("INSERT INTO meal_log (id,date,window,text,kcal,p,stable,slotId,carbs,fat,source) VALUES (7,'2026-09-29','','2 ovos',380,22,1,8,4,16,'user')")
            execSQL("INSERT INTO chat_message (id,date,role,text,createdAtEpochMs) VALUES (1,'2026-09-29','user','2 ovos',1000)")
            execSQL("INSERT INTO chat_message (id,date,role,text,createdAtEpochMs,estimateKcal,estimateP,estimateC,estimateG,estimateConfidence,estimateSlotId,estimateMealText,intent) VALUES (2,'2026-09-29','assistant','estimativa',1001,380,22,4,16,'high',8,'2 ovos mexidos','log')")
            execSQL("INSERT INTO chat_message (id,date,role,text,createdAtEpochMs,estimateKcal,estimateSlotId) VALUES (3,'2026-09-29','logged','Café',1002,380,8)")
            close()
        }
        helper.runMigrationsAndValidate(name, 6, true, MIGRATION_5_6).apply {
            query("SELECT id,role,estimateMealText,intent,pendingMemory,memoryUsedKinds,memoryUpdated FROM chat_message ORDER BY id").use {
                assertThat(it.count).isEqualTo(3)
                it.moveToPosition(1)
                assertThat(it.getString(1)).isEqualTo("assistant")
                assertThat(it.getString(2)).isEqualTo("2 ovos mexidos")
                assertThat(it.getString(3)).isEqualTo("log")
                assertThat(it.isNull(4)).isTrue()
                assertThat(it.isNull(5)).isTrue()
                assertThat(it.getInt(6)).isEqualTo(0)
            }
            query("SELECT slotId,text,kcal FROM meal_log").use {
                it.moveToFirst()
                assertThat(it.getLong(0)).isEqualTo(8)
                assertThat(it.getString(1)).isEqualTo("2 ovos")
                assertThat(it.getInt(2)).isEqualTo(380)
            }
            query("PRAGMA foreign_key_check").use { assertThat(it.count).isEqualTo(0) }
            close()
        }
    }
}
