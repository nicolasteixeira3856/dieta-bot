package com.nutri.android.feature.onboarding

import androidx.compose.runtime.Immutable
import com.nutri.android.domain.SlotSuggestions

@Immutable
data class SlotDraft(
    val id: Long = 0,
    val name: String = "",
    val minutes: Int,
)

@Immutable
data class OnboardingUiState(
    // O1
    val sex: String = "",
    val ageField: String = "",
    val heightField: String = "",
    val weightField: String = "",
    val ceilingMode: String = "same",
    val sameField: String = "2000",
    val weekdayField: String = "2000",
    val weekendField: String = "2300",
    val dayFields: List<String> = List(7) { "2000" },
    val ceilingEdited: Boolean = false,
    /** Mifflin-St Jeor rounded to 10. Null while a body field is missing. */
    val suggestedCeiling: Int? = null,
    // O2
    val eat: String = "zero",
    val pct: String = "50",
    // O3
    val slots: List<SlotDraft> = SlotSuggestions.defaultTimes(4).map { SlotDraft(minutes = it) },
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
    val o1Valid: Boolean
        get() = sex.isNotEmpty() && ceilingFields().all { (it.toIntOrNull() ?: 0) > 0 }

    val o3Valid: Boolean
        get() = slots.size in SlotSuggestions.MIN_SLOTS..SlotSuggestions.MAX_SLOTS && slots.all { it.name.isNotBlank() }

    val o4Valid: Boolean
        get() = listOf(proteinField, carbField, fatField).all { it.toIntOrNull() != null }

    fun ceilingFields(): List<String> = when (ceilingMode) {
        "weekdayWeekend" -> listOf(weekdayField, weekendField)
        "seven" -> dayFields
        else -> listOf(sameField)
    }
}
