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

/** A v15 file (A68) gains `onboarding_answer` and the profile's goal, closure time, notifications and phase (A71); every row survives. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class MigrationV15V16Test {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FibraiDatabase::class.java.canonicalName!!,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test fun profileDefaults_answersTableEmpty_rowsIntact() {
        val name = "migration_v15_v16.db"
        helper.createDatabase(name, 15).apply {
            execSQL("INSERT INTO profile (id,slotMode,ceilingMode,kcalSame,kcalWeekday,kcalWeekend,kcalDays,eat,pct,onboardingDone,firstDay,sex,ageYears,heightCm,weightKg,proteinTargetG,carbTargetG,fatTargetG,tone) VALUES (1,'same','same',2000,2000,2300,'[2000,2000,2000,2000,2000,2000,2000]','zero',50,1,'2026-09-19','male',27,180,116.0,150,200,67,'duro')")
            execSQL("INSERT INTO meal_slot (id,name,minutesFromMidnight,sortOrder,days) VALUES (2,'Jantar',1200,0,127)")
            execSQL("INSERT INTO meal_log (id,date,window,text,kcal,p,stable,slotId,carbs,fat,source) VALUES (1,'2026-10-08','','frango',610,52,1,2,60,14,'user')")
            close()
        }
        helper.runMigrationsAndValidate(name, 16, true, MIGRATION_15_16).apply {
            query("SELECT onboardingDone, tone, goalWeightKg, goalDate, closureTime, notificationsEnabled, onboardingPhase FROM profile").use {
                it.moveToFirst()
                assertThat(it.getInt(0)).isEqualTo(1)
                assertThat(it.getString(1)).isEqualTo("duro")
                assertThat(it.isNull(2)).isTrue()
                assertThat(it.isNull(3)).isTrue()
                assertThat(it.getString(4)).isEqualTo("22:00")
                assertThat(it.getInt(5)).isEqualTo(1)
                assertThat(it.getString(6)).isEqualTo("chat")
            }
            query("SELECT text, kcal FROM meal_log").use {
                it.moveToFirst()
                assertThat(it.getString(0)).isEqualTo("frango")
                assertThat(it.getInt(1)).isEqualTo(610)
            }
            query("SELECT COUNT(*) FROM onboarding_answer").use { it.moveToFirst(); assertThat(it.getInt(0)).isEqualTo(0) }
            close()
        }
    }
}
