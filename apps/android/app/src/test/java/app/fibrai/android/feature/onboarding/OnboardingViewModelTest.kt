package app.fibrai.android.feature.onboarding

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
import app.fibrai.android.core.memory.FactMemory
import app.fibrai.android.core.memory.FakeMemoryFile
import app.fibrai.android.core.network.ChatGoal
import app.fibrai.android.core.network.ProfileFact
import app.fibrai.android.core.network.ProfileIn
import app.fibrai.android.core.network.ProfileOut
import app.fibrai.android.core.telemetry.FakeTelemetry
import app.fibrai.android.core.telemetry.TelemetryEvents
import app.fibrai.android.domain.MemoryRules
import app.fibrai.android.domain.OnboardingScript
import app.fibrai.android.domain.OnboardingStep
import com.google.common.truth.Truth.assertThat
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
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Response

/** A71 (ADR-057): the scripted chat, the resume per phase, the profile build and its failures. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class OnboardingViewModelTest {
    private lateinit var context: Context
    private lateinit var db: FibraiDatabase
    private lateinit var storeScope: CoroutineScope
    private lateinit var store: DataStore<Preferences>
    private lateinit var repo: DayRepository
    private val memoryFile = FakeMemoryFile()
    private val memory = FactMemory(memoryFile)
    private val telemetry = FakeTelemetry()
    private val clock = InstantClock { Instant.parse("2026-10-09T20:10:00-03:00") }
    private val today = LocalDate.parse("2026-10-09")

    private var calls = mutableListOf<ProfileIn>()
    private var reply: (ProfileIn) -> ProfileOut = { ok(it) }
    private val service = ProfileService { body -> calls += body; reply(body) }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, FibraiDatabase::class.java).allowMainThreadQueries().build()
        storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val file = File(context.cacheDir, "test_onboarding_${System.nanoTime()}.preferences_pb")
        store = PreferenceDataStoreFactory.create(scope = storeScope, produceFile = { file })
        repo = DayRepository(db, clock, store)
    }

    @After
    fun tearDown() {
        db.close()
        storeScope.cancel()
        Dispatchers.resetMain()
    }

    private fun vm() = OnboardingViewModel(repo, memory, service, clock, telemetry)

    private suspend fun until(message: String = "condition", check: suspend () -> Boolean) {
        withTimeout(5_000) { while (!check()) delay(20) }
    }

    private fun ok(body: ProfileIn) = ProfileOut(
        goal = body.goal,
        facts = listOf(
            ProfileFact("permanent", "preference", "lactose", "Evita lactose"),
            ProfileFact("dynamic", "routine", "café", "pão com ovo", slot = "s1", kcal = 320, p = 15, c = 32, g = 14),
            ProfileFact("permanent", "equipment", "air fryer", "Tem air fryer"),
        ),
        summary = "Teto 1370 kcal.",
    )

    /** Answers every step with the D27 sample (the fixtures), through the composer or the quick replies. */
    private suspend fun answerAll(vm: OnboardingViewModel, until: OnboardingStep? = null) {
        until("welcome") { vm.uiState.value.screen != OnboardingScreen.LOADING }
        if (vm.uiState.value.screen == OnboardingScreen.WELCOME) vm.start()
        for ((step, answer) in OnboardingFixtures.answers) {
            if (step == until) return
            assertThat(vm.uiState.value.step).isEqualTo(step)
            val text = if (step == OnboardingStep.MEALS) OnboardingScript.DEFAULT_MEALS else answer.text
            vm.setComposer(text)
            vm.send()
        }
    }

    @Test
    fun freshInstall_welcomeThenFirstQuestion() = runBlocking<Unit> {
        val vm = vm()
        until { vm.uiState.value.screen == OnboardingScreen.WELCOME }
        vm.start()
        val ui = vm.uiState.value
        assertThat(ui.screen).isEqualTo(OnboardingScreen.CHAT)
        assertThat(ui.step).isEqualTo(OnboardingStep.SEX)
        assertThat(ui.quickReplies).containsExactly("Feminino", "Masculino").inOrder()
        assertThat(telemetry.params(TelemetryEvents.ONBOARDING_STARTED)).hasSize(1)
    }

    @Test
    fun invalidAnswer_notUnderstoodAndSameStep() = runBlocking<Unit> {
        val vm = vm()
        answerAll(vm, until = OnboardingStep.TONE)
        vm.setComposer("pode ser um meio termo")
        vm.send()
        val ui = vm.uiState.value
        assertThat(ui.step).isEqualTo(OnboardingStep.TONE)
        assertThat(ui.messages.takeLast(2).map { it.text }).containsExactly("pode ser um meio termo", OnboardingScript.NOT_UNDERSTOOD).inOrder()
        assertThat(ui.messages[ui.anchor].key).isEqualTo("q-TONE")
        assertThat(ui.selectedReply).isEqualTo("Seco")
    }

    @Test
    fun targetsStep_prefillsComposer_andShowsCounter() = runBlocking<Unit> {
        val vm = vm()
        answerAll(vm, until = OnboardingStep.TARGETS)
        val ui = vm.uiState.value
        assertThat(ui.composer).isEqualTo("1.370 kcal · P 103 · C 137 · G 46")
        assertThat(ui.counter).isEqualTo("33/2000")
        assertThat(ui.messages[ui.anchor].key).isEqualTo("q-WEIGHT")
    }

    @Test
    fun allAnswered_summary_andEveryAnswerStored() = runBlocking<Unit> {
        val vm = vm()
        answerAll(vm)
        assertThat(vm.uiState.value.screen).isEqualTo(OnboardingScreen.SUMMARY)
        until { repo.onboardingAnswers().size == OnboardingStep.TOTAL }
        until { repo.observeTodayFirst().onboardingPhase == DayRepository.PHASE_SUMMARY }
        assertThat(telemetry.params(TelemetryEvents.ONBOARDING_STEP)).hasSize(OnboardingStep.TOTAL)
        assertThat(telemetry.events.flatMap { it.second.values }.filterIsInstance<String>()).doesNotContain("fígado")
    }

    @Test
    fun killAtStep9_resumesAtStep9() = runBlocking<Unit> {
        val first = vm()
        answerAll(first, until = OnboardingStep.TONE)
        until { repo.onboardingAnswers().size == 8 }
        val resumed = vm()
        until { resumed.uiState.value.screen == OnboardingScreen.CHAT }
        assertThat(resumed.uiState.value.step).isEqualTo(OnboardingStep.TONE)
        assertThat(resumed.uiState.value.messages.count { it.fromUser }).isEqualTo(8)
        assertThat(telemetry.params(TelemetryEvents.ONBOARDING_RESUMED)).isNotEmpty()
    }

    @Test
    fun back_takesTheLastAnswerBackIntoTheComposer() = runBlocking<Unit> {
        val vm = vm()
        answerAll(vm, until = OnboardingStep.HEIGHT)
        assertThat(vm.back()).isTrue()
        assertThat(vm.uiState.value.step).isEqualTo(OnboardingStep.AGE)
        assertThat(vm.uiState.value.composer).isEqualTo("32 anos")
        until { repo.onboardingAnswers().none { it.step == "AGE" } }
    }

    @Test
    fun confirm_buildsProfile_writesFactsAndFinishes() = runBlocking<Unit> {
        val vm = vm()
        answerAll(vm)
        vm.confirm()
        until { vm.uiState.value.screen == OnboardingScreen.SUCCESS }
        val body = calls.single()
        assertThat(body.ceilingKcal).isEqualTo(1370)
        assertThat(body.eatBack.mode).isEqualTo("partial")
        assertThat(body.eatBack.pct).isEqualTo(50)
        assertThat(body.slots.map { it.id }).containsExactly("s1", "s2", "s3", "s4").inOrder()
        assertThat(body.goal).isEqualTo(ChatGoal(60.0, "2027-04-30"))
        assertThat(body.answers.dislikes).isEqualTo("fígado")

        val day = repo.observeTodayFirst()
        assertThat(day.onboardingDone).isTrue()
        assertThat(day.kcalSame).isEqualTo(1370)
        assertThat(day.proteinTargetG).isEqualTo(103)
        assertThat(day.slots.map { it.name }).containsExactly("Café da manhã", "Almoço", "Lanche", "Jantar").inOrder()
        assertThat(day.goalWeightKg).isEqualTo(60.0)
        assertThat(day.closureTime).isEqualTo("22:00")
        assertThat(day.firstDay).isEqualTo("2026-10-09")
        assertThat(repo.onboardingAnswers()).isEmpty()

        val facts = memory.read(today).facts
        assertThat(facts.map { it.source }.toSet()).containsExactly(MemoryRules.DECLARED)
        val routine = facts.single { it.category == MemoryRules.ROUTINE }
        assertThat(routine.slot).isEqualTo(day.slots.minBy { it.minutesFromMidnight }.id.toString())
        assertThat(routine.declared).isTrue()
        assertThat(telemetry.params(TelemetryEvents.ONBOARDING_COMPLETE).single()["facts"]).isEqualTo(3)
    }

    @Test
    fun goalRefused_successWithTheLine() = runBlocking<Unit> {
        reply = { ok(it).copy(goal = null, goalRefused = true) }
        val vm = vm()
        answerAll(vm)
        vm.confirm()
        until { vm.uiState.value.screen == OnboardingScreen.SUCCESS }
        assertThat(vm.uiState.value.goalRefused).isTrue()
        assertThat(repo.observeTodayFirst().goalWeightKg).isNull()
    }

    @Test
    fun serverError_errorScreen_resumesThere_retryReachesSuccess() = runBlocking<Unit> {
        reply = { throw HttpException(Response.error<ProfileOut>(500, "{}".toResponseBody())) }
        val vm = vm()
        answerAll(vm)
        vm.confirm()
        until { vm.uiState.value.screen == OnboardingScreen.ERROR }
        assertThat(telemetry.params(TelemetryEvents.ONBOARDING_ERROR).single()["reason"]).isEqualTo("server")
        until { repo.observeTodayFirst().onboardingPhase == DayRepository.PHASE_ERROR }
        assertThat(repo.onboardingAnswers()).hasSize(OnboardingStep.TOTAL)

        val resumed = vm()
        until { resumed.uiState.value.screen == OnboardingScreen.ERROR }
        reply = { ok(it) }
        resumed.retry()
        until { resumed.uiState.value.screen == OnboardingScreen.SUCCESS }
        assertThat(repo.observeTodayFirst().onboardingDone).isTrue()
    }

    @Test
    fun errorReasons_blockedAndNetwork() = runBlocking<Unit> {
        reply = { throw HttpException(Response.error<ProfileOut>(400, "{\"detail\":\"content_policy_blocked\"}".toResponseBody())) }
        val vm = vm()
        answerAll(vm)
        vm.confirm()
        until { vm.uiState.value.screen == OnboardingScreen.ERROR }
        reply = { throw IOException("offline") }
        vm.retry()
        until { telemetry.params(TelemetryEvents.ONBOARDING_ERROR).size == 2 }
        assertThat(telemetry.params(TelemetryEvents.ONBOARDING_ERROR).map { it["reason"] }).containsExactly("blocked", "network").inOrder()
        vm.backToSummary()
        assertThat(vm.uiState.value.screen).isEqualTo(OnboardingScreen.SUMMARY)
    }

    @Test
    fun editMeals_newCountReopensTheMealLines_thenSummary() = runBlocking<Unit> {
        val vm = vm()
        answerAll(vm)
        vm.edit("meals")
        assertThat(vm.uiState.value.step).isEqualTo(OnboardingStep.MEAL_COUNT)
        assertThat(vm.uiState.value.selectedReply).isEqualTo("4")
        vm.reply("3")
        assertThat(vm.uiState.value.step).isEqualTo(OnboardingStep.MEALS)
        vm.setComposer("Café 07:00\nAlmoço 12:00\nJantar 19:30")
        vm.send()
        assertThat(vm.uiState.value.screen).isEqualTo(OnboardingScreen.SUMMARY)
        val meals = vm.uiState.value.summary.single { it.id == "meals" }.lines
        assertThat(meals).containsExactly("Café 07:00 · Almoço 12:00", "Jantar 19:30").inOrder()
    }

    private suspend fun DayRepository.observeTodayFirst() = withTimeout(5_000) { observeToday().first() }
}
