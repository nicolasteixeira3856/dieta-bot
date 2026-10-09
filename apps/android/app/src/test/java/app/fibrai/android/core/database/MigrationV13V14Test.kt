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

/** A v13 file (A64) gains a null `chat_message.actions` (A66); every row survives. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class MigrationV13V14Test {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FibraiDatabase::class.java.canonicalName!!,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test fun rowsIntact_actionsNull() {
        val name = "migration_v13_v14.db"
        helper.createDatabase(name, 13).apply {
            execSQL("INSERT INTO chat_message (id,date,role,text,createdAtEpochMs,memoryUpdated,planBudget,noted) VALUES (1,'2026-10-08','assistant','Ok',1000,1,'{}','Usa leite')")
            close()
        }
        helper.runMigrationsAndValidate(name, 14, true, MIGRATION_13_14).apply {
            query("SELECT text, memoryUpdated, noted, actions FROM chat_message").use {
                it.moveToFirst()
                assertThat(it.getString(0)).isEqualTo("Ok")
                assertThat(it.getInt(1)).isEqualTo(1)
                assertThat(it.getString(2)).isEqualTo("Usa leite")
                assertThat(it.isNull(3)).isTrue()
            }
            close()
        }
    }
}
