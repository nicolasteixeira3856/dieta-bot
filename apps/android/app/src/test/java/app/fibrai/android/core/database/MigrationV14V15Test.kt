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

/** A v14 file (A66) gains the recipe tables and a null `meal_log.recipeVersionId` (A68); every row survives. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class MigrationV14V15Test {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FibraiDatabase::class.java.canonicalName!!,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test fun rowsIntact_recipeTablesEmpty() {
        val name = "migration_v14_v15.db"
        helper.createDatabase(name, 14).apply {
            execSQL("INSERT INTO meal_slot (id,name,minutesFromMidnight,sortOrder,days) VALUES (2,'Jantar',1200,0,127)")
            execSQL("INSERT INTO meal_log (id,date,window,text,kcal,p,stable,slotId,carbs,fat,source) VALUES (1,'2026-10-08','','frango',610,52,1,2,60,14,'user')")
            close()
        }
        helper.runMigrationsAndValidate(name, 15, true, MIGRATION_14_15).apply {
            query("SELECT text, kcal, recipeVersionId FROM meal_log").use {
                it.moveToFirst()
                assertThat(it.getString(0)).isEqualTo("frango")
                assertThat(it.getInt(1)).isEqualTo(610)
                assertThat(it.isNull(2)).isTrue()
            }
            query("SELECT COUNT(*) FROM recipe").use { it.moveToFirst(); assertThat(it.getInt(0)).isEqualTo(0) }
            query("SELECT COUNT(*) FROM recipe_version").use { it.moveToFirst(); assertThat(it.getInt(0)).isEqualTo(0) }
            close()
        }
    }
}
