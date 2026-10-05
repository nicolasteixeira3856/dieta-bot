package app.fibrai.android.feature.config

import androidx.compose.runtime.Immutable
import app.fibrai.android.domain.SlotSuggestions
import app.fibrai.android.feature.onboarding.SlotScheduleDraft
import app.fibrai.android.feature.onboarding.SlotDraft
import app.fibrai.android.feature.workout.WorkoutEditorState

/** Edit sheet opened from a cfg row. Layers of the Config screen, not screens (ADR-012). */
enum class ConfigEditor { CEILING, EAT_BACK, MACROS, SLOTS, WORKOUT }

@Immutable
data class ConfigSlotRow(val id: Long, val name: String, val time: String)

/** Field values of the open sheet. Nothing reaches Room before Salvar (and the wipe dialog). */
data class ConfigDraft(
    val ceilingMode: String = "same",
    val sameField: String = "",
    val weekdayField: String = "",
    val weekendField: String = "",
    val dayFields: List<String> = List(7) { "" },
    val eat: String = "zero",
    val pct: String = "50",
    val proteinField: String = "",
    val carbField: String = "",
    val fatField: String = "",
    val slots: List<SlotDraft> = emptyList(),
    val slotSchedule: SlotScheduleDraft = SlotScheduleDraft(),
    /** Empty = no workout today = credit 0. */
    val workoutField: String = "",
) {
    /** Credit uses the stored eat-back (the draft copies it on open). */
    val workoutEditor: WorkoutEditorState
        get() = WorkoutEditorState(workoutField, WorkoutEditorState.policyOf(eat), pct.toIntOrNull() ?: 50)

    fun ceilingFields(): List<String> = when (ceilingMode) {
        "weekdayWeekend" -> listOf(weekdayField, weekendField)
        "seven" -> dayFields
        else -> listOf(sameField)
    }

    fun valid(editor: ConfigEditor): Boolean = when (editor) {
        ConfigEditor.CEILING -> ceilingFields().all { (it.toIntOrNull() ?: 0) > 0 }
        ConfigEditor.EAT_BACK -> eat != "partial" || (pct.toIntOrNull() ?: 0) > 0
        ConfigEditor.MACROS -> listOf(proteinField, carbField, fatField).all { it.toIntOrNull() != null }
        ConfigEditor.SLOTS -> slotSchedule.pendingMode == null && slots.size in SlotSuggestions.MIN_SLOTS..SlotSuggestions.MAX_SLOTS && slots.all { it.name.isNotBlank() }
        ConfigEditor.WORKOUT -> true
    }
}

data class ConfigUiState(
    val loaded: Boolean = false,
    /** Base ceiling of today, "2000 kcal". */
    val ceilingValue: String = "",
    val ceilingDetail: String = "",
    val eatBackValue: String = "",
    val macrosValue: String = "",
    val slots: List<ConfigSlotRow> = emptyList(),
    val slotMode: String = "same",
    val slotGroups: List<ConfigSlotRow> = emptyList(),
    /** "Nenhum informado" or "450 kcal". */
    val workoutValue: String = "",
    val creditKcal: Int = 0,
    val editor: ConfigEditor? = null,
    val draft: ConfigDraft = ConfigDraft(),
    /** Ceiling changed: "Reiniciar registros de hoje?" is up. */
    val wipeConfirm: Boolean = false,
) {
    val canSave: Boolean
        get() = editor?.let { draft.valid(it) } ?: false
}

/** Every Config callback. Defaults are no-ops (previews, gold renders). */
@Immutable
class ConfigActions(
    val onBack: () -> Unit = {},
    val onOpen: (ConfigEditor) -> Unit = {},
    val onClose: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onConfirmWipe: () -> Unit = {},
    val onCancelWipe: () -> Unit = {},
    val onCeilingMode: (String) -> Unit = {},
    val onSame: (String) -> Unit = {},
    val onWeekday: (String) -> Unit = {},
    val onWeekend: (String) -> Unit = {},
    val onDay: (Int, String) -> Unit = { _, _ -> },
    val onEat: (String) -> Unit = {},
    val onPct: (String) -> Unit = {},
    val onProtein: (String) -> Unit = {},
    val onCarb: (String) -> Unit = {},
    val onFat: (String) -> Unit = {},
    val onSlotCount: (Int) -> Unit = {},
    val onSlotName: (Int, String) -> Unit = { _, _ -> },
    val onSlotTime: (Int, Int) -> Unit = { _, _ -> },
    val onSlotMode: (String) -> Unit = {},
    val onConfirmSlotMode: () -> Unit = {},
    val onCancelSlotMode: () -> Unit = {},
    val onCopySlots: () -> Unit = {},
    val onPreviousSlots: () -> Unit = {},
    val onOpenSlotGroup: (Int) -> Unit = {},
    val onWorkout: (String) -> Unit = {},
)
