package app.fibrai.android.core.closure

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.fibrai.android.core.database.DayRepository
import app.fibrai.android.core.database.FibraiDatabase
import app.fibrai.android.core.database.InstantClock
import app.fibrai.android.core.database.MealSlot
import app.fibrai.android.core.network.CloseIn
import app.fibrai.android.core.network.CloseOut
import app.fibrai.android.core.telemetry.FakeTelemetry
import app.fibrai.android.core.telemetry.TelemetryEvents
import app.fibrai.android.domain.ClosureDay
import app.fibrai.android.domain.ClosureWeek
import app.fibrai.android.domain.Closures
import app.fibrai.android.domain.PlannedSlot
import app.fibrai.android.feature.home.HomePanelMapper
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A60 part B (ADR-044): the closure from Room to the request, the stored row, idempotence, offline, fallback, the cards. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class ClosureRunnerTest {
    private lateinit var db: FibraiDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var repo: DayRepository
    private var now = Instant.parse("2026-10-11T22:00:00-03:00") // a Sunday
    private val clock = InstantClock { now }
    private val requests = mutableListOf<CloseIn>()
    private var reply: () -> CloseOut = { CloseOut("Passou 230 kcal do teto.\nAmanhã: ovos no café.", "gpt-6-luna") }
    private val notified = mutableListOf<Pair<String, String>>()
    private val telemetry = FakeTelemetry()
    private lateinit var runner: ClosureRunner
    private val slots = mutableMapOf<String, Long>()

    @Before
    fun setUp() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FibraiDatabase::class.java).allowMainThreadQueries().build()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(context.cacheDir, "closure_${System.nanoTime()}.preferences_pb") })
        repo = DayRepository(db, clock, store)
        repo.saveProfile("same", 2000, 2000, 2300, List(7) { 2000 }, "full", 50, true, "2026-10-01", proteinTargetG = 150, carbTargetG = 220, fatTargetG = 65, tone = "duro")
        repo.saveSlots(
            listOf(
                MealSlot(name = "Café", minutesFromMidnight = 420),
                MealSlot(name = "Almoço", minutesFromMidnight = 750),
                MealSlot(name = "Lanche", minutesFromMidnight = 960),
                MealSlot(name = "Jantar", minutesFromMidnight = 1200),
            ),
        )
        repo.observeToday().first().slots.forEach { slots[it.name] = it.id }
        runner = ClosureRunner(repo, { requests += it; reply() }, clock, { period, text -> notified += period to text }, telemetry)
    }

    @After
    fun tearDown() {
        db.close()
        scope.cancel()
    }

    private suspend fun eat(slot: String, kcal: Int, p: Int) = repo.addLog("", "refeição", kcal, p, true, slots.getValue(slot), carbs = kcal / 8, fat = kcal / 30)

    @Test
    fun dayClosure_sendsTheNumbersAndTone_storesTheText_notifiesOnce() = runBlocking<Unit> {
        eat("Café", 410, 20)
        eat("Almoço", 980, 40)
        repo.addSkip(slots.getValue("Lanche"))
        repo.setWorkout(350)
        val row = runner.run(Closures.DAY)!!
        val body = requests.single()
        assertThat(body.period).isEqualTo("day")
        assertThat(body.tone).isEqualTo("duro")
        assertThat(body.profile.ceilingKcal).isEqualTo(2000)
        val numbers = Closures.json.decodeFromJsonElement(ClosureDay.serializer(), body.numbers)
        assertThat(numbers.kcal).isEqualTo(1390)
        assertThat(numbers.ceilingKcal).isEqualTo(2350) // eat-back full: 2000 + 350
        assertThat(numbers.workoutKcal).isEqualTo(350)
        assertThat(numbers.slots.map { it.status }).containsExactly("eaten", "eaten", "skipped", "empty").inOrder()
        assertThat(body.numbers.jsonObject.keys).containsExactly("date", "kcal", "p", "c", "g", "ceiling_kcal", "workout_kcal", "slots")
        assertThat(row.status).isEqualTo(Closures.TEXT)
        assertThat(repo.closure("day:2026-10-11")!!.text).startsWith("Passou 230 kcal")
        assertThat(notified.single()).isEqualTo("day" to "Passou 230 kcal do teto.")
        assertThat(telemetry.events.last()).isEqualTo(TelemetryEvents.CLOSURE to mapOf("period" to "day", "outcome" to "text"))
        // Produced once: a second alarm or broadcast changes nothing.
        assertThat(runner.run(Closures.DAY)).isNull()
        assertThat(requests).hasSize(1)
    }

    @Test
    fun withoutNetwork_theNumbersAlone_thenOneRetryAtStart() = runBlocking<Unit> {
        eat("Almoço", 900, 40)
        reply = { error("offline") }
        val row = runner.run(Closures.DAY)!!
        assertThat(row.status).isEqualTo(Closures.OFFLINE)
        assertThat(notified.single().second).isEqualTo("Sem o texto: sem rede.")
        reply = { CloseOut("900 de 2000 kcal.", "gpt-6-luna") }
        runner.retryOffline()
        val stored = repo.closure("day:2026-10-11")!!
        assertThat(stored.status).isEqualTo(Closures.TEXT)
        assertThat(stored.retried).isTrue()
        // Retried once only.
        reply = { error("offline") }
        runner.retryOffline()
        assertThat(requests).hasSize(2)
    }

    @Test
    fun theServerFallback_isStoredAsFallback_andADayWithoutRecords_asksNothing() = runBlocking<Unit> {
        val empty = runner.run(Closures.DAY)!!
        assertThat(empty.status).isEqualTo(Closures.EMPTY)
        assertThat(requests).isEmpty()
        assertThat(notified.single().second).isEqualTo("Nenhum registro.")
        now = Instant.parse("2026-10-12T22:00:00-03:00")
        eat("Almoço", 900, 40)
        reply = { CloseOut("Dia fechado. 900 de 2350 kcal.", "gpt-6-luna") }
        assertThat(runner.run(Closures.DAY)!!.status).isEqualTo(Closures.FALLBACK)
    }

    @Test
    fun weekClosure_daysFromMonday_withRecordedFlags() = runBlocking<Unit> {
        now = Instant.parse("2026-10-07T12:00:00-03:00") // Wednesday
        eat("Jantar", 1300, 40)
        now = Instant.parse("2026-10-11T22:00:00-03:00")
        eat("Almoço", 400, 40)
        val produced = runner.runDue()
        assertThat(produced.map { it.period }).containsExactly("day", "week").inOrder()
        val week = Closures.json.decodeFromJsonElement(ClosureWeek.serializer(), requests.last().numbers)
        assertThat(week.days.map { it.date }).containsExactly(
            "2026-10-05", "2026-10-06", "2026-10-07", "2026-10-08", "2026-10-09", "2026-10-10", "2026-10-11",
        ).inOrder()
        assertThat(week.days.map { it.recorded }).containsExactly(false, false, true, false, false, false, true).inOrder()
        assertThat(week.overSlot!!.name).isEqualTo("Jantar")
        assertThat(repo.closure("week:2026-10-05")).isNotNull()
        assertThat(requests.last().numbers.jsonObject.getValue("over_slot").jsonObject.getValue("days").jsonPrimitive.content).isEqualTo("1")
    }

    @Test
    fun cards_liveNumbersToday_collapseYesterdayAfterARecord_weekAboveDay() = runBlocking<Unit> {
        eat("Almoço", 980, 40)
        runner.runDue()
        val rows = repo.observeClosures("2026-10-01").first()
        // A later record of today updates the numbers of the card, not the text.
        eat("Jantar", 300, 20)
        val today = LocalDate.parse("2026-10-11")
        var ui = HomePanelMapper.map(repo.observeToday().first(), today, closures = rows)
        assertThat(ui.closures.map { it.period }).containsExactly("week", "day").inOrder()
        val day = ui.closures.last()
        assertThat(day.expanded).isTrue()
        assertThat(day.kcalLine).isEqualTo("1280 de 2000 kcal")
        assertThat(day.text).startsWith("Passou 230 kcal")
        assertThat(day.title).isEqualTo("Fechamento de 11 de outubro")
        // The next day: expanded until the first record, then one line.
        now = Instant.parse("2026-10-12T08:00:00-03:00")
        ui = HomePanelMapper.map(repo.observeToday().first(), today.plusDays(1), closures = rows)
        assertThat(ui.closures.last().expanded).isTrue()
        eat("Café", 300, 20)
        ui = HomePanelMapper.map(repo.observeToday().first(), today.plusDays(1), closures = rows)
        assertThat(ui.closures.last().expanded).isFalse()
        assertThat(ui.closures.last().line).isEqualTo("Ontem: 980 de 2000 kcal")
        assertThat(HomePanelMapper.map(repo.observeToday().first(), today.plusDays(1), closures = rows, expanded = setOf("day:2026-10-11")).closures.last().expanded).isTrue()
    }

    @Test
    fun aPlannedMealCountsAsMissing() = runBlocking<Unit> {
        eat("Almoço", 900, 40)
        repo.reserve("2026-10-11", null, slots.getValue("Jantar"), PlannedSlot("omelete", 450, 30, 6, 32))
        runner.run(Closures.DAY)
        val n = Closures.json.decodeFromJsonElement(ClosureDay.serializer(), requests.single().numbers)
        assertThat(n.slots.last()).isEqualTo(app.fibrai.android.domain.ClosureSlot("Jantar", "planned", 450))
        assertThat(n.kcal).isEqualTo(900)
    }
}
