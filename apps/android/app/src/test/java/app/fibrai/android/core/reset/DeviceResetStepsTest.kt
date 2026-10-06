package app.fibrai.android.core.reset

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.fibrai.android.core.database.DayRepository
import app.fibrai.android.core.database.FibraiDatabase
import app.fibrai.android.core.database.InstantClock
import app.fibrai.android.core.database.MealSlot
import app.fibrai.android.core.push.SlotAlarmScheduler
import app.fibrai.android.core.telemetry.Telemetry
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A53 (ADR-040): the device reset erases Room, memory, photos and push state; the installation id stays. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class DeviceResetStepsTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val clock = InstantClock { Instant.parse("2026-09-25T18:00:00-03:00") }
    private lateinit var db: FibraiDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var repo: DayRepository
    private lateinit var steps: DeviceResetSteps
    private val events = mutableListOf<Pair<String, Map<String, Any>>>()
    private val telemetry = object : Telemetry {
        override fun event(name: String, params: Map<String, Any>) { events += name to params }
        override fun breadcrumb(message: String) = Unit
        override fun nonFatal(error: Throwable) = Unit
        override fun setKey(key: String, value: String) = Unit
    }

    private val memory get() = File(context.filesDir, "memory.bin")
    private val pending get() = File(context.filesDir, "memory.bin.new")
    private val legacy get() = File(context.filesDir, "memory.txt")
    private val photos get() = File(context.filesDir, "photos")
    private val installationId get() = File(context.noBackupFilesDir, "installation_id")

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(context, FibraiDatabase::class.java).allowMainThreadQueries().build()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val file = File(context.cacheDir, "reset_${System.nanoTime()}.preferences_pb")
        repo = DayRepository(db, clock, PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }))
        steps = DeviceResetSteps(context, repo, SlotAlarmScheduler(context, repo, clock))
        repo.saveProfile("same", 2000, 2000, 2300, List(7) { 2000 }, "partial", 50, true, "2026-09-25")
        repo.saveSlots(listOf(MealSlot(name = "Jantar", minutesFromMidnight = 1200)))
        repo.addLog("", "2 ovos", 380, 22, true, slotId = repo.observeToday().first().slots.first().id)
        repo.insertMessage("user", "2 ovos")
        memory.writeText("sealed")
        pending.writeText("sealed")
        legacy.writeText("old")
        File(photos.apply { mkdirs() }, "p.jpg").writeText("jpeg")
        installationId.apply { parentFile?.mkdirs() }.writeText("id")
        context.getSharedPreferences("fibrai_push", 0).edit().putBoolean("notifications_asked", true).commit()
    }

    @After
    fun tearDown() {
        db.close()
        scope.cancel()
    }

    @Test
    fun run_erasesEverything_keepsInstallationId() = runBlocking<Unit> {
        assertThat(steps.leftover()).isTrue()
        assertThat(AppReset(steps, telemetry).run()).isTrue()

        assertThat(repo.hasRows()).isFalse()
        assertThat(repo.observeToday().first().onboardingDone).isFalse()
        assertThat(memory.exists()).isFalse()
        assertThat(pending.exists()).isFalse()
        assertThat(legacy.exists()).isFalse()
        assertThat(photos.exists()).isFalse()
        assertThat(context.getSharedPreferences("fibrai_push", 0).all).isEmpty()
        assertThat(installationId.readText()).isEqualTo("id")
        assertThat(steps.leftover()).isFalse()
        assertThat(events).containsExactly("app_reset" to mapOf<String, Any>("outcome" to "done"))
    }

    @Test
    fun rerun_isNoOp_andFreshInstallHasNothingLeft() = runBlocking<Unit> {
        val reset = AppReset(steps, telemetry)
        assertThat(reset.run()).isTrue()
        assertThat(reset.run()).isTrue()
        events.clear()
        assertThat(reset.finishInterrupted()).isTrue()
        assertThat(events).isEmpty()
    }

    @Test
    fun interrupted_afterMark_isFinishedOnColdStart() = runBlocking<Unit> {
        steps.markOnboardingPending()
        assertThat(repo.observeToday().first().onboardingDone).isFalse()
        assertThat(AppReset(steps, telemetry).finishInterrupted()).isTrue()
        assertThat(repo.hasRows()).isFalse()
        assertThat(memory.exists()).isFalse()
    }
}
