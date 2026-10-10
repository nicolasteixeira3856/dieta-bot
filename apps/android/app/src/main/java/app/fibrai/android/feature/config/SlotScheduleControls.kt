package app.fibrai.android.feature.config

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import app.fibrai.android.core.designsystem.aero.AeroConfirmDialog
import app.fibrai.android.core.designsystem.aero.AeroIconName

/** Changing the meal mode drops the groups that no longer exist: Dialog/Confirm Tone=Danger asks first (no gold). */
@Composable
internal fun BoxScope.SlotModeConfirmation(schedule: SlotScheduleDraft, onConfirm: () -> Unit, onCancel: () -> Unit) {
    if (schedule.pendingMode == null) return
    AeroConfirmDialog(
        title = "Descartar os horários de ${schedule.discardedGroups.joinToString { it.label }}?",
        primary = "Descartar",
        onPrimary = onConfirm,
        secondary = "Cancelar",
        onSecondary = onCancel,
        dangerIcon = AeroIconName.Trash,
        primaryTag = "slot-mode-discard",
        secondaryTag = "slot-mode-cancel",
    )
}
