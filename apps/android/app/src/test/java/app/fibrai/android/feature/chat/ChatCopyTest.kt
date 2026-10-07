package app.fibrai.android.feature.chat

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
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

/** A61 part B (ADR-048): selecting messages the WhatsApp way and Copiar (chatCP, chatCC). */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class ChatCopyTest {
    private lateinit var context: Context
    private lateinit var db: FibraiDatabase
    private lateinit var storeScope: CoroutineScope
    private lateinit var store: DataStore<Preferences>
    private lateinit var repo: DayRepository
    private val memory = FactMemory(FakeMemoryFile())
    private val photos = FakePhotoFiles()
    private var now = Instant.parse("2026-09-25T20:10:00-03:00")
    private val clock = InstantClock { now }
    private val requests = mutableListOf<ChatIn>()
    private var answer: () -> ChatOut = { recipe() }
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
        val file = File(context.cacheDir, "copy_${System.nanoTime()}.preferences_pb")
        store = PreferenceDataStoreFactory.create(scope = storeScope, produceFile = { file })
        repo = DayRepository(db, clock, store)
        repo.saveProfile("same", 2200, 2200, 2300, List(7) { 2200 }, "zero", 50, true, "2026-09-25")
        repo.saveSlots(listOf(MealSlot(name = "Almoço", minutesFromMidnight = 750), MealSlot(name = "Jantar", minutesFromMidnight = 1200)))
    }

    @After
    fun tearDown() {
        db.close()
        storeScope.cancel()
        Dispatchers.resetMain()
    }

    private fun vm() = ChatViewModel(repo, service, clock, memory, photos, telemetry)

    private fun recipe() = ChatOut(
        reply = RECIPE,
        intent = "plan",
        record = "none",
        estimate = ChatEstimate(kcal = 520.0, p = 46.0, c = 41.0, g = 17.0, confidence = "medium", items = listOf(ItemOut("frango")), mealText = "frango com arroz"),
        model = "gpt-6-luna",
    )

    private suspend fun jantar() = repo.observeToday().first().slots.single { it.name == "Jantar" }.id

    private suspend fun logged() = ChatOut(
        reply = "Identifiquei 2 ovos mexidos.",
        intent = "log",
        record = "auto",
        estimate = ChatEstimate(
            kcal = 180.0, p = 12.0, c = 1.0, g = 14.0, confidence = "high", items = listOf(ItemOut("2 ovos mexidos")),
            suggestedSlot = jantar().toString(), mealText = "2 ovos mexidos",
        ),
        model = "gpt-6-luna",
    )

    private suspend fun ChatViewModel.await(pred: (ChatUiState) -> Boolean) = withTimeout(5_000) { uiState.first(pred) }

    private suspend fun answers() = repo.observeMessages().first().count { it.role == "assistant" }

    private suspend fun send(vm: ChatViewModel, text: String) {
        val before = answers()
        vm.setComposer(text)
        vm.send()
        withTimeout(5_000) { while (answers() <= before) delay(10) }
        vm.await { !it.sending && it.items.none { i -> i is ChatItem.Loading } && it.items.count { i -> i is ChatItem.Assistant } > before }
    }

    private fun ChatUiState.user() = items.filterIsInstance<ChatItem.User>().last()
    private fun ChatUiState.tali() = items.filterIsInstance<ChatItem.Assistant>().last()

    @Test
    fun longPressSelects_tapsToggle_andTheLastRemovalEnds() = runBlocking<Unit> {
        val vm = vm()
        send(vm, "Me passa uma receita de frango?")
        val ui = vm.uiState.value
        val user = ui.user().key
        val tali = ui.tali().key

        vm.tapWhileSelecting(user)
        assertThat(vm.uiState.value.selected).isEmpty()

        vm.longPress(user)
        assertThat(vm.await { it.selected.isNotEmpty() }.selected).containsExactly(user)
        vm.tapWhileSelecting(tali)
        assertThat(vm.await { it.selected.size == 2 }.selected).containsExactly(user, tali)
        // The date pill is not a text bubble.
        vm.tapWhileSelecting(ui.items.first { it is ChatItem.DateSeparator }.key)
        assertThat(vm.uiState.value.selected).hasSize(2)

        vm.tapWhileSelecting(user)
        vm.tapWhileSelecting(tali)
        assertThat(vm.await { it.selected.isEmpty() }.selected).isEmpty()
    }

    @Test
    fun receiptsAreNeverSelected() = runBlocking<Unit> {
        answer = { runBlocking { logged() } }
        val vm = vm()
        send(vm, "jantei 2 ovos mexidos")
        val receipt = vm.await { s -> s.items.any { it is ChatItem.Receipt } }.items.first { it is ChatItem.Receipt }

        vm.longPress(receipt.key)
        assertThat(vm.uiState.value.selected).isEmpty()
        assertThat(receipt.copyText).isNull()
        vm.longPress(vm.uiState.value.user().key)
        vm.await { it.selected.isNotEmpty() }
        vm.tapWhileSelecting(receipt.key)
        assertThat(vm.uiState.value.selected).hasSize(1)
    }

    @Test
    fun copy_givesBothMessagesInOrder_asPlainText_andEndsTheSelection() = runBlocking<Unit> {
        val vm = vm()
        send(vm, "Me passa uma receita de frango?")
        val ui = vm.uiState.value
        // Selected Tali first: the copy follows the conversation, not the taps.
        vm.longPress(ui.tali().key)
        vm.tapWhileSelecting(ui.user().key)
        vm.await { it.selected.size == 2 }

        val text = vm.copySelection()

        assertThat(text).isEqualTo("Me passa uma receita de frango?\n\n$RECIPE_PLAIN")
        assertThat(text).doesNotContain("**")
        assertThat(vm.await { it.selected.isEmpty() }.selected).isEmpty()
        assertThat(telemetry.params(TelemetryEvents.MESSAGE_COPIED)).containsExactly(mapOf("count" to 2, "has_user" to true, "has_tali" to true))
        assertThat(vm.copySelection()).isNull()
    }

    @Test
    fun copyOfOneTaliReply_andTheConfirmation() = runBlocking<Unit> {
        val vm = vm()
        send(vm, "Me passa uma receita de frango?")
        vm.longPress(vm.uiState.value.tali().key)
        vm.await { it.selected.isNotEmpty() }

        assertThat(vm.copySelection()).isEqualTo(RECIPE_PLAIN)
        assertThat(telemetry.params(TelemetryEvents.MESSAGE_COPIED).single()).isEqualTo(mapOf("count" to 1, "has_user" to false, "has_tali" to true))

        vm.showCopied(1)
        assertThat(vm.await { it.copied != null }.copied).isEqualTo(1)
        vm.dismissCopied()
        assertThat(vm.await { it.copied == null }.copied).isNull()
    }

    @Test
    fun closeSendAndTheDayChange_endTheSelection() = runBlocking<Unit> {
        val vm = vm()
        send(vm, "Me passa uma receita de frango?")
        val user = vm.uiState.value.user().key

        vm.longPress(user)
        vm.await { it.selected.isNotEmpty() }
        vm.clearSelection()
        assertThat(vm.await { it.selected.isEmpty() }.selected).isEmpty()

        vm.longPress(user)
        vm.await { it.selected.isNotEmpty() }
        send(vm, "Outra?")
        assertThat(vm.uiState.value.selected).isEmpty()

        vm.longPress(user)
        vm.await { it.selected.isNotEmpty() }
        now = Instant.parse("2026-09-26T00:05:00-03:00")
        vm.tick()
        assertThat(vm.await { it.selected.isEmpty() }.selected).isEmpty()
    }

    @Test
    fun aWipeEndsTheSelection() = runBlocking<Unit> {
        val vm = vm()
        send(vm, "Me passa uma receita de frango?")
        vm.longPress(vm.uiState.value.user().key)
        vm.await { it.selected.isNotEmpty() }

        repo.wipeToday()

        assertThat(vm.await { it.selected.isEmpty() }.selected).isEmpty()
    }

    @Test
    fun aQuestionAndAPhotoCaption_areText_aPhotoAloneIsNot() {
        assertThat(ChatItem.Question(1, "O frango foi grelhado?", "20:15", standalone = true).copyText).isEqualTo("O frango foi grelhado?")
        assertThat(ChatItem.User(2, "almoço de hoje", "20:15", photoPath = "p.jpg").copyText).isEqualTo("almoço de hoje")
        assertThat(ChatItem.User(3, "", "20:15", photoPath = "p.jpg").copyText).isNull()
        assertThat(ChatItem.User(4, "enviando", "20:15", pending = true).copyText).isNull()
        assertThat(ChatItem.Greeting("20:15").copyText).isNull()
        // An addition's card carries the numbers; its template prose is not shown, so not copied.
        assertThat(ChatItem.Assistant(5, "+120 kcal", "20:15", prose = false).copyText).isNull()
    }

    private companion object {
        const val RECIPE = "Frango com arroz:\n" +
            "- **Peito de frango** grelhado\n" +
            "| Item | Gramas |\n| --- | --- |\n| Peito de frango | 120 g |\n| Arroz cozido | 100 g |\n" +
            "1. Grelhe o frango por 8 min.\n" +
            "2. Sirva com o arroz.\n" +
            "Total: ~**520 kcal** · 46P · 41C · 17G"

        const val RECIPE_PLAIN = "Frango com arroz:\n" +
            "- Peito de frango grelhado\n" +
            "Peito de frango: 120 g\nArroz cozido: 100 g\n" +
            "1. Grelhe o frango por 8 min.\n" +
            "2. Sirva com o arroz.\n" +
            "Total: ~520 kcal · 46P · 41C · 17G"
    }
}
