package app.fibrai.android.core.database

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

/** A real v3 SQLite file from the shipped schema, including every retained table. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class MigrationV3V4Test {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FibraiDatabase::class.java.canonicalName!!,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test fun additiveMigrationPreservesSlotsHistoryChatAndDigests() {
        val name = "migration_v3_v4.db"
        helper.createDatabase(name, 3).apply {
            execSQL("INSERT INTO profile (id,ceilingMode,kcalSame,kcalWeekday,kcalWeekend,kcalDays,eat,pct,onboardingDone,firstDay) VALUES (1,'same',2000,2000,2300,'[2000,2000,2000,2000,2000,2000,2000]','partial',50,1,'2026-09-25')")
            execSQL("INSERT INTO meal_slot (id,name,minutesFromMidnight,sortOrder) VALUES (8,'Café',450,0),(9,'Jantar',1200,1)")
            execSQL("INSERT INTO day (date,workoutKcal,removedWindows,askedWindows) VALUES ('2026-09-25',350,'[]','[]')")
            execSQL("INSERT INTO meal_log (id,date,window,text,kcal,p,stable,slotId,carbs,fat,source) VALUES (7,'2026-09-25','','2 ovos',380,22,1,8,4,16,'user')")
            execSQL("INSERT INTO slot_skip (date,slotId) VALUES ('2026-09-25',9)")
            execSQL("INSERT INTO chat_message (id,date,role,text,createdAtEpochMs,estimateSlotId,estimateQuestion,estimateItems) VALUES (2,'2026-09-25','assistant','estimativa',1000,8,'porção?','ovo')")
            execSQL("INSERT INTO day_digest (date,seq,text,createdAtEpochMs) VALUES ('2026-09-25',1,'memória do dia',1000)")
            close()
        }
        helper.runMigrationsAndValidate(name, 4, true, MIGRATION_3_4).apply {
            query("SELECT slotMode,kcalSame,eat,pct FROM profile").use {
                assertThat(it.moveToFirst()).isTrue()
                assertThat(it.getString(0)).isEqualTo("same")
                assertThat(it.getInt(1)).isEqualTo(2000)
                assertThat(it.getString(2)).isEqualTo("partial")
                assertThat(it.getInt(3)).isEqualTo(50)
            }
            query("SELECT id,name,minutesFromMidnight,days FROM meal_slot ORDER BY id").use {
                assertThat(it.count).isEqualTo(2)
                it.moveToFirst()
                assertThat(it.getLong(0)).isEqualTo(8)
                assertThat(it.getString(1)).isEqualTo("Café")
                assertThat(it.getInt(2)).isEqualTo(450)
                assertThat(it.getInt(3)).isEqualTo(127)
                it.moveToNext()
                assertThat(it.getLong(0)).isEqualTo(9)
                assertThat(it.getInt(3)).isEqualTo(127)
            }
            query("SELECT slotId,text,kcal FROM meal_log").use {
                it.moveToFirst(); assertThat(it.getLong(0)).isEqualTo(8)
                assertThat(it.getString(1)).isEqualTo("2 ovos"); assertThat(it.getInt(2)).isEqualTo(380)
            }
            query("SELECT slotId FROM slot_skip").use { it.moveToFirst(); assertThat(it.getLong(0)).isEqualTo(9) }
            query("SELECT estimateSlotId,estimateQuestion,estimateItems FROM chat_message").use {
                it.moveToFirst(); assertThat(it.getLong(0)).isEqualTo(8)
                assertThat(it.getString(1)).isEqualTo("porção?"); assertThat(it.getString(2)).isEqualTo("ovo")
            }
            query("SELECT text FROM day_digest").use { it.moveToFirst(); assertThat(it.getString(0)).isEqualTo("memória do dia") }
            query("SELECT workoutKcal FROM day").use { it.moveToFirst(); assertThat(it.getInt(0)).isEqualTo(350) }
            query("PRAGMA foreign_key_check").use { assertThat(it.count).isEqualTo(0) }
            close()
        }
    }
}
