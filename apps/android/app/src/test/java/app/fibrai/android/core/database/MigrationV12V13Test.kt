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

/** A v12 file (A60) gains a null `chat_message.noted` (A64); every row survives. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class MigrationV12V13Test {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FibraiDatabase::class.java.canonicalName!!,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test fun rowsIntact_notedNull() {
        val name = "migration_v12_v13.db"
        helper.createDatabase(name, 12).apply {
            execSQL("INSERT INTO chat_message (id,date,role,text,createdAtEpochMs,memoryUpdated,planBudget) VALUES (1,'2026-10-08','assistant','Ok',1000,1,'{}')")
            close()
        }
        helper.runMigrationsAndValidate(name, 13, true, MIGRATION_12_13).apply {
            query("SELECT text, memoryUpdated, planBudget, noted FROM chat_message").use {
                it.moveToFirst()
                assertThat(it.getString(0)).isEqualTo("Ok")
                assertThat(it.getInt(1)).isEqualTo(1)
                assertThat(it.getString(2)).isEqualTo("{}")
                assertThat(it.isNull(3)).isTrue()
            }
            close()
        }
    }
}
