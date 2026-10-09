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

/** A65 (ADR-049): a workout reported in the Chat writes the day's number with a receipt and Desfazer, like the Home dialog. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class ChatWorkoutTest {
    private lateinit var db: FibraiDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var repo: DayRepository
    private var now = Instant.parse("2026-09-25T18:00:00-03:00")
    private val clock = InstantClock { now }
    private val requests = mutableListOf<ChatIn>()
    private var answer: () -> ChatOut = { workout(450, "replace") }
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
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(context.cacheDir, "workout_${System.nanoTime()}.preferences_pb") })
        repo = DayRepository(db, clock, store)
        repo.saveProfile("same", 2200, 2200, 2300, List(7) { 2200 }, "zero", 50, true, "2026-09-22")
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

    private fun workout(kcal: Int, mode: String) = ChatOut(
        reply = "Treino de hoje: $kcal kcal.",
        intent = "question",
        record = "auto",
        workout = ChatWorkout(kcal, mode),
        model = "gpt-6-luna",
    )

    private fun dinnerAndWorkout() = ChatOut(
        reply = "Jantar: frango e arroz, 610 kcal. Treino de hoje: 300 kcal.",
        intent = "log",
        record = "auto",
        estimate = ChatEstimate(kcal = 610.0, p = 52.0, c = 60.0, g = 14.0, confidence = "high", items = listOf(ItemOut("frango")),
            suggestedSlot = jantar.toString(), mealText = "frango e arroz"),
        workout = ChatWorkout(300, "replace"),
        model = "gpt-6-luna",
    )

    private suspend fun answers() = repo.observeMessages().first().count { it.role == "assistant" }

    private suspend fun send(vm: ChatViewModel, text: String) {
        val before = answers()
        vm.setComposer(text)
        vm.send()
        withTimeout(5_000) { while (answers() <= before) delay(10) }
        vm.await { !it.sending && it.items.count { i -> i is ChatItem.Assistant } > before }
    }

    private suspend fun ChatViewModel.await(pred: (ChatUiState) -> Boolean) = withTimeout(5_000) { uiState.first(pred) }

    private suspend fun workoutKcal() = repo.observeToday().first().workoutKcal

    private fun ChatUiState.workoutReceipts() = items.filterIsInstance<ChatItem.Receipt>().filter { it.kind == ReceiptKind.WORKOUT || it.kind == ReceiptKind.WORKOUT_ADDED }

    @Test
    fun replace_thenAdd_receipts_andDesfazerRestoresEachNumber() = runBlocking<Unit> {
        val vm = vm()
        send(vm, "treino de hoje 450 kcal")
        assertThat(requests.last().workout).isTrue()
        withTimeout(5_000) { while (workoutKcal() != 450) delay(10) }
        val first = vm.await { it.workoutReceipts().size == 1 }.workoutReceipts().single()
        assertThat(first.kind).isEqualTo(ReceiptKind.WORKOUT)
        assertThat(first.kcal).isEqualTo(450)
        assertThat(first.actions).containsExactly(ReceiptAction.UNDO)
        // A workout-only answer is not a meal left unrecorded.
        assertThat(repo.observeMessages().first().last { it.role == "assistant" }.recordState).isNull()
        assertThat(telemetry.events.last { it.first == TelemetryEvents.WORKOUT_SAVED }.second).isEqualTo(mapOf("from" to "chat", "mode" to "replace", "kcal" to 450))

        answer = { workout(200, "add") }
        send(vm, "mais 200 kcal de treino")
        withTimeout(5_000) { while (workoutKcal() != 650) delay(10) }
        val both = vm.await { s -> s.workoutReceipts().size == 2 && s.workoutReceipts().first().actions.isEmpty() }.workoutReceipts()
        assertThat(both.last().kind).isEqualTo(ReceiptKind.WORKOUT_ADDED)
        assertThat(both.last().kcal).isEqualTo(200)

        vm.receiptAction(both.last().id, ReceiptAction.UNDO)
        withTimeout(5_000) { while (workoutKcal() != 450) delay(10) }
        // The undone receipt is marked; as with slots, the newest receipt of the day's workout owns the actions, so none is left.
        val after = vm.await { s -> s.workoutReceipts().last().mark == ReceiptMark.UNDONE }.workoutReceipts()
        assertThat(after.flatMap { it.actions }).isEmpty()
    }

    @Test
    fun theHomeDialogChangesTheNumber_theReceiptLosesDesfazer() = runBlocking<Unit> {
        val vm = vm()
        send(vm, "treino de hoje 450 kcal")
        vm.await { it.workoutReceipts().singleOrNull()?.actions?.isNotEmpty() == true }
        repo.setWorkout(500)
        vm.await { it.workoutReceipts().single().actions.isEmpty() }
        assertThat(workoutKcal()).isEqualTo(500)
    }

    @Test
    fun aMealAndAWorkoutInOneAnswer_bothWritten_eachWithItsReceipt() = runBlocking<Unit> {
        answer = { dinnerAndWorkout() }
        val vm = vm()
        send(vm, "jantei frango e arroz e treinei 300 kcal")
        withTimeout(5_000) { while (workoutKcal() != 300) delay(10) }
        withTimeout(5_000) { while (repo.slotState("2026-09-25", jantar).kcal != 610) delay(10) }
        val ui = vm.await { s -> s.items.count { it is ChatItem.Receipt } == 2 }
        assertThat(ui.items.filterIsInstance<ChatItem.Receipt>().map { it.kind }).containsExactly(ReceiptKind.LOGGED, ReceiptKind.WORKOUT).inOrder()
    }

    @Test
    fun anOutOfBoundsNumberOrUnknownMode_writesNothing() = runBlocking<Unit> {
        answer = { workout(0, "replace") }
        val vm = vm()
        send(vm, "treino")
        answer = { workout(300, "double") }
        send(vm, "treino de novo")
        assertThat(workoutKcal()).isNull()
        assertThat(vm.uiState.value.workoutReceipts()).isEmpty()
    }

    @Test
    fun aCompactRequestSendsNoCapability() {
        val turn = PromptBuilder.build(app.fibrai.android.feature.home.HomeFixtures.home0, emptyList(), emptyList(), "oi", now)
        assertThat(turn.body.workout).isTrue()
        val block = PromptBuilder.CompactBlock(listOf(app.fibrai.android.core.network.ChatTurn("user", "oi")), 1)
        assertThat(PromptBuilder.compact(turn, block).workout).isFalse()
    }
}
