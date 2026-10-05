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

/** A v7 file (A32) with an estimate and its receipt: the A34 columns arrive null, so old rows get no actions. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class MigrationV7V8Test {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FibraiDatabase::class.java.canonicalName!!,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test fun recordColumnsArriveNull_rowsIntact() {
        val name = "migration_v7_v8.db"
        helper.createDatabase(name, 7).apply {
            execSQL("INSERT INTO chat_message (id,date,role,text,createdAtEpochMs,memoryUpdated) VALUES (1,'2026-09-30','user','2 ovos',1000,0)")
            execSQL("INSERT INTO chat_message (id,date,role,text,createdAtEpochMs,estimateKcal,estimateSlotId,intent,memoryUpdated) VALUES (2,'2026-09-30','assistant','estimativa',1001,380,1,'log',0)")
            execSQL("INSERT INTO chat_message (id,date,role,text,createdAtEpochMs,estimateKcal,estimateSlotId,memoryUpdated) VALUES (3,'2026-09-30','logged','Café da manhã',1002,380,1,1)")
            close()
        }
        helper.runMigrationsAndValidate(name, 8, true, MIGRATION_7_8).apply {
            query("SELECT id,role,text,estimateKcal,estimateSlotId,memoryUpdated,recordMode,recordState,receiptState,undoData,recordSource FROM chat_message ORDER BY id").use {
                assertThat(it.count).isEqualTo(3)
                it.moveToPosition(1)
                assertThat(it.getString(1)).isEqualTo("assistant")
                assertThat(it.getInt(3)).isEqualTo(380)
                it.moveToPosition(2)
                assertThat(it.getString(1)).isEqualTo("logged")
                assertThat(it.getString(2)).isEqualTo("Café da manhã")
                assertThat(it.getLong(4)).isEqualTo(1)
                assertThat(it.getInt(5)).isEqualTo(1)
                it.moveToFirst()
                do {
                    for (column in 6..10) assertThat(it.isNull(column)).isTrue()
                } while (it.moveToNext())
            }
            close()
        }
    }
}
