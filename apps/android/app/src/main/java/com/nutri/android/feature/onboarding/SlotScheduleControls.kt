package com.nutri.android.feature.onboarding

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.nutri.android.core.designsystem.LocalPalette

@Composable
internal fun SlotModeConfirmation(schedule: SlotScheduleDraft, onConfirm: () -> Unit, onCancel: () -> Unit) {
    if (schedule.pendingMode == null) return
    val p = LocalPalette.current
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Descartar os horários de ${schedule.discardedGroups.joinToString { it.label }}?", color = p.text) },
        containerColor = p.surf,
        confirmButton = { TextButton(onClick = onConfirm, modifier = Modifier.testTag("slot-mode-discard")) { Text("Descartar", color = p.bad) } },
        dismissButton = { TextButton(onClick = onCancel, modifier = Modifier.testTag("slot-mode-cancel")) { Text("Cancelar", color = p.text) } },
    )
}
