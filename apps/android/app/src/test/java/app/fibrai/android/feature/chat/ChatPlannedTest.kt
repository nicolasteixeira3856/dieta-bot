package app.fibrai.android.feature.chat

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.fibrai.android.core.database.DayRepository
import app.fibrai.android.core.database.FibraiDatabase
import app.fibrai.android.core.database.InstantClock
import app.fibrai.android.core.database.MealSlot
import app.fibrai.android.core.memory.FactMemory
import app.fibrai.android.core.memory.FakeMemoryFile
import app.fibrai.android.core.network.ChatEstimate
import app.fibrai.android.core.network.ChatIn
import app.fibrai.android.core.network.ChatOut
import app.fibrai.android.core.network.ItemOut
import app.fibrai.android.core.photo.FakePhotoFiles
import app.fibrai.android.core.telemetry.FakeTelemetry
import app.fibrai.android.core.telemetry.TelemetryEvents
import app.fibrai.android.domain.PlannedSlot
import app.fibrai.android.domain.ReceiptAction
import app.fibrai.android.domain.SlotState
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A60 part D (ADR-046): reserve a plan for its meal, replace, clear by record or skip, undo, rollover, wipe, the wire. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class ChatPlannedTest {
    private lateinit var db: FibraiDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var repo: DayRepository
    private var now = Instant.parse("2026-09-25T18:00:00-03:00")
    private val clock = InstantClock { now }
    private val requests = mutableListOf<ChatIn>()
    private var answer: () -> ChatOut = { plan(360) }
    private val service = ChatService { requests += it; answer() }
    private val telemetry = FakeTelemetry()
    private var jantar = 0L
    private var almoco = 0L

    @Before
    fun setUp() = runBlocking<Unit> {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FibraiDatabase::class.java).allowMainThreadQueries().build()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(context.cacheDir, "planned_${System.nanoTime()}.preferences_pb") })
        repo = DayRepository(db, clock, store)
        repo.saveProfile("same", 2200, 2200, 2300, List(7) { 2200 }, "zero", 50, true, "2026-09-25")
        repo.saveSlots(listOf(MealSlot(name = "Almoço", minutesFromMidnight = 750), MealSlot(name = "Jantar", minutesFromMidnight = 1200)))
        repo.observeToday().first().slots.forEach { if (it.name == "Jantar") jantar = it.id else almoco = it.id }
    }

    @After
    fun tearDown() {
        db.close()
        scope.cancel()
        Dispatchers.resetMain()
    }

    private fun vm() = ChatViewModel(repo, service, clock, FactMemory(FakeMemoryFile()), FakePhotoFiles(), telemetry)

    private fun plan(kcal: Int, slot: Long = jantar, text: String = "Omelete de forno: 3 ovos, 50 g de ricota e 1 fatia de pão integral") = ChatOut(
        reply = "- **Omelete de forno**: 3 ovos · **$kcal kcal**",
        intent = "plan",
        record = "none",
        estimate = ChatEstimate(kcal = kcal.toDouble(), p = 30.0, c = 20.0, g = 18.0, confidence = "medium", items = listOf(ItemOut("ovo")),
            suggestedSlot = slot.toString(), mealText = text),
        model = "gpt-6-luna",
    )

    private fun dinnerLog(kcal: Int) = ChatOut(
        reply = "Jantar: frango e arroz: **$kcal kcal**.",
        intent = "log",
        record = "auto",
        estimate = ChatEstimate(kcal = kcal.toDouble(), p = 52.0, c = 60.0, g = 14.0, confidence = "high", items = listOf(ItemOut("frango")),
            suggestedSlot = jantar.toString(), mealText = "frango e arroz"),
        model = "gpt-6-luna",
    )

    private suspend fun answers() = repo.observeMessages().first().count { it.role == "assistant" }

    private suspend fun send(vm: ChatViewModel, text: String) {
        val before = answers()
        vm.setComposer(text)
        vm.send()
        withTimeout(5_000) { while (answers() <= before) delay(10) }
        withTimeout(5_000) {
            while (repo.observeMessages().first().last { it.role == "assistant" }.let { it.recordMode == "auto" && it.recordState == null }) delay(10)
        }
        vm.await { !it.sending && it.items.count { i -> i is ChatItem.Assistant } > before }
    }

    private suspend fun ChatViewModel.await(pred: (ChatUiState) -> Boolean) = withTimeout(5_000) { uiState.first(pred) }

    private suspend fun planned() = repo.observeToday().first().planned

    @Test
    fun reserve_marksTheBubble_goesInDay_andTheProjectedDay() = runBlocking<Unit> {
        val vm = vm()
        send(vm, "o que eu janto?")
        val actions = vm.uiState.value.actions!!
        assertThat(actions.reserve!!.name).isEqualTo("Jantar")
        vm.reserve(actions.estimateId)
        vm.reserve(actions.estimateId) // a double tap reserves once
        vm.await { it.items.filterIsInstance<ChatItem.Assistant>().last().reservedFor == "Jantar" }
        assertThat(vm.uiState.value.actions!!.reserve).isNull()
        assertThat(vm.uiState.value.actions!!.plan).isTrue()
        assertThat(planned()[jantar]).isEqualTo(PlannedSlot("Omelete de forno: 3 ovos, 50 g de ricota e 1 fatia de pão integral", 360, 30, 20, 18, actions.estimateId))
        assertThat(telemetry.events.filter { it.first == TelemetryEvents.PLAN_RESERVED }).containsExactly(TelemetryEvents.PLAN_RESERVED to mapOf("action" to "reserved"))
        // The next turn carries the plan in DAY; a plan for another meal counts it in the projected day.
        answer = { plan(500, slot = almoco, text = "frango") }
        send(vm, "e um lanche?")
        val day = requests.last().day.slots.single { it.id == jantar.toString() }
        assertThat(day.status).isEqualTo("planned")
        assertThat(day.kcal).isEqualTo(360)
        assertThat(requests.last().day.eatenKcal).isEqualTo(0)
        val projected = vm.uiState.value.items.filterIsInstance<ChatItem.Assistant>().last().plan!!
        assertThat(projected.projected.kcal).isEqualTo(860)
    }

    @Test
    fun reservingAgain_replaces_theEarlierBubbleLosesItsMarker() = runBlocking<Unit> {
        val vm = vm()
        send(vm, "o que eu janto?")
        val first = vm.uiState.value.actions!!.estimateId
        vm.reserve(first)
        vm.await { it.items.any { i -> i is ChatItem.Assistant && i.reservedFor != null } }
        answer = { plan(420) }
        send(vm, "outra ideia")
        val second = vm.uiState.value.actions!!.estimateId
        vm.reserve(second)
        vm.await { s -> s.items.filterIsInstance<ChatItem.Assistant>().let { a -> a.last().reservedFor == "Jantar" && a.first().reservedFor == null } }
        assertThat(planned()[jantar]!!.kcal).isEqualTo(420)
        assertThat(telemetry.events.last()).isEqualTo(TelemetryEvents.PLAN_RESERVED to mapOf("action" to "replaced"))
    }

    @Test
    fun aRecordReplacesTheReservation_receiptLine_andDesfazerRestoresIt() = runBlocking<Unit> {
        val vm = vm()
        send(vm, "o que eu janto?")
        vm.reserve(vm.uiState.value.actions!!.estimateId)
        vm.await { it.items.any { i -> i is ChatItem.Assistant && i.reservedFor != null } }
        answer = { dinnerLog(610) }
        send(vm, "jantei frango e arroz")
        assertThat(planned()).isEmpty()
        assertThat(repo.slotState("2026-09-25", jantar).kcal).isEqualTo(610)
        val receipt = vm.await { it.items.any { i -> i is ChatItem.Receipt && i.planLine != null } }.items.filterIsInstance<ChatItem.Receipt>().last()
        assertThat(receipt.planLine).isEqualTo("Plano: 360 · Registrado: 610 (+250 kcal)")
        assertThat(receipt.actions).contains(ReceiptAction.UNDO)
        assertThat(telemetry.events.map { it.first to it.second["action"] }).contains(TelemetryEvents.PLAN_RESERVED to "cleared_by_record")
        assertThat(telemetry.events.last { it.first == TelemetryEvents.MEAL_SAVED }.second["had_plan"]).isEqualTo(true)
        vm.receiptAction(receipt.id, ReceiptAction.UNDO)
        withTimeout(5_000) { while (planned()[jantar] == null) delay(10) }
        assertThat(repo.slotState("2026-09-25", jantar).records).isEmpty()
    }

    @Test
    fun aSkip_theWipe_andTheNextDay_clearTheReservation() = runBlocking<Unit> {
        val plan = PlannedSlot("omelete", 360, 30, 20, 18)
        repo.reserve("2026-09-25", null, jantar, plan)
        repo.addSkip(jantar)
        assertThat(repo.slotState("2026-09-25", jantar)).isEqualTo(SlotState.SKIPPED)
        repo.reserve("2026-09-25", null, almoco, plan)
        now = Instant.parse("2026-09-26T08:00:00-03:00")
        assertThat(repo.observeToday().first { it.date == "2026-09-26" }.planned).isEmpty()
        now = Instant.parse("2026-09-25T19:00:00-03:00")
        repo.wipeToday()
        assertThat(repo.slotState("2026-09-25", almoco)).isEqualTo(SlotState.EMPTY)
    }

    @Test
    fun noPill_forAMealWithARecord_orWhenTheChoiceIsOpen() = runBlocking<Unit> {
        repo.addLog("", "sopa", 300, 10, true, jantar)
        val vm = vm()
        send(vm, "o que eu janto?")
        assertThat(vm.uiState.value.actions!!.reserve).isNull()
        assertThat(repo.reserve("2026-09-25", null, jantar, PlannedSlot("x", 1, 0, 0, 0))).isEqualTo(app.fibrai.android.core.database.ReserveResult.Stale)
    }
}
