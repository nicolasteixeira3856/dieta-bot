package app.fibrai.android.feature.onboarding

import androidx.compose.runtime.Immutable
import app.fibrai.android.domain.SlotSuggestions

@Immutable
data class SlotDraft(
    val id: Long = 0,
    val name: String = "",
    val minutes: Int,
)

data class OnboardingUiState(
    // O1
    val sex: String = "",
    val ageField: String = "",
    val heightField: String = "",
    val weightField: String = "",
    val ceilingMode: String = "same",
    /** Ceiling fields start blank; the TMB prefill fills them once the profile is valid (A31). */
    val sameField: String = "",
    val weekdayField: String = "",
    val weekendField: String = "",
    val dayFields: List<String> = List(7) { "" },
    val ceilingEdited: Boolean = false,
    /** Mifflin-St Jeor rounded to 10. Null while a body field is missing. */
    val suggestedCeiling: Int? = null,
    // O2
    val eat: String = "zero",
    val pct: String = "50",
    // O3
    val slots: List<SlotDraft> = SlotSuggestions.defaultTimes(4).map { SlotDraft(minutes = it) },
    val slotSchedule: SlotScheduleDraft = SlotScheduleDraft(),
    // O4
    val proteinField: String = "150",
    val carbField: String = "200",
    val fatField: String = "67",
    val macrosEdited: Boolean = false,
    /** Ceiling of day 1, drives the O4 prefill and "KCAL TOTAL ESTIMADA". */
    val day1Ceiling: Int = 2000,
    val isComplete: Boolean = false,
    /** True once the stored profile has been read into the fields. */
    val loaded: Boolean = false,
) {
    /** Sex, age, height and weight filled (> 0). Gates the ceiling controls and Continuar (A31). */
    val profileValid: Boolean
        get() = sex.isNotEmpty() &&
            (ageField.toIntOrNull() ?: 0) > 0 &&
            (heightField.toIntOrNull() ?: 0) > 0 &&
            (weightField.toDoubleOrNull() ?: 0.0) > 0.0

    val o1Valid: Boolean
        get() = profileValid && ceilingFields().all { (it.toIntOrNull() ?: 0) > 0 }

    val o3Valid: Boolean
        get() = slotSchedule.pendingMode == null && slots.size in SlotSuggestions.MIN_SLOTS..SlotSuggestions.MAX_SLOTS && slots.all { it.name.isNotBlank() }

    val o4Valid: Boolean
        get() = listOf(proteinField, carbField, fatField).all { it.toIntOrNull() != null }

    fun ceilingFields(): List<String> = when (ceilingMode) {
        "weekdayWeekend" -> listOf(weekdayField, weekendField)
        "seven" -> dayFields
        else -> listOf(sameField)
    }
}
