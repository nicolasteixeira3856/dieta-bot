package app.fibrai.android.core.database

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import app.fibrai.android.core.memory.FactMemory
import app.fibrai.android.core.memory.FakeMemoryFile
import app.fibrai.android.core.telemetry.NoopTelemetry
import app.fibrai.android.domain.PlannedSlot
import app.fibrai.android.domain.SlotRecord
import app.fibrai.android.domain.SlotState
import app.fibrai.android.feature.chat.ChatRecorder
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A v11 file (A59) with a profile, chat rows, a receipt, an A47 proposal and A59 skips: v12 (A60) adds a null `planBudget`,
 * the profile tone `seco` and two empty tables; every row survives, the receipt still undoes, and a reservation then
 * written is part of the slot state that a record replaces and Desfazer restores.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class MigrationV11V12Test {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FibraiDatabase::class.java.canonicalName!!,
        FrameworkSQLiteOpenHelperFactory(),
    )

    private val undo = """{"slots":[{"date":"2026-10-07","slotId":2,""" +
        """"before":{},"after":{"records":[{"text":"2 ovos mexidos e 1 pão francês","kcal":320,"p":17,"c":29,"g":16}]}}]}"""
    private val proposal = """{"operation":"new","date":"2026-10-07"}"""
    private val skips = """{"date":"2026-10-07","with":"log","slots":[{"slotId":3,"outcome":"skipped"}]}"""

    @Test fun rowsIntact_toneSeco_budgetNull_tablesEmpty_reservationReplacedAndRestored() {
        val name = "migration_v11_v12.db"
        helper.createDatabase(name, 11).apply {
            execSQL(
                "INSERT INTO profile (id,slotMode,ceilingMode,kcalSame,kcalWeekday,kcalWeekend,kcalDays,eat,pct,onboardingDone,firstDay," +
                    "sex,ageYears,heightCm,weightKg,proteinTargetG,carbTargetG,fatTargetG) " +
                    "VALUES (1,'same','same',2000,2000,2300,'[2000,2000,2000,2000,2000,2000,2000]','zero',50,1,'2026-10-01','',0,0,0,150,200,67)",
            )
            execSQL("INSERT INTO meal_slot (id,name,minutesFromMidnight,sortOrder,days) VALUES (2,'Café da manhã',450,0,127)")
            execSQL("INSERT INTO meal_slot (id,name,minutesFromMidnight,sortOrder,days) VALUES (4,'Jantar',1200,1,127)")
            execSQL(
                "INSERT INTO meal_log (id,date,window,text,kcal,p,stable,slotId,carbs,fat,source) " +
                    "VALUES (1,'2026-10-07','','2 ovos mexidos e 1 pão francês',320,17,1,2,29,16,'user')",
            )
            execSQL("INSERT INTO chat_message (id,date,role,text,createdAtEpochMs,memoryUpdated) VALUES (1,'2026-10-07','user','café',1000,0)")
            execSQL(
                "INSERT INTO chat_message (id,date,role,text,createdAtEpochMs,estimateKcal,estimateSlotId,intent,memoryUpdated,recordMode,recordState,mealChange,skipOutcomes) " +
                    "VALUES (2,'2026-10-07','assistant','Identifiquei 2 ovos',1001,320,2,'log',0,'auto','recorded','$proposal','$skips')",
            )
            execSQL(
                "INSERT INTO chat_message (id,date,role,text,createdAtEpochMs,estimateKcal,estimateSlotId,memoryUpdated,undoData,recordSource) " +
                    "VALUES (3,'2026-10-07','logged','Café da manhã',1002,320,2,0,'$undo','user')",
            )
            close()
        }
        helper.runMigrationsAndValidate(name, 12, true, MIGRATION_11_12).apply {
            query("SELECT id,recordState,mealChange,undoData,skipOutcomes,planBudget FROM chat_message ORDER BY id").use {
                assertThat(it.count).isEqualTo(3)
                it.moveToPosition(1)
                assertThat(it.getString(1)).isEqualTo("recorded")
                assertThat(it.getString(2)).isEqualTo(proposal)
                assertThat(it.getString(4)).isEqualTo(skips)
                it.moveToPosition(2)
                assertThat(it.getString(3)).isEqualTo(undo)
                it.moveToFirst()
                do assertThat(it.isNull(5)).isTrue() while (it.moveToNext())
            }
            query("SELECT tone, kcalSame FROM profile").use {
                it.moveToFirst()
                assertThat(it.getString(0)).isEqualTo("seco")
                assertThat(it.getInt(1)).isEqualTo(2000)
            }
            query("SELECT COUNT(*) FROM closure").use { it.moveToFirst(); assertThat(it.getInt(0)).isEqualTo(0) }
            query("SELECT COUNT(*) FROM planned_meal").use { it.moveToFirst(); assertThat(it.getInt(0)).isEqualTo(0) }
            close()
        }

        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.databaseBuilder(context, FibraiDatabase::class.java, name)
            .addMigrations(*ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(context.cacheDir, "v12_${System.nanoTime()}.preferences_pb") })
        val now = Instant.parse("2026-10-07T18:00:00-03:00")
        val repo = DayRepository(db, { now }, store)
        try {
            runBlocking {
                assertThat(repo.observeToday().first().tone).isEqualTo("seco")
                val recorder = ChatRecorder(repo, FactMemory(FakeMemoryFile()), NoopTelemetry, { now }) {}
                // The A59 receipt still undoes.
                assertThat(recorder.undo(repo.message(3)!!) { "Café da manhã" }).isTrue()
                assertThat(repo.slotState("2026-10-07", 2)).isEqualTo(SlotState.EMPTY)
                // A reservation is part of the slot; a record replaces it and Desfazer brings it back.
                val plan = PlannedSlot("omelete de 2 ovos", 450, 30, 6, 32, sourceMessageId = 2)
                assertThat(repo.reserve("2026-10-07", null, 4, plan)).isEqualTo(ReserveResult.Reserved(replaced = null))
                assertThat(repo.observeToday().first().planned[4]).isEqualTo(plan)
                val slot = app.fibrai.android.feature.chat.SlotRef(4, "Jantar", "20:00", 1200)
                val recorded = recorder.record(
                    app.fibrai.android.feature.chat.NewRecord(SlotRecord("frango e arroz", 610, 50, 60, 12), "user", emptyList(), null),
                    slot,
                )
                assertThat(recorded).isInstanceOf(app.fibrai.android.feature.chat.RecordOutcome.Recorded::class.java)
                assertThat(repo.slotState("2026-10-07", 4).planned).isNull()
                val receipt = repo.receipts().last()
                assertThat(recorder.undo(receipt) { "Jantar" }).isTrue()
                assertThat(repo.slotState("2026-10-07", 4)).isEqualTo(SlotState(planned = plan))
                // Wipe clears the reservations of the day.
                repo.wipeToday()
                assertThat(repo.slotState("2026-10-07", 4)).isEqualTo(SlotState.EMPTY)
            }
        } finally {
            db.close()
            scope.cancel()
        }
    }
}
