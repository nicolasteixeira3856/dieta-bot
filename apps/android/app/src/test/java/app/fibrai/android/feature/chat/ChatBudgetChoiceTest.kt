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
import app.fibrai.android.domain.PlanBudget
import com.google.common.truth.Truth.assertThat
import java.io.File
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
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A60 part A (ADR-039): the over-budget choice of a plan (chatRB): when it shows, Pode passar, Ajustar para caber, expiry. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class ChatBudgetChoiceTest {
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
    private var fail = false
    private var answer: () -> ChatOut = { plan(OVER) }
    private val service = ChatService { body ->
        requests += body
        if (fail) error("offline")
        answer()
    }
    private val telemetry = FakeTelemetry()

    @Before
    fun setUp() = runBlocking<Unit> {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, FibraiDatabase::class.java).allowMainThreadQueries().build()
        storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val file = File(context.cacheDir, "budget_${System.nanoTime()}.preferences_pb")
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

    private suspend fun jantar() = repo.observeToday().first().slots.single { it.name == "Jantar" }.id

    private fun plan(budget: String?, kcal: Double = 620.0) = ChatOut(
        reply = "Macarrão com atum ao sugo:\n- 80 g de macarrão cru\nTotal: ~${kcal.toInt()} kcal · 42P · 70C · 18G",
        intent = "plan",
        record = "none",
        estimate = ChatEstimate(kcal = kcal, p = 42.0, c = 70.0, g = 18.0, confidence = "medium", items = listOf(ItemOut("macarrão")),
            suggestedSlot = "2", mealText = "macarrão com atum"),
        planBudget = budget?.let { Json.parseToJsonElement(it) },
        model = "gpt-6-luna",
    )

    private suspend fun ChatViewModel.await(pred: (ChatUiState) -> Boolean) = withTimeout(5_000) { uiState.first(pred) }

    private suspend fun send(vm: ChatViewModel, text: String = "Me passa uma receita de macarrão com atum pro jantar?") {
        val before = answers()
        vm.setComposer(text)
        vm.send()
        settle(vm, before)
    }

    private suspend fun answers() = repo.observeMessages().first().count { it.role == "assistant" }

    /** Until the answer after [before] answers is stored and drawn. */
    private suspend fun settle(vm: ChatViewModel, before: Int) {
        withTimeout(5_000) { while (answers() <= before) kotlinx.coroutines.delay(10) }
        vm.await { !it.sending && it.items.none { i -> i is ChatItem.Loading || i is ChatItem.Failed } && it.items.count { i -> i is ChatItem.Assistant } > before }
    }

    private fun ChatUiState.planItem() = items.filterIsInstance<ChatItem.Assistant>().last()

    @Test
    fun everyNormalTurnSendsTheCapability_compactDoesNot() = runBlocking<Unit> {
        val vm = vm()
        send(vm)
        val body = requests.single()
        assertThat(body.planBudget).isTrue()
        assertThat(body.fitKcal).isNull()
        assertThat(body.profile.tone).isEqualTo("seco")
        val compact = PromptBuilder.compact(PromptBuilder.build(repo.observeToday().first(), emptyList(), emptyList(), "x", now), PromptBuilder.CompactBlock(emptyList(), 1))
        assertThat(compact.planBudget).isFalse()
        assertThat(compact.fitKcal).isNull()
    }

    @Test
    fun overThePlanWindow_showsTheLinesAndThePills() = runBlocking<Unit> {
        val vm = vm()
        send(vm)
        val ui = vm.uiState.value
        assertThat(ui.actions!!.choice).isEqualTo(BudgetChoice(limitKcal = 310, overKcal = 310))
        assertThat(ui.planItem().budget).isEqualTo(BudgetNote(310, listOf(250 to "fatia de bolo")))
        assertThat(PlanBudget.decode(repo.messagesOf("2026-09-25").last().planBudget)!!.overKcal).isEqualTo(310)
    }

    @Test
    fun noChoice_withoutBudget_malformed_fitting_orNothingToAdjustTo() = runBlocking<Unit> {
        val cases = listOf(
            null,
            """{"limit_kcal": "x", "over_kcal": 10}""",
            """[1, 2]""",
            """{"limit_kcal": 900, "over_kcal": 0, "reserved": [], "choice": null}""",
            """{"limit_kcal": 0, "over_kcal": 620, "reserved": [], "choice": null}""",
        )
        for (budget in cases) {
            answer = { plan(budget) }
            val vm = vm()
            send(vm, "plano $budget")
            val ui = vm.uiState.value
            assertThat(ui.actions!!.plan).isTrue()
            assertThat(ui.actions!!.choice).isNull()
            assertThat(ui.planItem().budget).isNull()
        }
    }

    @Test
    fun podePassar_isStored_survivesRecreation_andRegistrarAssimReturns() = runBlocking<Unit> {
        val vm = vm()
        send(vm)
        vm.acceptOver(vm.uiState.value.actions!!.estimateId)
        vm.await { it.actions?.choice == null && it.actions?.plan == true }
        assertThat(vm.uiState.value.planItem().budget).isNull()
        assertThat(requests).hasSize(1)
        assertThat(telemetry.events.last()).isEqualTo(TelemetryEvents.PLAN_BUDGET_CHOICE to mapOf("choice" to "over_ok", "over_kcal" to 310))
        val again = vm()
        again.await { it.loaded && it.actions != null }
        assertThat(again.uiState.value.actions!!.choice).isNull()
        // Registrar assim of an accepted over-budget plan records.
        again.recordPlan(again.uiState.value.actions!!.estimateId)
        withTimeout(5_000) { while (repo.slotState("2026-09-25", jantar()).records.isEmpty()) kotlinx.coroutines.delay(10) }
    }

    @Test
    fun ajustarParaCaber_sendsTheTextAndTarget_retryKeepsIt_stillOverShowsTheChoiceAgain() = runBlocking<Unit> {
        val vm = vm()
        send(vm)
        fail = true
        vm.adjustToFit(vm.uiState.value.actions!!.estimateId)
        vm.await { it.items.any { i -> i is ChatItem.Failed } }
        assertThat(requests.last().text).isEqualTo("Ajusta para caber em 310 kcal.")
        assertThat(requests.last().fitKcal).isEqualTo(310)
        fail = false
        answer = { plan("""{"limit_kcal": 310, "over_kcal": 40, "reserved": [], "choice": "fit"}""", kcal = 350.0) }
        vm.retry()
        settle(vm, 1)
        assertThat(requests.last().fitKcal).isEqualTo(310)
        assertThat(requests.last().text).isEqualTo("Ajusta para caber em 310 kcal.")
        assertThat(vm.uiState.value.actions!!.choice).isEqualTo(BudgetChoice(310, 40))
        // An adjusted plan that fits is chatR.
        answer = { plan("""{"limit_kcal": 310, "over_kcal": 0, "reserved": [], "choice": "fit"}""", kcal = 300.0) }
        vm.adjustToFit(vm.uiState.value.actions!!.estimateId)
        settle(vm, 2)
        assertThat(vm.uiState.value.actions!!.plan).isTrue()
    }

    @Test
    fun theChoiceEnds_onANewSend_aWipe_andADayChange() = runBlocking<Unit> {
        val vm = vm()
        send(vm)
        answer = { ChatOut(reply = "Oi.", intent = "question", record = "none", model = "gpt-6-luna") }
        send(vm, "oi")
        assertThat(vm.uiState.value.actions?.choice).isNull()

        answer = { plan(OVER) }
        send(vm)
        assertThat(vm.uiState.value.actions!!.choice).isNotNull()
        repo.wipeToday()
        vm.await { it.actions == null }

        now = now.plusSeconds(60)
        answer = { plan(OVER) }
        send(vm)
        assertThat(vm.uiState.value.actions!!.choice).isNotNull()
        now = Instant.parse("2026-09-26T00:20:00-03:00")
        vm.tick()
        vm.await { it.actions == null }
    }

    private companion object {
        const val OVER = """{"limit_kcal": 310, "over_kcal": 310, "reserved": [{"label": "fatia de bolo", "kcal": 250}], "choice": null}"""
    }
}
