package com.nutri.android.feature.chat

import com.nutri.android.core.designsystem.aero.Aero
import com.nutri.android.core.designsystem.aero.AeroButtonPrimary
import com.nutri.android.core.designsystem.aero.AeroIcon
import com.nutri.android.core.designsystem.aero.AeroText
import com.nutri.android.core.designsystem.aero.aeroGlass
import com.nutri.android.core.designsystem.aero.aeroLayerAlpha
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nutri.android.core.designsystem.DietaBotMeasure
import com.nutri.android.core.designsystem.DietaBotType
import com.nutri.android.core.designsystem.Haptic
import com.nutri.android.core.designsystem.Jakarta
import com.nutri.android.core.designsystem.LocalPalette
import com.nutri.android.core.designsystem.aero.AeroActionBar
import com.nutri.android.core.designsystem.aero.AeroIconName
import com.nutri.android.core.designsystem.dietaClick
import com.nutri.android.domain.ReceiptAction

/** A receipt that lost its actions by a tap on it (chatD): 50 % through one save layer (ADR-027 rule 4). */
private const val MARKED_ALPHA = 0.5f

/**
 * Chat/Receipt (A34, golds chatG / chatF / chatD): a record, replacement, skip, move or restore on a surface/2 card,
 * 12 dp in from the thread edges. The latest receipt of its slot stacks its Chat/ReceiptAction buttons 16 dp below;
 * a marked one is dimmed with the mark at the end of the row.
 */
@Composable
internal fun ReceiptCard(
    item: ChatItem.Receipt,
    onAction: (ReceiptAction) -> Unit,
    onMoveConfirm: () -> Unit,
    onMoveElsewhere: () -> Unit,
) {
    val c = Aero.colors
    val type = Aero.type
    val shape = Aero.shapes.card
    val tone = if (item.skipped) c.textDim else c.statusGood
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .testTag("chat-receipt-${item.id}")
                .aeroLayerAlpha(if (item.mark != null) MARKED_ALPHA else 1f)
                .clip(shape)
                .background(c.surface2)
                .border(1.dp, c.borderLine, shape)
                .padding(horizontal = 17.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(tone.copy(alpha = TINT_ALPHA)), contentAlignment = Alignment.Center) {
                AeroIcon(receiptIcon(item.kind), if (item.kind == ReceiptKind.RESTORED) c.iconPrimary else tone, size = 15.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    AeroText(
                        buildAnnotatedString {
                            append(receiptTitle(item.kind))
                            withStyle(SpanStyle(fontWeight = type.captionStrong.fontWeight)) { append(item.slotName) }
                            item.slotTime?.let {
                                withStyle(SpanStyle(color = c.textDim)) { append(" · ") }
                                withStyle(SpanStyle(color = c.textMuted)) { append(it) }
                            }
                        },
                        Modifier.weight(1f, fill = false),
                        style = type.caption.copy(color = c.textPrimary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // A restore fits its chip on the title row (chatD).
                    if (item.kind == ReceiptKind.RESTORED) item.kcal?.let { KcalChip("$it kcal") }
                }
                if (item.kind != ReceiptKind.RESTORED) receiptChip(item)?.let { KcalChip(it) }
            }
            item.mark?.let { ReceiptMarkLabel(it) }
        }
        MemoryChips(MemoryNotice(updated = item.memoryUpdated && item.mark == null), modifier = Modifier.align(Alignment.CenterHorizontally), horizontal = Alignment.CenterHorizontally)
        if (item.actions.isNotEmpty()) {
            Column(Modifier.padding(top = 16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item.actions.forEach { action -> ReceiptButton(action) { onAction(action) } }
            }
        }
        item.moveConfirm?.let {
            ReplaceCard(it, tag = "chat-move", onReplace = onMoveConfirm, onElsewhere = onMoveElsewhere, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/** The 15 % tint of the receipt well and kcal chip (Figma: a status/good layer at 15 % opacity). */
private const val TINT_ALPHA = 0.15f

private fun receiptIcon(kind: ReceiptKind): AeroIconName = when (kind) {
    ReceiptKind.SKIPPED -> AeroIconName.Minus
    ReceiptKind.RESTORED -> AeroIconName.ClockCounterClockwise
    ReceiptKind.MOVED -> AeroIconName.ArrowsLeftRight
    else -> AeroIconName.Checks
}

private fun receiptTitle(kind: ReceiptKind) = when (kind) {
    ReceiptKind.SKIPPED -> "Pulado "
    ReceiptKind.REPLACED -> "Atualizado em "
    ReceiptKind.MOVED -> "Movido para "
    ReceiptKind.RESTORED -> "Restaurado em "
    ReceiptKind.LOGGED -> "Registrado em "
}

/** `+380 kcal`, `380 → 620 kcal` (replacement), `380 kcal` (move, restore). Skip: none. */
private fun receiptChip(item: ChatItem.Receipt): String? {
    val kcal = item.kcal ?: return null
    return when (item.kind) {
        ReceiptKind.SKIPPED -> null
        ReceiptKind.LOGGED -> "+$kcal kcal"
        ReceiptKind.REPLACED -> item.fromKcal?.let { "$it → $kcal kcal" } ?: "$kcal kcal"
        else -> "$kcal kcal"
    }
}

@Composable
private fun KcalChip(text: String, modifier: Modifier = Modifier) {
    val c = Aero.colors
    AeroText(
        text,
        modifier.clip(RoundedCornerShape(6.dp)).background(c.statusGood.copy(alpha = TINT_ALPHA)).padding(horizontal = 6.dp, vertical = 1.dp),
        style = Aero.type.captionStrong.copy(color = c.statusGood),
        maxLines = 1,
    )
}

@Composable
private fun ReceiptMarkLabel(mark: ReceiptMark, modifier: Modifier = Modifier) {
    val c = Aero.colors
    Row(modifier.testTag("chat-receipt-mark"), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        AeroIcon(markIcon(mark), c.iconMuted, size = 14.dp)
        AeroText(mark.label, style = Aero.type.caption.copy(color = c.textMuted), maxLines = 1)
    }
}

private fun markIcon(mark: ReceiptMark): AeroIconName = when (mark) {
    ReceiptMark.UNDONE -> AeroIconName.ArrowCounterClockwise
    ReceiptMark.DELETED -> AeroIconName.Trash
    ReceiptMark.MOVED -> AeroIconName.ArrowsLeftRight
    ReceiptMark.EDITED -> AeroIconName.PencilSimple
}

/** Chat/ReceiptAction: 44 dp glass row, 22 dp icon, Button label; Excluir in status/bad (Tone=Danger). */
@Composable
private fun ReceiptButton(action: ReceiptAction, onClick: () -> Unit) {
    val c = Aero.colors
    val (icon, label) = when (action) {
        ReceiptAction.UNDO -> AeroIconName.ArrowCounterClockwise to "Desfazer"
        ReceiptAction.DELETE -> AeroIconName.Trash to "Excluir"
        ReceiptAction.MOVE -> AeroIconName.ArrowsLeftRight to "Trocar refeição"
        ReceiptAction.EDIT -> AeroIconName.PencilSimple to "Editar"
    }
    val danger = action == ReceiptAction.DELETE
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .aeroGlass(Aero.shapes.card)
            .dietaClick(if (danger) Haptic.Confirm else Haptic.Light, onClick = onClick)
            .padding(horizontal = 15.dp)
            .testTag("chat-receipt-${action.name.lowercase()}"),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AeroIcon(icon, c.iconPrimary, size = 22.dp)
        AeroText(label, style = Aero.type.button.copy(color = if (danger) c.statusBad else c.textPrimary), maxLines = 1)
    }
}

/**
 * chatU (A34), Chat/Receipt State=ReplacePending: `Substituir {slot}?` inside the conversation, Substituir as
 * Button/Primary, Outra refeição outlined. [tag]: chat-replace (below an answer) or chat-move (below a receipt).
 */
@Composable
internal fun ReplaceCard(confirm: ReplaceConfirm, tag: String, onReplace: () -> Unit, onElsewhere: () -> Unit, modifier: Modifier = Modifier) {
    val c = Aero.colors
    val type = Aero.type
    Column(
        modifier
            .fillMaxWidth()
            .aeroGlass(Aero.shapes.card)
            .padding(start = 19.dp, end = 19.dp, top = 17.dp, bottom = 19.dp)
            .testTag("$tag-card"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AeroText("Substituir ${confirm.slot.name}?", style = type.bodyStrong.copy(color = c.textPrimary))
        AeroText("${confirm.slot.name} tem ${confirm.oldKcal} kcal. Fica com ${confirm.newKcal} kcal.", style = type.body.copy(color = c.textMuted))
        Row(Modifier.padding(top = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AeroButtonPrimary("Substituir", onReplace, Modifier.weight(1f).testTag("$tag-confirm"))
            val pill = RoundedCornerShape(percent = 50)
            Box(
                Modifier
                    .height(58.dp)
                    .clip(pill)
                    .border(1.dp, c.borderLine, pill)
                    .dietaClick(onClick = onElsewhere)
                    .padding(horizontal = 17.dp)
                    .testTag("$tag-elsewhere"),
                contentAlignment = Alignment.Center,
            ) {
                AeroText("Outra refeição", style = type.button.copy(color = c.textPrimary), maxLines = 1)
            }
        }
    }
}

/** `Não registrado` below an answer whose Registrar or Substituir expired (A34, no gold). */
@Composable
internal fun NotRecordedLabel(modifier: Modifier = Modifier) {
    val c = Aero.colors
    Row(modifier.testTag("chat-not-recorded"), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        AeroIcon(AeroIconName.Minus, c.iconMuted, size = 14.dp)
        AeroText("Não registrado", style = Aero.type.caption.copy(color = c.textMuted), maxLines = 1)
    }
}

/** chatE (A34): one full-width Registrar in the actions slot (Chat/ActionBar). */
@Composable
internal fun RegisterBar(onClick: () -> Unit) {
    AeroActionBar("Registrar", AeroIconName.CheckCircle, onClick, Modifier.testTag("chat-register"))
}
