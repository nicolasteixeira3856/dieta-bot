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
import app.fibrai.android.core.network.ChatAction
import app.fibrai.android.core.network.ChatWorkout
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

/** A66 (ADR-050): typed actions applied as one batch: one row per log or plan, one receipt per action, Desfazer of the batch. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class ChatTypedActionsTest {
    private lateinit var db: FibraiDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var repo: DayRepository
    private var now = Instant.parse("2026-09-25T18:00:00-03:00")
    private val clock = InstantClock { now }
    private val requests = mutableListOf<ChatIn>()
    private var answer: () -> ChatOut = { wholeDay() }
    private val service = ChatService { requests += it; answer() }
    private val telemetry = FakeTelemetry()
    private var jantar = 0L
    private var almoco = 0L
    private var cafe = 0L
    private var lanche = 0L

    @Before
    fun setUp() = runBlocking<Unit> {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FibraiDatabase::class.java).allowMainThreadQueries().build()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(context.cacheDir, "actions_${System.nanoTime()}.preferences_pb") })
        repo = DayRepository(db, clock, store)
        repo.saveProfile("same", 2200, 2200, 2300, List(7) { 2200 }, "zero", 50, true, "2026-09-22")
        repo.saveSlots(listOf(MealSlot(name = "Café", minutesFromMidnight = 450), MealSlot(name = "Almoço", minutesFromMidnight = 750), MealSlot(name = "Lanche", minutesFromMidnight = 960), MealSlot(name = "Jantar", minutesFromMidnight = 1200)))
        repo.observeToday().first().slots.forEach { when (it.name) { "Jantar" -> jantar = it.id; "Almoço" -> almoco = it.id; "Café" -> cafe = it.id; else -> lanche = it.id } }
    }

    @After
    fun tearDown() {
        db.close()
        scope.cancel()
        Dispatchers.resetMain()
    }

    private fun vm() = ChatViewModel(repo, service, clock, FactMemory(FakeMemoryFile()), FakePhotoFiles(), telemetry)

    private fun estimate(kcal: Int, slot: Long, text: String) = ChatEstimate(
        kcal = kcal.toDouble(), p = 20.0, c = 30.0, g = 10.0, confidence = "high", items = listOf(ItemOut(text)), suggestedSlot = slot.toString(), mealText = text,
    )

    private fun log(id: String, kcal: Int, slot: Long, text: String) =
        ChatAction(id = id, type = "log", slot = slot.toString(), estimate = estimate(kcal, slot, text), record = "auto", recordIntent = "clear", mealDay = "today",
            mealChange = kotlinx.serialization.json.Json.parseToJsonElement("""{"operation":"new","base_slot":null,"addition":null}"""))

    private fun wholeDay() = ChatOut(
        reply = "Café, almoço e lanche de hoje.",
        // Legacy fields of a capable server are ignored: the actions rule.
        intent = "log",
        estimate = estimate(999, jantar, "ignorado"),
        record = "auto",
        actions = listOf(
            log("a1", 300, cafe, "2 ovos e 1 pão"),
            log("a2", 650, almoco, "arroz, feijão e frango"),
            ChatAction(id = "a3", type = "skip", slot = lanche.toString(), record = "auto"),
        ),
        model = "gpt-6-luna",
    )

    private fun logAndPlan() = ChatOut(
        reply = "Jantar registrado. Para o lanche: iogurte com fruta.",
        actions = listOf(
            log("a1", 610, jantar, "frango e arroz"),
            ChatAction(id = "a2", type = "plan", slot = lanche.toString(), estimate = estimate(180, lanche, "iogurte com fruta"), record = "none"),
        ),
        model = "gpt-6-luna",
    )

    private fun oneHeld() = ChatOut(
        reply = "Café anotado. E o almoço, quanto de arroz?",
        actions = listOf(
            log("a1", 300, cafe, "2 ovos e 1 pão"),
            ChatAction(id = "a2", type = "log", slot = almoco.toString(), estimate = null, question = "Quanto de arroz no almoço?", record = "none"),
        ),
        model = "gpt-6-luna",
    )

    private suspend fun answers() = repo.observeMessages().first().count { it.role == "assistant" }

    private suspend fun send(vm: ChatViewModel, text: String, rows: Int) {
        val before = answers()
        vm.setComposer(text)
        vm.send()
        withTimeout(5_000) { while (answers() < before + rows) delay(10) }
        vm.await { !it.sending }
    }

    private suspend fun ChatViewModel.await(pred: (ChatUiState) -> Boolean) = withTimeout(5_000) { uiState.first(pred) }

    @Test
    fun theWholeDay_twoRecordsAndASkip_threeReceipts_desfazerRevertsTheBatch() = runBlocking<Unit> {
        val vm = vm()
        send(vm, "café 2 ovos, almoço arroz feijão frango, pulei o lanche", rows = 2)
        assertThat(requests.last().actions).isTrue()
        withTimeout(5_000) { while (repo.slotState("2026-09-25", lanche) != SlotState.SKIPPED) delay(10) }
        assertThat(repo.slotState("2026-09-25", cafe).kcal).isEqualTo(300)
        assertThat(repo.slotState("2026-09-25", almoco).kcal).isEqualTo(650)
        // The legacy estimate (999 kcal into the dinner) was ignored.
        assertThat(repo.slotState("2026-09-25", jantar).records).isEmpty()
        val receipts = vm.await { s -> s.items.count { it is ChatItem.Receipt && ReceiptAction.UNDO in it.actions } == 3 }.items.filterIsInstance<ChatItem.Receipt>()
        assertThat(receipts.map { it.kind }).containsExactly(ReceiptKind.LOGGED, ReceiptKind.LOGGED, ReceiptKind.SKIPPED).inOrder()
        assertThat(receipts.map { it.batch }.toSet()).hasSize(1)
        // Only the first row has the reply; the second is the card alone and stays out of the history.
        val rows = repo.observeMessages().first().filter { it.role == "assistant" }
        assertThat(rows.map { it.text.isBlank() }).containsExactly(false, true).inOrder()
        assertThat(rows.first().actions).contains("\"a3\"")
        vm.receiptAction(receipts[1].id, ReceiptAction.UNDO)
        withTimeout(5_000) { while (repo.slotState("2026-09-25", lanche) != SlotState.EMPTY) delay(10) }
        assertThat(repo.slotState("2026-09-25", cafe).records).isEmpty()
        assertThat(repo.slotState("2026-09-25", almoco).records).isEmpty()
        answer = { ChatOut(reply = "Ok.", actions = listOf(ChatAction(id = "a1", type = "question", record = "none")), model = "gpt-6-luna") }
        send(vm, "obrigado", rows = 1)
        assertThat(requests.last().messages.filter { it.role == "assistant" }.map { it.text }).doesNotContain("")
    }

    @Test
    fun aLogAndAPlan_theLogIsRecorded_thePlanKeepsRegistrarAssimAndReservar() = runBlocking<Unit> {
        answer = { logAndPlan() }
        val vm = vm()
        send(vm, "jantei frango e arroz, me sugere o lanche", rows = 2)
        withTimeout(5_000) { while (repo.slotState("2026-09-25", jantar).kcal != 610) delay(10) }
        val ui = vm.await { it.actions?.plan == true && it.items.any { i -> i is ChatItem.Receipt } }
        assertThat(ui.actions!!.reserve!!.name).isEqualTo("Lanche")
        // The plan is projected on the day with the dinner already eaten.
        val plan = ui.items.filterIsInstance<ChatItem.Assistant>().last().plan!!
        assertThat(plan.projected.kcal).isEqualTo(790)
    }

    @Test
    fun aClarificationOnOneOfTwo_theOtherIsRecorded_theQuestionShows() = runBlocking<Unit> {
        answer = { oneHeld() }
        val vm = vm()
        send(vm, "café 2 ovos e almoço arroz e frango", rows = 2)
        withTimeout(5_000) { while (repo.slotState("2026-09-25", cafe).kcal != 300) delay(10) }
        // The café's receipt is written after its record: wait for it next to the question, then count.
        val ui = vm.await { s -> s.items.any { it is ChatItem.Question && it.text == "Quanto de arroz no almoço?" } && s.items.any { it is ChatItem.Receipt } }
        assertThat(ui.items.count { it is ChatItem.Receipt }).isEqualTo(1)
        assertThat(repo.slotState("2026-09-25", almoco).records).isEmpty()
        // One receipt alone: its own actions, no batch Desfazer added.
        assertThat(ui.items.filterIsInstance<ChatItem.Receipt>().single().actions).doesNotContain(ReceiptAction.UNDO)
    }

    @Test
    fun aCompactRequestSendsNoCapability() {
        val turn = PromptBuilder.build(app.fibrai.android.feature.home.HomeFixtures.home0, emptyList(), emptyList(), "oi", now)
        assertThat(turn.body.actions).isTrue()
        val block = PromptBuilder.CompactBlock(listOf(app.fibrai.android.core.network.ChatTurn("user", "oi")), 1)
        assertThat(PromptBuilder.compact(turn, block).actions).isFalse()
    }
}
