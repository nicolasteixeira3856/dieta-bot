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
import com.nutri.android.core.database.MealSlot
import com.nutri.android.core.database.NutriDatabase
import com.nutri.android.core.memory.FakeMemoryFile
import com.nutri.android.core.memory.MemoryStore
import com.nutri.android.core.photo.FakePhotoFiles
import com.nutri.android.core.network.ChatEstimate
import com.nutri.android.core.network.ChatIn
import com.nutri.android.core.network.ChatOut
import com.nutri.android.core.network.ItemOut
import java.io.File
import java.io.IOException
import java.time.Instant
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
    private lateinit var db: NutriDatabase
    private lateinit var storeScope: CoroutineScope
    private lateinit var store: DataStore<Preferences>
    private lateinit var repo: DayRepository
    private val memoryFile = FakeMemoryFile()
    private val memory = MemoryStore(memoryFile)
    private val photos = FakePhotoFiles()
    private val clock = InstantClock { Instant.parse("2026-09-25T08:10:00-03:00") }
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
        db = Room.inMemoryDatabaseBuilder(context, NutriDatabase::class.java).allowMainThreadQueries().build()
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

    @Test
    fun gravar_appendsOneMemoryLine_nextPostCarriesIt() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        val cafe = slotIds().first()
        answer = { estimateOut(cafe.toString()) }
        sendAndAwait(vm, "2 pães franceses com 2 ovos mexidos")
        assertThat(requests.single().memory).isEmpty()
        // A send with no Gravar and no question does not touch the memory.
        assertThat(memoryFile.writes).isEqualTo(0)

        vm.record(vm.uiState.value.actions!!.estimateId, cafe)
        vm.await { it.actions == null && it.items.any { i -> i is ChatItem.Receipt } }
        withTimeout(5_000) { while (memoryFile.writes == 0) kotlinx.coroutines.delay(10) }
        assertThat(memory.read()).isEqualTo("Café da manhã: 2 pães franceses com 2 ovos mexidos (380 kcal)")

        vm.setComposer("e um café")
        vm.send()
        withTimeout(5_000) { while (requests.size < 2) kotlinx.coroutines.delay(10) }
        assertThat(requests.last().memory).contains("Café da manhã: 2 pães franceses com 2 ovos mexidos (380 kcal)")
    }

    @Test
    fun answerToAssumption_appendsMemoryLine() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        answer = {
            estimateOut(null).copy(estimate = estimateOut(null).estimate!!.copy(confidence = "medium", question = "O pão era francês ou de forma?"))
        }
        sendAndAwait(vm, "pão com ovo")
        assertThat(memory.read()).isEmpty()

        answer = { ChatOut(reply = "Anotado.", model = "gpt-6-luna") }
        vm.setComposer("francês")
        vm.send()
        withTimeout(5_000) { while (memoryFile.writes == 0) kotlinx.coroutines.delay(10) }
        assertThat(memory.read()).isEqualTo("Respondeu \"O pão era francês ou de forma?\": francês")
    }

    @Test
    fun wipeToday_keepsMemory() = runBlocking<Unit> {
        memory.append("Café da manhã: pão com ovo (300 kcal)")
        repo.wipeToday()
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        answer = { ChatOut(reply = "oi", model = "gpt-6-luna") }
        vm.setComposer("oi")
        vm.send()
        withTimeout(5_000) { while (requests.isEmpty()) kotlinx.coroutines.delay(10) }
        assertThat(requests.single().memory).isEqualTo("Café da manhã: pão com ovo (300 kcal)")
        assertThat(memoryFile.writes).isEqualTo(1)
    }

    @Test
    fun photo_postsImageWithCaption_storesPhotoPath_historyGetsMarker() = runBlocking<Unit> {
        val vm = ChatViewModel(repo, service, clock, memory, photos)
        answer = { estimateOut(null) }
        photos.nextImport = com.nutri.android.core.photo.PhotoResult.Ready("/photos/pf.jpg")
        vm.setComposer("Almoço de hoje")
        vm.onPicked(android.net.Uri.parse("content://media/1"))
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
        vm.await { it.items.any { i -> i is ChatItem.Failed } }
        assertThat(repo.observeMessages().first()).isEmpty()
        assertThat(vm.uiState.value.items.filterIsInstance<ChatItem.User>().single().photoPath).isEqualTo("/photos/x.jpg")

        answer = { estimateOut(null) }
        vm.retry()
        vm.await { it.actions != null }
        assertThat(requests.map { it.imageB64 }).containsExactly("B64:/photos/x.jpg", "B64:/photos/x.jpg")
        assertThat(photos.deleted).isEmpty()
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
}
