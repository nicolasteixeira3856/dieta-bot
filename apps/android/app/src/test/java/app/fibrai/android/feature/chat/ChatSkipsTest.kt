package app.fibrai.android.feature.chat

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import app.fibrai.android.core.database.DayRepository
import app.fibrai.android.core.database.FibraiDatabase
import app.fibrai.android.core.database.InstantClock
import app.fibrai.android.core.database.MealSlot
import app.fibrai.android.core.memory.FactMemory
import app.fibrai.android.core.memory.FakeMemoryFile
import app.fibrai.android.core.network.ChatEstimate
import app.fibrai.android.core.network.ChatIn
import app.fibrai.android.core.network.ChatMemoryUpdate
import app.fibrai.android.core.network.ChatOut
import app.fibrai.android.core.network.ItemOut
import app.fibrai.android.core.photo.FakePhotoFiles
import app.fibrai.android.core.telemetry.FakeTelemetry
import app.fibrai.android.core.telemetry.TelemetryEvents
import app.fibrai.android.domain.ReceiptAction
import app.fibrai.android.domain.SkipEntry
import app.fibrai.android.domain.SkipOutcomes
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
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

/** A59 (ADR-047): skips listed next to any intent, their receipts and the delete-and-skip proposal (chatSK, chatSD). */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class ChatSkipsTest {
    private lateinit var context: Context
    private lateinit var db: FibraiDatabase
    private lateinit var storeScope: CoroutineScope
    private lateinit var store: DataStore<Preferences>
    private lateinit var repo: DayRepository
    private val memoryFile = FakeMemoryFile()
    private val memory = FactMemory(memoryFile)
    private val today = LocalDate.parse("2026-09-25")
    private val photos = FakePhotoFiles()
    private var now = Instant.parse("2026-09-25T08:10:00-03:00")
    private val clock = InstantClock { now }
    private val requests = mutableListOf<ChatIn>()
    private var answer: () -> ChatOut = { ChatOut(reply = "ok", model = "gpt-6-luna") }
    private val service = ChatService { body ->
        requests += body
        answer()
    }
    private val telemetry = FakeTelemetry()

    @Before
    fun setUp() = runBlocking<Unit> {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, FibraiDatabase::class.java).allowMainThreadQueries().build()
        storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val file = File(context.cacheDir, "skips_${System.nanoTime()}.preferences_pb")
        store = PreferenceDataStoreFactory.create(scope = storeScope, produceFile = { file })
        repo = DayRepository(db, clock, store)
        repo.saveProfile("same", 2000, 2000, 2300, List(7) { 2000 }, "zero", 50, true, "2026-09-25")
        repo.saveSlots(
            listOf(
                MealSlot(name = "Pré-treino", minutesFromMidnight = 360),
                MealSlot(name = "Café da manhã", minutesFromMidnight = 450),
                MealSlot(name = "Almoço", minutesFromMidnight = 750),
                MealSlot(name = "Jantar", minutesFromMidnight = 1200),
            ),
        )
    }

    @After
    fun tearDown() {
        db.close()
        storeScope.cancel()
        Dispatchers.resetMain()
    }

    private fun vm() = ChatViewModel(repo, service, clock, memory, photos, telemetry)

    private suspend fun slots() = repo.observeToday().first().slots.sortedBy { it.minutesFromMidnight }.map { it.id }

    /** The tester's message (2026-10-07): breakfast recorded with no tap and [skips] listed next to it. */
    private fun logAndSkip(slot: Long, skips: List<Long>, kcal: Double = 320.0, memoryUpdates: List<ChatMemoryUpdate> = emptyList()) = ChatOut(
        reply = "Pré-treino de hoje fora. Identifiquei 2 ovos mexidos e 1 pão francês.",
        intent = "log",
        record = "auto",
        estimate = ChatEstimate(
            kcal = kcal, p = 17.0, c = 29.0, g = 16.0, confidence = "high",
            items = listOf(ItemOut("2 ovos mexidos"), ItemOut("1 pão francês")),
            suggestedSlot = slot.toString(),
            mealText = "2 ovos mexidos e 1 pão francês",
        ),
        memoryUpdates = memoryUpdates,
        skipSlots = skips.map { it.toString() },
        model = "gpt-6-luna",
    )

    private fun only(intent: String, skips: List<Any>) = ChatOut(
        reply = "Almoço de hoje fora.",
        intent = intent,
        record = if (intent == "skip") "auto" else "none",
        skipSlot = skips.firstOrNull()?.toString().takeIf { intent == "skip" },
        skipSlots = skips.map { it.toString() },
        model = "gpt-6-luna",
    )

    private suspend fun ChatViewModel.await(pred: (ChatUiState) -> Boolean) = withTimeout(5_000) { uiState.first(pred) }

    /** Sends and waits until the answer is stored, its record decided (auto) and every listed skip applied. */
    private suspend fun send(vm: ChatViewModel, text: String) {
        val before = repo.observeMessages().first().count { it.role == "assistant" }
        vm.setComposer(text)
        vm.send()
        withTimeout(5_000) {
            while (true) {
                val rows = repo.observeMessages().first()
                val last = rows.lastOrNull { it.role == "assistant" }
                val recorded = last?.recordMode != "auto" || last.recordState != null || last.estimateKcal == null
                val skipsDone = SkipOutcomes.decode(last?.skipOutcomes)?.slots?.none { it.pending } ?: true
                if (rows.count { it.role == "assistant" } > before && recorded && skipsDone) break
                delay(10)
            }
        }
        vm.await { !it.sending && it.items.none { i -> i is ChatItem.Loading } }
    }

    private suspend fun lastAnswer() = repo.observeMessages().first().last { it.role == "assistant" }

    private suspend fun outcomes() = SkipOutcomes.decode(lastAnswer().skipOutcomes)!!.slots.associate { it.slotId to it.outcome }

    private fun ChatUiState.receipts() = items.filterIsInstance<ChatItem.Receipt>()
    private fun ChatUiState.skipPrompt() = items.filterIsInstance<ChatItem.SkipDeletePrompt>().singleOrNull()
    private fun ChatUiState.marks() = items.filterIsInstance<ChatItem.SkipMark>()

    private suspend fun recordAlmoco(vm: ChatViewModel, almoco: Long, memoryUpdates: List<ChatMemoryUpdate> = emptyList()) {
        answer = { logAndSkip(almoco, emptyList(), kcal = 640.0, memoryUpdates = memoryUpdates) }
        send(vm, "almocei arroz, feijão e frango")
        assertThat(repo.slotState(today.toString(), almoco).kcal).isEqualTo(640)
    }

    @Test
    fun incidentShape_recordsTheBreakfastAndSkipsThePreWorkout_twoReceipts() = runBlocking<Unit> {
        val vm = vm()
        val (pre, cafe) = slots()
        answer = { logAndSkip(cafe, listOf(pre)) }
        send(vm, "Pular pré treino. No café comi 2 ovos mexidos e 1 pão francês")

        assertThat(requests.single().skipSlots).isTrue()
        val day = repo.observeToday().first()
        assertThat(day.logs.single().slotId).isEqualTo(cafe)
        assertThat(day.skippedSlotIds).containsExactly(pre)
        val ui = vm.await { it.receipts().size == 2 && it.receipts().all { r -> r.actions.isNotEmpty() } }
        val (logged, skipped) = ui.receipts()
        assertThat(logged.kind).isEqualTo(ReceiptKind.LOGGED)
        assertThat(logged.slotName).isEqualTo("Café da manhã")
        assertThat(skipped.kind).isEqualTo(ReceiptKind.SKIPPED)
        assertThat(skipped.slotName).isEqualTo("Pré-treino")
        assertThat(skipped.actions).containsExactly(ReceiptAction.UNDO)
        assertThat(outcomes()).containsExactly(pre, SkipOutcomes.SKIPPED)
        assertThat(telemetry.params(TelemetryEvents.MEAL_SKIPPED).single()).containsExactly("from", "chat", "with", "log")
        assertThat(telemetry.params(TelemetryEvents.MEAL_AUTO_RECORDED).map { it["kind"] }).containsExactly("log", "skip").inOrder()
    }

    @Test
    fun twoSkips_eachWithItsReceipt_inListOrder_andTheLogsSlotIsNeverSkipped() = runBlocking<Unit> {
        val vm = vm()
        val (pre, cafe, almoco) = slots()
        // A server that listed the log's own slot: the log wins.
        answer = { logAndSkip(cafe, listOf(pre, almoco, cafe)) }
        send(vm, "pulei o pré e não vou almoçar; no café comi ovos")

        assertThat(repo.observeToday().first().skippedSlotIds).containsExactly(pre, almoco)
        val ui = vm.await { it.receipts().size == 3 }
        assertThat(ui.receipts().map { it.slotName }).containsExactly("Café da manhã", "Pré-treino", "Almoço").inOrder()
        assertThat(SkipOutcomes.decode(lastAnswer().skipOutcomes)!!.slots.map { it.slotId }).containsExactly(pre, almoco).inOrder()
    }

    @Test
    fun skipsNextToAPlanAndAQuestion_areApplied_withTheirIntent() = runBlocking<Unit> {
        val vm = vm()
        val (pre, _, almoco) = slots()
        answer = { only("plan", listOf(pre)) }
        send(vm, "pulei o pré, o que almoço?")
        answer = { only("question", listOf(almoco)) }
        send(vm, "não vou almoçar, isso é ruim?")

        assertThat(repo.observeToday().first().skippedSlotIds).containsExactly(pre, almoco)
        assertThat(telemetry.params(TelemetryEvents.MEAL_SKIPPED).map { it["with"] }).containsExactly("plan", "question").inOrder()
        assertThat(vm.await { it.receipts().size == 2 }.receipts().all { it.skipped }).isTrue()
    }

    @Test
    fun alreadySkipped_writesNothing_andLeavesNoReceipt() = runBlocking<Unit> {
        val vm = vm()
        val pre = slots().first()
        repo.addSkip(pre)
        answer = { only("skip", listOf(pre)) }
        send(vm, "pulei o pré")

        assertThat(outcomes()).containsExactly(pre, SkipOutcomes.ALREADY)
        assertThat(repo.observeMessages().first().none { it.role == "skipped" }).isTrue()
        assertThat(vm.uiState.value.marks()).isEmpty()
    }

    @Test
    fun unknownIds_areDroppedAndReported() = runBlocking<Unit> {
        val vm = vm()
        val pre = slots().first()
        answer = { only("skip", listOf("999", "x", pre)) }
        send(vm, "pulei o pré e a ceia")

        assertThat(repo.observeToday().first().skippedSlotIds).containsExactly(pre)
        assertThat(telemetry.params(TelemetryEvents.RECORD_GUARD).map { it["reason"] }).containsExactly("slot_not_today", "slot_not_today")
    }

    @Test
    fun skipOverARecord_asks_confirmDeletesAndSkips_revertsTheMemory_undoBringsBothBack() = runBlocking<Unit> {
        val vm = vm()
        val almoco = slots()[2]
        val routine = ChatMemoryUpdate("add", null, "dynamic", "routine", "almoco", "arroz, feijão e frango", almoco.toString())
        recordAlmoco(vm, almoco, listOf(routine))
        assertThat(memory.read(today).facts).hasSize(1)

        answer = { only("skip", listOf(almoco)) }
        send(vm, "acabei não almoçando hoje")
        // Nothing changes before the tap; the card shows the recorded kcal.
        val prompt = vm.await { it.skipPrompt() != null }.skipPrompt()!!
        assertThat(prompt.confirm.slot.name).isEqualTo("Almoço")
        assertThat(prompt.confirm.kcal).isEqualTo(640)
        assertThat(repo.slotState(today.toString(), almoco).kcal).isEqualTo(640)
        assertThat(telemetry.params(TelemetryEvents.SKIP_DELETE).map { it["action"] }).containsExactly("shown")

        vm.confirmSkipDelete(prompt.answerId, almoco)
        val ui = vm.await { it.skipPrompt() == null && it.receipts().any { r -> r.skipped && r.actions.isNotEmpty() } }
        assertThat(repo.observeToday().first().logs).isEmpty()
        assertThat(repo.observeToday().first().skippedSlotIds).containsExactly(almoco)
        assertThat(memory.read(today).facts).isEmpty()
        assertThat(ui.receipts().first { it.kind == ReceiptKind.LOGGED }.mark).isEqualTo(ReceiptMark.DELETED)
        assertThat(outcomes()).containsExactly(almoco, SkipOutcomes.DELETED)

        // A double tap never runs it twice.
        vm.confirmSkipDelete(prompt.answerId, almoco)
        val skip = ui.receipts().last { it.skipped }
        vm.receiptAction(skip.id, ReceiptAction.UNDO)
        val back = vm.await { it.receipts().any { r -> r.kind == ReceiptKind.RESTORED } }
        assertThat(repo.slotState(today.toString(), almoco).kcal).isEqualTo(640)
        assertThat(repo.observeToday().first().skippedSlotIds).isEmpty()
        assertThat(memory.read(today).facts.single().text).isEqualTo("arroz, feijão e frango")
        assertThat(back.receipts().last().actions).isNotEmpty()
        assertThat(telemetry.params(TelemetryEvents.SKIP_DELETE).map { it["action"] }).containsExactly("shown", "confirmed", "undone").inOrder()
    }

    @Test
    fun keepRecord_writesNothing_andMarksTheCard() = runBlocking<Unit> {
        val vm = vm()
        val almoco = slots()[2]
        recordAlmoco(vm, almoco)
        answer = { only("skip", listOf(almoco)) }
        send(vm, "não almocei")
        val prompt = vm.await { it.skipPrompt() != null }.skipPrompt()!!

        vm.keepRecord(prompt.answerId, almoco)
        val ui = vm.await { it.skipPrompt() == null && it.marks().isNotEmpty() }
        assertThat(ui.marks().single().kept).isTrue()
        assertThat(repo.slotState(today.toString(), almoco).kcal).isEqualTo(640)
        assertThat(repo.observeToday().first().skippedSlotIds).isEmpty()
        assertThat(telemetry.params(TelemetryEvents.SKIP_DELETE).map { it["action"] }).containsExactly("shown", "kept").inOrder()
    }

    /** Shows `Pular Almoço?`, runs [step] and expects the proposal expired: `Não registrado`, nothing skipped. */
    private suspend fun expiresAfter(step: suspend (ChatViewModel, Long) -> Unit) {
        val vm = vm()
        val almoco = slots()[2]
        recordAlmoco(vm, almoco)
        answer = { only("skip", listOf(almoco)) }
        send(vm, "não almocei")
        val answerId = vm.await { it.skipPrompt() != null }.skipPrompt()!!.answerId

        step(vm, almoco)
        val ui = vm.await { it.skipPrompt() == null && it.marks().any { m -> m.answerId == answerId } }
        assertThat(ui.marks().single { it.answerId == answerId }.kept).isFalse()
        withTimeout(5_000) { while (SkipOutcomes.decode(repo.message(answerId)!!.skipOutcomes)!!.entry(almoco)!!.outcome != SkipOutcomes.EXPIRED) delay(10) }
        assertThat(repo.observeToday().first().skippedSlotIds).isEmpty()
        assertThat(telemetry.params(TelemetryEvents.SKIP_DELETE).map { it["action"] }).containsExactly("shown", "expired").inOrder()
    }

    @Test
    fun proposal_expiresOnTheNextSend() = runBlocking<Unit> {
        expiresAfter { vm, _ ->
            answer = { ChatOut(reply = "ok", intent = "question", record = "none", skipSlots = emptyList(), model = "gpt-6-luna") }
            send(vm, "obrigado")
        }
    }

    @Test
    fun proposal_expiresOnADayChange() = runBlocking<Unit> {
        expiresAfter { vm, _ ->
            now = now.plus(Duration.ofDays(1))
            vm.tick()
        }
    }

    @Test
    fun proposal_expiresOnAWipe() = runBlocking<Unit> {
        expiresAfter { _, _ -> repo.wipeToday() }
    }

    @Test
    fun proposal_expiresWhenAnotherPathChangesTheSlot() = runBlocking<Unit> {
        expiresAfter { _, almoco -> db.mealLogDao().deleteBySlot(today.toString(), almoco) }
    }

    @Test
    fun theLogsConfirmationComesFirst_thenTheDeleteProposal() = runBlocking<Unit> {
        val vm = vm()
        val (_, cafe, almoco) = slots()
        recordAlmoco(vm, almoco)
        repo.addLog(window = "", text = "pão", kcal = 150, p = 4, stable = true, slotId = cafe)
        // The breakfast already has a record (chatU) and the lunch is skipped over its record (chatSD).
        answer = { logAndSkip(cafe, listOf(almoco)) }
        send(vm, "no café comi ovos, e não almocei")

        val first = vm.await { it.items.any { i -> i is ChatItem.ReplacePrompt } }
        assertThat(first.skipPrompt()).isNull()
        vm.confirmReplace(first.items.filterIsInstance<ChatItem.ReplacePrompt>().single().estimateId)
        val next = vm.await { it.items.none { i -> i is ChatItem.ReplacePrompt } && it.skipPrompt() != null }
        assertThat(next.skipPrompt()!!.confirm.slot.id).isEqualTo(almoco)
        // Below the answer's receipts, never between the answer and them.
        assertThat(next.items.last()).isInstanceOf(ChatItem.SkipDeletePrompt::class.java)
    }

    @Test
    fun recreationAndProcessDeath_neverApplyASkipTwice() = runBlocking<Unit> {
        val (pre, cafe, almoco) = slots()
        val first = vm()
        answer = { logAndSkip(cafe, listOf(pre)) }
        send(first, "pulei o pré, café com ovos")
        // A recreated screen reads the stored outcomes: nothing more.
        vm().await { it.receipts().size == 2 }
        assertThat(repo.observeMessages().first().count { it.role == "skipped" }).isEqualTo(1)

        // The process died after the answer was stored and before its skip: the next screen applies it once.
        val stored = repo.insertMessage(
            role = "assistant",
            text = "Almoço de hoje fora.",
            intent = "skip",
            recordMode = "auto",
            skipOutcomes = SkipOutcomes(date = today.toString(), wipeId = null, with = "skip", slots = listOf(SkipEntry(almoco))).encode(),
        )
        val a = vm()
        val b = vm()
        withTimeout(5_000) { while (SkipOutcomes.decode(repo.message(stored)!!.skipOutcomes)!!.slots.any { it.pending }) delay(10) }
        a.await { it.receipts().size == 3 }
        b.await { it.receipts().size == 3 }
        assertThat(repo.observeMessages().first().count { it.role == "skipped" }).isEqualTo(2)
        assertThat(repo.observeToday().first().skippedSlotIds).containsExactly(pre, almoco)
    }

    @Test
    fun aServerWithoutSkipSlots_keepsTheSingleSkipPath() = runBlocking<Unit> {
        val vm = vm()
        val pre = slots().first()
        answer = { ChatOut(reply = "Tudo bem.", intent = "skip", record = "auto", skipSlot = pre.toString(), model = "gpt-6-luna") }
        send(vm, "pulei o pré")
        withTimeout(5_000) { while (lastAnswer().recordState == null) delay(10) }

        assertThat(repo.observeToday().first().skippedSlotIds).containsExactly(pre)
        assertThat(lastAnswer().skipOutcomes).isNull()
        assertThat(lastAnswer().recordState).isEqualTo("recorded")
    }
}
