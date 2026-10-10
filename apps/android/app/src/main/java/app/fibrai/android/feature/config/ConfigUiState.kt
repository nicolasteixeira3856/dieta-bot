package app.fibrai.android.feature.config

import androidx.compose.runtime.Immutable
import app.fibrai.android.domain.SlotSuggestions
import app.fibrai.android.feature.workout.WorkoutEditorState

/** Edit sheet opened from a cfg row. Layers of the Config screen, not screens (ADR-012). */
enum class ConfigEditor { CEILING, EAT_BACK, MACROS, SLOTS, WORKOUT, TONE, GOAL }

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
    /** A60 part B (cfgT): "seco" | "duro". */
    val tone: String = "seco",
    /** A71: the goal sheet; an empty weight removes the goal. The body and today check the safety limits. */
    val goalWeightField: String = "",
    val goalDateField: String = "",
    val heightCm: Int = 0,
    val weightKg: Double = 0.0,
    val today: String = "",
) {
    /** A71: the goal of the sheet, null when it is empty or not a whole value. */
    val goal: app.fibrai.android.domain.OnboardingGoal?
        get() {
            val kg = goalWeightField.replace(',', '.').toDoubleOrNull() ?: return null
            val date = if (goalDateField.isBlank()) null else app.fibrai.android.domain.GoalRules.parseDate(goalDateField) ?: return null
            return app.fibrai.android.domain.OnboardingGoal(kg, date)
        }

    /** The typed goal is outside the safety limits (or incomplete): Salvar stays off and the note shows. */
    val goalRefused: Boolean
        get() = goalWeightField.isNotBlank() && goal?.let { g ->
            val today = runCatching { java.time.LocalDate.parse(this.today) }.getOrNull() ?: return@let false
            app.fibrai.android.domain.GoalRules.accepted(g, heightCm, weightKg, today)
        } != true

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
        ConfigEditor.TONE -> tone == "seco" || tone == "duro"
        ConfigEditor.GOAL -> !goalRefused
    }
}

data class ConfigUiState(
    val loaded: Boolean = false,
    /** Base ceiling of today, "2000 kcal". */
    val ceilingValue: String = "",
    val ceilingDetail: String = "",
    val eatBackValue: String = "",
    val macrosValue: String = "",
    /** A60 part B: "Seco" or "Duro", the Tom da Tali row. */
    val toneValue: String = "",
    /** A71 (ADR-057 decision 10): "Ligadas" | "Desligadas", the closure time and the goal ("Nenhuma"). */
    val notificationsValue: String = "",
    val closureValue: String = "22:00",
    val closureMinutes: Int = 22 * 60,
    val goalValue: String = "",
    /** The closure time wheel is up. */
    val closurePicking: Boolean = false,
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
    /** cfgR: "Resetar o app?" is up (ADR-040). */
    val resetConfirm: Boolean = false,
    /** The reset is running: the dialog ignores taps. */
    val resetRunning: Boolean = false,
    /** The reset finished: the route opens O1 with no back stack. */
    val resetDone: Boolean = false,
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
    val onTone: (String) -> Unit = {},
    val onOpenReset: () -> Unit = {},
    val onConfirmReset: () -> Unit = {},
    val onCancelReset: () -> Unit = {},
    /** A68 (cfg, D24): Da Tali → Receitas. */
    val onOpenRecipes: () -> Unit = {},
    /** A69 (cfg, D24): Da Tali → O que a Tali sabe. */
    val onOpenMemory: () -> Unit = {},
    /** A71: Avisos e meta. */
    val onToggleNotifications: () -> Unit = {},
    val onOpenClosureTime: () -> Unit = {},
    val onClosureTime: (Int) -> Unit = {},
    val onCancelClosureTime: () -> Unit = {},
    val onGoalWeight: (String) -> Unit = {},
    val onGoalDate: (String) -> Unit = {},
)
