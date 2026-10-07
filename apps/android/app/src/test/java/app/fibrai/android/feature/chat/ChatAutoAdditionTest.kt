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
import app.fibrai.android.core.network.ChatOut
import app.fibrai.android.core.network.ItemOut
import app.fibrai.android.core.photo.FakePhotoFiles
import app.fibrai.android.core.telemetry.FakeTelemetry
import app.fibrai.android.core.telemetry.TelemetryEvents
import app.fibrai.android.domain.ReceiptAction
import app.fibrai.android.domain.SlotRecord
import app.fibrai.android.domain.SlotState
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
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A54: an `auto` addition into an empty meal of today is recorded; when it cannot be, the answer says
 * `Não registrado` with a `record_guard` reason, and the next HISTORY does not claim the meal's total.
 * Synthetic foods in the shape of the dev log turn of 2026-10-06 (six slots, four eaten, plans for the dinner first).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class ChatAutoAdditionTest {
    private lateinit var context: Context
    private lateinit var db: FibraiDatabase
    private lateinit var storeScope: CoroutineScope
    private lateinit var store: DataStore<Preferences>
    private lateinit var repo: DayRepository
    private val memory = FactMemory(FakeMemoryFile())
    private val photos = FakePhotoFiles()
    private var now = Instant.parse("2026-10-06T16:50:00-03:00")
    private val clock = InstantClock { now }
    private val requests = mutableListOf<ChatIn>()
    private var answer: suspend (ChatIn) -> ChatOut = { error("no answer") }
    private val service = ChatService { body ->
        requests += body
        answer(body)
    }
    private val telemetry = FakeTelemetry()
    private val ids = mutableListOf<Long>()
    private val jantar get() = ids[4]

    private val eaten = listOf(
        SlotRecord("2 torradas com requeijão e café com leite", 410, 18, 50, 14),
        SlotRecord("1 banana", 90, 1, 23, 0),
        SlotRecord("arroz, feijão e carne assada", 520, 38, 60, 14),
        SlotRecord("1 pote de iogurte natural com aveia", 180, 9, 26, 4),
    )

    /** The added food of the answer: whole totals, item grams as decimals, as the server sends them. */
    private val addition = "2 omeletes de queijo (160 g), feitas com 5 g de manteiga"

    @Before
    fun setUp() = runBlocking<Unit> {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, FibraiDatabase::class.java).allowMainThreadQueries().build()
        storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val file = File(context.cacheDir, "a54_${System.nanoTime()}.preferences_pb")
        store = PreferenceDataStoreFactory.create(scope = storeScope, produceFile = { file })
        repo = DayRepository(db, clock, store)
        repo.saveProfile("same", 2000, 2000, 2300, List(7) { 2000 }, "zero", 50, true, "2026-10-06")
        repo.saveSlots(
            listOf(
                MealSlot(name = "Café", minutesFromMidnight = 450),
                MealSlot(name = "Lanche da manhã", minutesFromMidnight = 600),
                MealSlot(name = "Almoço", minutesFromMidnight = 720),
                MealSlot(name = "Lanche", minutesFromMidnight = 900),
                MealSlot(name = "Jantar", minutesFromMidnight = 1080),
                MealSlot(name = "Ceia", minutesFromMidnight = 1230),
            ),
        )
        ids += repo.observeToday().first().slots.map { it.id }
        eaten.forEachIndexed { i, r -> repo.addLog(window = "", text = r.text, kcal = r.kcal, p = r.p, stable = true, slotId = ids[i], carbs = r.c, fat = r.g) }
    }

    @After
    fun tearDown() {
        db.close()
        storeScope.cancel()
        Dispatchers.resetMain()
    }

    private fun vm() = ChatViewModel(repo, service, clock, memory, photos, telemetry)

    private suspend fun until(pred: suspend () -> Boolean) = withTimeout(5_000) {
        while (!pred()) delay(10)
    }

    private suspend fun send(vm: ChatViewModel, text: String, out: ChatOut) {
        now = now.plusSeconds(30)
        answer = { out }
        val count = requests.size
        val rows = repo.observeMessages().first().count { it.role == "assistant" }
        vm.setComposer(text)
        vm.send()
        until { requests.size > count }
        withTimeout(5_000) { vm.uiState.first { !it.sending } }
        until {
            val all = repo.observeMessages().first()
            val last = all.lastOrNull { it.role == "assistant" }
            all.count { it.role == "assistant" } > rows && (last?.recordMode != "auto" || last.recordState != null)
        }
    }

    private suspend fun lastAssistant(): ChatMessageEntity = repo.observeMessages().first().last { it.role == "assistant" }

    private fun plan(kcal: Int) = ChatOut(
        reply = "Uma opção para o jantar: $kcal kcal.",
        intent = "plan",
        estimate = ChatEstimate(
            kcal.toDouble(), 20.0, 30.0, 10.0, "medium",
            items = listOf(ItemOut("pão", 50.0, kcal.toDouble())),
            suggestedSlot = jantar.toString(),
            mealText = "uma opção de jantar",
        ),
        record = "none",
        model = "gpt-6-luna",
        mealChange = Json.parseToJsonElement("null"),
    )

    private fun questionTurn() = ChatOut(
        reply = "Quer manter o pão ou prefere outra opção?",
        intent = "question",
        record = "none",
        model = "gpt-6-luna",
        mealChange = Json.parseToJsonElement("null"),
    )

    /** The 16:53:56 shape: `log`, `auto`, an addition with base_slot null into the empty dinner, reply rewritten by the server. */
    private fun additionAnswer(target: Long = jantar, kcal: Double = 263.0, change: String? = null) = ChatOut(
        reply = "$addition: +263 kcal\nTotal do Jantar: 263 kcal",
        intent = "log",
        estimate = ChatEstimate(
            kcal, 20.0, 1.0, 24.0, "medium",
            items = listOf(ItemOut("Omelete de queijo", 160.0, 219.0), ItemOut("Manteiga", 5.0, 44.0)),
            suggestedSlot = target.toString(),
            mealText = addition,
        ),
        record = "auto",
        model = "gpt-6-luna",
        mealChange = Json.parseToJsonElement(
            change ?: """{"operation":"add","base_slot":null,"addition":{"meal_text":"$addition","kcal":263,"p":20,"c":1,"g":24,""" +
                """"items":[{"name":"Omelete de queijo","g":160.0,"kcal":219},{"name":"Manteiga","g":5.0,"kcal":44}]}}""",
        ),
    )

    private suspend fun dinnerConversation(vm: ChatViewModel) {
        send(vm, "Não sei o que comer na janta", plan(443))
        send(vm, "Poderia ser algo com pão?", plan(365))
        send(vm, "Pão com ovo e maionese?", plan(280))
        send(vm, "Preciso de mais proteína", questionTurn())
        send(vm, "Quero manter o ovo, adicione mais", plan(280))
    }

    @Test
    fun autoAdditionIntoTheEmptyDinner_afterPlans_isRecorded_withReceipt_andTheNextDaySaysEaten() = runBlocking<Unit> {
        val vm = vm()
        dinnerConversation(vm)
        send(vm, "Vamos com 2 omeletes", additionAnswer())

        assertThat(repo.slotStates("2026-10-06")[jantar]).isEqualTo(SlotState(listOf(SlotRecord(addition, 263, 20, 1, 24, "user"))))
        assertThat(lastAssistant().recordState).isEqualTo(ChatRecorder.RECORDED)
        assertThat(telemetry.events.map { it.first }).contains(TelemetryEvents.MEAL_AUTO_RECORDED)

        send(vm, "obrigado", questionTurn())
        val dinner = requests.last().day.slots.single { it.id == jantar.toString() }
        assertThat(dinner.status).isEqualTo("eaten")
        assertThat(dinner.kcal).isEqualTo(263)
    }

    private fun guards() = telemetry.params(TelemetryEvents.RECORD_GUARD).map { it["reason"] }

    /** The HISTORY line the model reads for the addition answer on the next turn. */
    private suspend fun nextHistoryLine(vm: ChatViewModel): String {
        send(vm, "e agora?", questionTurn())
        return requests.last().messages.single { it.role == "assistant" && it.text.startsWith(addition) }.text
    }

    @Test
    fun excluirAfterTheAutoRecord_nextHistoryKeepsOnlyTheAddedFood_notTheMealTotal() = runBlocking<Unit> {
        val vm = vm()
        dinnerConversation(vm)
        send(vm, "Vamos com 2 omeletes", additionAnswer())
        val receipt = withTimeout(5_000) { vm.uiState.first { s -> s.items.any { it is ChatItem.Receipt } } }
            .items.filterIsInstance<ChatItem.Receipt>().single()
        vm.receiptAction(receipt.id, ReceiptAction.DELETE)
        until { repo.slotStates("2026-10-06")[jantar] == null }

        assertThat(nextHistoryLine(vm)).isEqualTo("$addition: +263 kcal\n[refeição sugerida: Jantar]")
        assertThat(requests.last().day.slots.single { it.id == jantar.toString() }.status).isEqualTo("empty")
    }

    @Test
    fun recordedAddition_nextHistoryKeepsTheWholeReply() = runBlocking<Unit> {
        val vm = vm()
        send(vm, "Vamos com 2 omeletes", additionAnswer())
        assertThat(nextHistoryLine(vm)).isEqualTo("$addition: +263 kcal\nTotal do Jantar: 263 kcal\n[refeição sugerida: Jantar]")
    }

    @Test
    fun dinnerChangedWhileTheAnswerWasOnItsWay_notRecorded_stale() = runBlocking<Unit> {
        val vm = vm()
        now = now.plusSeconds(30)
        answer = {
            repo.addLog(window = "", text = "sopa de legumes", kcal = 200, p = 8, stable = true, slotId = jantar, carbs = 30, fat = 5)
            additionAnswer()
        }
        val count = requests.size
        vm.setComposer("Vamos com 2 omeletes")
        vm.send()
        until { requests.size > count }
        until { repo.observeMessages().first().lastOrNull { it.role == "assistant" }?.recordState != null }

        assertThat(lastAssistant().recordState).isEqualTo(ChatRecorder.NOT_RECORDED)
        assertThat(guards()).containsExactly("stale")
        assertThat(repo.slotStates("2026-10-06")[jantar]!!.records.map { it.text }).containsExactly("sopa de legumes")
    }

    @Test
    fun contradictingTotal_auto_notRecorded_contradicts() = runBlocking<Unit> {
        val vm = vm()
        send(vm, "Vamos com 2 omeletes", additionAnswer(kcal = 300.0))

        assertThat(lastAssistant().recordState).isEqualTo(ChatRecorder.NOT_RECORDED)
        assertThat(guards()).containsExactly("contradicts")
        assertThat(repo.slotStates("2026-10-06")[jantar]).isNull()
    }

    @Test
    fun malformedChange_auto_notRecorded_malformed() = runBlocking<Unit> {
        val vm = vm()
        send(vm, "Vamos com 2 omeletes", additionAnswer(change = """{"operation":"add","base_slot":null}"""))

        assertThat(lastAssistant().recordState).isEqualTo(ChatRecorder.NOT_RECORDED)
        assertThat(guards()).containsExactly("malformed")
        assertThat(repo.slotStates("2026-10-06")[jantar]).isNull()
    }

    @Test
    fun additionThatWouldPassTheBound_auto_notRecorded_overflow() = runBlocking<Unit> {
        val long = "x".repeat(1990)
        repo.addLog(window = "", text = long, kcal = 100, p = 1, stable = true, slotId = jantar, carbs = 1, fat = 1)
        val vm = vm()
        val change = """{"operation":"add","base_slot":"$jantar","addition":{"meal_text":"$addition","kcal":263,"p":20,"c":1,"g":24,""" +
            """"items":[{"name":"Omelete de queijo","g":160.0,"kcal":219},{"name":"Manteiga","g":5.0,"kcal":44}]}}"""
        send(vm, "Vamos com 2 omeletes", additionAnswer(kcal = 363.0, change = change))

        assertThat(lastAssistant().recordState).isEqualTo(ChatRecorder.NOT_RECORDED)
        assertThat(guards()).containsExactly("overflow")
        assertThat(repo.slotStates("2026-10-06")[jantar]!!.records.map { it.text }).containsExactly(long)
    }
}
