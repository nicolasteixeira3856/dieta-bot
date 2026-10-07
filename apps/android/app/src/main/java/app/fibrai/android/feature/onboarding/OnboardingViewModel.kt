package app.fibrai.android.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.fibrai.android.core.database.DayRepository
import app.fibrai.android.core.database.InstantClock
import app.fibrai.android.core.database.MealSlot
import app.fibrai.android.core.telemetry.NoopTelemetry
import app.fibrai.android.core.telemetry.Telemetry
import app.fibrai.android.core.telemetry.TelemetryEvents
import app.fibrai.android.domain.MacroSplit
import app.fibrai.android.domain.SameEveryDayCeiling
import app.fibrai.android.domain.SaoPaulo
import app.fibrai.android.domain.SevenDayCeiling
import app.fibrai.android.domain.Sex
import app.fibrai.android.domain.SlotSuggestions
import app.fibrai.android.domain.TmbCalculator
import app.fibrai.android.domain.WeekdayWeekendCeiling
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: DayRepository,
    private val clock: InstantClock,
    private val telemetry: Telemetry = NoopTelemetry,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val day = repository.observeToday().first()
            // Without a stored body profile the ceiling stays blank until the TMB prefill (A31).
            val storedProfile = day.sex.isNotEmpty() && day.ageYears > 0 && day.heightCm > 0 && day.weightKg > 0
            _uiState.update { current ->
                val loaded = current.copy(
                    sex = day.sex,
                    ageField = day.ageYears.positiveOrBlank(),
                    heightField = day.heightCm.positiveOrBlank(),
                    weightField = if (day.weightKg > 0) formatWeight(day.weightKg) else "",
                    ceilingMode = day.ceilingMode,
                    eat = day.eat,
                    pct = day.pct.toString(),
                    slotSchedule = SlotScheduleDraft.stored(day.slotMode, day.slots),
                    slots = if (day.slots.size >= SlotSuggestions.MIN_SLOTS) {
                        SlotScheduleDraft.stored(day.slotMode, day.slots).slots
                    } else {
                        current.slots
                    },
                    proteinField = day.proteinTargetG.toString(),
                    carbField = day.carbTargetG.toString(),
                    fatField = day.fatTargetG.toString(),
                    tone = day.tone,
                    loaded = true,
                )
                val withCeiling = if (!storedProfile) {
                    loaded
                } else {
                    loaded.copy(
                        sameField = day.kcalSame.toString(),
                        weekdayField = day.kcalWeekday.toString(),
                        weekendField = day.kcalWeekend.toString(),
                        dayFields = day.kcalDays.map { it.toString() }.let { if (it.size == 7) it else List(7) { "2000" } },
                    )
                }
                withCeiling.withSuggestion()
            }
        }
    }

    // O1
    fun setSex(value: String) = _uiState.update { it.copy(sex = value).withSuggestion() }
    fun setAge(v: String) = _uiState.update { it.copy(ageField = v.digits(3)).withSuggestion() }
    fun setHeight(v: String) = _uiState.update { it.copy(heightField = v.digits(3)).withSuggestion() }
    fun setWeight(v: String) = _uiState.update {
        val clean = v.replace(',', '.').filter { c -> c.isDigit() || c == '.' }.take(5)
        it.copy(weightField = clean).withSuggestion()
    }

    fun setCeilingMode(mode: String) = _uiState.update { it.copy(ceilingMode = mode) }
    fun setSameField(v: String) = _uiState.update { it.copy(sameField = v.digits(5), ceilingEdited = true) }
    fun setWeekdayField(v: String) = _uiState.update { it.copy(weekdayField = v.digits(5), ceilingEdited = true) }
    fun setWeekendField(v: String) = _uiState.update { it.copy(weekendField = v.digits(5), ceilingEdited = true) }
    fun setDayField(index: Int, v: String) = _uiState.update {
        val list = it.dayFields.toMutableList()
        if (index in list.indices) list[index] = v.digits(5)
        it.copy(dayFields = list, ceilingEdited = true)
    }

    // O2
    fun setEat(mode: String) = _uiState.update { it.copy(eat = mode) }
    fun setPct(v: String) = _uiState.update { it.copy(pct = v.digits(3)) }

    // O3
    /** Edited rows are kept; untouched rows (no name, default time) move to the new count's defaults. */
    fun setSlotCount(count: Int) = _uiState.update { state ->
        val n = count.coerceIn(SlotSuggestions.MIN_SLOTS, SlotSuggestions.MAX_SLOTS)
        val oldTimes = SlotSuggestions.defaultTimes(state.slots.size)
        val times = SlotSuggestions.defaultTimes(n)
        val slots = List(n) { i ->
            val current = state.slots.getOrNull(i)
            val untouched = current == null || (current.name.isEmpty() && current.id == 0L && current.minutes == oldTimes.getOrNull(i))
            if (untouched) SlotDraft(minutes = times[i]) else current!!
        }
        state.copy(slots = slots)
    }

    fun setSlotName(index: Int, name: String) = updateSlot(index) { it.copy(name = name.take(40)) }
    fun setSlotTime(index: Int, minutes: Int) = updateSlot(index) { it.copy(minutes = minutes.coerceIn(0, 1439)) }

    private fun updateSlot(index: Int, change: (SlotDraft) -> SlotDraft) = _uiState.update { state ->
        if (index !in state.slots.indices) return@update state
        state.copy(slots = state.slots.toMutableList().also { it[index] = change(it[index]) })
    }

    fun setSlotMode(mode: String) = changeSchedule { it.requestMode(mode) }
    fun confirmSlotMode() = changeSchedule { it.confirmMode() }
    fun cancelSlotMode() = changeSchedule { it.copy(pendingMode = null) }
    fun copyPreviousSlots() = changeSchedule { it.copyPrevious() }

    fun nextSlotGroup(onLast: () -> Unit) {
        val state = _uiState.value
        if (!state.o3Valid) return
        if (state.slotSchedule.last) {
            changeSchedule { it }
            onLast()
        } else changeSchedule { it.copy(index = it.index + 1) }
    }

    fun previousSlotGroup(onFirst: () -> Unit) {
        if (_uiState.value.slotSchedule.index == 0) onFirst()
        else changeSchedule { it.copy(index = it.index - 1) }
    }

    private fun changeSchedule(change: (SlotScheduleDraft) -> SlotScheduleDraft) = _uiState.update {
        val schedule = change(it.slotSchedule.withSlots(it.slots))
        it.copy(slotSchedule = schedule, slots = schedule.slots)
    }

    // O4
    /** Called when O4 opens. Prefills 30/40/30 of day-1 ceiling unless the user edited macros. */
    fun enterMacros() = _uiState.update { state ->
        val ceiling = day1Ceiling(state)
        if (state.macrosEdited) return@update state.copy(day1Ceiling = ceiling)
        val split = MacroSplit.of(ceiling)
        state.copy(
            day1Ceiling = ceiling,
            proteinField = split.proteinG.toString(),
            carbField = split.carbG.toString(),
            fatField = split.fatG.toString(),
        )
    }

    fun setProtein(v: String) = _uiState.update { it.copy(proteinField = v.digits(4), macrosEdited = true) }
    fun setCarb(v: String) = _uiState.update { it.copy(carbField = v.digits(4), macrosEdited = true) }
    fun setFat(v: String) = _uiState.update { it.copy(fatField = v.digits(4), macrosEdited = true) }

    // O5
    fun setTone(tone: String) = _uiState.update { if (tone == "seco" || tone == "duro") it.copy(tone = tone) else it }

    /** Slots first, profile last: onboardingDone=1 only once everything is stored. */
    fun completeOnboarding(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (!state.o1Valid || !state.slotSchedule.withSlots(state.slots).valid || !state.o4Valid) return
        viewModelScope.launch {
            val today = SaoPaulo.date(clock.now()).toString()
            repository.saveSlots(
                state.slotSchedule.withSlots(state.slots).meals(),
                state.slotSchedule.mode,
            )
            repository.saveProfile(
                ceilingMode = state.ceilingMode,
                kcalSame = state.sameField.toIntOrNull() ?: 2000,
                kcalWeekday = state.weekdayField.toIntOrNull() ?: 2000,
                kcalWeekend = state.weekendField.toIntOrNull() ?: 2300,
                kcalDays = state.dayFields.map { it.toIntOrNull() ?: 2000 },
                eat = state.eat,
                pct = state.pct.toIntOrNull() ?: 50,
                onboardingDone = true,
                firstDay = today,
                sex = state.sex,
                ageYears = state.ageField.toIntOrNull() ?: 0,
                heightCm = state.heightField.toIntOrNull() ?: 0,
                weightKg = state.weightField.toDoubleOrNull() ?: 0.0,
                proteinTargetG = state.proteinField.toIntOrNull() ?: 0,
                carbTargetG = state.carbField.toIntOrNull() ?: 0,
                fatTargetG = state.fatField.toIntOrNull() ?: 0,
                tone = state.tone,
            )
            telemetry.event(TelemetryEvents.TONE_SET, mapOf("tone" to state.tone, "from" to "onboarding"))
            telemetry.event(
                TelemetryEvents.ONBOARDING_COMPLETE,
                mapOf("slots" to state.slots.size, "ceiling_mode" to state.ceilingMode, "eat" to state.eat),
            )
            _uiState.update { it.copy(isComplete = true) }
            onSuccess()
        }
    }

    private fun day1Ceiling(state: OnboardingUiState): Int {
        val date = SaoPaulo.date(clock.now())
        val profile = when (state.ceilingMode) {
            "weekdayWeekend" -> WeekdayWeekendCeiling(
                state.weekdayField.toIntOrNull() ?: 0,
                state.weekendField.toIntOrNull() ?: 0,
            )
            "seven" -> state.dayFields.map { it.toIntOrNull() ?: 0 }.let { d ->
                SevenDayCeiling(d[0], d[1], d[2], d[3], d[4], d[5], d[6])
            }
            else -> SameEveryDayCeiling(state.sameField.toIntOrNull() ?: 0)
        }
        return profile.ceilingOn(date)
    }

    /** Recomputes the TMB suggestion; prefills every ceiling field until the user edits one. */
    private fun OnboardingUiState.withSuggestion(): OnboardingUiState {
        val sexValue = when (sex) {
            "male" -> Sex.MALE
            "female" -> Sex.FEMALE
            else -> null
        }
        val suggestion = TmbCalculator.ceilingPrefill(
            sexValue,
            ageField.toIntOrNull() ?: 0,
            heightField.toIntOrNull() ?: 0,
            weightField.toDoubleOrNull() ?: 0.0,
        )
        if (suggestion == null || ceilingEdited) return copy(suggestedCeiling = suggestion)
        val v = suggestion.toString()
        return copy(
            suggestedCeiling = suggestion,
            sameField = v,
            weekdayField = v,
            weekendField = v,
            dayFields = List(7) { v },
        )
    }

    private fun String.digits(max: Int) = filter { it.isDigit() }.take(max)
    private fun Int.positiveOrBlank() = if (this > 0) toString() else ""
    private fun formatWeight(kg: Double) = if (kg % 1.0 == 0.0) kg.toInt().toString() else kg.toString()
}
