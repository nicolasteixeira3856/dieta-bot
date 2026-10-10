package app.fibrai.android.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.fibrai.android.core.database.DayRepository
import app.fibrai.android.core.database.InstantClock
import app.fibrai.android.core.database.MealSlot
import app.fibrai.android.core.database.OnboardingAnswerEntity
import app.fibrai.android.core.memory.FactMemory
import app.fibrai.android.core.network.ChatGoal
import app.fibrai.android.core.network.ChatSlot
import app.fibrai.android.core.network.ProfileAnswers
import app.fibrai.android.core.network.ProfileBody
import app.fibrai.android.core.network.ProfileEatBack
import app.fibrai.android.core.network.ProfileIn
import app.fibrai.android.core.network.ProfileNotifications
import app.fibrai.android.core.network.ProfileOut
import app.fibrai.android.core.telemetry.NoopTelemetry
import app.fibrai.android.core.telemetry.Telemetry
import app.fibrai.android.core.telemetry.TelemetryEvents
import app.fibrai.android.domain.MemoryRules
import app.fibrai.android.domain.MemoryUpdate
import app.fibrai.android.domain.OnboardingAnswer
import app.fibrai.android.domain.OnboardingProfile
import app.fibrai.android.domain.OnboardingScript
import app.fibrai.android.domain.OnboardingStep
import app.fibrai.android.domain.OnboardingSummary
import app.fibrai.android.domain.SaoPaulo
import app.fibrai.android.domain.SlotSuggestions
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/**
 * The conversational onboarding (A71, ADR-057): the scripted chat on the device ([OnboardingScript]), every answer saved in
 * Room as it is given, the summary, one `POST /v1/profile` on confirm, and the profile, slots and declared facts written on
 * success. A closed app resumes at the same step (`profile.onboardingPhase` + `onboarding_answer`).
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: DayRepository,
    private val memory: FactMemory,
    private val service: ProfileService,
    private val clock: InstantClock,
    private val telemetry: Telemetry = NoopTelemetry,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private var answers: Map<OnboardingStep, OnboardingAnswer> = emptyMap()
    private var times: Map<OnboardingStep, Long> = emptyMap()
    private var startedAt: Long = clock.now().toEpochMilli()

    /** Invalid answers of the current step: (text, time), each followed by the "não entendi" bubble. */
    private var retries: List<Pair<String, Long>> = emptyList()

    /** The steps a summary pencil reopened, in order; empty outside an edit. */
    private var editing: List<OnboardingStep> = emptyList()
    private var building: Job? = null

    init {
        viewModelScope.launch {
            val day = repository.observeToday().first()
            load(repository.onboardingAnswers())
            val resumed = answers.isNotEmpty() || day.onboardingPhase != DayRepository.PHASE_CHAT
            if (resumed) telemetry.event(TelemetryEvents.ONBOARDING_RESUMED, mapOf("phase" to day.onboardingPhase, "answered" to answers.size))
            when {
                day.onboardingPhase == DayRepository.PHASE_BUILDING && complete() != null -> build()
                day.onboardingPhase == DayRepository.PHASE_ERROR && complete() != null -> _uiState.update { it.copy(screen = OnboardingScreen.ERROR) }
                answers.isEmpty() -> _uiState.update { it.copy(screen = OnboardingScreen.WELCOME) }
                OnboardingScript.nextUnanswered(answers) == null -> showSummary()
                else -> ask(OnboardingScript.nextUnanswered(answers)!!, composer = null)
            }
        }
    }

    // ------------------------------------------------------------------ welcome and chat

    /** `Vamos começar`. */
    fun start() {
        startedAt = clock.now().toEpochMilli()
        telemetry.event(TelemetryEvents.ONBOARDING_STARTED, emptyMap())
        ask(OnboardingScript.first(), composer = null)
    }

    fun setComposer(text: String) = _uiState.update { it.copy(composer = text) }

    fun send() = answer(_uiState.value.composer)

    fun reply(label: String) = answer(label)

    private fun answer(input: String) {
        val step = _uiState.value.step ?: return
        if (_uiState.value.screen != OnboardingScreen.CHAT || input.isBlank()) return
        val now = clock.now().toEpochMilli()
        val parsed = OnboardingScript.parse(step, input, answers)
        if (parsed == null) {
            retries = retries + (input.trim() to now)
            render(composer = "")
            return
        }
        answers = answers + (step to parsed)
        times = times + (step to now)
        retries = emptyList()
        telemetry.event(TelemetryEvents.ONBOARDING_STEP, mapOf("step" to step.telemetry))
        val next = nextStep(step)
        val phase = if (next == null) DayRepository.PHASE_SUMMARY else DayRepository.PHASE_CHAT
        viewModelScope.launch {
            repository.saveOnboardingAnswer(
                OnboardingAnswerEntity(step.name, json.encodeToString(OnboardingAnswer.serializer(), parsed), now),
                phase,
            )
        }
        if (step == OnboardingStep.NOTIFICATIONS && parsed.notifications == true) _uiState.update { it.copy(askNotifications = true) }
        if (next == null) showSummary() else ask(next, composer = null)
    }

    /** The step after [answered]: the next one of an edit, else the first unanswered (or a meal list a new count broke). */
    private fun nextStep(answered: OnboardingStep): OnboardingStep? {
        if (editing.isNotEmpty()) {
            val rest = editing.dropWhile { it != answered }.drop(1)
            editing = rest
            rest.firstOrNull()?.let { return it }
        }
        return OnboardingScript.nextUnanswered(answers)
    }

    fun notificationsAsked() = _uiState.update { it.copy(askNotifications = false) }

    /**
     * Back: in the chat, the previous answer is taken back and its step asked again with that answer in the composer; at the
     * first step the welcome comes back. In an edit, back returns to the summary. On the summary, the last step reopens.
     */
    fun back(): Boolean {
        val ui = _uiState.value
        when (ui.screen) {
            OnboardingScreen.CHAT -> {
                if (editing.isNotEmpty()) {
                    editing = emptyList()
                    retries = emptyList()
                    showSummary()
                    return true
                }
                val current = ui.step ?: return false
                val previous = OnboardingStep.entries.lastOrNull { it.ordinal < current.ordinal && it in answers }
                if (previous == null) {
                    retries = emptyList()
                    _uiState.update { it.copy(screen = OnboardingScreen.WELCOME, step = null, messages = emptyList()) }
                    return true
                }
                takeBack(previous)
                return true
            }
            OnboardingScreen.SUMMARY -> {
                takeBack(OnboardingStep.entries.last())
                return true
            }
            else -> return false
        }
    }

    private fun takeBack(step: OnboardingStep) {
        val text = answers[step]?.text.orEmpty()
        answers = answers - step
        times = times - step
        retries = emptyList()
        viewModelScope.launch { repository.deleteOnboardingAnswer(step.name) }
        ask(step, composer = text)
    }

    private fun ask(step: OnboardingStep, composer: String?) {
        val text = composer
            ?: answers[step]?.text?.takeIf { editing.isNotEmpty() }
            ?: if (step == OnboardingStep.TARGETS) OnboardingScript.prefill(answers) else ""
        _uiState.update { it.copy(screen = OnboardingScreen.CHAT, step = step) }
        render(composer = text)
    }

    /** Rebuilds the thread from the answers: each answered step's question and answer in order, then the current question. */
    private fun render(composer: String) {
        val step = _uiState.value.step ?: return
        val messages = mutableListOf<OnboardingMessage>()
        var last = startedAt
        val shown = OnboardingStep.entries.filter { it in answers && (it.ordinal < step.ordinal || editing.isNotEmpty()) && it != step }
        for (s in shown) {
            messages += OnboardingMessage("q-${s.name}", OnboardingScript.prompt(s, answers), false, hhmm(last))
            val at = times[s] ?: last
            messages += OnboardingMessage("a-${s.name}", answers.getValue(s).text, true, hhmm(at))
            last = at
        }
        val promptIndex = messages.size
        messages += OnboardingMessage("q-${step.name}", OnboardingScript.prompt(step, answers), false, hhmm(last))
        retries.forEachIndexed { i, (text, at) ->
            messages += OnboardingMessage("r-$i", text, true, hhmm(at))
            messages += OnboardingMessage("n-$i", OnboardingScript.NOT_UNDERSTOOD, false, hhmm(at))
        }
        // The viewport opens on the Tali message before the latest user message (golds ob1, ob1e, ob2).
        val lastUser = messages.indexOfLast { it.fromUser }
        val anchor = if (lastUser < 0) promptIndex else (0 until lastUser).lastOrNull { !messages[it].fromUser } ?: 0
        _uiState.update {
            it.copy(
                messages = messages,
                anchor = anchor,
                quickReplies = OnboardingScript.quickReplies(step),
                selectedReply = OnboardingScript.selectedReply(step, answers[step]),
                composer = composer,
            )
        }
    }

    // ------------------------------------------------------------------ summary

    private fun showSummary() {
        editing = emptyList()
        _uiState.update {
            it.copy(screen = OnboardingScreen.SUMMARY, step = null, summary = OnboardingSummary.blocks(answers), composer = "")
        }
    }

    /** A pencil: the block's questions reopen in the chat, each with its answer in the composer, then the summary again. */
    fun edit(blockId: String) {
        val block = OnboardingSummary.blocks(answers).firstOrNull { it.id == blockId } ?: return
        editing = block.steps
        retries = emptyList()
        ask(block.steps.first(), composer = null)
    }

    /** `Confirmar e montar o perfil`. */
    fun confirm() {
        if (complete() == null) return
        build()
    }

    // ------------------------------------------------------------------ build

    /** `Tentar de novo`: the same call. */
    fun retry() = build()

    /** `Voltar ao chat`: the summary, answers kept. */
    fun backToSummary() {
        viewModelScope.launch { repository.setOnboardingPhase(DayRepository.PHASE_SUMMARY) }
        showSummary()
    }

    private fun build() {
        val profile = complete() ?: return
        if (building?.isActive == true) return
        _uiState.update { it.copy(screen = OnboardingScreen.BUILDING) }
        building = viewModelScope.launch {
            repository.setOnboardingPhase(DayRepository.PHASE_BUILDING)
            val outcome = try {
                val out = withTimeout(TIMEOUT_MS) { service.build(request(profile)) }
                store(profile, out)
                null
            } catch (e: TimeoutCancellationException) {
                "timeout"
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                if (e.code() == 400) "blocked" else "server"
            } catch (e: IOException) {
                "network"
            } catch (e: Exception) {
                telemetry.nonFatal(e)
                "server"
            }
            if (outcome == null) return@launch
            repository.setOnboardingPhase(DayRepository.PHASE_ERROR)
            telemetry.event(TelemetryEvents.ONBOARDING_ERROR, mapOf("reason" to outcome))
            _uiState.update { it.copy(screen = OnboardingScreen.ERROR) }
        }
    }

    private fun complete(): OnboardingProfile? = OnboardingScript.profile(answers)

    /** The success: slots and profile, then the declared facts, then the onboarding done and the answers gone. */
    private suspend fun store(profile: OnboardingProfile, out: ProfileOut) {
        val now = clock.now()
        val today = SaoPaulo.date(now)
        val meals = profile.meals.sortedBy { it.minutes }
        val ids = repository.storeOnboardingProfile(meals.map { MealSlot(name = it.name, minutesFromMidnight = it.minutes) }) { p ->
            val kcal = profile.targets.kcal
            p.copy(
                ceilingMode = "same",
                kcalSame = kcal,
                kcalWeekday = kcal,
                kcalWeekend = kcal,
                kcalDays = List(7) { kcal },
                eat = profile.eatBackMode,
                pct = if (profile.eatBackMode == "partial") profile.eatBackPct else p.pct,
                firstDay = today.toString(),
                sex = profile.sex,
                ageYears = profile.age,
                heightCm = profile.heightCm,
                weightKg = profile.weightKg,
                proteinTargetG = profile.targets.p,
                carbTargetG = profile.targets.c,
                fatTargetG = profile.targets.g,
                tone = profile.tone,
                slotMode = "same",
                goalWeightKg = out.goal?.weightKg,
                goalDate = out.goal?.date,
                closureTime = profile.closureTime,
                notificationsEnabled = if (profile.notifications) 1 else 0,
            )
        }
        val slotIds = ids.mapIndexed { i, id -> "s${i + 1}" to id.toString() }.toMap()
        val facts = out.facts.mapNotNull { f ->
            val routine = f.category == MemoryRules.ROUTINE
            val slot = f.slot?.let(slotIds::get)
            if (routine && slot == null) return@mapNotNull null
            MemoryUpdate(MemoryRules.ADD, null, f.kind, f.category, f.key, f.text, slot, f.kcal, f.p, f.c, f.g, declared = true)
        }
        val stored = memory.declare(facts, today)
        repository.finishOnboarding()
        telemetry.event(TelemetryEvents.TONE_SET, mapOf("tone" to profile.tone, "from" to "onboarding"))
        telemetry.event(
            TelemetryEvents.ONBOARDING_COMPLETE,
            mapOf(
                "slots" to meals.size,
                "eat" to profile.eatBackMode,
                "facts" to stored,
                "goal" to (if (out.goal != null) "set" else if (out.goalRefused) "refused" else "none"),
                "notifications" to profile.notifications,
            ),
        )
        _uiState.update { it.copy(screen = OnboardingScreen.SUCCESS, goalRefused = out.goalRefused) }
    }

    private fun request(profile: OnboardingProfile): ProfileIn {
        val meals = profile.meals.sortedBy { it.minutes }
        return ProfileIn(
            localTime = clock.now().atZone(SaoPaulo.zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            body = ProfileBody(profile.sex, profile.age, profile.heightCm, profile.weightKg),
            ceilingKcal = profile.targets.kcal,
            pTarget = profile.targets.p,
            cTarget = profile.targets.c,
            gTarget = profile.targets.g,
            eatBack = ProfileEatBack(profile.eatBackMode, profile.eatBackPct.takeIf { profile.eatBackMode == "partial" }),
            slots = meals.mapIndexed { i, m -> ChatSlot("s${i + 1}", m.name.take(40), SlotSuggestions.format(m.minutes)) },
            tone = profile.tone,
            notifications = ProfileNotifications(profile.notifications, profile.closureTime.takeIf { profile.notifications }),
            goal = profile.goal?.let { ChatGoal(it.weightKg, it.date) },
            answers = ProfileAnswers(profile.restrictions, profile.measuring, profile.foods, profile.dislikes, profile.equipment),
        )
    }

    // ------------------------------------------------------------------ helpers

    private fun load(rows: List<OnboardingAnswerEntity>) {
        val parsed = rows.mapNotNull { row ->
            val step = OnboardingStep.entries.firstOrNull { it.name == row.step } ?: return@mapNotNull null
            val value = runCatching { json.decodeFromString(OnboardingAnswer.serializer(), row.value) }.getOrNull() ?: return@mapNotNull null
            Triple(step, value, row.createdAtEpochMs)
        }
        answers = parsed.associate { it.first to it.second }
        times = parsed.associate { it.first to it.third }
        parsed.minOfOrNull { it.third }?.let { startedAt = it }
    }

    private fun hhmm(epochMs: Long): String = Instant.ofEpochMilli(epochMs).atZone(SaoPaulo.zone).format(HHMM)

    private companion object {
        /** The server's 25 s deadline plus the network. */
        const val TIMEOUT_MS = 30_000L
        val HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val json = Json { ignoreUnknownKeys = true }
    }
}
