package app.fibrai.android.core.database

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import app.fibrai.android.core.memory.FactMemory
import app.fibrai.android.core.memory.FakeMemoryFile
import app.fibrai.android.core.telemetry.NoopTelemetry
import app.fibrai.android.domain.SlotRecord
import app.fibrai.android.domain.SlotState
import app.fibrai.android.feature.chat.ChatRecorder
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A v9 file (A38) with messages, meal logs, an open replace and an active receipt: v10 adds a null `mealChange`, every
 * row survives and the receipt's Desfazer still restores the slot (A47).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class MigrationV9V10Test {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FibraiDatabase::class.java.canonicalName!!,
        FrameworkSQLiteOpenHelperFactory(),
    )

    private val undo = """{"slots":[{"date":"2026-10-04","slotId":4,""" +
        """"before":{"records":[{"text":"sopa","kcal":300,"p":10,"c":30,"g":8}]},""" +
        """"after":{"records":[{"text":"sopa e pudim","kcal":540,"p":16,"c":68,"g":15}]}}]}"""

    @Test fun mealChangeArrivesNull_rowsIntact_undoStillWorks() {
        val name = "migration_v9_v10.db"
        helper.createDatabase(name, 9).apply {
            execSQL("INSERT INTO meal_slot (id,name,minutesFromMidnight,sortOrder,days) VALUES (4,'Jantar',1200,0,127)")
            execSQL("INSERT INTO meal_log (id,date,window,text,kcal,p,stable,slotId,carbs,fat,source) VALUES (1,'2026-10-04','','sopa e pudim',540,16,1,4,68,15,'user')")
            execSQL("INSERT INTO chat_message (id,date,role,text,createdAtEpochMs,memoryUpdated) VALUES (1,'2026-10-04','user','sopa e pudim',1000,0)")
            execSQL(
                "INSERT INTO chat_message (id,date,role,text,createdAtEpochMs,estimateKcal,estimateSlotId,intent,memoryUpdated,recordMode,recordState,undoData) " +
                    "VALUES (2,'2026-10-04','assistant','Juntei o pudim',1001,540,4,'log',0,'auto','pending_replace','{}')",
            )
            execSQL(
                "INSERT INTO chat_message (id,date,role,text,createdAtEpochMs,estimateKcal,estimateSlotId,memoryUpdated,undoData,recordSource) " +
                    "VALUES (3,'2026-10-04','replaced','Jantar',1002,540,4,0,'$undo','user')",
            )
            close()
        }
        helper.runMigrationsAndValidate(name, 10, true, MIGRATION_9_10).apply {
            query("SELECT id,role,recordMode,recordState,undoData,recordSource,mealChange FROM chat_message ORDER BY id").use {
                assertThat(it.count).isEqualTo(3)
                it.moveToPosition(1)
                assertThat(it.getString(2)).isEqualTo("auto")
                assertThat(it.getString(3)).isEqualTo("pending_replace")
                it.moveToPosition(2)
                assertThat(it.getString(4)).isEqualTo(undo)
                assertThat(it.getString(5)).isEqualTo("user")
                it.moveToFirst()
                do assertThat(it.isNull(6)).isTrue() while (it.moveToNext())
            }
            query("SELECT text,kcal FROM meal_log").use {
                it.moveToFirst()
                assertThat(it.getString(0)).isEqualTo("sopa e pudim")
                assertThat(it.getInt(1)).isEqualTo(540)
            }
            close()
        }

        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.databaseBuilder(context, FibraiDatabase::class.java, name)
            .addMigrations(MIGRATION_9_10, MIGRATION_10_11)
            .allowMainThreadQueries()
            .build()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(context.cacheDir, "v10_${System.nanoTime()}.preferences_pb") })
        val repo = DayRepository(db, { Instant.parse("2026-10-04T21:00:00-03:00") }, store)
        try {
            runBlocking {
                val recorder = ChatRecorder(repo, FactMemory(FakeMemoryFile()), NoopTelemetry, { Instant.parse("2026-10-04T21:00:00-03:00") }) {}
                val receipt = repo.message(3)!!
                assertThat(receipt.mealChange).isNull()
                assertThat(recorder.undo(receipt) { "Jantar" }).isTrue()
                assertThat(repo.slotState("2026-10-04", 4)).isEqualTo(SlotState.of(SlotRecord("sopa", 300, 10, 30, 8)))
                assertThat(repo.message(3)!!.receiptState).isEqualTo("undone")
            }
        } finally {
            db.close()
            scope.cancel()
        }
    }
}
