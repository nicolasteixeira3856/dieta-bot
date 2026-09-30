package com.nutri.android.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nutri.android.core.designsystem.DietaBotType
import com.nutri.android.core.designsystem.LocalPalette
import com.nutri.android.core.designsystem.dietaClick
import com.nutri.android.domain.SlotModes

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SlotScheduleControls(schedule: SlotScheduleDraft, onMode: (String) -> Unit, onCopy: () -> Unit, tag: String = "o3") {
    val p = LocalPalette.current
    SectionLabel("Dias da semana", bottom = 10.dp)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Gold width wraps Cada dia; smaller windows also wrap the middle chip.
        SlotModes.labels.forEach { (mode, label) ->
            val selected = schedule.mode == mode
            val shape = RoundedCornerShape(14.dp)
            Box(
                Modifier.width(if (mode == "same") (if (p.isDark) 120.dp else 117.dp) else if (mode == "split") (if (p.isDark) 162.dp else 153.dp) else 85.dp).height(40.dp).clip(shape)
                    .background(if (selected) (if (p.isDark) p.segSel else p.ctaBg) else p.card)
                    .border(1.dp, if (selected) (if (p.isDark) p.segSel else p.ctaBg) else p.line, shape)
                    .dietaClick { onMode(mode) }
                    .testTag("$tag-mode-$mode"),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = DietaBotType.bodyMd.copy(fontSize = if (p.isDark) 14.sp else 12.sp, letterSpacing = if (p.isDark) 0.sp else 0.6.sp, fontWeight = if (selected) FontWeight.W600 else FontWeight.W500), color = if (selected) p.ctaText else p.text)
            }
        }
    }
    if (schedule.mode != "same") {
        Row(Modifier.fillMaxWidth().padding(top = if (p.isDark) 23.dp else 25.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(schedule.group.label, style = DietaBotType.bodyLg.copy(fontSize = 20.sp, lineHeight = 25.sp, letterSpacing = 0.sp, fontWeight = FontWeight.W600), color = p.text, modifier = Modifier.testTag("$tag-group"))
            Text("Etapa ${schedule.index + 1} de ${schedule.groups.size}", style = DietaBotType.bodyMd.copy(fontSize = 13.sp), color = p.muted)
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            schedule.groups.forEachIndexed { i, _ ->
                Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(if (i <= schedule.index) p.gold else p.surf2))
            }
        }
        if (schedule.index > 0) {
            val shape = RoundedCornerShape(14.dp)
            Row(Modifier.padding(top = 12.dp).fillMaxWidth().height(44.dp).clip(shape).background(p.card).border(1.dp, p.line, shape).dietaClick(onClick = onCopy).testTag("$tag-copy"), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ContentCopy, null, tint = p.gold, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Copiar de ${schedule.groups[schedule.index - 1].label}", style = DietaBotType.bodyMd.copy(fontWeight = FontWeight.W500, letterSpacing = 0.sp), color = p.text)
            }
        }
    }
    Spacer(Modifier.height(24.dp))
}

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
