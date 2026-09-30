package com.nutri.android.feature.devtools

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutri.android.core.database.DayRepository
import com.nutri.android.core.database.DaySnapshot
import com.nutri.android.core.database.InstantClock
import com.nutri.android.core.database.MealSlot
import com.nutri.android.core.memory.MemoryStore
import com.nutri.android.core.network.ChatProfile
import com.nutri.android.core.telemetry.Telemetry
import com.nutri.android.domain.CreditPolicy
import com.nutri.android.domain.SaoPaulo
import com.nutri.android.domain.workoutCredit
import com.nutri.android.feature.chat.PromptBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Immutable
data class DevMemoryUiState(
    val loaded: Boolean = false,
    val profileText: String = "",
    val memoryText: String = "",
    /** DAY block of the next turn, read-only. */
    val dayText: String = "",
    val error: String? = null,
    /** teto_kcal changed: the Config wipe dialog is up. */
    val wipeConfirm: Boolean = false,
    val saved: Boolean = false,
) {
    val memoryLength: Int get() = memoryText.length
}

/**
 * A23 (ADR-019, dev only): the profile and memory of the next POST /v1/chat, edited and saved.
 * Nothing is deleted: an empty memory, a removed key or a removed slot is refused.
 */
@HiltViewModel
class DevMemoryViewModel @Inject constructor(
    private val repository: DayRepository,
    private val memory: MemoryStore,
    private val clock: InstantClock,
    private val telemetry: Telemetry,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DevMemoryUiState())
    val uiState: StateFlow<DevMemoryUiState> = _uiState.asStateFlow()

    private var day = DaySnapshot()
    private var current: ChatProfile? = null
    private var pending: ChatProfile? = null

    init {
        viewModelScope.launch {
            day = repository.observeToday().first()
            val text = memory.read()
            // Same builder as the Chat: what is shown is what the next turn sends.
            val body = PromptBuilder.build(day, emptyList(), emptyList(), "", clock.now(), memory = text).body
            current = body.profile
            _uiState.value = DevMemoryUiState(
                loaded = true,
                profileText = ProfileText.format(body.profile),
                memoryText = body.memory,
                dayText = dayJson.encodeToString(body.day),
            )
        }
    }

    fun setProfile(text: String) = _uiState.update { it.copy(profileText = text, error = null) }

    fun setMemory(text: String) = _uiState.update { it.copy(memoryText = text, error = null) }

    /** Salvar. Everything is validated first; a changed ceiling asks before wiping today. */
    fun save() {
        val ui = _uiState.value
        val before = current ?: return
        if (ui.memoryText.isBlank()) return fail(EMPTY_MEMORY)
        val edited = when (val r = ProfileText.parse(ui.profileText, before)) {
            is ProfileText.Result.Error -> return fail(r.message)
            is ProfileText.Result.Ok -> r.profile
        }
        if (edited.ceilingKcal != before.ceilingKcal) {
            if (baseCeiling(edited) <= 0) return fail("teto_kcal menor que o crédito do treino.")
            pending = edited
            _uiState.update { it.copy(wipeConfirm = true, error = null) }
            return
        }
        commit(edited, wipe = false)
    }

    fun confirmWipe() {
        val edited = pending ?: return
        pending = null
        _uiState.update { it.copy(wipeConfirm = false) }
        commit(edited, wipe = true)
    }

    /** Cancelar in the wipe dialog: nothing is stored, the edits stay on screen. */
    fun cancelWipe() {
        pending = null
        _uiState.update { it.copy(wipeConfirm = false) }
    }

    private fun commit(edited: ChatProfile, wipe: Boolean) {
        val before = current ?: return
        val text = _uiState.value.memoryText
        viewModelScope.launch {
            if (!memory.replace(text)) return@launch fail(EMPTY_MEMORY)
            val (eat, pct) = ProfileText.eatAndPct(edited.eatBack)
            if (edited.eatBack != before.eatBack) repository.saveEatBack(eat, pct ?: day.pct)
            if (listOf(edited.pTarget, edited.cTarget, edited.gTarget) != listOf(before.pTarget, before.cTarget, before.gTarget)) {
                repository.saveMacroTargets(edited.pTarget, edited.cTarget, edited.gTarget)
            }
            if (edited.slots != before.slots) {
                repository.saveSlots(
                    edited.slots.map { MealSlot(it.id.toLong(), it.name, minutesOf(it.time)) }
                        .sortedBy { it.minutesFromMidnight },
                )
            }
            if (wipe) changeCeiling(baseCeiling(edited))
            telemetry.event(
                DEV_MEMORY_SAVED,
                mapOf("memory_len_bucket" to lengthBucket(text.length), "profile_changed" to (edited != before)),
            )
            _uiState.update { it.copy(saved = true) }
        }
    }

    /** teto_kcal is the effective ceiling (base + workout credit under the edited eat-back). */
    private fun baseCeiling(edited: ChatProfile): Int {
        val (eat, pct) = ProfileText.eatAndPct(edited.eatBack)
        val policy = when (eat) {
            "partial" -> CreditPolicy.PARTIAL
            "full" -> CreditPolicy.FULL
            else -> CreditPolicy.ZERO
        }
        return edited.ceilingKcal - workoutCredit(policy, pct ?: day.pct, day.workoutKcal)
    }

    /** The new base replaces today's ceiling only: the other weekdays keep theirs. */
    private suspend fun changeCeiling(base: Int) {
        val today: LocalDate = SaoPaulo.date(clock.now())
        val weekend = today.dayOfWeek == DayOfWeek.SATURDAY || today.dayOfWeek == DayOfWeek.SUNDAY
        repository.changeCeiling(
            ceilingMode = day.ceilingMode,
            kcalSame = if (day.ceilingMode == "same") base else day.kcalSame,
            kcalWeekday = if (day.ceilingMode == "weekdayWeekend" && !weekend) base else day.kcalWeekday,
            kcalWeekend = if (day.ceilingMode == "weekdayWeekend" && weekend) base else day.kcalWeekend,
            kcalDays = day.kcalDays.mapIndexed { i, v -> if (day.ceilingMode == "seven" && i == today.dayOfWeek.value - 1) base else v },
        )
    }

    private fun fail(message: String) = _uiState.update { it.copy(error = message) }

    private fun minutesOf(time: String) = time.substringBefore(':').toInt() * 60 + time.substringAfter(':').toInt()

    companion object {
        const val DEV_MEMORY_SAVED = "dev_memory_saved"
        const val EMPTY_MEMORY = "Memória vazia não é salva."
        private val dayJson = Json { prettyPrint = true; explicitNulls = false }

        /** Memory length as a bucket (max 4000): the text never leaves through telemetry. */
        fun lengthBucket(length: Int): String = when {
            length < 1000 -> "<1000"
            length < 3000 -> "1000-2999"
            else -> ">=3000"
        }
    }
}
