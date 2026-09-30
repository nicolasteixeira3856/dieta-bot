package com.nutri.android.feature.chat

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nutri.android.core.database.DayRepository
import com.nutri.android.core.database.InstantClock
import com.nutri.android.core.database.MealLogEntity
import com.nutri.android.core.database.MealSlot
import com.nutri.android.core.database.DietaBotDatabase
import com.nutri.android.core.memory.FakeMemoryFile
import com.nutri.android.core.memory.FactMemory
import com.nutri.android.core.photo.FakePhotoFiles
import com.nutri.android.core.network.ChatEstimate
import com.nutri.android.core.network.ChatIn
import com.nutri.android.core.network.ChatMemoryUpdate
import com.nutri.android.core.network.ChatOut
import com.nutri.android.core.network.ItemOut
import com.nutri.android.core.telemetry.ChatFallback
import com.nutri.android.core.telemetry.FakeTelemetry
import com.nutri.android.core.telemetry.RequestIds
import com.nutri.android.core.telemetry.TelemetryEvents
import com.nutri.android.domain.Fact
import com.nutri.android.domain.Macros
import com.nutri.android.domain.Memory
import com.nutri.android.domain.MemoryUpdate
import com.nutri.android.domain.NextIds
import com.nutri.android.domain.RecordedMeal
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
    private lateinit var db: DietaBotDatabase
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
        db = Room.inMemoryDatabaseBuilder(context, DietaBotDatabase::class.java).allowMainThreadQueries().build()
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
        vm.setComposer(text)
        vm.send()
        vm.await { it.actions != null || it.items.any { i -> i is ChatItem.Failed } }
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

        vm.record(estimateId, cafe)
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
    fun trocar_recordsIntoChosenSlot() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val (cafe, _, lanche) = slotIds()
        answer = { estimateOut(cafe.toString()) }
        sendAndAwait(vm, "pão com ovo")
        val estimateId = vm.uiState.value.actions!!.estimateId

        vm.openSheet(estimateId)
        assertThat(vm.uiState.value.sheetSelection).isEqualTo(cafe)
        vm.selectInSheet(lanche)
        vm.confirmSheet()
        vm.await { it.actions == null && it.sheetFor == null }

        assertThat(repo.observeToday().first().logs.single().slotId).isEqualTo(lanche)
        assertThat(requests).hasSize(1)
    }

    @Test
    fun suggestedSlotOutsideProfile_hidesGravar_keepsTrocar() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        answer = { estimateOut("cafe") }
        sendAndAwait(vm, "pão com ovo")
        val actions = vm.uiState.value.actions!!
        assertThat(actions.record).isNull()
        // Pular falls back to the slot of the hour (08:10 -> Café da manhã).
        assertThat(actions.skip!!.name).isEqualTo("Café da manhã")
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

    /** A18 / ADR-017: 4 esfihas in the Jantar, then "também tomei suco" re-estimated as the whole meal. */
    private suspend fun jantarTakenThenJuice(vm: ChatViewModel): Long {
        val jantar = slotIds()[3]
        answer = { estimateOut(jantar.toString()) }
        sendAndAwait(vm, "4 esfihas")
        vm.record(vm.uiState.value.actions!!.estimateId, jantar)
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

    @Test
    fun gravarOnTakenSlot_asksFirst_thenReplacesWithOneLog() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val jantar = jantarTakenThenJuice(vm)

        vm.record(vm.uiState.value.actions!!.estimateId, jantar)
        val ui = vm.await { it.replaceConfirm != null }
        assertThat(ui.replaceConfirm!!.slot.name).isEqualTo("Jantar")
        assertThat(ui.replaceConfirm!!.oldKcal).isEqualTo(380)
        assertThat(ui.replaceConfirm!!.newKcal).isEqualTo(1220)
        // Nothing written until Substituir.
        assertThat(repo.observeToday().first().logs.single().kcal).isEqualTo(380)

        vm.confirmReplace()
        vm.await { it.replaceConfirm == null && it.actions == null && it.items.any { i -> i is ChatItem.Receipt && i.replaced } }

        val log = repo.observeToday().first().logs.single()
        assertThat(log.slotId).isEqualTo(jantar)
        assertThat(log.kcal).isEqualTo(1220)
        assertThat(log.p).isEqualTo(40)
        assertThat(log.carbs).isEqualTo(150)
        assertThat(log.fat).isEqualTo(45)
        assertThat(log.text).isEqualTo("também tomei 2 copos de suco")
        val receipt = vm.uiState.value.items.filterIsInstance<ChatItem.Receipt>().last()
        assertThat(receipt.replaced).isTrue()
        assertThat(receipt.slotName).isEqualTo("Jantar")
        assertThat(receipt.slotTime).isEqualTo("20:00")
        assertThat(receipt.kcal).isEqualTo(1220)
        // No A8 line on Substituir: without memory_updates the memory is untouched (A28).
        assertThat(memoryFile.writes).isEqualTo(0)
        assertThat(requests).hasSize(2)
    }

    @Test
    fun replaceElsewhere_opensTrocarEmpty_roomIntact() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val jantar = jantarTakenThenJuice(vm)
        val estimateId = vm.uiState.value.actions!!.estimateId

        vm.record(estimateId, jantar)
        vm.await { it.replaceConfirm != null }
        vm.replaceElsewhere()
        val ui = vm.await { it.replaceConfirm == null && it.sheetFor != null }
        assertThat(ui.sheetFor).isEqualTo(estimateId)
        assertThat(ui.sheetSelection).isNull()
        assertThat(ui.actions).isNotNull()
        assertThat(repo.observeToday().first().logs.single().kcal).isEqualTo(380)

        // Trocar into an empty slot records at once.
        val lanche = slotIds()[2]
        vm.selectInSheet(lanche)
        vm.confirmSheet()
        vm.await { it.actions == null }
        assertThat(repo.observeToday().first().logs.map { it.slotId to it.kcal }).containsExactly(jantar to 380, lanche to 1220)
    }

    @Test
    fun trocarIntoTakenSlot_asksToo_cancelKeepsRoom() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val jantar = jantarTakenThenJuice(vm)
        vm.openSheet(vm.uiState.value.actions!!.estimateId)
        vm.selectInSheet(jantar)
        vm.confirmSheet()
        vm.await { it.replaceConfirm != null && it.sheetFor == null }

        vm.cancelReplace()
        val ui = vm.await { it.replaceConfirm == null }
        assertThat(ui.actions).isNotNull()
        assertThat(repo.observeToday().first().logs.single().kcal).isEqualTo(380)
    }

    @Test
    fun pular_asksThenSkips_withoutKcal() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val cafe = slotIds().first()
        answer = { estimateOut(cafe.toString()) }
        sendAndAwait(vm, "pão com ovo")

        vm.askSkip(vm.uiState.value.actions!!.skip!!)
        assertThat(vm.uiState.value.skipConfirm!!.name).isEqualTo("Café da manhã")
        vm.confirmSkip()
        vm.await { it.actions == null && it.skipConfirm == null }

        val day = repo.observeToday().first()
        assertThat(day.skippedSlotIds).containsExactly(cafe)
        assertThat(day.logs).isEmpty()
        assertThat(vm.uiState.value.items.filterIsInstance<ChatItem.Receipt>().single().skipped).isTrue()
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

        vm.record(vm.uiState.value.actions!!.estimateId, cafe)
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
        vm.askSkip(vm.uiState.value.actions!!.skip!!)
        vm.confirmSkip()
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
        photos.nextImport = com.nutri.android.core.photo.PhotoResult.Ready("/photos/pf.jpg")
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
        photos.nextImport = com.nutri.android.core.photo.PhotoResult.TooLarge
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
        photos.nextImport = com.nutri.android.core.photo.PhotoResult.Ready("/photos/x.jpg")
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
        photos.nextImport = com.nutri.android.core.photo.PhotoResult.Ready("/photos/x.jpg")
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
        photos.nextImport = com.nutri.android.core.photo.PhotoResult.Ready("/photos/a.jpg")
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
        photos.nextImport = com.nutri.android.core.photo.PhotoResult.Ready("/photos/cam.jpg")
        vm.newCapture()
        vm.onCaptured(true)
        vm.await { it.attachment == "/photos/cam.jpg" }
        assertThat(requests).isEmpty()
    }

    @Test
    fun removeAttachment_deletesFile_andDisablesSend() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        photos.nextImport = com.nutri.android.core.photo.PhotoResult.Ready("/photos/b.jpg")
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
        photos.nextImport = com.nutri.android.core.photo.PhotoResult.Ready("/photos/c1.jpg")
        vm.onPicked(android.net.Uri.parse("content://media/6"))
        vm.await { it.attachment == "/photos/c1.jpg" }
        photos.nextImport = com.nutri.android.core.photo.PhotoResult.Ready("/photos/c2.jpg")
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
        photos.nextImport = com.nutri.android.core.photo.PhotoResult.Ready("/photos/d.jpg")
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
        assertThat(question.estimateId).isEqualTo((items[bot] as ChatItem.Assistant).id)
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
            .containsExactly(mapOf("outcome" to "fallback", "has_estimate" to false))
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
            .containsExactly(mapOf("outcome" to "ok", "has_estimate" to true, "confidence" to "high", "intent" to "none"))
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
            .containsExactly(mapOf("outcome" to "error", "has_estimate" to false))
    }

    @Test
    fun gravar_reportsMealSaved_withNumbersOnly() = runBlocking<Unit> {
        val telemetry = FakeTelemetry()
        val vm = ChatViewModel(repo, service, clock, memory, photos, telemetry, RequestIds())
        val cafe = slotIds().first()
        answer = { estimateOut(cafe.toString()) }
        sendAndAwait(vm, "2 pães franceses com 2 ovos mexidos")
        vm.record(vm.uiState.value.actions!!.estimateId, cafe)
        vm.await { it.actions == null && it.items.any { i -> i is ChatItem.Receipt } }

        assertThat(telemetry.params(TelemetryEvents.MEAL_SAVED))
            .containsExactly(mapOf("from" to "chat", "has_photo" to false, "kcal" to 380))
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
        vm.record(vm.uiState.value.actions!!.estimateId, cafe)
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
        vm.record(vm.uiState.value.actions!!.estimateId, cafe)
        vm.await { it.actions == null && it.items.any { i -> i is ChatItem.Receipt } }
        assertThat(repo.observeToday().first().logs.single().text).isEqualTo("o de sempre")
    }

    @Test
    fun photoOnly_withoutMealText_recordsTheAiText() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val almoco = slotIds()[1]
        answer = { estimateOut(almoco.toString()).copy(reply = "Prato feito com frango, arroz e feijão.") }
        photos.nextImport = com.nutri.android.core.photo.PhotoResult.Ready("/photos/pf.jpg")
        vm.onPicked(android.net.Uri.parse("content://media/1"))
        vm.await { it.attachment != null }
        vm.send()
        vm.await { it.actions != null }
        vm.record(vm.uiState.value.actions!!.estimateId, almoco)
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
            .containsExactly(mapOf("outcome" to "ok", "has_estimate" to true, "confidence" to "medium", "intent" to "plan"))

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
        vm.recordPlan(vm.uiState.value.actions!!.estimateId)
        val ui = vm.await { it.replaceConfirm != null }
        assertThat(ui.replaceConfirm!!.oldKcal).isEqualTo(300)
        assertThat(ui.replaceConfirm!!.newKcal).isEqualTo(420)

        vm.confirmReplace()
        vm.await { it.items.any { i -> i is ChatItem.Receipt && i.replaced } }
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

        vm.record(vm.uiState.value.actions!!.estimateId, cafe)
        val ui = vm.await { it.items.any { i -> i is ChatItem.Receipt } }
        assertThat(ui.items.filterIsInstance<ChatItem.Receipt>().single().memoryUpdated).isTrue()
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
        val ui = vm.await { it.routine == null && it.items.any { i -> i is ChatItem.Receipt } }
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
}
