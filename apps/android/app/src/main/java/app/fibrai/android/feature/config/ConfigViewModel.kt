package app.fibrai.android.feature.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.fibrai.android.core.database.DayRepository
import app.fibrai.android.core.database.DaySnapshot
import app.fibrai.android.core.database.InstantClock
import app.fibrai.android.core.database.ceilingProfile
import app.fibrai.android.core.reset.AppReset
import app.fibrai.android.domain.CreditPolicy
import app.fibrai.android.domain.SaoPaulo
import app.fibrai.android.domain.SlotSuggestions
import app.fibrai.android.domain.workoutCredit
import app.fibrai.android.domain.SlotModes
import app.fibrai.android.feature.workout.WorkoutEditorState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import app.fibrai.android.core.telemetry.NoopTelemetry
import app.fibrai.android.core.telemetry.Telemetry
import app.fibrai.android.core.telemetry.TelemetryEvents
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ConfigViewModel @Inject constructor(
    private val repository: DayRepository,
    private val clock: InstantClock,
    private val appReset: AppReset,
    private val telemetry: Telemetry = NoopTelemetry,
) : ViewModel() {
    private val local = MutableStateFlow(Local())
    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState: StateFlow<ConfigUiState> = _uiState.asStateFlow()

    private var day: DaySnapshot = DaySnapshot()

    private data class Local(
        val editor: ConfigEditor? = null,
        val draft: ConfigDraft = ConfigDraft(),
        val wipeConfirm: Boolean = false,
        val resetConfirm: Boolean = false,
        val resetRunning: Boolean = false,
        val resetDone: Boolean = false,
        val closurePicking: Boolean = false,
    )

    init {
        viewModelScope.launch {
            combine(repository.observeToday(), local) { d, l -> d to l }.collect { (d, l) ->
                day = d
                _uiState.value = ConfigMapper.map(d, SaoPaulo.date(clock.now()))
                    .copy(
                        editor = l.editor,
                        draft = l.draft,
                        wipeConfirm = l.wipeConfirm,
                        resetConfirm = l.resetConfirm,
                        resetRunning = l.resetRunning,
                        resetDone = l.resetDone,
                        closurePicking = l.closurePicking,
                    )
            }
        }
    }

    /** Opens an editor with the stored values. */
    fun open(editor: ConfigEditor) = local.update { Local(editor = editor, draft = ConfigMapper.draftOf(day)) }

    fun close() = local.update { it.copy(editor = null) }

    // Ceiling
    fun setCeilingMode(mode: String) = edit { it.copy(ceilingMode = mode) }
    fun setSame(v: String) = edit { it.copy(sameField = v.digits(5)) }
    fun setWeekday(v: String) = edit { it.copy(weekdayField = v.digits(5)) }
    fun setWeekend(v: String) = edit { it.copy(weekendField = v.digits(5)) }
    fun setDay(index: Int, v: String) = edit {
        it.copy(dayFields = it.dayFields.toMutableList().also { list -> if (index in list.indices) list[index] = v.digits(5) })
    }

    // Eat-back
    fun setEat(mode: String) = edit { it.copy(eat = mode) }
    fun setPct(v: String) = edit { it.copy(pct = v.digits(3)) }

    // Macros
    fun setProtein(v: String) = edit { it.copy(proteinField = v.digits(4)) }
    fun setCarb(v: String) = edit { it.copy(carbField = v.digits(4)) }
    fun setFat(v: String) = edit { it.copy(fatField = v.digits(4)) }

    // Slots: same rules as O3.
    fun setSlotCount(count: Int) = edit { draft ->
        val n = count.coerceIn(SlotSuggestions.MIN_SLOTS, SlotSuggestions.MAX_SLOTS)
        val times = SlotSuggestions.defaultTimes(n)
        draft.copy(slots = List(n) { i -> draft.slots.getOrNull(i) ?: SlotDraft(minutes = times[i]) })
    }

    fun setSlotName(index: Int, name: String) = editSlot(index) { it.copy(name = name.take(40)) }
    fun setSlotTime(index: Int, minutes: Int) = editSlot(index) { it.copy(minutes = minutes.coerceIn(0, 1439)) }

    fun openSlotGroup(index: Int) = local.update {
        val draft = ConfigMapper.draftOf(day)
        val schedule = draft.slotSchedule.copy(index = index.coerceIn(0, draft.slotSchedule.groups.lastIndex))
        Local(editor = ConfigEditor.SLOTS, draft = draft.copy(slotSchedule = schedule, slots = schedule.slots))
    }
    fun setSlotMode(mode: String) = changeSchedule { it.requestMode(mode) }
    fun confirmSlotMode() = changeSchedule { it.confirmMode() }
    fun cancelSlotMode() = changeSchedule { it.copy(pendingMode = null) }
    fun copyPreviousSlots() = changeSchedule { it.copyPrevious() }
    fun previousSlotGroup() {
        if (local.value.draft.slotSchedule.index == 0) close()
        else changeSchedule { it.copy(index = it.index - 1) }
    }
    private fun changeSchedule(change: (SlotScheduleDraft) -> SlotScheduleDraft) = edit {
        val schedule = change(it.slotSchedule.withSlots(it.slots))
        it.copy(slotSchedule = schedule, slots = schedule.slots)
    }

    // Workout
    fun setWorkout(v: String) = edit { it.copy(workoutField = WorkoutEditorState.clean(v)) }

    /** A71 (ADR-057 decision 10): one row turns the meal reminders and the closure notifications on or off. */
    fun toggleNotifications() {
        val enabled = !day.notificationsEnabled
        viewModelScope.launch { repository.saveNotifications(enabled) }
        telemetry.event(TelemetryEvents.NOTIFICATIONS_SET, mapOf("enabled" to enabled, "from" to "config"))
    }

    fun openClosureTime() = local.update { Local(closurePicking = true) }

    fun cancelClosureTime() = local.update { it.copy(closurePicking = false) }

    /** The closure time from the wheel; the alarms follow through PushSync. */
    fun setClosureTime(minutes: Int) {
        local.update { it.copy(closurePicking = false) }
        viewModelScope.launch { repository.saveClosureTime(SlotSuggestions.format(minutes.coerceIn(0, 1439))) }
    }

    // Goal (A71)
    fun setGoalWeight(v: String) = edit { it.copy(goalWeightField = v.replace('.', ',').filter { c -> c.isDigit() || c == ',' }.take(5)) }
    fun setGoalDate(v: String) = edit { it.copy(goalDateField = app.fibrai.android.domain.GoalRules.formatDateField(v)) }

    /** A60 part B (cfgT): the tone picked in the sheet; nothing is stored before Salvar. */
    fun setTone(tone: String) = edit { if (tone == "seco" || tone == "duro") it.copy(tone = tone) else it }

    /**
     * Salvar. A changed ceiling only raises the wipe dialog; everything else is stored at once
     * and never wipes (slot name/time relabels).
     */
    fun save() {
        val l = local.value
        val editor = l.editor ?: return
        val draft = l.draft
        if (!draft.valid(editor)) return
        if (editor == ConfigEditor.SLOTS && !draft.slotSchedule.last) {
            changeSchedule { it.copy(index = it.index + 1) }
            return
        }
        if (editor == ConfigEditor.SLOTS && !draft.slotSchedule.withSlots(draft.slots).valid) return
        if (editor == ConfigEditor.CEILING) {
            local.update {
                if (ceilingChanged(draft)) it.copy(editor = null, wipeConfirm = true) else it.copy(editor = null)
            }
            return
        }
        local.update { it.copy(editor = null) }
        viewModelScope.launch {
            when (editor) {
                ConfigEditor.EAT_BACK -> repository.saveEatBack(draft.eat, draft.pct.toIntOrNull() ?: 50)
                ConfigEditor.MACROS -> repository.saveMacroTargets(
                    draft.proteinField.toIntOrNull() ?: 0,
                    draft.carbField.toIntOrNull() ?: 0,
                    draft.fatField.toIntOrNull() ?: 0,
                )
                ConfigEditor.SLOTS -> repository.saveSlots(
                    draft.slotSchedule.withSlots(draft.slots).meals(),
                    draft.slotSchedule.mode,
                )
                ConfigEditor.WORKOUT -> repository.setWorkout(draft.workoutField.toIntOrNull())
                // No wipe, no confirmation: the next turn uses the new tone.
                ConfigEditor.TONE -> if (draft.tone != day.tone) {
                    repository.saveTone(draft.tone)
                    telemetry.event(TelemetryEvents.TONE_SET, mapOf("tone" to draft.tone, "from" to "config"))
                }
                ConfigEditor.GOAL -> {
                    val goal = draft.goal.takeIf { draft.goalWeightField.isNotBlank() }
                    repository.saveGoal(goal?.weightKg, goal?.date)
                    telemetry.event(TelemetryEvents.GOAL_SET, mapOf("goal" to if (goal == null) "none" else "set", "from" to "config"))
                }
                ConfigEditor.CEILING -> Unit
            }
        }
    }

    /** Default yes: new ceiling + wipeToday (meal_log, skip, digest). Chat and profile stay. */
    fun confirmWipe() {
        val l = local.value
        if (!l.wipeConfirm) return
        val d = l.draft
        local.update { Local() }
        viewModelScope.launch {
            repository.changeCeiling(
                ceilingMode = d.ceilingMode,
                kcalSame = d.sameField.toIntOrNull() ?: day.kcalSame,
                kcalWeekday = d.weekdayField.toIntOrNull() ?: day.kcalWeekday,
                kcalWeekend = d.weekendField.toIntOrNull() ?: day.kcalWeekend,
                kcalDays = d.dayFields.mapIndexed { i, v -> v.toIntOrNull() ?: day.kcalDays.getOrElse(i) { 2000 } },
            )
        }
    }

    /** Cancelar: the new ceiling is dropped, nothing is stored. */
    fun cancelWipe() = local.update { Local() }

    /** Resetar app: the cfgR dialog. Nothing changes until Apagar tudo. */
    fun openReset() = local.update { Local(resetConfirm = true) }

    fun cancelReset() = local.update { if (it.resetRunning) it else Local() }

    /** Apagar tudo (ADR-040): one reset per dialog; success opens O1, a failure closes the dialog and stays. */
    fun confirmReset() {
        if (!local.value.resetConfirm || local.value.resetRunning) return
        local.update { it.copy(resetRunning = true) }
        viewModelScope.launch {
            val done = appReset.run()
            local.update { if (done) Local(resetDone = true) else Local() }
        }
    }

    private fun ceilingChanged(d: ConfigDraft): Boolean {
        if (d.ceilingMode != day.ceilingMode) return true
        val stored = when (day.ceilingMode) {
            "weekdayWeekend" -> listOf(day.kcalWeekday, day.kcalWeekend)
            "seven" -> day.kcalDays
            else -> listOf(day.kcalSame)
        }
        return d.ceilingFields().map { it.toIntOrNull() } != stored
    }

    private fun edit(change: (ConfigDraft) -> ConfigDraft) = local.update { it.copy(draft = change(it.draft)) }

    private fun editSlot(index: Int, change: (SlotDraft) -> SlotDraft) = edit { draft ->
        if (index !in draft.slots.indices) draft
        else draft.copy(slots = draft.slots.toMutableList().also { it[index] = change(it[index]) })
    }

    private fun String.digits(max: Int) = filter { it.isDigit() }.take(max)
}

/** Stored day → cfg rows. Pure: shared by the ViewModel and the gold renders. */
object ConfigMapper {
    const val NO_WORKOUT = "Nenhum informado"

    fun map(day: DaySnapshot, today: LocalDate): ConfigUiState {
        val policy = policyOf(day.eat)
        return ConfigUiState(
            loaded = true,
            ceilingValue = "${day.ceilingProfile().ceilingOn(today)} kcal",
            ceilingDetail = when (day.ceilingMode) {
                "weekdayWeekend" -> "Úteis ${day.kcalWeekday} · fim de semana ${day.kcalWeekend}"
                "seven" -> "Valor diferente por dia"
                else -> "Mesmo valor todos os dias"
            },
            eatBackValue = when (policy) {
                CreditPolicy.ZERO -> "0% (desativado)"
                CreditPolicy.PARTIAL -> "${day.pct}%"
                CreditPolicy.FULL -> "100%"
            },
            macrosValue = "${day.proteinTargetG}g · ${day.carbTargetG}g · ${day.fatTargetG}g",
            toneValue = if (day.tone == "duro") "Duro" else "Seco",
            notificationsValue = if (day.notificationsEnabled) "Ligadas" else "Desligadas",
            closureValue = day.closureTime,
            closureMinutes = app.fibrai.android.domain.Closures.time(day.closureTime).let { it.hour * 60 + it.minute },
            goalValue = day.goalWeightKg?.let { app.fibrai.android.domain.OnboardingScript.goalText(app.fibrai.android.domain.OnboardingGoal(it, day.goalDate)) } ?: "Nenhuma",
            slotMode = day.slotMode,
            slotGroups = SlotModes.groups(day.slotMode).map { group ->
                val rows = day.slots.filter { it.days == group.days }.sortedBy { it.minutesFromMidnight }
                ConfigSlotRow(group.days.toLong(), group.label, if (rows.isEmpty()) "0 refeições" else
                    "${rows.size} refeições · ${SlotSuggestions.format(rows.first().minutesFromMidnight)} a ${SlotSuggestions.format(rows.last().minutesFromMidnight)}")
            },
            slots = day.slots.sortedBy { it.minutesFromMidnight }
                .map { ConfigSlotRow(it.id, it.name, SlotSuggestions.format(it.minutesFromMidnight)) },
            workoutValue = day.workoutKcal?.let { "$it kcal" } ?: NO_WORKOUT,
            creditKcal = workoutCredit(policy, day.pct, day.workoutKcal),
        )
    }

    fun draftOf(day: DaySnapshot) = ConfigDraft(
        ceilingMode = day.ceilingMode,
        sameField = day.kcalSame.toString(),
        weekdayField = day.kcalWeekday.toString(),
        weekendField = day.kcalWeekend.toString(),
        dayFields = day.kcalDays.map { it.toString() },
        eat = day.eat,
        pct = day.pct.toString(),
        proteinField = day.proteinTargetG.toString(),
        carbField = day.carbTargetG.toString(),
        fatField = day.fatTargetG.toString(),
        slotSchedule = SlotScheduleDraft.stored(day.slotMode, day.slots),
        slots = SlotScheduleDraft.stored(day.slotMode, day.slots).slots,
        workoutField = day.workoutKcal?.toString().orEmpty(),
        tone = day.tone,
        goalWeightField = day.goalWeightKg?.let(app.fibrai.android.domain.OnboardingScript::decimal).orEmpty(),
        goalDateField = day.goalDate?.let { java.time.LocalDate.parse(it).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.orEmpty(),
        heightCm = day.heightCm,
        weightKg = day.weightKg,
        today = day.date,
    )

    fun policyOf(eat: String) = WorkoutEditorState.policyOf(eat)
}
