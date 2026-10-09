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
 * A v10 file (A47) with messages, a meal log, a proposal and an active receipt: v11 adds a null `skipOutcomes`, every
 * row survives and the receipt's Desfazer still restores the slot (A59).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class MigrationV10V11Test {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FibraiDatabase::class.java.canonicalName!!,
        FrameworkSQLiteOpenHelperFactory(),
    )

    private val undo = """{"slots":[{"date":"2026-10-07","slotId":2,""" +
        """"before":{},"after":{"records":[{"text":"2 ovos mexidos e 1 pão francês","kcal":320,"p":17,"c":29,"g":16}]}}]}"""
    private val proposal = """{"operation":"new","date":"2026-10-07"}"""

    @Test fun skipOutcomesArriveNull_rowsIntact_undoStillWorks() {
        val name = "migration_v10_v11.db"
        helper.createDatabase(name, 10).apply {
            execSQL("INSERT INTO meal_slot (id,name,minutesFromMidnight,sortOrder,days) VALUES (2,'Café da manhã',450,0,127)")
            execSQL(
                "INSERT INTO meal_log (id,date,window,text,kcal,p,stable,slotId,carbs,fat,source) " +
                    "VALUES (1,'2026-10-07','','2 ovos mexidos e 1 pão francês',320,17,1,2,29,16,'user')",
            )
            execSQL("INSERT INTO chat_message (id,date,role,text,createdAtEpochMs,memoryUpdated) VALUES (1,'2026-10-07','user','café',1000,0)")
            execSQL(
                "INSERT INTO chat_message (id,date,role,text,createdAtEpochMs,estimateKcal,estimateSlotId,intent,memoryUpdated,recordMode,recordState,mealChange) " +
                    "VALUES (2,'2026-10-07','assistant','Identifiquei 2 ovos',1001,320,2,'log',0,'auto','recorded','$proposal')",
            )
            execSQL(
                "INSERT INTO chat_message (id,date,role,text,createdAtEpochMs,estimateKcal,estimateSlotId,memoryUpdated,undoData,recordSource) " +
                    "VALUES (3,'2026-10-07','logged','Café da manhã',1002,320,2,0,'$undo','user')",
            )
            close()
        }
        helper.runMigrationsAndValidate(name, 11, true, MIGRATION_10_11).apply {
            query("SELECT id,role,recordState,mealChange,undoData,skipOutcomes FROM chat_message ORDER BY id").use {
                assertThat(it.count).isEqualTo(3)
                it.moveToPosition(1)
                assertThat(it.getString(2)).isEqualTo("recorded")
                assertThat(it.getString(3)).isEqualTo(proposal)
                it.moveToPosition(2)
                assertThat(it.getString(4)).isEqualTo(undo)
                it.moveToFirst()
                do assertThat(it.isNull(5)).isTrue() while (it.moveToNext())
            }
            close()
        }

        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.databaseBuilder(context, FibraiDatabase::class.java, name)
            .addMigrations(*ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(context.cacheDir, "v11_${System.nanoTime()}.preferences_pb") })
        val now = Instant.parse("2026-10-07T08:00:00-03:00")
        val repo = DayRepository(db, { now }, store)
        try {
            runBlocking {
                assertThat(repo.openSkips()).isEmpty()
                assertThat(repo.slotState("2026-10-07", 2)).isEqualTo(SlotState.of(SlotRecord("2 ovos mexidos e 1 pão francês", 320, 17, 29, 16)))
                val recorder = ChatRecorder(repo, FactMemory(FakeMemoryFile()), NoopTelemetry, { now }) {}
                val receipt = repo.message(3)!!
                assertThat(receipt.skipOutcomes).isNull()
                assertThat(recorder.undo(receipt) { "Café da manhã" }).isTrue()
                assertThat(repo.slotState("2026-10-07", 2)).isEqualTo(SlotState.EMPTY)
                assertThat(repo.message(3)!!.receiptState).isEqualTo("undone")
            }
        } finally {
            db.close()
            scope.cancel()
        }
    }
}
