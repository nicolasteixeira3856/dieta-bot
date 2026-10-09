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
import app.fibrai.android.core.network.ChatMemoryUpdate
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

/** A64: the day balance on the receipt, the projection of an `ask` estimate, `Anotado`, and `recent_days` on the wire. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class ChatDayBalanceTest {
    private lateinit var db: FibraiDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var repo: DayRepository
    private var now = Instant.parse("2026-09-25T18:00:00-03:00")
    private val clock = InstantClock { now }
    private val requests = mutableListOf<ChatIn>()
    private var answer: () -> ChatOut = { dinnerLog(610) }
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
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(context.cacheDir, "balance_${System.nanoTime()}.preferences_pb") })
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

    private fun dinnerLog(kcal: Int, record: String = "auto", updates: List<ChatMemoryUpdate> = emptyList()) = ChatOut(
        reply = "Jantar: frango e arroz: **$kcal kcal**.",
        intent = "log",
        record = record,
        estimate = ChatEstimate(kcal = kcal.toDouble(), p = 52.0, c = 60.0, g = 14.0, confidence = "high", items = listOf(ItemOut("frango")),
            suggestedSlot = jantar.toString(), mealText = "frango e arroz"),
        memoryUpdates = updates,
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

    @Test
    fun theNewestRecordReceipt_carriesTheDayBalance() = runBlocking<Unit> {
        repo.addLog("", "café", 400, 20, true, almoco)
        val vm = vm()
        send(vm, "jantei frango e arroz")
        val receipts = vm.await { it.items.any { i -> i is ChatItem.Receipt && i.balance != null } }.items.filterIsInstance<ChatItem.Receipt>()
        // 400 + 610 of 2.200; protein 20 + 52 of the default 150.
        assertThat(receipts.last().balance).isEqualTo("1.010 de 2.200 kcal · faltam 78 g de proteína")
        // Excluir: the receipt is marked and the balance leaves it.
        vm.receiptAction(receipts.last().id, ReceiptAction.DELETE)
        vm.await { s -> s.items.filterIsInstance<ChatItem.Receipt>().none { it.balance != null } }
    }

    @Test
    fun anAskEstimate_projectsTheDay_untilItIsRecorded() = runBlocking<Unit> {
        answer = { dinnerLog(610, record = "ask") }
        val vm = vm()
        send(vm, "frango e arroz")
        val bot = vm.uiState.value.items.filterIsInstance<ChatItem.Assistant>().last()
        assertThat(bot.projection).isEqualTo("Projeção: 610 de 2.200 kcal · faltam 98 g de proteína")
        vm.register(vm.uiState.value.actions!!.estimateId)
        val after = vm.await { s -> s.items.any { it is ChatItem.Receipt } }
        assertThat(after.items.filterIsInstance<ChatItem.Assistant>().last().projection).isNull()
        assertThat(after.items.filterIsInstance<ChatItem.Receipt>().last().balance).isEqualTo("610 de 2.200 kcal · faltam 98 g de proteína")
    }

    @Test
    fun anExplicitPermanentFact_isNotedAsStored_aDynamicOneIsNot() = runBlocking<Unit> {
        answer = {
            dinnerLog(
                610,
                updates = listOf(
                    ChatMemoryUpdate("add", null, "permanent", "preference", "leite", "  Usa   leite semidesnatado "),
                    ChatMemoryUpdate("add", null, "dynamic", "portion", "arroz", "Arroz: 150 g"),
                ),
            )
        }
        val vm = vm()
        send(vm, "jantei frango e arroz, e uso sempre leite semidesnatado")
        val bot = vm.uiState.value.items.filterIsInstance<ChatItem.Assistant>().last()
        assertThat(bot.noted).containsExactly("Usa leite semidesnatado")
        // The same statement again changes nothing: nothing new is noted.
        send(vm, "uso leite semidesnatado")
        assertThat(vm.uiState.value.items.filterIsInstance<ChatItem.Assistant>().last().noted).isEmpty()
    }

    @Test
    fun theRequest_carriesTheDaysSinceTheFirstDay_withTheirOwnCeiling() = runBlocking<Unit> {
        now = Instant.parse("2026-09-24T20:00:00-03:00")
        repo.addLog("", "jantar", 1500, 60, true, jantar)
        repo.setWorkout(400)
        now = Instant.parse("2026-09-25T18:00:00-03:00")
        val vm = vm()
        send(vm, "jantei frango e arroz")
        val days = requests.last().recentDays
        // First day 22/09: 24, 23 and 22, newest first.
        assertThat(days.map { it.date }).containsExactly("2026-09-24", "2026-09-23", "2026-09-22").inOrder()
        val yesterday = days.first()
        assertThat(yesterday.recorded).isTrue()
        assertThat(yesterday.kcal).isEqualTo(1500)
        // Eat-back zero: the workout gives no credit.
        assertThat(yesterday.ceilingKcal).isEqualTo(2200)
        assertThat(yesterday.overSlot).isEqualTo(jantar.toString())
        assertThat(yesterday.missingSlots).containsExactly(almoco.toString())
        assertThat(days[1].recorded).isFalse()
    }
}
