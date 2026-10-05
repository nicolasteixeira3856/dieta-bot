package app.fibrai.android.feature.chat

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import app.fibrai.android.core.database.ChatMessageEntity
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
import app.fibrai.android.core.photo.FakePhotoFiles
import app.fibrai.android.core.telemetry.FakeTelemetry
import app.fibrai.android.core.telemetry.TelemetryEvents
import app.fibrai.android.domain.MealProposal
import app.fibrai.android.domain.MealText
import app.fibrai.android.domain.ReceiptAction
import app.fibrai.android.domain.SlotRecord
import app.fibrai.android.domain.SlotState
import java.io.File
import java.io.IOException
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
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A47 (ADR-032, S18): additions and revisions through a fake /v1/chat and a real Room. Every test compares food text,
 * record counts and every affected slot, not only a total.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class ChatMealUpdatesTest {
    private lateinit var context: Context
    private lateinit var db: FibraiDatabase
    private lateinit var storeScope: CoroutineScope
    private lateinit var store: DataStore<Preferences>
    private lateinit var repo: DayRepository
    private val memoryFile = FakeMemoryFile()
    private val memory = FactMemory(memoryFile)
    private val photos = FakePhotoFiles()
    private val today = LocalDate.parse("2026-09-25")
    private var now = Instant.parse("2026-09-25T21:02:00-03:00")
    private val clock = InstantClock { now }
    private val requests = mutableListOf<ChatIn>()
    private var answer: suspend (ChatIn) -> ChatOut = { error("no answer") }
    private val service = ChatService { body ->
        requests += body
        answer(body)
    }
    private val telemetry = FakeTelemetry()

    private var cafe = 0L
    private var almoco = 0L
    private var lanche = 0L
    private var jantar = 0L

    /** Synthetic fixtures (S18 discovery situations rewritten): a lunch similar to the dinner, a dinner with a drink. */
    private val lunch = SlotRecord("macarrão com carne moída e salada", 650, 35, 80, 18)
    private val dinner = SlotRecord("arroz, feijão e omelete de 2 ovos; 1 lata de refrigerante", 380, 22, 40, 14)
    private val pudding = Addition("Pudim de leite, 1 fatia média (100 g)", 240, 6, 38, 7)
    private val composed = "${dinner.text}; ${pudding.text}"

    data class Addition(val text: String, val kcal: Int, val p: Int, val c: Int, val g: Int)

    @Before
    fun setUp() = runBlocking<Unit> {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, FibraiDatabase::class.java).allowMainThreadQueries().build()
        storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val file = File(context.cacheDir, "meal_${System.nanoTime()}.preferences_pb")
        store = PreferenceDataStoreFactory.create(scope = storeScope, produceFile = { file })
        repo = DayRepository(db, clock, store)
        repo.saveProfile("same", 2000, 2000, 2300, List(7) { 2000 }, "zero", 50, true, "2026-09-25")
        repo.saveSlots(
            listOf(
                MealSlot(name = "Café da manhã", minutesFromMidnight = 450),
                MealSlot(name = "Almoço", minutesFromMidnight = 750),
                MealSlot(name = "Lanche", minutesFromMidnight = 960),
                MealSlot(name = "Jantar", minutesFromMidnight = 1200),
            ),
        )
        val ids = repo.observeToday().first().slots.map { it.id }
        cafe = ids[0]; almoco = ids[1]; lanche = ids[2]; jantar = ids[3]
        seed(almoco, lunch)
        seed(jantar, dinner)
    }

    @After
    fun tearDown() {
        db.close()
        storeScope.cancel()
        Dispatchers.resetMain()
    }

    private suspend fun seed(slot: Long, r: SlotRecord) =
        repo.addLog(window = "", text = r.text, kcal = r.kcal, p = r.p, stable = true, slotId = slot, carbs = r.c, fat = r.g)

    private fun json(text: String): JsonElement = Json.parseToJsonElement(text)

    private fun additionJson(a: Addition) =
        """{"meal_text":${Json.encodeToString(String.serializer(), a.text)},"kcal":${a.kcal},"p":${a.p},"c":${a.c},"g":${a.g},""" +
            """"items":[{"name":"item","g":100,"kcal":${a.kcal}}]}"""

    /** What the S18 server answers for an addition, its consolidated numbers derived from the request's DAY. */
    private fun additionOut(
        body: ChatIn,
        a: Addition,
        target: Long?,
        record: String = "auto",
        memory: List<ChatMemoryUpdate> = emptyList(),
    ): ChatOut {
        val day = target?.let { id -> body.day.slots.single { it.id == id.toString() } }
        val base = day?.takeIf { it.status == "eaten" }
        val estimate = if (base != null) {
            ChatEstimate(
                kcal = (base.kcal!! + a.kcal).toDouble(), p = (base.p!! + a.p).toDouble(), c = (base.c!! + a.c).toDouble(), g = (base.g!! + a.g).toDouble(),
                confidence = "high", suggestedSlot = target.toString(), mealText = base.text + "; " + a.text,
            )
        } else {
            ChatEstimate(a.kcal.toDouble(), a.p.toDouble(), a.c.toDouble(), a.g.toDouble(), "high", suggestedSlot = target?.toString(), mealText = a.text)
        }
        return ChatOut(
            reply = "${a.text}: +${a.kcal} kcal",
            intent = "log",
            estimate = estimate,
            record = record,
            memoryUpdates = memory,
            model = "gpt-6-luna",
            mealChange = json("""{"operation":"add","base_slot":${base?.let { "\"${target}\"" } ?: "null"},"addition":${additionJson(a)}}"""),
        )
    }

    private fun revisionOut(text: String, kcal: Int, p: Int, c: Int, g: Int) = ChatOut(
        reply = "Atualizar Jantar?\nAntes: 380 kcal\nNovo total: $kcal kcal",
        intent = "log",
        estimate = ChatEstimate(kcal.toDouble(), p.toDouble(), c.toDouble(), g.toDouble(), "high", suggestedSlot = jantar.toString(), mealText = text),
        record = "auto",
        model = "gpt-6-luna",
        mealChange = json("""{"operation":"revise","base_slot":"$jantar","addition":null}"""),
    )

    private fun vm() = ChatViewModel(repo, service, clock, memory, photos, telemetry)

    private suspend fun ChatViewModel.await(pred: (ChatUiState) -> Boolean) = withTimeout(5_000) { uiState.first(pred) }

    private suspend fun until(pred: suspend () -> Boolean) = withTimeout(5_000) {
        while (!pred()) delay(10)
    }

    /** Sends and waits until the answer is stored and its automatic step (record, prompt or nothing) is done. */
    private suspend fun send(vm: ChatViewModel, text: String) {
        now = now.plusSeconds(60)
        val count = requests.size
        val rows = repo.observeMessages().first().count { it.role == "assistant" }
        vm.setComposer(text)
        vm.send()
        until { requests.size > count }
        vm.await { !it.sending }
        until {
            val answer = repo.observeMessages().first().lastOrNull { it.role == "assistant" }
            val stored = repo.observeMessages().first().count { it.role == "assistant" } > rows
            stored && (answer?.recordMode != "auto" || answer.recordState != null || MealProposal.decode(answer.mealChange)?.target == null)
        }
    }

    private suspend fun lastAssistant(): ChatMessageEntity = repo.observeMessages().first().last { it.role == "assistant" }

    private suspend fun states(date: String = "2026-09-25") = repo.slotStates(date)

    private suspend fun logsCount() = repo.observeToday().first().logs.size

    private fun ChatUiState.additionPrompt() = items.filterIsInstance<ChatItem.AdditionPrompt>().singleOrNull()
    private fun ChatUiState.revisionPrompt() = items.filterIsInstance<ChatItem.RevisionPrompt>().singleOrNull()
    private fun ChatUiState.receipts() = items.filterIsInstance<ChatItem.Receipt>()
    private fun ChatUiState.assistant(id: Long) = items.filterIsInstance<ChatItem.Assistant>().single { it.id == id }

    private suspend fun pendingDinnerAddition(vm: ChatViewModel, record: String = "auto"): Long {
        answer = { additionOut(it, pudding, jantar, record) }
        send(vm, "Também comi uma fatia de pudim de leite no jantar")
        vm.await { it.additionPrompt() != null }
        return lastAssistant().id
    }

    private fun rec(r: SlotRecord) = SlotState(listOf(r))

    // ------------------------------------------------------------------ the original meal

    @Test
    fun additionToTheOccupiedDinner_asksFirst_thenRecordsBasePlusDeltaExactlyOnce() = runBlocking<Unit> {
        val vm = vm()
        val id = pendingDinnerAddition(vm)

        val request = requests.single()
        assertThat(request.mealChanges).isTrue()
        assertThat(request.pendingAddition).isNull()
        val ui = vm.uiState.value
        val prompt = ui.additionPrompt()!!
        assertThat(prompt.estimateId).isEqualTo(id)
        assertThat(prompt.confirm.slot.name).isEqualTo("Jantar")
        assertThat(prompt.confirm.previousKcal).isEqualTo(380)
        assertThat(prompt.confirm.total).isEqualTo(app.fibrai.android.domain.Macros(620, 28, 78, 21))
        // The bubble shows the added food and +numbers from the structured fields, not the prose.
        val bot = ui.assistant(id)
        assertThat(bot.prose).isFalse()
        assertThat(bot.estimate!!.kind).isEqualTo(EstimateKind.ADDITION)
        assertThat(bot.estimate!!.label).isEqualTo(pudding.text)
        assertThat(bot.estimate!!.kcal).isEqualTo(240)
        // Right below its answer, no Registrar pill, nothing written yet.
        assertThat(ui.items[ui.items.indexOf(bot) + 1]).isEqualTo(prompt)
        assertThat(ui.actions).isNull()
        assertThat(states()).isEqualTo(mapOf(almoco to rec(lunch), jantar to rec(dinner)))
        assertThat(lastAssistant().recordState).isEqualTo("pending_add")

        // A double tap and a repeated confirmation apply it once.
        vm.confirmAddition(id)
        vm.confirmAddition(id)
        vm.await { it.receipts().any { r -> r.kind == ReceiptKind.REPLACED } }
        vm.confirmAddition(id)
        until { lastAssistant().recordState == "recorded" }

        val after = states()
        assertThat(after[almoco]).isEqualTo(rec(lunch))
        assertThat(after[lanche]).isNull()
        assertThat(after[cafe]).isNull()
        assertThat(after.getValue(jantar).records).containsExactly(SlotRecord(composed, 620, 28, 78, 21, "user"))
        assertThat(logsCount()).isEqualTo(2)
        val receipt = vm.uiState.value.receipts().single()
        assertThat(receipt.slotName).isEqualTo("Jantar")
        assertThat(receipt.fromKcal).isEqualTo(380)
        assertThat(receipt.kcal).isEqualTo(620)
        assertThat(vm.uiState.value.additionPrompt()).isNull()

        // The receipt, the stored description and the next outgoing DAY agree.
        answer = { ChatOut(reply = "ok", intent = "question", record = "none", model = "gpt-6-luna", mealChange = json("null")) }
        send(vm, "obrigado")
        val day = requests.last().day
        val dinnerDay = day.slots.single { it.id == jantar.toString() }
        assertThat(dinnerDay.text).isEqualTo(composed)
        assertThat(dinnerDay.kcal).isEqualTo(620)
        assertThat(day.eatenKcal).isEqualTo(650 + 620)
        assertThat(requests.last().pendingAddition).isNull()
        assertThat(telemetry.events.filter { it.first == TelemetryEvents.MEAL_UPDATE }.map { it.second["action"] }).containsAtLeast("shown", "confirmed")
    }

    // ------------------------------------------------------------------ another destination

    @Test
    fun escolherOutraRefeicao_emptyTarget_recordsOnlyTheAddition_sourceUntouched_excluirLeavesTheSource() = runBlocking<Unit> {
        val vm = vm()
        val id = pendingDinnerAddition(vm)

        vm.additionElsewhere(id)
        val sheet = vm.await { it.sheetFor == id }
        assertThat(sheet.sheetSelection).isNull()
        assertThat(sheet.sheetCurrent).isNull()
        assertThat(sheet.sheetAddition).isEqualTo(SheetAddition(pudding.text, 240, "Jantar"))
        vm.selectInSheet(lanche)
        vm.confirmSheet()
        val ui = vm.await { it.receipts().any { r -> r.slotName == "Lanche" } }

        val after = states()
        assertThat(after.getValue(lanche).records).containsExactly(SlotRecord(pudding.text, 240, 6, 38, 7, "user"))
        assertThat(after[jantar]).isEqualTo(rec(dinner))
        assertThat(after[almoco]).isEqualTo(rec(lunch))
        assertThat(logsCount()).isEqualTo(3)
        assertThat(ui.receipts().single().kind).isEqualTo(ReceiptKind.LOGGED)
        assertThat(ui.receipts().single().kcal).isEqualTo(240)
        assertThat(ui.additionPrompt()).isNull()

        val receipt = vm.await { it.receipts().single().actions.isNotEmpty() }.receipts().single()
        vm.receiptAction(receipt.id, ReceiptAction.DELETE)
        vm.await { it.receipts().single().mark == ReceiptMark.DELETED }
        assertThat(states()).isEqualTo(mapOf(almoco to rec(lunch), jantar to rec(dinner)))
    }

    @Test
    fun rerouteToASkippedMeal_clearsTheSkip_desfazerBringsTheSkipBack_andNeverTouchesTheSource() = runBlocking<Unit> {
        repo.addSkip(lanche)
        val vm = vm()
        val id = pendingDinnerAddition(vm)
        vm.additionElsewhere(id)
        vm.selectInSheet(lanche)
        vm.confirmSheet()
        val receipt = vm.await { it.receipts().singleOrNull()?.actions?.isNotEmpty() == true }.receipts().single()
        assertThat(states().getValue(lanche)).isEqualTo(rec(SlotRecord(pudding.text, 240, 6, 38, 7, "user")))
        assertThat(receipt.actions.first()).isEqualTo(ReceiptAction.UNDO)

        vm.receiptAction(receipt.id, ReceiptAction.UNDO)
        vm.await { it.receipts().first().mark == ReceiptMark.UNDONE }
        assertThat(states()).isEqualTo(mapOf(almoco to rec(lunch), jantar to rec(dinner), lanche to SlotState.SKIPPED))
    }

    @Test
    fun rerouteToAnotherOccupiedMeal_asksWithThatMealsNumbers_thenAddsThere_desfazerRestoresIt() = runBlocking<Unit> {
        val vm = vm()
        val id = pendingDinnerAddition(vm)
        vm.additionElsewhere(id)
        vm.selectInSheet(almoco)
        vm.confirmSheet()
        val ui = vm.await { it.additionPrompt()?.confirm?.slot?.name == "Almoço" }
        // One active proposal: the dinner confirmation is gone, the lunch one shows the lunch base + the same addition.
        val prompt = ui.additionPrompt()!!
        assertThat(prompt.confirm.previousKcal).isEqualTo(650)
        assertThat(prompt.confirm.total).isEqualTo(app.fibrai.android.domain.Macros(890, 41, 118, 25))
        assertThat(states()).isEqualTo(mapOf(almoco to rec(lunch), jantar to rec(dinner)))
        val stored = MealProposal.decode(lastAssistant().mealChange)!!
        assertThat(stored.chosenSlotId).isEqualTo(almoco)
        assertThat(stored.chosen).isEqualTo(rec(lunch))
        assertThat(stored.source).isEqualTo(rec(dinner))

        vm.confirmAddition(id)
        val receipt = vm.await { it.receipts().singleOrNull()?.actions?.isNotEmpty() == true }.receipts().single()
        assertThat(receipt.slotName).isEqualTo("Almoço")
        assertThat(states().getValue(almoco).records).containsExactly(SlotRecord("${lunch.text}; ${pudding.text}", 890, 41, 118, 25, "user"))
        assertThat(states()[jantar]).isEqualTo(rec(dinner))

        vm.receiptAction(receipt.id, ReceiptAction.UNDO)
        vm.await { it.receipts().any { r -> r.kind == ReceiptKind.RESTORED } }
        assertThat(states()).isEqualTo(mapOf(almoco to rec(lunch), jantar to rec(dinner)))
    }

    @Test
    fun pickingTheSourceAgain_returnsToItsOwnConfirmation() = runBlocking<Unit> {
        val vm = vm()
        val id = pendingDinnerAddition(vm)
        vm.additionElsewhere(id)
        vm.selectInSheet(almoco)
        vm.confirmSheet()
        vm.await { it.additionPrompt()?.confirm?.slot?.name == "Almoço" }
        vm.additionElsewhere(id)
        vm.selectInSheet(jantar)
        vm.confirmSheet()
        vm.await { it.additionPrompt()?.confirm?.slot?.name == "Jantar" }
        assertThat(MealProposal.decode(lastAssistant().mealChange)!!.chosenSlotId).isNull()
        assertThat(states()).isEqualTo(mapOf(almoco to rec(lunch), jantar to rec(dinner)))
    }

    // ------------------------------------------------------------------ stale proposals

    @Test
    fun sourceChangedWhileWaiting_expiresTheProposal_rerouteWritesNothing() = runBlocking<Unit> {
        val vm = vm()
        val id = pendingDinnerAddition(vm)
        val edited = SlotRecord("sopa", 200, 10, 20, 5)
        repo.replaceSlotLog(jantar, edited.text, edited.kcal, edited.p, edited.c, edited.g)
        vm.await { it.additionPrompt() == null && it.assistant(id).notRecorded }
        until { lastAssistant().recordState == "not_recorded" }

        vm.additionElsewhere(id)
        vm.selectInSheet(lanche)
        vm.confirmSheet()
        vm.confirmAddition(id)
        delay(100)
        assertThat(states()).isEqualTo(mapOf(almoco to rec(lunch), jantar to rec(edited)))
        assertThat(vm.uiState.value.receipts()).isEmpty()
    }

    @Test
    fun sourceChangedJustBeforeTheReroute_theTransactionChecksIt_andWritesNothing() = runBlocking<Unit> {
        val vm = vm()
        val id = pendingDinnerAddition(vm)
        vm.additionElsewhere(id)
        vm.selectInSheet(lanche)
        // The dinner changes between the render and the transaction: only the read-only check can see it.
        val edited = SlotRecord("sopa", 200, 10, 20, 5)
        db.mealLogDao().deleteBySlot("2026-09-25", jantar)
        seed(jantar, edited)
        vm.confirmSheet()
        until { lastAssistant().recordState == "not_recorded" }
        assertThat(states()).isEqualTo(mapOf(almoco to rec(lunch), jantar to rec(edited)))
    }

    @Test
    fun chosenDestinationChanged_beforeAdicionar_writesNothing() = runBlocking<Unit> {
        val vm = vm()
        val id = pendingDinnerAddition(vm)
        vm.additionElsewhere(id)
        vm.selectInSheet(almoco)
        vm.confirmSheet()
        vm.await { it.additionPrompt()?.confirm?.slot?.name == "Almoço" }
        seed(almoco, SlotRecord("suco", 100, 1, 24, 0))
        vm.confirmAddition(id)
        until { lastAssistant().recordState == "not_recorded" }
        assertThat(states().getValue(almoco).records.map { it.text }).containsExactly(lunch.text, "suco").inOrder()
        assertThat(states()[jantar]).isEqualTo(rec(dinner))
    }

    @Test
    fun answerArrivingAfterTheSourceChanged_isNotRecorded_noRoutine() = runBlocking<Unit> {
        val vm = vm()
        val edited = SlotRecord("sopa", 200, 10, 20, 5)
        val routine = ChatMemoryUpdate("add", null, "dynamic", "routine", "jantar", "arroz, feijão e pudim", jantar.toString())
        answer = { body ->
            repo.replaceSlotLog(jantar, edited.text, edited.kcal, edited.p, edited.c, edited.g)
            additionOut(body, pudding, jantar, memory = listOf(routine))
        }
        send(vm, "também comi pudim no jantar")
        val ui = vm.await { it.items.any { i -> i is ChatItem.Assistant && i.notRecorded } }
        assertThat(ui.additionPrompt()).isNull()
        assertThat(lastAssistant().recordState).isEqualTo("not_recorded")
        assertThat(lastAssistant().pendingMemory).isNull()
        assertThat(states()).isEqualTo(mapOf(almoco to rec(lunch), jantar to rec(edited)))
        assertThat(memory.read(today).facts).isEmpty()
    }

    @Test
    fun dayRollover_andWipe_expireTheProposal() = runBlocking<Unit> {
        val vm = vm()
        val id = pendingDinnerAddition(vm)
        repo.wipeToday()
        vm.confirmAddition(id)
        until { lastAssistant().recordState == "not_recorded" }
        assertThat(states()).isEmpty()

        val vm2 = vm()
        seed(jantar, dinner)
        val second = pendingDinnerAddition(vm2)
        now = Instant.parse("2026-09-26T00:05:00-03:00")
        vm2.tick()
        vm2.confirmAddition(second)
        delay(100)
        assertThat(states()).isEqualTo(mapOf(jantar to rec(dinner)))
        assertThat(states("2026-09-26")).isEmpty()
    }

    // ------------------------------------------------------------------ metadata

    @Test
    fun malformedMetadata_isNotRecorded_noCard_noMemory_evenWithRecordAuto() = runBlocking<Unit> {
        val vm = vm()
        answer = { body ->
            additionOut(body, pudding, jantar).copy(
                mealChange = json("""{"operation":"add","base_slot":"$jantar"}"""),
                memoryUpdates = listOf(ChatMemoryUpdate("add", null, "permanent", "preference", "leite", "Leite semidesnatado")),
            )
        }
        send(vm, "também comi pudim")
        val ui = vm.await { it.items.any { i -> i is ChatItem.Assistant && i.notRecorded } }
        val bot = ui.items.filterIsInstance<ChatItem.Assistant>().single()
        assertThat(bot.estimate).isNull()
        assertThat(bot.prose).isTrue()
        assertThat(ui.additionPrompt()).isNull()
        assertThat(ui.actions).isNull()
        assertThat(memoryFile.writes).isEqualTo(0)
        assertThat(states()).isEqualTo(mapOf(almoco to rec(lunch), jantar to rec(dinner)))
        assertThat(MealProposal.decode(lastAssistant().mealChange)!!.reason).isEqualTo(MealProposal.MALFORMED)
    }

    @Test
    fun contradictingTotal_isNotRecorded() = runBlocking<Unit> {
        val vm = vm()
        answer = { body -> additionOut(body, pudding, jantar).let { it.copy(estimate = it.estimate!!.copy(kcal = 850.0)) } }
        send(vm, "também comi pudim")
        vm.await { it.items.any { i -> i is ChatItem.Assistant && i.notRecorded } }
        assertThat(MealProposal.decode(lastAssistant().mealChange)!!.reason).isEqualTo(MealProposal.CONTRADICTS)
        assertThat(states()).isEqualTo(mapOf(almoco to rec(lunch), jantar to rec(dinner)))
    }

    @Test
    fun serverWithoutTheCapability_keepsTheLegacyWholeMealReplace() = runBlocking<Unit> {
        val vm = vm()
        answer = { body -> additionOut(body, pudding, jantar).copy(mealChange = null) }
        send(vm, "também comi pudim")
        val ui = vm.await { it.items.any { i -> i is ChatItem.ReplacePrompt } }
        assertThat(ui.additionPrompt()).isNull()
        assertThat(lastAssistant().mealChange).isNull()
        assertThat(lastAssistant().recordState).isEqualTo("pending_replace")
        assertThat(ui.items.filterIsInstance<ChatItem.Assistant>().single().estimate!!.kind).isEqualTo(EstimateKind.MEAL)
    }

    // ------------------------------------------------------------------ revisions

    @Test
    fun revision_cancelar_changesNothing_atualizar_replacesTheWholeMeal() = runBlocking<Unit> {
        val vm = vm()
        answer = { revisionOut("Arroz, feijão e omelete de 3 ovos", 455, 28, 40, 19) }
        send(vm, "corrigindo: o omelete foi de 3 ovos")
        val ui = vm.await { it.revisionPrompt() != null }
        val first = lastAssistant().id
        val prompt = ui.revisionPrompt()!!
        assertThat(prompt.confirm).isEqualTo(RevisionConfirm(prompt.confirm.slot, 380, 455))
        assertThat(prompt.confirm.slot.name).isEqualTo("Jantar")
        val bot = ui.assistant(first)
        assertThat(bot.estimate!!.kind).isEqualTo(EstimateKind.REVISION)
        assertThat(bot.estimate!!.label).isEqualTo("Arroz, feijão e omelete de 3 ovos")
        assertThat(bot.prose).isFalse()

        vm.cancelRevision(first)
        vm.await { it.revisionPrompt() == null && it.assistant(first).notRecorded }
        assertThat(states()).isEqualTo(mapOf(almoco to rec(lunch), jantar to rec(dinner)))
        assertThat(memoryFile.writes).isEqualTo(0)

        send(vm, "corrigindo de novo: 3 ovos")
        val second = vm.await { it.revisionPrompt() != null }.revisionPrompt()!!.estimateId
        vm.confirmRevision(second)
        vm.await { it.receipts().any { r -> r.kind == ReceiptKind.REPLACED } }
        assertThat(states()).isEqualTo(
            mapOf(almoco to rec(lunch), jantar to rec(SlotRecord("Arroz, feijão e omelete de 3 ovos", 455, 28, 40, 19, "user"))),
        )
    }

    // ------------------------------------------------------------------ continuation (pending_addition)

    @Test
    fun followUpDinnerIncident_pendingAdditionGoesBack_thenBasePlusDeltaOnce_lunchAndSnackUntouched() = runBlocking<Unit> {
        val vm = vm()
        val first = pendingDinnerAddition(vm)
        // The user confirms the dinner and repeats the quantity. The pending addition is captured before it expires.
        answer = { additionOut(it, pudding, jantar) }
        send(vm, "foi no jantar mesmo, 1 fatia de 100 g")
        val sent = requests.last().pendingAddition!!
        assertThat(sent.baseSlot).isEqualTo(jantar.toString())
        assertThat(sent.addition.mealText).isEqualTo(pudding.text)
        assertThat(sent.addition.kcal).isEqualTo(240)
        // The DAY of that request still carries the original dinner base, not the proposal.
        assertThat(requests.last().day.slots.single { it.id == jantar.toString() }.text).isEqualTo(dinner.text)
        assertThat(repo.message(first)!!.recordState).isEqualTo("not_recorded")

        val second = vm.await { it.additionPrompt()?.estimateId != null && it.additionPrompt()!!.estimateId != first }.additionPrompt()!!
        vm.confirmAddition(second.estimateId)
        vm.confirmAddition(first)
        vm.await { it.receipts().isNotEmpty() }
        until { lastAssistant().recordState == "recorded" }
        assertThat(states()).isEqualTo(mapOf(almoco to rec(lunch), jantar to rec(SlotRecord(composed, 620, 28, 78, 21, "user"))))

        answer = { ChatOut(reply = "ok", intent = "question", record = "none", model = "gpt-6-luna", mealChange = json("null")) }
        send(vm, "valeu")
        val day = requests.last().day
        val dinnerText = day.slots.single { it.id == jantar.toString() }.text!!
        assertThat(Regex("Pudim").findAll(dinnerText).count()).isEqualTo(1)
        assertThat(day.slots.single { it.id == almoco.toString() }.text).isEqualTo(lunch.text)
        assertThat(day.slots.single { it.id == lanche.toString() }.status).isEqualTo("empty")
        assertThat(day.eatenKcal).isEqualTo(650 + 620)
        assertThat(requests.last().pendingAddition).isNull()
    }

    @Test
    fun pendingAddition_notSentAfterRecord_cancelOrASourceChange_keptOnRetry() = runBlocking<Unit> {
        val vm = vm()
        val id = pendingDinnerAddition(vm)
        // Retry: the first attempt fails after taking the context; the retry carries the same one.
        answer = { throw IOException("timeout") }
        vm.setComposer("era pudim de leite condensado")
        now = now.plusSeconds(60)
        vm.send()
        vm.await { it.items.any { i -> i is ChatItem.Failed } }
        assertThat(requests.last().pendingAddition!!.baseSlot).isEqualTo(jantar.toString())
        assertThat(repo.message(id)!!.recordState).isEqualTo("not_recorded")
        answer = { additionOut(it, pudding, jantar) }
        vm.retry()
        vm.await { it.additionPrompt() != null && !it.sending }
        assertThat(requests.last().pendingAddition!!.addition.mealText).isEqualTo(pudding.text)

        // Recorded: never sent again.
        val second = lastAssistant().id
        vm.confirmAddition(second)
        until { repo.message(second)!!.recordState == "recorded" }
        answer = { ChatOut(reply = "ok", intent = "question", record = "none", model = "gpt-6-luna", mealChange = json("null")) }
        send(vm, "ok")
        assertThat(requests.last().pendingAddition).isNull()

        // Source changed by another path: not sent.
        answer = { additionOut(it, pudding, almoco) }
        send(vm, "e um pudim no almoço")
        vm.await { it.additionPrompt()?.confirm?.slot?.name == "Almoço" }
        seed(almoco, SlotRecord("café", 5, 0, 1, 0))
        answer = { ChatOut(reply = "ok", intent = "question", record = "none", model = "gpt-6-luna", mealChange = json("null")) }
        send(vm, "ok")
        assertThat(requests.last().pendingAddition).isNull()
    }

    // ------------------------------------------------------------------ recreation, receipts, memory

    @Test
    fun recreation_keepsThePersistedProposal_andConfirmsOnce() = runBlocking<Unit> {
        val id = pendingDinnerAddition(vm())
        val recreated = vm()
        recreated.await { it.additionPrompt()?.estimateId == id }
        recreated.confirmAddition(id)
        recreated.await { it.receipts().isNotEmpty() }
        assertThat(states().getValue(jantar).records).containsExactly(SlotRecord(composed, 620, 28, 78, 21, "user"))
        assertThat(logsCount()).isEqualTo(2)
    }

    @Test
    fun additionReceipt_editarAndTrocarRefeicao_keepTheirWholeRecordMeaning() = runBlocking<Unit> {
        val vm = vm()
        val id = pendingDinnerAddition(vm)
        vm.confirmAddition(id)
        val receipt = vm.await { it.receipts().singleOrNull()?.actions?.isNotEmpty() == true }.receipts().single()
        assertThat(receipt.actions).containsExactly(ReceiptAction.UNDO, ReceiptAction.DELETE, ReceiptAction.MOVE, ReceiptAction.EDIT).inOrder()

        // Trocar refeição moves the consolidated meal, base and addition together.
        vm.receiptAction(receipt.id, ReceiptAction.MOVE)
        vm.await { it.sheetFor == receipt.id }
        assertThat(vm.uiState.value.sheetAddition).isNull()
        vm.selectInSheet(lanche)
        vm.confirmSheet()
        val moved = vm.await { it.receipts().lastOrNull()?.kind == ReceiptKind.MOVED && it.receipts().last().actions.isNotEmpty() }.receipts().last()
        assertThat(states()).isEqualTo(mapOf(almoco to rec(lunch), lanche to rec(SlotRecord(composed, 620, 28, 78, 21, "user"))))

        // Editar removes the consolidated record and puts its whole text in the composer.
        vm.receiptAction(moved.id, ReceiptAction.EDIT)
        val ui = vm.await { it.composer == composed }
        assertThat(ui.composer).isEqualTo(composed)
        assertThat(states()).isEqualTo(mapOf(almoco to rec(lunch)))
    }

    @Test
    fun routine_isAppliedOnceAfterTheTransaction_toTheFinalMeal_notTheAbandonedSource() = runBlocking<Unit> {
        val vm = vm()
        val routine = ChatMemoryUpdate("add", null, "dynamic", "routine", "sobremesa", "pudim de leite", jantar.toString())
        answer = { additionOut(it, pudding, jantar, memory = listOf(routine)) }
        send(vm, "também comi pudim no jantar")
        val id = vm.await { it.additionPrompt() != null }.additionPrompt()!!.estimateId
        // Nothing before the record.
        assertThat(memory.read(today).facts).isEmpty()
        vm.additionElsewhere(id)
        vm.selectInSheet(lanche)
        vm.confirmSheet()
        vm.await { it.receipts().singleOrNull()?.memoryUpdated == true }
        val fact = memory.read(today).facts.single()
        assertThat(fact.slot).isEqualTo(lanche.toString())
        assertThat(fact.kcal).isEqualTo(240)
        vm.confirmAddition(id)
        delay(100)
        assertThat(memory.read(today).facts).hasSize(1)
    }

    // ------------------------------------------------------------------ targets and descriptions

    @Test
    fun additionToAnEmptyMeal_auto_recordsOnlyTheAddition() = runBlocking<Unit> {
        val vm = vm()
        answer = { additionOut(it, pudding, lanche) }
        send(vm, "comi um pudim no lanche")
        val ui = vm.await { it.receipts().isNotEmpty() }
        assertThat(ui.receipts().single().kind).isEqualTo(ReceiptKind.LOGGED)
        assertThat(states()).isEqualTo(mapOf(almoco to rec(lunch), jantar to rec(dinner), lanche to rec(SlotRecord(pudding.text, 240, 6, 38, 7, "user"))))
    }

    @Test
    fun unknownTarget_registrarOpensThePickerWithNothingPicked_anOccupiedChoiceAsksFirst() = runBlocking<Unit> {
        val vm = vm()
        answer = { additionOut(it, pudding, null, record = "ask") }
        send(vm, "comi um pudim")
        val actions = vm.await { it.actions != null }.actions!!
        assertThat(actions.record).isNull()
        vm.register(actions.estimateId)
        val sheet = vm.await { it.sheetFor == actions.estimateId }
        assertThat(sheet.sheetSelection).isNull()
        assertThat(sheet.sheetCurrent).isNull()
        vm.selectInSheet(jantar)
        vm.confirmSheet()
        val prompt = vm.await { it.additionPrompt() != null }.additionPrompt()!!
        assertThat(prompt.confirm.slot.name).isEqualTo("Jantar")
        assertThat(prompt.confirm.total.kcal).isEqualTo(620)
        assertThat(states()).isEqualTo(mapOf(almoco to rec(lunch), jantar to rec(dinner)))
        vm.confirmAddition(actions.estimateId)
        vm.await { it.receipts().isNotEmpty() }
        assertThat(states().getValue(jantar)).isEqualTo(rec(SlotRecord(composed, 620, 28, 78, 21, "user")))
    }

    @Test
    fun rerouteThatWouldPassTheBound_isNotCut_noteShown_nothingWritten() = runBlocking<Unit> {
        db.mealLogDao().deleteBySlot("2026-09-25", almoco)
        val longLunch = SlotRecord("🍝".repeat(1990), 650, 35, 80, 18)
        seed(almoco, longLunch)
        val vm = vm()
        val id = pendingDinnerAddition(vm)
        vm.additionElsewhere(id)
        vm.selectInSheet(almoco)
        vm.confirmSheet()
        vm.await { it.notice == ChatViewModel.OVERFLOW_NOTICE }
        assertThat(states()).isEqualTo(mapOf(almoco to rec(longLunch), jantar to rec(dinner)))
        // The proposal stays open for its original meal.
        assertThat(vm.uiState.value.additionPrompt()!!.confirm.slot.name).isEqualTo("Jantar")
    }

    @Test
    fun composedDescriptionAtTheBound_withSupplementaryCharacters_roundTripsComplete() = runBlocking<Unit> {
        db.mealLogDao().deleteBySlot("2026-09-25", jantar)
        // 1950 + "; " + 48 = 2000 code points, 3950 UTF-16 units.
        val base = SlotRecord("🍚".repeat(1950), 380, 22, 40, 14)
        seed(jantar, base)
        val addition = Addition("🍮".repeat(47) + "x", 240, 6, 38, 7)
        val vm = vm()
        answer = { additionOut(it, addition, jantar) }
        send(vm, "e o pudim")
        val id = vm.await { it.additionPrompt() != null }.additionPrompt()!!.estimateId
        vm.confirmAddition(id)
        vm.await { it.receipts().isNotEmpty() }
        val stored = states().getValue(jantar).records.single().text
        assertThat(MealText.length(stored)).isEqualTo(2000)
        assertThat(stored).isEqualTo(base.text + "; " + addition.text)
        answer = { ChatOut(reply = "ok", intent = "question", record = "none", model = "gpt-6-luna", mealChange = json("null")) }
        send(vm, "ok")
        assertThat(requests.last().day.slots.single { it.id == jantar.toString() }.text).isEqualTo(stored)
    }
}
