package com.nutri.android.feature.chat

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

/** ST9 draws the receipts and their buttons in Plus Jakarta Sans. */
private val ReceiptTitle = DietaBotType.bodyMd.copy(fontFamily = Jakarta, fontSize = 13.5.sp, lineHeight = 20.sp, fontWeight = FontWeight.W500, letterSpacing = 0.sp)

/** Receipt, its buttons and the inline confirmation: radius 14 (AGENTS card/chip). */
private val CardShape = RoundedCornerShape(DietaBotMeasure.cardDp.dp)

/** A receipt that lost its actions by a tap on it (chatD): 50 % through one save layer (ADR-027 rule 4). */
private const val MARKED_ALPHA = 0.5f

private fun Modifier.layerAlpha(alpha: Float): Modifier = if (alpha >= 1f) this else drawWithContent {
    drawContext.canvas.saveLayer(Rect(Offset.Zero, size), Paint().apply { this.alpha = alpha })
    drawContent()
    drawContext.canvas.restore()
}

/**
 * A record, replacement, skip, move or restore (A34, golds chatG / chatF / chatD). The latest receipt of its
 * slot stacks its buttons below it; a marked one is dimmed with the mark at the end of the title row.
 */
@Composable
internal fun ReceiptCard(
    item: ChatItem.Receipt,
    onAction: (ReceiptAction) -> Unit,
    onMoveConfirm: () -> Unit,
    onMoveElsewhere: () -> Unit,
) {
    val p = LocalPalette.current
    val tone = if (item.skipped) p.dim else p.good
    // Same width as the buttons: the thread content minus 12 dp on each side (gold chatG).
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .testTag("chat-receipt-${item.id}")
                .layerAlpha(if (item.mark != null) MARKED_ALPHA else 1f)
                .clip(CardShape)
                // ST9 dark: the receipt one step above its buttons (surf2 over surf).
                .background(p.surf2)
                .border(1.dp, p.line, CardShape)
                // ST9 receipts: 65 dp with the chip below, 46 dp with it on the title row.
                .padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(tone.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(receiptIcon(item.kind), contentDescription = null, tint = tone, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        buildAnnotatedString {
                            append(receiptTitle(item.kind))
                            withStyle(SpanStyle(fontWeight = FontWeight.W700)) { append(item.slotName) }
                            item.slotTime?.let {
                                withStyle(SpanStyle(color = p.dim)) { append(" · ") }
                                withStyle(SpanStyle(color = p.muted)) { append(it) }
                            }
                        },
                        style = ReceiptTitle,
                        color = p.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    // A restore fits its chip on the title row (chatD).
                    if (item.kind == ReceiptKind.RESTORED) item.kcal?.let { KcalChip("$it kcal", Modifier.padding(start = 6.dp)) }
                }
                if (item.kind != ReceiptKind.RESTORED) {
                    receiptChip(item)?.let { KcalChip(it, Modifier.padding(top = 5.dp)) }
                }
            }
            item.mark?.let { ReceiptMarkLabel(it, Modifier.padding(start = 8.dp)) }
        }
        MemoryChips(MemoryNotice(updated = item.memoryUpdated && item.mark == null), modifier = Modifier.align(Alignment.CenterHorizontally), horizontal = Alignment.CenterHorizontally)
        if (item.actions.isNotEmpty()) {
            // The ST9 golds put 16 (chatD) to 28 dp (chatG) here: one gap for all (ADR-027 rule 2).
            Column(Modifier.padding(top = 22.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item.actions.forEach { action -> ReceiptButton(action) { onAction(action) } }
            }
        }
        item.moveConfirm?.let {
            ReplaceCard(it, tag = "chat-move", onReplace = onMoveConfirm, onElsewhere = onMoveElsewhere, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

private fun receiptIcon(kind: ReceiptKind): ImageVector = when (kind) {
    ReceiptKind.SKIPPED -> Icons.Outlined.Remove
    ReceiptKind.RESTORED -> Icons.Outlined.History
    ReceiptKind.MOVED -> Icons.Outlined.SwapHoriz
    else -> Icons.Filled.DoneAll
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
    val p = LocalPalette.current
    Text(
        text,
        style = DietaBotType.labelMd.copy(fontFamily = Jakarta, fontSize = 11.5.sp, lineHeight = 17.sp, fontWeight = FontWeight.W600, letterSpacing = 0.sp),
        color = p.good,
        maxLines = 1,
        modifier = modifier.clip(RoundedCornerShape(6.dp)).background(p.good.copy(alpha = 0.15f)).padding(horizontal = 6.dp),
    )
}

@Composable
private fun ReceiptMarkLabel(mark: ReceiptMark, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Row(modifier.testTag("chat-receipt-mark"), verticalAlignment = Alignment.CenterVertically) {
        Icon(markIcon(mark), contentDescription = null, tint = p.muted, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(mark.label, style = DietaBotType.labelMd.copy(fontFamily = Jakarta, fontSize = 13.sp, fontWeight = FontWeight.W500, letterSpacing = 0.sp), color = p.muted, maxLines = 1)
    }
}

private fun markIcon(mark: ReceiptMark): ImageVector = when (mark) {
    ReceiptMark.UNDONE -> Icons.AutoMirrored.Outlined.Undo
    ReceiptMark.DELETED -> Icons.Outlined.Delete
    ReceiptMark.MOVED -> Icons.Outlined.SwapHoriz
    ReceiptMark.EDITED -> Icons.Outlined.Edit
}

/** 44 dp, radius 14, surface + 1 dp line, icon left, 15 sp semibold; Excluir in `bad` (ST9). */
@Composable
private fun ReceiptButton(action: ReceiptAction, onClick: () -> Unit) {
    val p = LocalPalette.current
    val (icon, label) = when (action) {
        ReceiptAction.UNDO -> Icons.AutoMirrored.Outlined.Undo to "Desfazer"
        ReceiptAction.DELETE -> Icons.Filled.Delete to "Excluir"
        ReceiptAction.MOVE -> Icons.Outlined.SwapHoriz to "Trocar refeição"
        ReceiptAction.EDIT -> Icons.Filled.Edit to "Editar"
    }
    val danger = action == ReceiptAction.DELETE
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(CardShape)
            .background(p.surf)
            .border(1.dp, p.line, CardShape)
            .dietaClick(if (danger) Haptic.Confirm else Haptic.Light, onClick = onClick)
            .padding(horizontal = 14.dp)
            .testTag("chat-receipt-${action.name.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (danger) p.bad else p.muted, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(11.dp))
        Text(
            label,
            style = DietaBotType.labelLg.copy(fontFamily = Jakarta, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.W600, letterSpacing = 0.sp),
            color = if (danger) p.bad else p.text,
            maxLines = 1,
        )
    }
}

/**
 * chatU (A34): `Substituir {slot}?` inside the conversation. Substituir in CTA tokens, Outra refeição
 * outlined. [tag]: chat-replace (below an answer) or chat-move (below a receipt).
 */
@Composable
internal fun ReplaceCard(confirm: ReplaceConfirm, tag: String, onReplace: () -> Unit, onElsewhere: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(p.surf)
            .border(1.dp, p.line, CardShape)
            .padding(start = 18.dp, end = 18.dp, top = 15.5.dp, bottom = 16.75.dp)
            .testTag("$tag-card"),
    ) {
        Text(
            "Substituir ${confirm.slot.name}?",
            style = DietaBotType.bodyLg.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.W700, letterSpacing = 0.sp),
            color = p.text,
        )
        Text(
            "${confirm.slot.name} tem ${confirm.oldKcal} kcal. Fica com ${confirm.newKcal} kcal.",
            style = DietaBotType.bodyMd.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
            color = p.muted,
            modifier = Modifier.padding(top = 4.5.dp),
        )
        Row(Modifier.padding(top = 17.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(CircleShape)
                    .background(p.ctaBg)
                    .dietaClick(Haptic.Confirm, onClick = onReplace)
                    .testTag("$tag-confirm"),
                contentAlignment = Alignment.Center,
            ) {
                Text("Substituir", style = DietaBotType.labelLg.copy(fontSize = 14.sp, fontWeight = FontWeight.W700, letterSpacing = 0.sp), color = p.ctaText, maxLines = 1)
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(CircleShape)
                    .border(1.dp, p.line, CircleShape)
                    .dietaClick(onClick = onElsewhere)
                    .testTag("$tag-elsewhere"),
                contentAlignment = Alignment.Center,
            ) {
                Text("Outra refeição", style = DietaBotType.labelLg.copy(fontSize = 14.sp, fontWeight = FontWeight.W700, letterSpacing = 0.sp), color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** `Não registrado` below an answer whose Registrar or Substituir expired (A34). */
@Composable
internal fun NotRecordedLabel(modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Row(modifier.testTag("chat-not-recorded"), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.DoNotDisturbOn, contentDescription = null, tint = p.muted, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text("Não registrado", style = DietaBotType.labelMd.copy(fontSize = 12.sp, fontWeight = FontWeight.W500, letterSpacing = 0.sp), color = p.muted, maxLines = 1)
    }
}

/** chatE (A34): one full-width Registrar in the actions slot (Chat/ActionBar). */
@Composable
internal fun RegisterBar(onClick: () -> Unit) {
    AeroActionBar("Registrar", AeroIconName.CheckCircle, onClick, Modifier.testTag("chat-register"))
}
