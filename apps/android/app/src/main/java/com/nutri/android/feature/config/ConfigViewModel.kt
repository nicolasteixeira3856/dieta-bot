package com.nutri.android.feature.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutri.android.core.database.DayRepository
import com.nutri.android.core.database.DaySnapshot
import com.nutri.android.core.database.InstantClock
import com.nutri.android.core.database.MealSlot
import com.nutri.android.core.database.ceilingProfile
import com.nutri.android.domain.CreditPolicy
import com.nutri.android.domain.SaoPaulo
import com.nutri.android.domain.SlotSuggestions
import com.nutri.android.domain.workoutCredit
import com.nutri.android.feature.onboarding.SlotDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
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
) : ViewModel() {
    private val local = MutableStateFlow(Local())
    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState: StateFlow<ConfigUiState> = _uiState.asStateFlow()

    private var day: DaySnapshot = DaySnapshot()

    private data class Local(
        val editor: ConfigEditor? = null,
        val draft: ConfigDraft = ConfigDraft(),
        val wipeConfirm: Boolean = false,
    )

    init {
        viewModelScope.launch {
            combine(repository.observeToday(), local) { d, l -> d to l }.collect { (d, l) ->
                day = d
                _uiState.value = ConfigMapper.map(d, SaoPaulo.date(clock.now()))
                    .copy(editor = l.editor, draft = l.draft, wipeConfirm = l.wipeConfirm)
            }
        }
    }

    /** Opens a sheet with the stored values. */
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

    // Workout
    fun setWorkout(v: String) = edit { it.copy(workoutField = v.digits(5)) }

    /**
     * Salvar. A changed ceiling only raises the wipe dialog; everything else is stored at once
     * and never wipes (slot name/time relabels).
     */
    fun save() {
        val l = local.value
        val editor = l.editor ?: return
        val draft = l.draft
        if (!draft.valid(editor)) return
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
                    draft.slots.map { MealSlot(id = it.id, name = it.name.trim(), minutesFromMidnight = it.minutes) }
                        .sortedBy { it.minutesFromMidnight },
                )
                ConfigEditor.WORKOUT -> repository.setWorkout(draft.workoutField.toIntOrNull())
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
        slots = day.slots.sortedBy { it.minutesFromMidnight }
            .map { SlotDraft(id = it.id, name = it.name, minutes = it.minutesFromMidnight) },
        workoutField = day.workoutKcal?.toString().orEmpty(),
    )

    fun policyOf(eat: String) = when (eat) {
        "partial" -> CreditPolicy.PARTIAL
        "full" -> CreditPolicy.FULL
        else -> CreditPolicy.ZERO
    }
}
