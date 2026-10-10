package app.fibrai.android.feature.chat

import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import app.fibrai.android.core.database.DayRepository
import app.fibrai.android.core.database.InstantClock
import app.fibrai.android.core.database.MealLogEntity
import app.fibrai.android.core.database.MealSlot
import app.fibrai.android.core.database.FibraiDatabase
import app.fibrai.android.core.memory.FakeMemoryFile
import app.fibrai.android.core.memory.FactMemory
import app.fibrai.android.core.photo.FakePhotoFiles
import app.fibrai.android.core.network.ChatEstimate
import app.fibrai.android.core.network.ChatIn
import app.fibrai.android.core.network.ChatMemoryUpdate
import app.fibrai.android.core.network.ChatOut
import app.fibrai.android.core.network.ItemOut
import app.fibrai.android.core.telemetry.ChatFallback
import app.fibrai.android.core.telemetry.FakeTelemetry
import app.fibrai.android.core.telemetry.RequestIds
import app.fibrai.android.core.telemetry.TelemetryEvents
import app.fibrai.android.domain.Fact
import app.fibrai.android.domain.Macros
import app.fibrai.android.domain.Memory
import app.fibrai.android.domain.MemoryUpdate
import app.fibrai.android.domain.NextIds
import app.fibrai.android.domain.ReceiptAction
import app.fibrai.android.domain.RecordedMeal
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class ChatViewModelTest {
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
    private var answer: () -> ChatOut = { estimateOut("1") }

    private val service = ChatService { body ->
        requests += body
        answer()
    }

    @Before
    fun setUp() = runBlocking<Unit> {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, FibraiDatabase::class.java).allowMainThreadQueries().build()
        storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val file = File(context.cacheDir, "chat_${System.nanoTime()}.preferences_pb")
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
    }

    @After
    fun tearDown() {
        db.close()
        storeScope.cancel()
        Dispatchers.resetMain()
    }

    private fun estimateOut(slot: String?) = ChatOut(
        reply = "Identifiquei 2 pães franceses e 2 ovos mexidos.",
        estimate = ChatEstimate(
            kcal = 380.4, p = 22.0, c = 36.0, g = 16.0, confidence = "high",
            items = listOf(ItemOut("2 pães franceses"), ItemOut("2 ovos mexidos")),
            suggestedSlot = slot,
        ),
        model = "gpt-6-luna",
    )

    private suspend fun ChatViewModel.await(pred: (ChatUiState) -> Boolean) = withTimeout(5_000) { uiState.first(pred) }

    private suspend fun sendAndAwait(vm: ChatViewModel, text: String) {
        val calls = requests.size
        vm.setComposer(text)
        vm.send()
        // The state from before the send may still carry the previous answer actions: wait for this turn first.
        withTimeout(5_000) { while (requests.size == calls) delay(10) }
        vm.await { !it.sending && it.actions != null || it.items.any { i -> i is ChatItem.Failed } }
    }

    private suspend fun slotIds() = repo.observeToday().first().slots.map { it.id }

    @Test
    fun send_storesBothMessages_andOffersGravarForSuggestedSlot() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val cafe = slotIds().first()
        answer = { estimateOut(cafe.toString()) }
        sendAndAwait(vm, "2 pães franceses com 2 ovos mexidos")

        assertThat(requests).hasSize(1)
        assertThat(requests.single().text).isEqualTo("2 pães franceses com 2 ovos mexidos")
        assertThat(requests.single().compact).isFalse()
        val ui = vm.uiState.value
        assertThat(ui.actions!!.record!!.name).isEqualTo("Café da manhã")
        val bot = ui.items.filterIsInstance<ChatItem.Assistant>().single()
        assertThat(bot.estimate!!.kcal).isEqualTo(380)
        assertThat(bot.estimate!!.slotQuestion).isEqualTo("Deseja registrar essa refeição no Café da manhã?")
        assertThat(bot.highlights).containsExactly("2 pães franceses", "2 ovos mexidos")
        // No tap, no count (spec rule 6).
        assertThat(repo.observeToday().first().logs).isEmpty()
    }

    @Test
    fun gravar_addsLog_withoutSecondPost_andClosesActions() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val cafe = slotIds().first()
        answer = { estimateOut(cafe.toString()) }
        sendAndAwait(vm, "2 pães franceses com 2 ovos mexidos")
        val estimateId = vm.uiState.value.actions!!.estimateId

        vm.register(estimateId)
        vm.await { it.actions == null && it.items.any { i -> i is ChatItem.Receipt } }

        assertThat(requests).hasSize(1)
        val log = repo.observeToday().first().logs.single()
        assertThat(log.slotId).isEqualTo(cafe)
        assertThat(log.kcal).isEqualTo(380)
        assertThat(log.carbs).isEqualTo(36)
        assertThat(log.fat).isEqualTo(16)
        assertThat(log.text).isEqualTo("2 pães franceses com 2 ovos mexidos")
        val receipt = vm.uiState.value.items.filterIsInstance<ChatItem.Receipt>().single()
        assertThat(receipt.slotName).isEqualTo("Café da manhã")
        assertThat(receipt.slotTime).isEqualTo("07:30")
        assertThat(receipt.kcal).isEqualTo(380)
    }

    @Test
    fun suggestedSlotOutsideProfile_registrarOpensTrocarEmpty_recordsIntoChosenSlot() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val lanche = slotIds()[2]
        answer = { estimateOut("cafe") }
        sendAndAwait(vm, "pão com ovo")
        val actions = vm.uiState.value.actions!!
        assertThat(actions.record).isNull()
        assertThat(actions.plan).isFalse()

        vm.register(actions.estimateId)
        val sheet = vm.await { it.sheetFor != null }
        assertThat(sheet.sheetSelection).isNull()
        // The slot of the hour (08:10 → Café da manhã) is the one marked "(atual)".
        assertThat(sheet.sheetCurrent).isEqualTo(slotIds().first())
        vm.selectInSheet(lanche)
        vm.confirmSheet()
        vm.await { it.actions == null && it.sheetFor == null && it.items.any { i -> i is ChatItem.Receipt } }

        assertThat(repo.observeToday().first().logs.single().slotId).isEqualTo(lanche)
        assertThat(requests).hasSize(1)
    }

    @Test
    fun failure_leavesRoomUntouched_andRetryRecovers() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        answer = { throw IOException("timeout") }
        sendAndAwait(vm, "pão com ovo")

        assertThat(vm.uiState.value.items.last()).isEqualTo(ChatItem.Failed)
        assertThat(repo.observeMessages().first()).isEmpty()
        assertThat(repo.observeToday().first().logs).isEmpty()

        answer = { estimateOut(null) }
        vm.retry()
        vm.await { it.actions != null }
        assertThat(repo.observeMessages().first().map { it.role }).containsExactly("user", "assistant").inOrder()
        assertThat(requests).hasSize(2)
    }

    /** CP2 refusal (HTTP 200, fixed reply): a plain bubble, no action card, no memory change (CP4 check). */
    @Test
    fun contentRefusal_isPlainBubble_withoutActionsOrMemory() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val refusal = "Posso ajudar com refeições, porções e o orçamento alimentar do dia."
        answer = { ChatOut(reply = refusal, intent = "question", model = "gpt-6-luna") }
        vm.setComposer("me conta uma piada")
        vm.send()
        vm.await { it.items.any { i -> i is ChatItem.Assistant } && !it.sending }

        val ui = vm.uiState.value
        val bot = ui.items.filterIsInstance<ChatItem.Assistant>().single()
        assertThat(bot.text).isEqualTo(refusal)
        assertThat(bot.estimate).isNull()
        assertThat(bot.memory).isEqualTo(MemoryNotice())
        assertThat(ui.actions).isNull()
        assertThat(ui.items.filterIsInstance<ChatItem.Question>()).isEmpty()
        assertThat(memoryFile.read()).isNull()
        assertThat(repo.observeToday().first().logs).isEmpty()
    }

    /** CP2/CP3 400 and 503: the retry state, no raw code shown, no automatic retry (CP4 check). */
    @Test
    fun http400And503_showRetryState_withoutCodeOrAutoRetry() = runBlocking<Unit> {
        listOf(400 to "invalid_client_instance_id", 503 to "moderation_unavailable").forEach { (code, detail) ->
            requests.clear()
            val vm = ChatViewModel(repo, service, clock, memory, photos)
            answer = {
                throw retrofit2.HttpException(
                    retrofit2.Response.error<ChatOut>(code, """{"detail":"$detail"}""".toResponseBody("application/json".toMediaType())),
                )
            }
            sendAndAwait(vm, "pão com ovo")
            delay(200)

            val ui = vm.uiState.value
            assertThat(ui.items.last()).isEqualTo(ChatItem.Failed)
            assertThat(ui.items.filterIsInstance<ChatItem.Assistant>()).isEmpty()
            assertThat(ui.toString()).doesNotContain(detail)
            assertThat(requests).hasSize(1)
            assertThat(repo.observeMessages().first()).isEmpty()
        }
    }

    /** A18 / ADR-017: 4 esfihas in the Jantar, then "também tomei suco" re-estimated as the whole meal. */
    private suspend fun jantarTakenThenJuice(vm: ChatViewModel): Long {
        val jantar = slotIds()[3]
        answer = { estimateOut(jantar.toString()) }
        sendAndAwait(vm, "4 esfihas")
        vm.register(vm.uiState.value.actions!!.estimateId)
        vm.await { it.actions == null && it.items.any { i -> i is ChatItem.Receipt } }
        // A later message: a receipt in the same millisecond would close the new estimate.
        now = now.plusSeconds(60)
        answer = {
            estimateOut(jantar.toString()).copy(
                reply = "4 esfihas e 2 copos de suco.",
                estimate = estimateOut(jantar.toString()).estimate!!.copy(kcal = 1220.0, p = 40.0, c = 150.0, g = 45.0),
            )
        }
        sendAndAwait(vm, "também tomei 2 copos de suco")
        return jantar
    }

    private fun ChatUiState.replacePrompt() = items.filterIsInstance<ChatItem.ReplacePrompt>().singleOrNull()

    private fun ChatUiState.receipts() = items.filterIsInstance<ChatItem.Receipt>()

    @Test
    fun registrarOnTakenSlot_asksInline_thenReplacesWithOneLog() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val jantar = jantarTakenThenJuice(vm)
        val estimateId = vm.uiState.value.actions!!.estimateId

        vm.register(estimateId)
        val ui = vm.await { it.replacePrompt() != null }
        val prompt = ui.replacePrompt()!!
        assertThat(prompt.estimateId).isEqualTo(estimateId)
        assertThat(prompt.confirm.slot.name).isEqualTo("Jantar")
        assertThat(prompt.confirm.oldKcal).isEqualTo(380)
        assertThat(prompt.confirm.newKcal).isEqualTo(1220)
        // Right below its answer (chatU), and Registrar is gone.
        assertThat(ui.items[ui.items.indexOfFirst { it is ChatItem.Assistant && it.id == estimateId } + 1]).isEqualTo(prompt)
        assertThat(ui.actions).isNull()
        // Nothing written until Substituir.
        assertThat(repo.observeToday().first().logs.single().kcal).isEqualTo(380)

        vm.confirmReplace(estimateId)
        vm.await { it.replacePrompt() == null && it.receipts().any { r -> r.kind == ReceiptKind.REPLACED } }

        val log = repo.observeToday().first().logs.single()
        assertThat(log.slotId).isEqualTo(jantar)
        assertThat(log.kcal).isEqualTo(1220)
        assertThat(log.p).isEqualTo(40)
        assertThat(log.carbs).isEqualTo(150)
        assertThat(log.fat).isEqualTo(45)
        assertThat(log.text).isEqualTo("também tomei 2 copos de suco")
        val receipt = vm.uiState.value.receipts().last()
        assertThat(receipt.kind).isEqualTo(ReceiptKind.REPLACED)
        assertThat(receipt.slotName).isEqualTo("Jantar")
        assertThat(receipt.slotTime).isEqualTo("20:00")
        assertThat(receipt.kcal).isEqualTo(1220)
        assertThat(receipt.fromKcal).isEqualTo(380)
        // The replacement takes the actions; the first record's receipt loses them (latest per slot).
        assertThat(receipt.actions).containsExactly(ReceiptAction.UNDO, ReceiptAction.DELETE, ReceiptAction.MOVE, ReceiptAction.EDIT).inOrder()
        assertThat(vm.uiState.value.receipts().first().actions).isEmpty()
        assertThat(storedAssistant().recordState).isEqualTo("recorded")
        // No A8 line on Substituir: without memory_updates the memory is untouched (A28).
        assertThat(memoryFile.writes).isEqualTo(0)
        assertThat(requests).hasSize(2)
    }

    @Test
    fun replaceElsewhere_opensTrocarEmpty_roomIntact_emptySlotRecords() = runBlocking<Unit> {
        val telemetry = FakeTelemetry()
        val vm = ChatViewModel(repo, service, clock, memory, photos, telemetry)
        val jantar = jantarTakenThenJuice(vm)
        val estimateId = vm.uiState.value.actions!!.estimateId

        vm.register(estimateId)
        vm.await { it.replacePrompt() != null }
        vm.replaceElsewhere(estimateId)
        val ui = vm.await { it.sheetFor != null }
        assertThat(ui.sheetFor).isEqualTo(estimateId)
        assertThat(ui.sheetSelection).isNull()
        assertThat(repo.observeToday().first().logs.single().kcal).isEqualTo(380)

        // Trocar into an empty slot records at once.
        val lanche = slotIds()[2]
        vm.selectInSheet(lanche)
        vm.confirmSheet()
        vm.await { it.replacePrompt() == null && it.receipts().size == 2 }
        assertThat(repo.observeToday().first().logs.map { it.slotId to it.kcal }).containsExactly(jantar to 380, lanche to 1220)
        assertThat(telemetry.params(TelemetryEvents.REPLACE_CONFIRM)).containsExactly(
            mapOf("action" to "shown", "from" to "answer"),
            mapOf("action" to "elsewhere", "from" to "answer"),
        ).inOrder()
    }

    @Test
    fun outraRefeicao_intoAnotherTakenSlot_asksAgain() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val jantar = jantarTakenThenJuice(vm)
        val almoco = slotIds()[1]
        repo.addLog(window = "", text = "arroz", kcal = 500, p = 10, stable = true, slotId = almoco)
        val estimateId = vm.uiState.value.actions!!.estimateId
        vm.register(estimateId)
        vm.await { it.replacePrompt()?.confirm?.slot?.id == jantar }
        vm.replaceElsewhere(estimateId)
        vm.await { it.sheetFor != null }
        vm.selectInSheet(almoco)
        vm.confirmSheet()

        val ui = vm.await { it.replacePrompt()?.confirm?.slot?.id == almoco }
        assertThat(ui.replacePrompt()!!.confirm.oldKcal).isEqualTo(500)
        assertThat(repo.observeToday().first().logs.map { it.kcal }).containsExactly(380, 500)
    }

    @Test
    fun pendingReplace_expiresOnTheNextSend_notRecorded() = runBlocking<Unit> {
        val telemetry = FakeTelemetry()
        val vm = ChatViewModel(repo, service, clock, memory, photos, telemetry)
        jantarTakenThenJuice(vm)
        val estimateId = vm.uiState.value.actions!!.estimateId
        vm.register(estimateId)
        vm.await { it.replacePrompt() != null }

        answer = { ChatOut(reply = "ok", intent = "question", model = "gpt-6-luna") }
        sendAndAwaitReply(vm, "obrigado")
        val ui = vm.await { it.replacePrompt() == null }
        assertThat(ui.items.filterIsInstance<ChatItem.Assistant>().single { it.id == estimateId }.notRecorded).isTrue()
        assertThat(repo.message(estimateId)!!.recordState).isEqualTo("not_recorded")
        assertThat(repo.observeToday().first().logs.single().kcal).isEqualTo(380)
        vm.confirmReplace(estimateId)
        delay(100)
        assertThat(repo.observeToday().first().logs.single().kcal).isEqualTo(380)
        assertThat(telemetry.params(TelemetryEvents.REPLACE_CONFIRM).last()).isEqualTo(mapOf("action" to "expired", "from" to "answer"))
    }

    @Test
    fun pendingReplace_expiresWhenTheSlotChangesByAnotherPath() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val jantar = jantarTakenThenJuice(vm)
        val estimateId = vm.uiState.value.actions!!.estimateId
        vm.register(estimateId)
        vm.await { it.replacePrompt() != null }

        // The Home skips the dinner: the question no longer matches the slot.
        repo.addSkip(jantar)
        vm.await { it.replacePrompt() == null && it.items.any { i -> i is ChatItem.Assistant && i.id == estimateId && i.notRecorded } }
        withTimeout(5_000) { while (repo.message(estimateId)!!.recordState != "not_recorded") delay(10) }
    }

    @Test
    fun pendingReplace_ofYesterday_isNotRecorded() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        jantarTakenThenJuice(vm)
        val estimateId = vm.uiState.value.actions!!.estimateId
        vm.register(estimateId)
        vm.await { it.replacePrompt() != null }

        now = now.plus(java.time.Duration.ofDays(1))
        vm.tick()
        val ui = vm.await { it.replacePrompt() == null && it.items.any { i -> i is ChatItem.Assistant && i.id == estimateId && i.notRecorded } }
        assertThat(ui.actions).isNull()
    }

    // ------------------------------------------------------------------ A34: automatic record

    private fun autoOut(slot: Long, kcal: Double = 380.0, record: String = "auto") = estimateOut(slot.toString()).copy(
        intent = "log",
        record = record,
        estimate = estimateOut(slot.toString()).estimate!!.copy(kcal = kcal, mealText = "2 pães franceses e 2 ovos mexidos"),
    )

    private suspend fun sendAndAwaitAuto(vm: ChatViewModel, text: String) {
        val before = repo.observeMessages().first().count { it.role == "assistant" }
        vm.setComposer(text)
        vm.send()
        vm.await { !it.sending && it.items.none { i -> i is ChatItem.Loading } }
        // The automatic record runs right after the answer is stored. Only this turn answer counts: the previous one
        // may already be recorded while the new one is not stored yet.
        withTimeout(5_000) {
            while (true) {
                val rows = repo.observeMessages().first()
                val answer = rows.lastOrNull { it.role == "assistant" }
                if (rows.count { it.role == "assistant" } > before && (answer?.recordMode != "auto" || answer.recordState != null)) break
                delay(10)
            }
        }
    }

    @Test
    fun auto_emptySlot_recordsWithoutTap_receiptWithActions() = runBlocking<Unit> {
        val telemetry = FakeTelemetry()
        val vm = ChatViewModel(repo, service, clock, memory, photos, telemetry)
        val cafe = slotIds().first()
        answer = { autoOut(cafe) }
        sendAndAwaitAuto(vm, "Registra aí, comi 2 pães franceses com 2 ovos mexidos")

        assertThat(requests.single().autoRecord).isTrue()
        val log = repo.observeToday().first().logs.single()
        assertThat(log.slotId).isEqualTo(cafe)
        assertThat(log.kcal).isEqualTo(380)
        assertThat(log.text).isEqualTo("2 pães franceses e 2 ovos mexidos")
        val ui = vm.await { it.receipts().singleOrNull()?.actions?.isNotEmpty() == true }
        val receipt = ui.receipts().single()
        assertThat(receipt.kind).isEqualTo(ReceiptKind.LOGGED)
        assertThat(receipt.actions).containsExactly(ReceiptAction.DELETE, ReceiptAction.MOVE, ReceiptAction.EDIT).inOrder()
        assertThat(ui.actions).isNull()
        // chatG: no slot question once recorded.
        assertThat(ui.items.filterIsInstance<ChatItem.Assistant>().single().estimate!!.slotQuestion).isNull()
        assertThat(storedAssistant().recordMode).isEqualTo("auto")
        assertThat(storedAssistant().recordState).isEqualTo("recorded")
        assertThat(telemetry.params(TelemetryEvents.MEAL_AUTO_RECORDED).single())
            .isEqualTo(mapOf("kind" to "log", "slot_state" to "empty", "source" to "user", "rounds" to 0))
        assertThat(telemetry.params(TelemetryEvents.CHAT_RESULT).single()["record"]).isEqualTo("auto")
    }

    @Test
    fun auto_skippedSlot_replacesTheSkipWithoutAsking_undoBringsTheSkipBack() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val cafe = slotIds().first()
        repo.addSkip(cafe)
        answer = { autoOut(cafe) }
        sendAndAwaitAuto(vm, "na verdade comi pão com ovo no café")

        val day = repo.observeToday().first()
        assertThat(day.skippedSlotIds).isEmpty()
        assertThat(day.logs.single().kcal).isEqualTo(380)
        val receipt = vm.await { it.receipts().singleOrNull()?.actions?.isNotEmpty() == true }.receipts().single()
        assertThat(receipt.actions).containsExactly(ReceiptAction.UNDO, ReceiptAction.DELETE, ReceiptAction.MOVE, ReceiptAction.EDIT).inOrder()

        vm.receiptAction(receipt.id, ReceiptAction.UNDO)
        vm.await { it.receipts().single().mark == ReceiptMark.UNDONE }
        val after = repo.observeToday().first()
        assertThat(after.skippedSlotIds).containsExactly(cafe)
        assertThat(after.logs).isEmpty()
    }

    @Test
    fun auto_takenSlot_asksInline_nothingChanges() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val jantar = slotIds()[3]
        repo.addLog(window = "", text = "arroz e frango", kcal = 380, p = 30, stable = true, slotId = jantar)
        answer = { autoOut(jantar, kcal = 620.0) }
        sendAndAwaitAuto(vm, "também comi um pudim")

        val ui = vm.await { it.replacePrompt() != null }
        assertThat(ui.replacePrompt()!!.confirm.oldKcal).isEqualTo(380)
        assertThat(ui.replacePrompt()!!.confirm.newKcal).isEqualTo(620)
        assertThat(ui.actions).isNull()
        assertThat(repo.observeToday().first().logs.single().kcal).isEqualTo(380)
        assertThat(storedAssistant().recordState).isEqualTo("pending_replace")
    }

    @Test
    fun auto_guards_downgradeToAsk() = runBlocking<Unit> {
        val telemetry = FakeTelemetry()
        val vm = ChatViewModel(repo, service, clock, memory, photos, telemetry)
        answer = { autoOut(99) }
        sendAndAwait(vm, "pão com ovo")
        assertThat(storedAssistant().recordMode).isEqualTo("ask")
        answer = { autoOut(1).copy(estimate = autoOut(1).estimate!!.copy(suggestedSlot = null)) }
        sendAndAwait(vm, "pão com ovo")
        answer = { autoOut(1).copy(estimate = autoOut(1).estimate!!.copy(question = "Pão francês?")) }
        sendAndAwait(vm, "pão com ovo")

        assertThat(repo.observeToday().first().logs).isEmpty()
        assertThat(telemetry.params(TelemetryEvents.RECORD_GUARD).map { it["reason"] })
            .containsExactly("slot_not_today", "no_slot", "question_pending").inOrder()
        assertThat(vm.uiState.value.actions).isNotNull()
    }

    @Test
    fun serverWithoutRecord_logIsAsk_nothingRecordedWithoutATap() = runBlocking<Unit> {
        val telemetry = FakeTelemetry()
        val vm = ChatViewModel(repo, service, clock, memory, photos, telemetry)
        val cafe = slotIds().first()
        answer = { estimateOut(cafe.toString()) }
        sendAndAwait(vm, "pão com ovo")
        assertThat(storedAssistant().recordMode).isEqualTo("ask")
        assertThat(repo.observeToday().first().logs).isEmpty()
        assertThat(telemetry.params(TelemetryEvents.CHAT_RESULT).single()["record"]).isEqualTo("missing")
        assertThat(telemetry.params(TelemetryEvents.RECORD_ASK).single()).isEqualTo(mapOf("action" to "shown"))
    }

    @Test
    fun ask_registrarRecords_thenNextSendExpiresTheNextOne() = runBlocking<Unit> {
        val telemetry = FakeTelemetry()
        val vm = ChatViewModel(repo, service, clock, memory, photos, telemetry)
        val (cafe, almoco) = slotIds()
        answer = { autoOut(cafe, record = "ask") }
        sendAndAwait(vm, "pudim de leite com calda")
        val first = vm.uiState.value.actions!!.estimateId
        assertThat(vm.uiState.value.items.filterIsInstance<ChatItem.Assistant>().single().estimate!!.slotQuestion)
            .isEqualTo("Deseja registrar essa refeição no Café da manhã?")
        vm.register(first)
        vm.await { it.actions == null && it.receipts().isNotEmpty() }
        assertThat(repo.observeToday().first().logs.single().slotId).isEqualTo(cafe)

        now = now.plusSeconds(60)
        answer = { autoOut(almoco, record = "ask") }
        sendAndAwait(vm, "e um pudim no almoço")
        val second = vm.uiState.value.actions!!.estimateId
        answer = { ChatOut(reply = "ok", intent = "question", model = "gpt-6-luna") }
        sendAndAwaitReply(vm, "obrigado")
        val ui = vm.await { it.actions == null }
        assertThat(ui.items.filterIsInstance<ChatItem.Assistant>().single { it.id == second }.notRecorded).isTrue()
        assertThat(ui.items.filterIsInstance<ChatItem.Assistant>().single { it.id == first }.notRecorded).isFalse()
        assertThat(repo.observeToday().first().logs).hasSize(1)
        assertThat(telemetry.params(TelemetryEvents.RECORD_ASK).map { it["action"] })
            .containsExactly("shown", "tapped", "shown", "expired").inOrder()
    }

    @Test
    fun skipByText_emptySlot_skipsWithReceipt_recordedSlotChangesNothing() = runBlocking<Unit> {
        val telemetry = FakeTelemetry()
        val vm = ChatViewModel(repo, service, clock, memory, photos, telemetry)
        val (cafe, almoco) = slotIds()
        answer = { ChatOut(reply = "Tudo bem.", intent = "skip", record = "auto", skipSlot = cafe.toString(), model = "gpt-6-luna") }
        sendAndAwaitAuto(vm, "pulei o café")

        assertThat(repo.observeToday().first().skippedSlotIds).containsExactly(cafe)
        val receipt = vm.await { it.receipts().singleOrNull()?.actions?.isNotEmpty() == true }.receipts().single()
        assertThat(receipt.kind).isEqualTo(ReceiptKind.SKIPPED)
        assertThat(receipt.actions).containsExactly(ReceiptAction.UNDO)
        assertThat(telemetry.params(TelemetryEvents.MEAL_AUTO_RECORDED).single()["kind"]).isEqualTo("skip")

        // Skipped again, or a slot with a record: nothing changes, the turn is Não registrado.
        repo.addLog(window = "", text = "arroz", kcal = 500, p = 10, stable = true, slotId = almoco)
        now = now.plusSeconds(60)
        answer = { ChatOut(reply = "Tudo bem.", intent = "skip", record = "auto", skipSlot = almoco.toString(), model = "gpt-6-luna") }
        sendAndAwaitAuto(vm, "pulei o almoço")
        assertThat(repo.observeToday().first().logs.single().kcal).isEqualTo(500)
        assertThat(storedAssistant().recordState).isEqualTo("not_recorded")
        assertThat(repo.observeToday().first().skippedSlotIds).containsExactly(cafe)

        // Desfazer removes the skip.
        vm.receiptAction(receipt.id, ReceiptAction.UNDO)
        vm.await { it.receipts().first().mark == ReceiptMark.UNDONE }
        assertThat(repo.observeToday().first().skippedSlotIds).isEmpty()
    }

    @Test
    fun otherDay_isNeverRecorded() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        answer = { ChatOut(reply = "Só registro as refeições de hoje.", intent = "log", record = "none", model = "gpt-6-luna") }
        sendAndAwaitReply(vm, "ontem jantei pizza")
        assertThat(repo.observeToday().first().logs).isEmpty()
        assertThat(storedAssistant().recordMode).isEqualTo("none")
        assertThat(vm.uiState.value.actions).isNull()
    }

    // ------------------------------------------------------------------ A34: receipt actions

    /** An automatic record in [slot] (today), its receipt drawn with actions. */
    private suspend fun recorded(vm: ChatViewModel, slot: Long, kcal: Double = 380.0, text: String = "comi pão com ovo"): ChatItem.Receipt {
        now = now.plusSeconds(60)
        answer = { autoOut(slot, kcal) }
        val count = vm.uiState.value.receipts().size
        sendAndAwaitAuto(vm, text)
        return vm.await { it.receipts().size > count && it.receipts().last().actions.isNotEmpty() }.receipts().last()
    }

    @Test
    fun excluir_deletesTheRecord_marksTheReceipt_noConfirmation() = runBlocking<Unit> {
        val telemetry = FakeTelemetry()
        val vm = ChatViewModel(repo, service, clock, memory, photos, telemetry)
        val receipt = recorded(vm, slotIds().first())
        now = now.plusSeconds(30)

        vm.receiptAction(receipt.id, ReceiptAction.DELETE)
        val ui = vm.await { it.receipts().single().mark == ReceiptMark.DELETED }
        assertThat(ui.receipts().single().actions).isEmpty()
        assertThat(repo.observeToday().first().logs).isEmpty()
        assertThat(telemetry.params(TelemetryEvents.RECEIPT_ACTION).single()).isEqualTo(
            mapOf("action" to "delete", "receipt" to "logged", "source" to "user", "age_s" to 30L, "same_day" to true),
        )
    }

    @Test
    fun desfazer_ofAReplacement_restoresThePreviousRecord_withARestoredReceipt() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val jantar = slotIds()[3]
        recorded(vm, jantar, 380.0)
        now = now.plusSeconds(60)
        answer = { autoOut(jantar, 620.0) }
        sendAndAwaitAuto(vm, "também comi um pudim")
        val estimateId = storedAssistant().id
        vm.confirmReplace(estimateId)
        val replaced = vm.await { it.receipts().lastOrNull()?.kind == ReceiptKind.REPLACED && it.receipts().last().actions.isNotEmpty() }.receipts().last()

        vm.receiptAction(replaced.id, ReceiptAction.UNDO)
        val ui = vm.await { it.receipts().last().kind == ReceiptKind.RESTORED }
        assertThat(ui.receipts().map { it.kind }).containsExactly(ReceiptKind.LOGGED, ReceiptKind.REPLACED, ReceiptKind.RESTORED).inOrder()
        val restored = ui.receipts().last()
        assertThat(restored.kcal).isEqualTo(380)
        assertThat(restored.slotName).isEqualTo("Jantar")
        assertThat(restored.actions).containsExactly(ReceiptAction.DELETE, ReceiptAction.MOVE, ReceiptAction.EDIT).inOrder()
        assertThat(ui.receipts()[1].mark).isEqualTo(ReceiptMark.UNDONE)
        assertThat(ui.receipts()[0].actions).isEmpty()
        val log = repo.observeToday().first().logs.single()
        assertThat(log.kcal).isEqualTo(380)
        assertThat(log.text).isEqualTo("2 pães franceses e 2 ovos mexidos")
    }

    @Test
    fun trocarRefeicao_toAnEmptySlot_movesTheRecord_desfazerMovesItBack() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val (cafe, _, lanche) = slotIds()
        val receipt = recorded(vm, cafe)

        vm.receiptAction(receipt.id, ReceiptAction.MOVE)
        val sheet = vm.await { it.sheetFor == receipt.id }
        assertThat(sheet.sheetCurrent).isEqualTo(cafe)
        assertThat(sheet.sheetSelection).isNull()
        vm.selectInSheet(lanche)
        vm.confirmSheet()
        val ui = vm.await { it.receipts().lastOrNull()?.kind == ReceiptKind.MOVED }
        val moved = ui.receipts().last()
        assertThat(moved.slotName).isEqualTo("Lanche")
        assertThat(moved.kcal).isEqualTo(380)
        assertThat(ui.receipts().first().mark).isEqualTo(ReceiptMark.MOVED)
        assertThat(repo.observeToday().first().logs.single().slotId).isEqualTo(lanche)

        val withActions = vm.await { it.receipts().last().actions.isNotEmpty() }.receipts().last()
        assertThat(withActions.actions).containsExactly(ReceiptAction.UNDO, ReceiptAction.DELETE, ReceiptAction.MOVE, ReceiptAction.EDIT).inOrder()
        vm.receiptAction(moved.id, ReceiptAction.UNDO)
        vm.await { it.receipts().last().kind == ReceiptKind.RESTORED }
        assertThat(repo.observeToday().first().logs.single().slotId).isEqualTo(cafe)
    }

    @Test
    fun trocarRefeicao_toATakenSlot_asksBelowTheReceipt_substituirMovesAndReplaces() = runBlocking<Unit> {
        val telemetry = FakeTelemetry()
        val vm = ChatViewModel(repo, service, clock, memory, photos, telemetry)
        val (cafe, almoco) = slotIds()
        repo.addLog(window = "", text = "arroz", kcal = 500, p = 10, stable = true, slotId = almoco)
        val receipt = recorded(vm, cafe)

        vm.receiptAction(receipt.id, ReceiptAction.MOVE)
        vm.await { it.sheetFor == receipt.id }
        vm.selectInSheet(almoco)
        vm.confirmSheet()
        val asking = vm.await { it.receipts().single().moveConfirm != null }
        assertThat(asking.receipts().single().moveConfirm!!.oldKcal).isEqualTo(500)
        assertThat(asking.receipts().single().moveConfirm!!.newKcal).isEqualTo(380)
        assertThat(repo.observeToday().first().logs.map { it.slotId }).containsExactly(almoco, cafe)

        vm.confirmMove()
        vm.await { it.receipts().lastOrNull()?.kind == ReceiptKind.MOVED }
        assertThat(repo.observeToday().first().logs.map { it.slotId to it.kcal }).containsExactly(almoco to 380)
        assertThat(telemetry.params(TelemetryEvents.REPLACE_CONFIRM).map { it["action"] to it["from"] })
            .containsExactly("shown" to "move", "confirmed" to "move").inOrder()

        // Desfazer: the meal goes back to the café and the almoço gets its record back.
        val moved = vm.await { it.receipts().last().actions.isNotEmpty() }.receipts().last()
        vm.receiptAction(moved.id, ReceiptAction.UNDO)
        vm.await { it.receipts().count { r -> r.kind == ReceiptKind.RESTORED } == 2 }
        assertThat(repo.observeToday().first().logs.map { it.slotId to it.kcal }).containsExactly(almoco to 500, cafe to 380)
    }

    @Test
    fun editar_removesTheRecord_fillsTheComposer_focuses() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val receipt = recorded(vm, slotIds().first())
        vm.setComposer("rascunho")
        val focus = vm.uiState.value.focusComposer

        vm.receiptAction(receipt.id, ReceiptAction.EDIT)
        val ui = vm.await { it.receipts().single().mark == ReceiptMark.EDITED }
        assertThat(ui.composer).isEqualTo("2 pães franceses e 2 ovos mexidos")
        assertThat(ui.focusComposer).isEqualTo(focus + 1)
        assertThat(repo.observeToday().first().logs).isEmpty()
    }

    @Test
    fun photoRecord_hasNoEditar() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val almoco = slotIds()[1]
        answer = { autoOut(almoco) }
        photos.nextImport = app.fibrai.android.core.photo.PhotoResult.Ready("/photos/pf.jpg")
        vm.onPicked(android.net.Uri.parse("content://media/1"))
        vm.await { it.attachment != null }
        vm.send()
        val receipt = vm.await { it.receipts().singleOrNull()?.actions?.isNotEmpty() == true }.receipts().single()
        assertThat(receipt.actions).containsExactly(ReceiptAction.DELETE, ReceiptAction.MOVE).inOrder()
        assertThat(repo.observeToday().first().logs.single().source).isEqualTo("photo")
    }

    @Test
    fun actions_onlyOnTheLatestReceiptOfTheSlot_andHiddenWhenTheSlotChangedElsewhere() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val (cafe, almoco) = slotIds()
        val first = recorded(vm, cafe)
        val second = recorded(vm, almoco, text = "almocei arroz")
        val ui = vm.uiState.value
        assertThat(ui.receipts().map { it.actions.isNotEmpty() }).containsExactly(true, true).inOrder()
        assertThat(ui.receipts().first().id).isEqualTo(first.id)

        // The Home adds a second log to the almoço: its receipt no longer matches, no actions.
        repo.addLog(window = "", text = "suco", kcal = 100, p = 1, stable = true, slotId = almoco)
        val after = vm.await { it.receipts().single { r -> r.id == second.id }.actions.isEmpty() }
        assertThat(after.receipts().single { it.id == first.id }.actions).isNotEmpty()
        vm.receiptAction(second.id, ReceiptAction.DELETE)
        delay(100)
        assertThat(repo.observeToday().first().logs).hasSize(3)
    }

    @Test
    fun receiptOfYesterday_keepsItsActions_andDeletesOnItsDay() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val cafe = slotIds().first()
        val receipt = recorded(vm, cafe)
        now = now.plus(java.time.Duration.ofDays(1))
        vm.tick()
        val ui = vm.await { it.receipts().single().actions.isNotEmpty() && it.items.any { i -> i is ChatItem.DateSeparator && i.label.startsWith("Ontem") } }
        assertThat(ui.receipts().single().id).isEqualTo(receipt.id)

        vm.receiptAction(receipt.id, ReceiptAction.DELETE)
        vm.await { it.receipts().single().mark == ReceiptMark.DELETED }
        assertThat(db.mealLogDao().getByDate("2026-09-25")).isEmpty()
    }

    @Test
    fun oldRows_withoutRecordMode_haveNoActions() = runBlocking<Unit> {
        val cafe = slotIds().first()
        repo.insertMessage("user", "pão com ovo")
        repo.insertMessage("assistant", "Pão com ovo.", estimateKcal = 300, estimateP = 10, estimateC = 30, estimateG = 10, estimateSlotId = cafe, intent = "log")
        repo.addLog(window = "", text = "pão com ovo", kcal = 300, p = 10, stable = true, slotId = cafe)
        repo.insertMessage("logged", "Café da manhã", estimateKcal = 300, estimateSlotId = cafe)
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val ui = vm.await { it.loaded && it.receipts().isNotEmpty() }
        assertThat(ui.actions).isNull()
        assertThat(ui.receipts().single().actions).isEmpty()
        assertThat(ui.items.filterIsInstance<ChatItem.Assistant>().single().notRecorded).isFalse()
    }

    @Test
    fun excluir_revertsTheRoutineItApplied_keepsAFactChangedSince() = runBlocking<Unit> {
        val telemetry = FakeTelemetry()
        val vm = ChatViewModel(repo, service, clock, memory, photos, telemetry)
        val cafe = slotIds().first()
        answer = {
            autoOut(cafe).copy(
                memoryUpdates = listOf(update("add", null, "dynamic", "routine", "cafe", "pão com ovo", cafe.toString())),
            )
        }
        sendAndAwaitAuto(vm, "comi pão com ovo")
        val receipt = vm.await { it.receipts().singleOrNull()?.actions?.isNotEmpty() == true }.receipts().single()
        assertThat(receipt.memoryUpdated).isTrue()
        assertThat(memory.read(today).facts.single().id).isEqualTo("D1")

        vm.receiptAction(receipt.id, ReceiptAction.DELETE)
        vm.await { it.receipts().single().mark == ReceiptMark.DELETED }
        assertThat(memory.read(today).facts).isEmpty()
        assertThat(telemetry.params(TelemetryEvents.MEMORY_REVERTED).single()).isEqualTo(mapOf("reverted" to 1, "kept" to 0))
    }

    @Test
    fun routineCard_record_hasActions_excluirRevertsTheReinforce() = runBlocking<Unit> {
        val cafe = slotIds().first()
        seedRoutine(routineFact(3, cafe))
        val before = memory.read(today).facts.single()
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        vm.await { it.routine != null }
        vm.recordRoutine()
        val receipt = vm.await { it.receipts().singleOrNull()?.actions?.isNotEmpty() == true }.receipts().single()
        assertThat(receipt.actions).containsExactly(ReceiptAction.DELETE, ReceiptAction.MOVE, ReceiptAction.EDIT).inOrder()
        assertThat(memory.read(today).facts.single().days).contains(today.toString())

        vm.receiptAction(receipt.id, ReceiptAction.DELETE)
        vm.await { it.receipts().single().mark == ReceiptMark.DELETED }
        assertThat(memory.read(today).facts.single()).isEqualTo(before)
    }

    @Test
    fun emptyDay_showsGreetingAndMeta_notSentToServer() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val ui = vm.await { it.metaTotal > 0 }
        assertThat(ui.emptyDay).isTrue()
        assertThat(ui.items.last()).isInstanceOf(ChatItem.Greeting::class.java)
        assertThat(ui.metaRemaining).isEqualTo(2000)
        assertThat(requests).isEmpty()
    }

    private fun update(op: String, id: String?, kind: String, category: String, key: String, text: String, slot: String? = null) =
        ChatMemoryUpdate(op, id, kind, category, key, text, slot)

    private suspend fun storedAssistant() = repo.observeMessages().first().last { it.role == "assistant" }

    @Test
    fun preference_appliedOnTheAnswer_marksTheAssistant_nextPostCarriesFacts() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        answer = {
            ChatOut(
                reply = "Anotado: leite semidesnatado.",
                intent = "question",
                memoryUpdates = listOf(update("add", null, "permanent", "preference", "leite", "Leite semidesnatado")),
                model = "gpt-6-luna",
            )
        }
        sendAndAwaitReply(vm, "Sempre uso leite semidesnatado")

        // v2 client: facts always present, even empty; no legacy memory text.
        assertThat(requests.single().facts).isEmpty()
        assertThat(requests.single().memory).isEmpty()
        val p1 = memory.read(today).facts.single()
        assertThat(p1.id).isEqualTo("P1")
        assertThat(p1.kind).isEqualTo("permanent")
        assertThat(storedAssistant().memoryUpdated).isTrue()
        assertThat(storedAssistant().pendingMemory).isNull()
        assertThat(repo.observeMessages().first().single { it.role == "user" }.memoryUpdated).isFalse()

        answer = { ChatOut(reply = "ok", intent = "question", model = "gpt-6-luna") }
        sendAndAwaitReply(vm, "café com leite")
        val fact = requests.last().facts!!.single()
        assertThat(fact.id).isEqualTo("P1")
        assertThat(fact.daysSeen).isEqualTo(1)
        assertThat(fact.lastSeen).isEqualTo("2026-09-25")
        assertThat(storedAssistant().memoryUpdated).isFalse()
    }

    @Test
    fun routine_onlyOnGravar_marksTheReceipt() = runBlocking<Unit> {
        val telemetry = FakeTelemetry()
        val vm = ChatViewModel(repo, service, clock, memory, photos, telemetry)
        val cafe = slotIds().first()
        answer = {
            estimateOut(cafe.toString()).copy(
                memoryUpdates = listOf(update("add", null, "dynamic", "routine", "cafe", "2 pães franceses e 2 ovos", cafe.toString())),
            )
        }
        sendAndAwait(vm, "2 pães franceses com 2 ovos mexidos")
        assertThat(memory.read(today).facts).isEmpty()
        assertThat(memoryFile.writes).isEqualTo(0)
        assertThat(storedAssistant().pendingMemory).contains("\"routine\"")
        assertThat(storedAssistant().memoryUpdated).isFalse()

        vm.register(vm.uiState.value.actions!!.estimateId)
        vm.await { it.actions == null && it.items.any { i -> i is ChatItem.Receipt } }
        val routine = memory.read(today).facts.single()
        assertThat(routine.id).isEqualTo("D1")
        assertThat(routine.slot).isEqualTo(cafe.toString())
        assertThat(listOf(routine.kcal, routine.p, routine.c, routine.g)).containsExactly(380, 22, 36, 16).inOrder()
        val rows = repo.observeMessages().first()
        assertThat(rows.single { it.role == "logged" }.memoryUpdated).isTrue()
        assertThat(rows.single { it.role == "assistant" }.memoryUpdated).isFalse()
        assertThat(telemetry.params(TelemetryEvents.MEMORY_CHANGED).single()).containsExactly(
            "add", 1, "reinforce", 0, "replace", 0, "remove", 0, "promote", 0, "expire", 0, "permanent", 0, "dynamic", 1,
            "temp_add", 0, "temp_replace", 0, "temp_remove", 0, "temp_expire", 0, "temp", 0,
        )
    }

    @Test
    fun estimateNotRecorded_doesNotReinforceTheRoutine() = runBlocking<Unit> {
        val cafe = slotIds().first()
        memory.apply(
            listOf(MemoryUpdate("add", null, "dynamic", "routine", "cafe", "pão com ovo", cafe.toString())),
            today.minusDays(1),
            RecordedMeal(cafe.toString(), 300, 15, 30, 12),
        )
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        answer = {
            estimateOut(cafe.toString()).copy(
                memoryUpdates = listOf(update("reinforce", "D1", "dynamic", "routine", "cafe", "pão com ovo", cafe.toString())),
            )
        }
        sendAndAwait(vm, "pão com ovo")
        // Registrar never tapped: the next send expires it.
        answer = { ChatOut(reply = "ok", intent = "question", model = "gpt-6-luna") }
        sendAndAwaitReply(vm, "deixa pra lá")
        vm.await { it.actions == null }

        val fact = memory.read(today).facts.single()
        assertThat(fact.days).containsExactly("2026-09-24")
        assertThat(fact.kcal).isEqualTo(300)
        assertThat(repo.observeMessages().first().none { it.memoryUpdated }).isTrue()
    }

    @Test
    fun memoryUsed_resolvedToKinds_unknownIgnored() = runBlocking<Unit> {
        memory.apply(
            listOf(
                MemoryUpdate("add", null, "permanent", "preference", "leite", "Leite semidesnatado"),
                MemoryUpdate("add", null, "dynamic", "portion", "arroz", "4 colheres de arroz"),
            ),
            today,
        )
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        answer = { ChatOut(reply = "ok", intent = "question", memoryUsed = listOf("P1", "D77"), model = "gpt-6-luna") }
        sendAndAwaitReply(vm, "café com leite")
        assertThat(storedAssistant().memoryUsedKinds).isEqualTo("permanent")

        answer = { ChatOut(reply = "ok", intent = "question", memoryUsed = listOf("D1", "P1"), model = "gpt-6-luna") }
        sendAndAwaitReply(vm, "arroz com leite")
        assertThat(storedAssistant().memoryUsedKinds).isEqualTo("permanent,dynamic")

        answer = { ChatOut(reply = "ok", intent = "question", memoryUsed = listOf("P9"), model = "gpt-6-luna") }
        sendAndAwaitReply(vm, "oi")
        assertThat(storedAssistant().memoryUsedKinds).isNull()
    }

    @Test
    fun answerToAssumption_writesNoRespondeuLine() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        answer = {
            estimateOut(null).copy(estimate = estimateOut(null).estimate!!.copy(confidence = "medium", question = "O pão era francês ou de forma?"))
        }
        sendAndAwait(vm, "pão com ovo")

        answer = { ChatOut(reply = "Anotado.", model = "gpt-6-luna") }
        sendAndAwaitReply(vm, "francês")
        assertThat(memoryFile.writes).isEqualTo(0)
        assertThat(memory.read(today).facts).isEmpty()
    }

    @Test
    fun wipeToday_keepsMemory() = runBlocking<Unit> {
        memory.apply(listOf(MemoryUpdate("add", null, "permanent", "preference", "leite", "Leite semidesnatado")), today)
        repo.wipeToday()
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        answer = { ChatOut(reply = "oi", model = "gpt-6-luna") }
        vm.setComposer("oi")
        vm.send()
        withTimeout(5_000) { while (requests.isEmpty()) kotlinx.coroutines.delay(10) }
        assertThat(requests.single().facts!!.single().text).isEqualTo("Leite semidesnatado")
        assertThat(memoryFile.writes).isEqualTo(1)
    }

    @Test
    fun photo_postsImageWithCaption_storesPhotoPath_historyGetsMarker() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        answer = { estimateOut(null) }
        photos.nextImport = app.fibrai.android.core.photo.PhotoResult.Ready("/photos/pf.jpg")
        vm.setComposer("Almoço de hoje")
        vm.onPicked(android.net.Uri.parse("content://media/1"))
        vm.await { it.attachment != null }
        vm.send()
        vm.await { it.actions != null }

        val sent = requests.single()
        assertThat(sent.imageB64).isEqualTo("B64:/photos/pf.jpg")
        assertThat(sent.text).isEqualTo("Almoço de hoje")
        val stored = repo.observeMessages().first().first { it.role == "user" }
        assertThat(stored.photoPath).isEqualTo("/photos/pf.jpg")
        assertThat(vm.uiState.value.items.filterIsInstance<ChatItem.User>().single().photoPath).isEqualTo("/photos/pf.jpg")
        assertThat(vm.uiState.value.composer).isEmpty()

        // The image goes once; the history keeps a marker.
        vm.setComposer("e um suco")
        vm.send()
        withTimeout(5_000) { while (requests.size < 2) kotlinx.coroutines.delay(10) }
        assertThat(requests.last().imageB64).isNull()
        assertThat(requests.last().messages.first().text).isEqualTo("[foto] Almoço de hoje")
    }

    @Test
    fun photoOver16Mb_showsNotice_noPost() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        photos.nextImport = app.fibrai.android.core.photo.PhotoResult.TooLarge
        vm.onPicked(android.net.Uri.parse("content://media/2"))
        vm.await { it.notice != null }
        assertThat(vm.uiState.value.notice).isEqualTo("Foto grande demais.")
        assertThat(requests).isEmpty()
        assertThat(repo.observeMessages().first()).isEmpty()
        vm.dismissNotice()
        assertThat(vm.uiState.value.notice).isNull()
    }

    @Test
    fun photoFailure_keepsPhotoForRetry_roomUntouched() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        answer = { throw IOException("timeout") }
        photos.nextImport = app.fibrai.android.core.photo.PhotoResult.Ready("/photos/x.jpg")
        vm.onPicked(android.net.Uri.parse("content://media/3"))
        vm.await { it.attachment != null }
        vm.send()
        vm.await { it.items.any { i -> i is ChatItem.Failed } }
        assertThat(repo.observeMessages().first()).isEmpty()
        assertThat(vm.uiState.value.items.filterIsInstance<ChatItem.User>().single().photoPath).isEqualTo("/photos/x.jpg")

        answer = { estimateOut(null) }
        vm.retry()
        vm.await { it.actions != null }
        assertThat(requests.map { it.imageB64 }).containsExactly("B64:/photos/x.jpg", "B64:/photos/x.jpg")
        assertThat(photos.deleted).isEmpty()
    }

    // ------------------------------------------------------------------ text limit (A25, chatX)

    @Test
    fun composer_keeps1500Chars_andSendsThemWhole() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        answer = { estimateOut(null) }
        val text = "a".repeat(1500)
        vm.setComposer(text)
        val typed = vm.await { it.composer.isNotEmpty() }
        assertThat(typed.composer).hasLength(1500)
        assertThat(typed.composerTooLong).isFalse()
        vm.send()
        vm.await { it.actions != null }
        assertThat(requests.single().text).hasLength(1500)
    }

    @Test
    fun over2000_blocksSendAndPhoto_backTo2000_sends() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        answer = { estimateOut(null) }
        vm.setComposer("a".repeat(2001))
        val blocked = vm.await { it.composer.isNotEmpty() }
        assertThat(blocked.composer).hasLength(2001)
        assertThat(blocked.composerTooLong).isTrue()
        assertThat(blocked.canSend).isFalse()
        assertThat(blocked.canAttach).isFalse()
        vm.send()
        vm.openPhotoSheet()
        assertThat(vm.uiState.value.photoSheet).isFalse()
        assertThat(vm.uiState.value.composer).hasLength(2001)

        // Photo attached some other way (suggestion chip): the caption is still too long, nothing leaves.
        photos.nextImport = app.fibrai.android.core.photo.PhotoResult.Ready("/photos/x.jpg")
        vm.onPicked(android.net.Uri.parse("content://media/9"))
        vm.await { it.attachment != null }
        assertThat(vm.uiState.value.canSend).isFalse()
        vm.send()
        assertThat(requests).isEmpty()

        vm.removeAttachment()
        vm.setComposer("a".repeat(2000))
        val back = vm.await { !it.composerTooLong && it.attachment == null }
        assertThat(back.composer).hasLength(2000)
        assertThat(back.canSend).isTrue()
        vm.send()
        vm.await { it.actions != null }
        assertThat(requests.single().text).hasLength(2000)
        assertThat(vm.uiState.value.composerTooLong).isFalse()
    }

    // ------------------------------------------------------------------ attachment (A19, chatA)

    @Test
    fun pickedPhoto_isAttached_noPost_untilSend() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        answer = { estimateOut(null) }
        photos.nextImport = app.fibrai.android.core.photo.PhotoResult.Ready("/photos/a.jpg")
        vm.onPicked(android.net.Uri.parse("content://media/4"))
        val attached = vm.await { it.attachment != null }
        assertThat(attached.attachment).isEqualTo("/photos/a.jpg")
        assertThat(attached.canSend).isTrue()
        assertThat(attached.items.filterIsInstance<ChatItem.User>()).isEmpty()
        assertThat(requests).isEmpty()

        vm.setComposer("almoço de hoje, comi tudo")
        vm.send()
        vm.await { it.actions != null }
        assertThat(requests).hasSize(1)
        assertThat(requests.single().text).isEqualTo("almoço de hoje, comi tudo")
        assertThat(requests.single().imageB64).isEqualTo("B64:/photos/a.jpg")
        assertThat(vm.uiState.value.attachment).isNull()
        assertThat(photos.deleted).isEmpty()
    }

    @Test
    fun capturedPhoto_isAttached_noPost() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        photos.nextImport = app.fibrai.android.core.photo.PhotoResult.Ready("/photos/cam.jpg")
        vm.newCapture()
        vm.onCaptured(true)
        vm.await { it.attachment == "/photos/cam.jpg" }
        assertThat(requests).isEmpty()
    }

    @Test
    fun removeAttachment_deletesFile_andDisablesSend() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        photos.nextImport = app.fibrai.android.core.photo.PhotoResult.Ready("/photos/b.jpg")
        vm.onPicked(android.net.Uri.parse("content://media/5"))
        vm.await { it.attachment != null }
        vm.removeAttachment()
        val cleared = vm.await { it.attachment == null }
        assertThat(cleared.canSend).isFalse()
        assertThat(photos.deleted).containsExactly("/photos/b.jpg")
        vm.send()
        assertThat(requests).isEmpty()
    }

    @Test
    fun secondPhoto_replacesAttachment_deletesThePrevious() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        photos.nextImport = app.fibrai.android.core.photo.PhotoResult.Ready("/photos/c1.jpg")
        vm.onPicked(android.net.Uri.parse("content://media/6"))
        vm.await { it.attachment == "/photos/c1.jpg" }
        photos.nextImport = app.fibrai.android.core.photo.PhotoResult.Ready("/photos/c2.jpg")
        vm.onPicked(android.net.Uri.parse("content://media/7"))
        vm.await { it.attachment == "/photos/c2.jpg" }
        assertThat(photos.deleted).containsExactly("/photos/c1.jpg")
        assertThat(requests).isEmpty()
    }

    @Test
    fun leavingChat_withUnsentAttachment_deletesFile() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val owner = androidx.lifecycle.ViewModelStore()
        androidx.lifecycle.ViewModelProvider(
            owner,
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = vm as T
            },
        )[ChatViewModel::class.java]
        photos.nextImport = app.fibrai.android.core.photo.PhotoResult.Ready("/photos/d.jpg")
        vm.onPicked(android.net.Uri.parse("content://media/8"))
        vm.await { it.attachment != null }
        owner.clear()
        assertThat(photos.deleted).containsExactly("/photos/d.jpg")
        assertThat(requests).isEmpty()
    }

    @Test
    fun estimateWithQuestion_rendersQuestionItemRightBelow() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        answer = {
            estimateOut("1").copy(estimate = estimateOut("1").estimate!!.copy(confidence = "medium", question = "Os pães tinham manteiga ou requeijão?"))
        }
        sendAndAwait(vm, "2 pães franceses com 2 ovos mexidos")
        val items = vm.uiState.value.items
        val bot = items.indexOfFirst { it is ChatItem.Assistant }
        val question = items[bot + 1] as ChatItem.Question
        assertThat(question.text).isEqualTo("Os pães tinham manteiga ou requeijão?")
        assertThat(question.messageId).isEqualTo((items[bot] as ChatItem.Assistant).id)
        assertThat(question.time).isEqualTo((items[bot] as ChatItem.Assistant).time)
    }

    @Test
    fun cancelledCameraOrPicker_doesNothing() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        vm.openPhotoSheet()
        vm.await { it.photoSheet }
        vm.onPicked(null)
        vm.await { !it.photoSheet }
        vm.newCapture()
        vm.onCaptured(false)
        assertThat(photos.deleted).containsExactly("/tmp/capture.jpg".replace('/', java.io.File.separatorChar))
        assertThat(requests).isEmpty()
    }

    // ------------------------------------------------------------------ telemetry (A11)

    @Test
    fun serverFallback_isShown_andReportedAsNonFatalWithRequestId() = runBlocking<Unit> {
        val telemetry = FakeTelemetry()
        val ids = RequestIds()
        val vm = ChatViewModel(repo, service, clock, memory, photos, telemetry, ids)
        answer = {
            ids.last = "req-fallback"
            ChatOut(reply = "nao deu pra estimar", estimate = null, model = "gpt-6-luna")
        }
        vm.setComposer("o que encaixa no dia?")
        vm.send()
        vm.await { ui -> ui.items.any { it is ChatItem.Assistant } }

        // Shown as a normal reply (unchanged behavior), stored in Room.
        assertThat(vm.uiState.value.items.filterIsInstance<ChatItem.Assistant>().single().text).isEqualTo("nao deu pra estimar")
        val fallback = telemetry.nonFatals.single()
        assertThat(fallback).isInstanceOf(ChatFallback::class.java)
        assertThat(fallback.message).isEqualTo("request_id=req-fallback has_photo=false")
        assertThat(telemetry.params(TelemetryEvents.CHAT_RESULT))
            .containsExactly(mapOf("outcome" to "fallback", "has_estimate" to false, "question_only" to false))
    }

    @Test
    fun chatSend_carriesBucketsOnly_neverTheText() = runBlocking<Unit> {
        val telemetry = FakeTelemetry()
        val vm = ChatViewModel(repo, service, clock, memory, photos, telemetry, RequestIds())
        answer = { estimateOut(null) }
        val text = "2 pães franceses com 2 ovos mexidos"
        sendAndAwait(vm, text)

        assertThat(telemetry.params(TelemetryEvents.CHAT_SEND))
            .containsExactly(mapOf("has_photo" to false, "text_len" to "20-100"))
        assertThat(telemetry.params(TelemetryEvents.CHAT_RESULT))
            .containsExactly(mapOf("outcome" to "ok", "has_estimate" to true, "confidence" to "high", "intent" to "none", "question_only" to false, "record" to "missing"))
        assertThat(telemetry.nonFatals).isEmpty()
        val allValues = telemetry.events.flatMap { it.second.values }.map { it.toString() } + telemetry.breadcrumbs
        assertThat(allValues.none { it.contains("pães") || it.contains("ovos") }).isTrue()
    }

    @Test
    fun networkError_reportsErrorOutcome() = runBlocking<Unit> {
        val telemetry = FakeTelemetry()
        val vm = ChatViewModel(repo, service, clock, memory, photos, telemetry, RequestIds())
        answer = { throw IOException("down") }
        sendAndAwait(vm, "pão")

        assertThat(telemetry.params(TelemetryEvents.CHAT_RESULT))
            .containsExactly(mapOf("outcome" to "error", "has_estimate" to false, "question_only" to false))
    }

    @Test
    fun gravar_reportsMealSaved_withNumbersOnly() = runBlocking<Unit> {
        val telemetry = FakeTelemetry()
        val vm = ChatViewModel(repo, service, clock, memory, photos, telemetry, RequestIds())
        val cafe = slotIds().first()
        answer = { estimateOut(cafe.toString()) }
        sendAndAwait(vm, "2 pães franceses com 2 ovos mexidos")
        vm.register(vm.uiState.value.actions!!.estimateId)
        vm.await { it.actions == null && it.items.any { i -> i is ChatItem.Receipt } }

        assertThat(telemetry.params(TelemetryEvents.MEAL_SAVED))
            .containsExactly(mapOf("from" to "chat", "has_photo" to false, "kcal" to 380, "had_plan" to false, "kind" to "slot", "day" to "today"))
    }

    // ------------------------------------------------------------------ A27: meal text and intent

    private suspend fun sendAndAwaitReply(vm: ChatViewModel, text: String) {
        val before = vm.uiState.value.items.count { it is ChatItem.Assistant }
        vm.setComposer(text)
        vm.send()
        vm.await { !it.sending && it.items.count { i -> i is ChatItem.Assistant } > before }
    }

    private fun cafeOut(slot: Long, question: String?, mealText: String?, intent: String? = "log") = ChatOut(
        reply = "Café de ontem: 2 ovos mexidos, pão francês e leite.",
        intent = intent,
        estimate = ChatEstimate(
            kcal = 430.0, p = 25.0, c = 38.0, g = 20.0,
            confidence = if (question == null) "high" else "medium",
            question = question,
            items = listOf(ItemOut("2 ovos mexidos"), ItemOut("pão francês"), ItemOut("200 ml leite")),
            suggestedSlot = slot.toString(),
            mealText = mealText,
        ),
        model = "gpt-6-luna",
    )

    /** Café igual ao de ontem → pergunta do leite → resposta → Gravar. */
    private suspend fun cafeWithMilkAnswer(vm: ChatViewModel, mealText: String?): Long {
        val cafe = slotIds().first()
        answer = { cafeOut(cafe, "O leite era integral ou desnatado?", mealText = null) }
        sendAndAwaitReply(vm, "café igual ao de ontem")
        answer = { cafeOut(cafe, question = null, mealText = mealText) }
        sendAndAwaitReply(vm, "Sempre uso leite semi desnatado")
        vm.register(vm.uiState.value.actions!!.estimateId)
        vm.await { it.actions == null && it.items.any { i -> i is ChatItem.Receipt } }
        return cafe
    }

    @Test
    fun gravarAfterAnswer_recordsServerMealText_notTheAnswer() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val meal = "2 ovos mexidos, 1 pão francês c/ manteiga, 200 ml leite semidesnatado"
        cafeWithMilkAnswer(vm, mealText = meal)

        assertThat(repo.observeToday().first().logs.single().text).isEqualTo(meal)
        val stored = repo.observeMessages().first().last { it.role == "assistant" }
        assertThat(stored.estimateMealText).isEqualTo(meal)
        assertThat(stored.intent).isEqualTo("log")
    }

    @Test
    fun gravarAfterAnswer_withoutMealText_recordsFirstMessageOfTheChain() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        cafeWithMilkAnswer(vm, mealText = null)
        assertThat(repo.observeToday().first().logs.single().text).isEqualTo("café igual ao de ontem")
    }

    @Test
    fun oldServer_blankMealText_recordsTheUserText() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val cafe = slotIds().first()
        answer = { cafeOut(cafe, question = null, mealText = "  ", intent = null) }
        sendAndAwaitReply(vm, "o de sempre")
        vm.register(vm.uiState.value.actions!!.estimateId)
        vm.await { it.actions == null && it.items.any { i -> i is ChatItem.Receipt } }
        assertThat(repo.observeToday().first().logs.single().text).isEqualTo("o de sempre")
    }

    @Test
    fun photoOnly_withoutMealText_recordsTheAiText() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val almoco = slotIds()[1]
        answer = { estimateOut(almoco.toString()).copy(reply = "Prato feito com frango, arroz e feijão.") }
        photos.nextImport = app.fibrai.android.core.photo.PhotoResult.Ready("/photos/pf.jpg")
        vm.onPicked(android.net.Uri.parse("content://media/1"))
        vm.await { it.attachment != null }
        vm.send()
        vm.await { it.actions != null }
        vm.register(vm.uiState.value.actions!!.estimateId)
        vm.await { it.actions == null && it.items.any { i -> i is ChatItem.Receipt } }
        val log = repo.observeToday().first().logs.single()
        assertThat(log.text).isEqualTo("Prato feito com frango, arroz e feijão.")
        assertThat(log.source).isEqualTo("photo")
    }

    @Test
    fun planWithEstimate_showsBubbleAndPanel_registrarAssimRecords() = runBlocking<Unit> {
        val telemetry = FakeTelemetry()
        val vm = ChatViewModel(repo, service, clock, memory, photos, telemetry)
        val jantar = slotIds()[3]
        answer = {
            cafeOut(jantar, question = "Vai usar muçarela?", mealText = "pizza de pão sírio", intent = "plan")
                .copy(reply = "Pão sírio 80 g, muçarela 60 g, tomate 50 g. Total: 495 kcal · 47P · 39C · 15G.")
        }
        sendAndAwaitReply(vm, "vou fazer pizza de pão sírio, quantas gramas?")

        val ui = vm.uiState.value
        // A29: Registrar assim instead of Gravar | Trocar | Pular; no card, no question bubble.
        assertThat(ui.actions!!.plan).isTrue()
        assertThat(ui.actions!!.record!!.name).isEqualTo("Jantar")
        assertThat(ui.items.filterIsInstance<ChatItem.Question>()).isEmpty()
        val bot = ui.items.filterIsInstance<ChatItem.Assistant>().single()
        assertThat(bot.estimate).isNull()
        assertThat(bot.text).startsWith("Pão sírio 80 g")
        assertThat(bot.plan!!.projected).isEqualTo(Macros(430, 25, 38, 20))
        assertThat(bot.plan!!.ceilingKcal).isEqualTo(2000)
        val stored = repo.observeMessages().first().single { it.role == "assistant" }
        assertThat(stored.intent).isEqualTo("plan")
        assertThat(stored.estimateKcal).isEqualTo(430)
        assertThat(stored.estimateMealText).isEqualTo("pizza de pão sírio")
        assertThat(telemetry.params(TelemetryEvents.CHAT_RESULT))
            .containsExactly(mapOf("outcome" to "ok", "has_estimate" to true, "confidence" to "medium", "intent" to "plan", "question_only" to false, "record" to "missing"))

        vm.recordPlan(stored.id)
        vm.await { it.actions == null && it.items.any { i -> i is ChatItem.Receipt } }
        val log = repo.observeToday().first().logs.single()
        assertThat(log.slotId).isEqualTo(jantar)
        assertThat(log.kcal).isEqualTo(430)
        assertThat(log.text).isEqualTo("pizza de pão sírio")
        assertThat(vm.uiState.value.items.filterIsInstance<ChatItem.Receipt>().single().slotName).isEqualTo("Jantar")
        // Recorded: the panel counts the plan once (0 → 430), never on top of itself (430 → 860).
        val recorded = vm.await { it.items.filterIsInstance<ChatItem.Assistant>().single().plan!!.eatenKcal == 0 }
        assertThat(recorded.items.filterIsInstance<ChatItem.Assistant>().single().plan!!.projected.kcal).isEqualTo(430)
        assertThat(requests).hasSize(1)
        // No memory line for a plan (A28: only memory_updates change the memory).
        assertThat(memoryFile.writes).isEqualTo(0)
    }

    @Test
    fun planAfterOpenLog_takesTheActions_lastEstimateRule() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val cafe = slotIds().first()
        answer = { cafeOut(cafe, question = null, mealText = "café", intent = "log") }
        sendAndAwaitReply(vm, "comi o café")
        val logId = vm.uiState.value.actions!!.estimateId
        answer = { cafeOut(cafe, question = null, mealText = "pizza", intent = "plan") }
        sendAndAwaitReply(vm, "vou fazer pizza?")
        // A29: a plan is an estimate too; the last one without a receipt owns the bar.
        val actions = vm.uiState.value.actions!!
        assertThat(actions.estimateId).isNotEqualTo(logId)
        assertThat(actions.plan).isTrue()
        val cards = vm.uiState.value.items.filterIsInstance<ChatItem.Assistant>().map { (it.estimate != null) to (it.plan != null) }
        assertThat(cards).containsExactly(true to false, false to true).inOrder()
    }

    @Test
    fun questionIntent_isOnlyTheBubble() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        answer = { ChatOut(reply = "Banana média tem uns 90 kcal.", intent = "question", model = "gpt-6-luna") }
        sendAndAwaitReply(vm, "quantas calorias tem uma banana?")
        assertThat(vm.uiState.value.actions).isNull()
        assertThat(vm.uiState.value.items.filterIsInstance<ChatItem.Assistant>().single().estimate).isNull()
    }

    @Test
    fun send_carriesRecentMealsAndRemainingKcal() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val cafe = slotIds().first()
        db.mealLogDao().insert(MealLogEntity(date = "2026-09-24", text = "café de ontem", kcal = 440, p = 25, slotId = cafe, carbs = 38, fat = 22))
        db.mealLogDao().insert(MealLogEntity(date = "2026-09-17", text = "velho", kcal = 1))
        sendAndAwaitReply(vm, "café igual ao de ontem")
        val body = requests.single()
        assertThat(body.recent.map { it.text }).containsExactly("café de ontem")
        assertThat(body.recent.single().slotName).isEqualTo("Café da manhã")
        assertThat(body.day.remainingKcal).isEqualTo(2000)
    }

    // ------------------------------------------------------------------ A29: plan, memory notices, routine

    private fun planOut(slot: String?, kcal: Double = 420.0) = ChatOut(
        reply = "Para caber nas 560 kcal que sobram hoje:\n• 1 pão sírio (60 g)\nTotal: ~420 kcal · 40P · 38C · 12G",
        intent = "plan",
        estimate = ChatEstimate(
            kcal = kcal, p = 40.0, c = 38.0, g = 12.0, confidence = "high",
            items = listOf(ItemOut("pão sírio")), suggestedSlot = slot, mealText = "pizza de pão sírio",
        ),
        model = "gpt-6-luna",
    )

    @Test
    fun registrarAssim_withoutSlot_opensTrocarWithNothingPicked() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        answer = { planOut(slot = null) }
        sendAndAwaitReply(vm, "o que como na janta?")
        val actions = vm.uiState.value.actions!!
        assertThat(actions.plan).isTrue()
        assertThat(actions.record).isNull()

        vm.recordPlan(actions.estimateId)
        val ui = vm.await { it.sheetFor != null }
        assertThat(ui.sheetFor).isEqualTo(actions.estimateId)
        assertThat(ui.sheetSelection).isNull()
        assertThat(repo.observeToday().first().logs).isEmpty()

        val almoco = slotIds()[1]
        vm.selectInSheet(almoco)
        vm.confirmSheet()
        vm.await { it.actions == null && it.items.any { i -> i is ChatItem.Receipt } }
        assertThat(repo.observeToday().first().logs.single().slotId).isEqualTo(almoco)
    }

    @Test
    fun registrarAssim_onTakenSlot_asksToReplace() = runBlocking<Unit> {
        val jantar = slotIds()[3]
        repo.addLog(window = "", text = "sopa", kcal = 300, p = 10, stable = true, slotId = jantar)
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        answer = { planOut(jantar.toString()) }
        sendAndAwaitReply(vm, "vou fazer pizza")
        val estimateId = vm.uiState.value.actions!!.estimateId
        vm.recordPlan(estimateId)
        val ui = vm.await { it.replacePrompt() != null }
        assertThat(ui.replacePrompt()!!.confirm.oldKcal).isEqualTo(300)
        assertThat(ui.replacePrompt()!!.confirm.newKcal).isEqualTo(420)

        vm.confirmReplace(estimateId)
        vm.await { it.receipts().any { r -> r.kind == ReceiptKind.REPLACED } }
        assertThat(repo.observeToday().first().logs.single().kcal).isEqualTo(420)
    }

    @Test
    fun planPanel_followsARecordMadeAfterIt_andGoesBadAboveTheCeiling() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val jantar = slotIds()[3]
        answer = { planOut(jantar.toString()) }
        sendAndAwaitReply(vm, "vou fazer pizza")
        fun panel() = vm.uiState.value.items.filterIsInstance<ChatItem.Assistant>().single().plan!!
        assertThat(panel().eatenKcal).isEqualTo(0)
        assertThat(panel().projected.kcal).isEqualTo(420)

        repo.addLog(window = "", text = "almoço", kcal = 1640, p = 86, stable = true, slotId = slotIds()[1], carbs = 152, fat = 46)
        vm.await { it.items.filterIsInstance<ChatItem.Assistant>().single().plan!!.eatenKcal == 1640 }
        assertThat(panel().projected).isEqualTo(Macros(2060, 126, 190, 58))
        assertThat(panel().targets).isEqualTo(Macros(0, 150, 200, 67))
        assertThat(panel().over).isTrue() // 2060 > 2000
    }

    @Test
    fun memoryNotices_mappedOnTheBubbleAndTheReceipt() = runBlocking<Unit> {
        memory.apply(listOf(MemoryUpdate("add", null, "permanent", "preference", "leite", "Leite semidesnatado")), today)
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val cafe = slotIds().first()
        answer = {
            estimateOut(cafe.toString()).copy(
                memoryUsed = listOf("P1"),
                memoryUpdates = listOf(
                    update("add", null, "dynamic", "portion", "pao", "Pão integral"),
                    update("add", null, "dynamic", "routine", "cafe", "café de sempre", cafe.toString()),
                ),
            )
        }
        sendAndAwait(vm, "café de sempre com pão integral")
        val bot = vm.uiState.value.items.filterIsInstance<ChatItem.Assistant>().single()
        assertThat(bot.memory).isEqualTo(MemoryNotice(updated = true, permanent = true, dynamic = false))

        vm.register(vm.uiState.value.actions!!.estimateId)
        // The memory change lands on the receipt right after the record (A34: it is stored with its undo data).
        vm.await { it.items.any { i -> i is ChatItem.Receipt && i.memoryUpdated } }
    }

    private fun routineFact(days: Int, slot: Long, permanent: Boolean = false, id: String = if (permanent) "P1" else "D1") = Fact(
        id = id,
        kind = if (permanent) "permanent" else "dynamic",
        category = "routine",
        key = "cafe",
        text = "2 ovos mexidos, 1 pão francês c/ manteiga, 200 ml leite semidesnatado, café",
        slot = slot.toString(),
        source = "observed",
        days = (days downTo 1).map { today.minusDays(it.toLong()).toString() },
        created = today.minusDays(days.toLong()).toString(),
        kcal = 440, p = 25, c = 38, g = 22,
    )

    private suspend fun seedRoutine(vararg facts: Fact) = memory.replaceAll(Memory(NextIds(2, 2), facts.toList()))

    private suspend fun ChatViewModel.routine() = uiState.value.routine

    @Test
    fun routine_strongForTheSlotOfTheHour_showsTheCardLast_reportedOnce() = runBlocking<Unit> {
        val cafe = slotIds().first()
        seedRoutine(routineFact(3, cafe))
        val telemetry = FakeTelemetry()
        val vm = ChatViewModel(repo, service, clock, memory, photos, telemetry)
        val ui = vm.await { it.routine != null }
        assertThat(ui.routine!!.slot.name).isEqualTo("Café da manhã")
        assertThat(ui.routine!!.kcal).isEqualTo(440)
        assertThat(ui.routine!!.permanent).isFalse()
        // Empty day: after the greeting (which carries the meta card), last item.
        assertThat(ui.items.last()).isEqualTo(ChatItem.Routine(ui.routine!!))
        assertThat(ui.items[ui.items.size - 2]).isInstanceOf(ChatItem.Greeting::class.java)
        vm.tick()
        vm.tick()
        assertThat(telemetry.params(TelemetryEvents.ROUTINE_SUGGESTION)).containsExactly(mapOf("action" to "shown"))
        // Never sent, never stored.
        assertThat(requests).isEmpty()
        assertThat(repo.observeMessages().first()).isEmpty()
    }

    @Test
    fun routine_weakOrOtherSlot_noCard_severalTakesTheMostSeen() = runBlocking<Unit> {
        val cafe = slotIds().first()
        seedRoutine(routineFact(2, cafe), routineFact(4, slotIds()[1], id = "D2"))
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        vm.tick()
        assertThat(vm.routine()).isNull()

        seedRoutine(routineFact(3, cafe, id = "D1"), routineFact(1, cafe, permanent = true, id = "P1").copy(days = listOf("2026-09-24", "2026-09-23", "2026-09-22", "2026-09-21")))
        val vm2 = ChatViewModel(repo, service, clock, memory, photos)
        assertThat(vm2.await { it.routine != null }.routine!!.factId).isEqualTo("P1")
    }

    @Test
    fun routine_goesAwayOnSend_onSkip_onRecord_andWhenTheHourMoves() = runBlocking<Unit> {
        val cafe = slotIds().first()
        seedRoutine(routineFact(3, cafe))

        val sent = ChatViewModel(repo, service, clock, memory, photos)
        sent.await { it.routine != null }
        answer = { ChatOut(reply = "ok", intent = "question", model = "gpt-6-luna") }
        sendAndAwaitReply(sent, "oi")
        assertThat(sent.routine()).isNull()
        assertThat(sent.uiState.value.items.none { it is ChatItem.Routine }).isTrue()

        val moved = ChatViewModel(repo, service, clock, memory, photos)
        moved.await { it.routine != null }
        now = Instant.parse("2026-09-25T12:40:00-03:00")
        moved.tick()
        moved.await { it.routine == null }

        now = Instant.parse("2026-09-25T08:10:00-03:00")
        val skipped = ChatViewModel(repo, service, clock, memory, photos)
        skipped.await { it.routine != null }
        repo.addSkip(cafe)
        skipped.await { it.routine == null }

        db.slotSkipDao().delete(today.toString(), cafe)
        val recorded = ChatViewModel(repo, service, clock, memory, photos)
        recorded.await { it.routine != null }
        repo.addLog(window = "", text = "outro café", kcal = 200, p = 5, stable = true, slotId = cafe)
        recorded.await { it.routine == null }
    }

    @Test
    fun routineRegistrar_recordsItsNumbers_reinforces_receiptWithMemoryUpdated_noPost() = runBlocking<Unit> {
        val cafe = slotIds().first()
        seedRoutine(routineFact(3, cafe))
        val telemetry = FakeTelemetry()
        val vm = ChatViewModel(repo, service, clock, memory, photos, telemetry)
        vm.await { it.routine != null }

        vm.recordRoutine()
        val ui = vm.await { it.routine == null && it.items.any { i -> i is ChatItem.Receipt && i.memoryUpdated } }
        val log = repo.observeToday().first().logs.single()
        assertThat(log.slotId).isEqualTo(cafe)
        assertThat(log.text).isEqualTo("2 ovos mexidos, 1 pão francês c/ manteiga, 200 ml leite semidesnatado, café")
        assertThat(listOf(log.kcal, log.p, log.carbs, log.fat)).containsExactly(440, 25, 38, 22).inOrder()
        assertThat(log.source).isEqualTo("routine")
        val receipt = ui.items.filterIsInstance<ChatItem.Receipt>().single()
        assertThat(receipt.slotName).isEqualTo("Café da manhã")
        assertThat(receipt.slotTime).isEqualTo("07:30")
        assertThat(receipt.kcal).isEqualTo(440)
        assertThat(receipt.memoryUpdated).isTrue()
        assertThat(memory.read(today).facts.single().days).contains(today.toString())
        assertThat(requests).isEmpty()
        assertThat(telemetry.params(TelemetryEvents.ROUTINE_SUGGESTION).map { it["action"] }).containsExactly("shown", "record").inOrder()
        assertThat(telemetry.params(TelemetryEvents.MEMORY_CHANGED)).hasSize(1)
    }

    @Test
    fun routineQuaseIgual_fillsTheComposer_focuses_recordsNothing() = runBlocking<Unit> {
        val cafe = slotIds().first()
        seedRoutine(routineFact(3, cafe))
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        vm.await { it.routine != null }
        vm.setComposer("rascunho")

        vm.editRoutine()
        val ui = vm.await { it.focusComposer == 1 }
        assertThat(ui.composer).isEqualTo("2 ovos mexidos, 1 pão francês c/ manteiga, 200 ml leite semidesnatado, café")
        // Still there until something is sent.
        assertThat(ui.routine).isNotNull()
        assertThat(repo.observeToday().first().logs).isEmpty()
        assertThat(requests).isEmpty()
        assertThat(memoryFile.writes).isEqualTo(1) // only the seed
    }

    // ------------------------------------------------------------------ A30: questions before the estimate

    private val dinner = "Jantei macarrão com frango ao molho branco"

    private fun questionOut(question: String) = ChatOut(
        reply = "Entendi: macarrão com frango ao molho branco.\n$question",
        intent = "log",
        estimate = null,
        question = question,
        model = "gpt-6-luna",
    )

    private fun dinnerOut(slot: Long, mealText: String?) = ChatOut(
        reply = "Macarrão com frango grelhado ao molho branco.",
        intent = "log",
        estimate = ChatEstimate(
            kcal = 820.0, p = 48.0, c = 92.0, g = 26.0, confidence = "medium",
            items = listOf(ItemOut("macarrão"), ItemOut("frango grelhado")), suggestedSlot = slot.toString(), mealText = mealText,
        ),
        model = "gpt-6-luna",
    )

    /** Sends [text] (or Forçar estimativa when null) and waits for the answer: a new question or assistant item. */
    private suspend fun turn(vm: ChatViewModel, text: String? = null) {
        val before = vm.uiState.value.items.count { it is ChatItem.Assistant || it is ChatItem.Question }
        if (text == null) {
            vm.forceEstimate()
        } else {
            vm.setComposer(text)
            vm.send()
        }
        vm.await { !it.sending && it.items.count { i -> i is ChatItem.Assistant || i is ChatItem.Question } > before }
    }

    @Test
    fun questionOnly_isStoredWithoutEstimate_andDrawnAsItsQuestionBubble() = runBlocking<Unit> {
        val telemetry = FakeTelemetry()
        val vm = ChatViewModel(repo, service, clock, memory, photos, telemetry, RequestIds())
        answer = { questionOut("O frango foi grelhado ou empanado?") }
        turn(vm, dinner)

        assertThat(requests.single().clarifyRounds).isEqualTo(0)
        assertThat(requests.single().forceEstimate).isFalse()
        val stored = repo.observeMessages().first().last { it.role == "assistant" }
        assertThat(stored.text).isEqualTo("Entendi: macarrão com frango ao molho branco.\nO frango foi grelhado ou empanado?")
        assertThat(stored.estimateQuestion).isEqualTo("O frango foi grelhado ou empanado?")
        assertThat(stored.intent).isEqualTo("log")
        assertThat(stored.estimateKcal).isNull()

        val ui = vm.uiState.value
        assertThat(ui.items.filterIsInstance<ChatItem.Assistant>()).isEmpty()
        val question = ui.items.filterIsInstance<ChatItem.Question>().single()
        assertThat(question.standalone).isTrue()
        assertThat(question.text).isEqualTo("O frango foi grelhado ou empanado?")
        assertThat(ui.actions).isNull()
        // Round 1: no Forçar estimativa.
        assertThat(ui.forceEstimate).isFalse()
        assertThat(telemetry.params(TelemetryEvents.CHAT_RESULT).single())
            .isEqualTo(mapOf("outcome" to "ok", "has_estimate" to false, "intent" to "log", "question_only" to true, "record" to "missing"))
    }

    @Test
    fun secondQuestion_showsForce_andForceSendsTheFlagOnce() = runBlocking<Unit> {
        val telemetry = FakeTelemetry()
        val vm = ChatViewModel(repo, service, clock, memory, photos, telemetry, RequestIds())
        val jantar = slotIds().last()
        answer = { questionOut("O molho levou creme de leite?") }
        turn(vm, dinner)
        answer = { questionOut("O frango foi grelhado ou empanado?") }
        turn(vm, "Creme de leite, prato fundo")

        assertThat(requests.map { it.clarifyRounds }).containsExactly(0, 1).inOrder()
        assertThat(vm.uiState.value.forceEstimate).isTrue()
        // A draft in the composer stays where it is.
        vm.setComposer("rascunho")
        answer = { dinnerOut(jantar, "macarrão com frango grelhado ao molho branco") }
        turn(vm)

        val forced = requests.last()
        assertThat(forced.text).isEqualTo(ChatViewModel.FORCE_TEXT)
        assertThat(forced.forceEstimate).isTrue()
        assertThat(forced.clarifyRounds).isEqualTo(2)
        assertThat(telemetry.params(TelemetryEvents.CHAT_FORCE_ESTIMATE)).containsExactly(mapOf("round" to 2))
        val ui = vm.uiState.value
        assertThat(ui.forceEstimate).isFalse()
        assertThat(ui.composer).isEqualTo("rascunho")
        assertThat(ui.items.filterIsInstance<ChatItem.User>().last().text).isEqualTo("Pode estimar assim.")
        // The estimate comes once, with no follow-up bubble under it.
        val last = ui.items.last() as ChatItem.Assistant
        assertThat(last.estimate!!.kcal).isEqualTo(820)
        assertThat(ui.actions!!.record!!.name).isEqualTo("Jantar")
    }

    @Test
    fun force_isHiddenWithAnAttachment_andWhileSending_andRetryKeepsTheFlag() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        answer = { questionOut("O molho levou creme de leite?") }
        turn(vm, dinner)
        answer = { questionOut("O frango foi grelhado ou empanado?") }
        turn(vm, "Creme de leite")
        assertThat(vm.uiState.value.forceEstimate).isTrue()

        photos.nextImport = app.fibrai.android.core.photo.PhotoResult.Ready("/photos/q.jpg")
        vm.onPicked(android.net.Uri.parse("content://media/9"))
        vm.await { it.attachment != null }
        assertThat(vm.uiState.value.forceEstimate).isFalse()
        vm.forceEstimate()
        assertThat(requests).hasSize(2)
        vm.removeAttachment()
        vm.await { it.attachment == null && it.forceEstimate }

        answer = { throw IOException("down") }
        vm.forceEstimate()
        vm.await { it.items.any { i -> i is ChatItem.Failed } }
        assertThat(vm.uiState.value.forceEstimate).isFalse()
        assertThat(requests.last().forceEstimate).isTrue()

        val jantar = slotIds().last()
        answer = { dinnerOut(jantar, "macarrão com frango") }
        vm.retry()
        vm.await { it.actions != null }
        assertThat(requests.last().forceEstimate).isTrue()
        assertThat(requests.last().text).isEqualTo(ChatViewModel.FORCE_TEXT)
    }

    @Test
    fun threeRounds_thenEstimate_recordsMealText() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val jantar = slotIds().last()
        answer = { questionOut("Q1?") }
        turn(vm, dinner)
        answer = { questionOut("Q2?") }
        turn(vm, "R1")
        answer = { questionOut("Q3?") }
        turn(vm, "R2")
        answer = { dinnerOut(jantar, "macarrão com frango grelhado ao molho branco") }
        turn(vm, "R3")

        assertThat(requests.map { it.clarifyRounds }).containsExactly(0, 1, 2, 3).inOrder()
        assertThat(requests.none { it.forceEstimate }).isTrue()
        vm.register(vm.uiState.value.actions!!.estimateId)
        vm.await { it.items.any { i -> i is ChatItem.Receipt } }
        assertThat(repo.observeToday().first().logs.single().text).isEqualTo("macarrão com frango grelhado ao molho branco")
    }

    @Test
    fun threeRounds_withoutMealText_recordsTheFirstMessage_neverAnAnswer() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val jantar = slotIds().last()
        answer = { questionOut("Q1?") }
        turn(vm, dinner)
        answer = { questionOut("Q2?") }
        turn(vm, "R1")
        answer = { questionOut("Q3?") }
        turn(vm, "R2")
        answer = { dinnerOut(jantar, mealText = null) }
        turn(vm, "R3")
        vm.register(vm.uiState.value.actions!!.estimateId)
        vm.await { it.items.any { i -> i is ChatItem.Receipt } }
        assertThat(repo.observeToday().first().logs.single().text).isEqualTo(dinner)
    }

    // ------------------------------------------------------------------ A32: paging

    /** [count] user rows of today, one second apart, from [now]. */
    private suspend fun filler(count: Int, prefix: String = "f") {
        val at = now
        repeat(count) {
            now = at.plusSeconds(it.toLong())
            repo.insertMessage("user", "$prefix$it")
        }
        now = at.plusSeconds(count.toLong())
    }

    private fun ChatUiState.userTexts() = items.filterIsInstance<ChatItem.User>().map { it.text }

    @Test
    fun paging_last20First_thenOlderPagesOf20() = runBlocking<Unit> {
        filler(45, "m")
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val first = vm.await { it.loaded && it.items.isNotEmpty() }
        assertThat(first.userTexts()).isEqualTo((25..44).map { "m$it" })
        assertThat(first.hasOlder).isTrue()

        vm.loadOlder()
        // One page in flight: a second call before it arrives asks nothing more.
        vm.loadOlder()
        val second = vm.await { it.userTexts().size >= 40 && !it.loadingOlder }
        assertThat(second.userTexts()).isEqualTo((5..44).map { "m$it" })
        assertThat(second.hasOlder).isTrue()

        vm.loadOlder()
        val all = vm.await { it.userTexts().size == 45 && !it.loadingOlder }
        assertThat(all.hasOlder).isFalse()
        vm.loadOlder()
        assertThat(vm.uiState.value.userTexts()).hasSize(45)

        // A new message lands at the newest end of the window.
        sendAndAwait(vm, "2 ovos")
        assertThat(vm.uiState.value.userTexts().last()).isEqualTo("2 ovos")
    }

    @Test
    fun estimateOutsideTheWindow_keepsItsActions_andRecordsItsChainStart() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val jantar = slotIds().last()
        answer = { questionOut("Q1?") }
        turn(vm, dinner)
        answer = { dinnerOut(jantar, mealText = null) }
        turn(vm, "R1")
        val estimateId = vm.uiState.value.actions!!.estimateId
        filler(25)
        val ui = vm.await { it.userTexts().lastOrNull() == "f24" }
        // Drawn: only the newest 20 rows; the estimate is out of the window, its actions are not.
        assertThat(ui.items.filterIsInstance<ChatItem.Assistant>()).isEmpty()
        assertThat(ui.actions!!.estimateId).isEqualTo(estimateId)

        vm.register(estimateId)
        vm.await { it.items.any { i -> i is ChatItem.Receipt } }
        val log = repo.observeToday().first().logs.single()
        assertThat(log.text).isEqualTo(dinner)
        assertThat(log.kcal).isEqualTo(820)
    }

    @Test
    fun clarifyRounds_countTodayEvenBeyondTheWindow() = runBlocking<Unit> {
        filler(25)
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        answer = { questionOut("Q1?") }
        turn(vm, dinner)
        answer = { questionOut("Q2?") }
        turn(vm, "R1")
        // Over 12 raw rows today: compact requests go too; only the turns count here.
        val turns = requests.filter { it.text == dinner || it.text == "R1" }
        assertThat(turns.map { it.clarifyRounds }).containsExactly(0, 1).inOrder()
        assertThat(vm.uiState.value.forceEstimate).isTrue()
        assertThat(vm.uiState.value.emptyDay).isFalse()
    }

    // ------------------------------------------------------------------ A38

    private suspend fun tempFacts() = memory.read(today).facts.filter { it.temp }

    @Test
    fun tempFact_addedOnTheAnswer_survivesRecordsAndEveryReceiptAction() = runBlocking<Unit> {
        val telemetry = FakeTelemetry()
        val vm = ChatViewModel(repo, service, clock, memory, photos, telemetry)
        val (cafe, almoco, lanche, jantar) = slotIds()
        answer = {
            ChatOut(
                reply = "Guardei o rótulo da lasanha.",
                intent = "question",
                memoryUpdates = listOf(update("add", null, "temp", "portion", "lasanha", "Lasanha: 100 g = 150 kcal, 8P 15C 6G")),
                model = "gpt-6-luna",
            )
        }
        sendAndAwaitAuto(vm, "rótulo da lasanha: 150 kcal por 100 g")
        assertThat(storedAssistant().memoryUpdated).isTrue()
        val t1 = tempFacts().single()
        assertThat(t1.id).isEqualTo("T1")
        assertThat(requests.single().tempFacts).isTrue()
        val changed = telemetry.params(TelemetryEvents.MEMORY_CHANGED).single()
        assertThat(changed["temp"]).isEqualTo(1)
        assertThat(changed["temp_add"]).isEqualTo(1)

        // Two recorded meals that used T1: it stays, no origin chip.
        val withT1: () -> ChatOut = { autoOut(cafe, 450.0).copy(memoryUsed = listOf("T1")) }
        now = now.plusSeconds(60)
        answer = withT1
        sendAndAwaitAuto(vm, "comi 300 g da lasanha")
        assertThat(requests.last().facts!!.single { it.id == "T1" }.kind).isEqualTo("temp")
        assertThat(storedAssistant().memoryUsedKinds).isNull()
        val first = vm.await { it.receipts().size == 1 && it.receipts().last().actions.isNotEmpty() }.receipts().last()
        val second = recorded(vm, jantar, 300.0, "comi o resto da lasanha")
        assertThat(tempFacts()).containsExactly(t1)

        // Trocar refeição to an empty slot, Excluir, Editar: T1 as it was.
        vm.receiptAction(first.id, ReceiptAction.MOVE)
        vm.await { it.sheetFor == first.id }
        vm.selectInSheet(lanche)
        vm.confirmSheet()
        val moved = vm.await { it.receipts().lastOrNull()?.kind == ReceiptKind.MOVED && it.receipts().last().actions.isNotEmpty() }.receipts().last()
        assertThat(tempFacts()).containsExactly(t1)
        vm.receiptAction(moved.id, ReceiptAction.DELETE)
        vm.await { it.receipts().single { r -> r.id == moved.id }.mark == ReceiptMark.DELETED }
        assertThat(tempFacts()).containsExactly(t1)
        vm.receiptAction(second.id, ReceiptAction.EDIT)
        vm.await { it.receipts().single { r -> r.id == second.id }.mark == ReceiptMark.EDITED }
        assertThat(tempFacts()).containsExactly(t1)

        // Substituir over a recorded slot, then Desfazer: the previous record comes back, T1 unchanged.
        recorded(vm, almoco, 380.0)
        now = now.plusSeconds(60)
        answer = { autoOut(almoco, 620.0).copy(memoryUsed = listOf("T1")) }
        sendAndAwaitAuto(vm, "na verdade foi lasanha no almoço")
        vm.confirmReplace(storedAssistant().id)
        val replaced = vm.await { it.receipts().lastOrNull()?.kind == ReceiptKind.REPLACED && it.receipts().last().actions.isNotEmpty() }.receipts().last()
        vm.receiptAction(replaced.id, ReceiptAction.UNDO)
        vm.await { it.receipts().last().kind == ReceiptKind.RESTORED }
        assertThat(repo.observeToday().first().logs.single { it.slotId == almoco }.kcal).isEqualTo(380)
        assertThat(tempFacts()).containsExactly(t1)
    }

    @Test
    fun questionSlot_storedOnTheQuestionOnlyRow_andSentBackAsTheSuggestedMeal() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val jantar = slotIds()[3]
        answer = { questionOut("Quantos gramas?").copy(questionSlot = jantar.toString()) }
        turn(vm, dinner)
        assertThat(storedAssistant().estimateSlotId).isEqualTo(jantar)
        assertThat(storedAssistant().estimateKcal).isNull()
        assertThat(vm.uiState.value.actions).isNull()

        answer = { questionOut("Era grelhado?").copy(questionSlot = "999") }
        turn(vm, "200 g")
        assertThat(requests.last().messages.last().text)
            .isEqualTo("Entendi: macarrão com frango ao molho branco.\nQuantos gramas?\n[refeição sugerida: Jantar]")
        // A slot that is not today's is not kept.
        assertThat(storedAssistant().estimateSlotId).isNull()
    }
}
