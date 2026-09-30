package com.nutri.android.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nutri.android.core.designsystem.DietaBotMeasure
import com.nutri.android.core.designsystem.DietaBotType
import com.nutri.android.core.designsystem.Haptic
import com.nutri.android.core.designsystem.LocalPalette
import com.nutri.android.core.designsystem.Palette
import com.nutri.android.core.designsystem.dietaClick
import com.nutri.android.core.designsystem.formatRemaining
import com.nutri.android.domain.ProjectedDay

private val ChipShape = RoundedCornerShape(DietaBotMeasure.cardDp.dp)
private val PanelShape = RoundedCornerShape(DietaBotMeasure.cardDp.dp)

// ----------------------------------------------------------------------------- plan (chatR)

/**
 * The ST6 golds were generated per theme and measure differently: dark 23 dp lines and a 13.5/12 sp
 * panel on the phone colour; light 22 dp lines and a 15/13 sp panel on the panel colour.
 */
private class PlanMetrics(val lineSp: Float, val listGapDp: Float, val daySp: Float, val macroSp: Float, val macroLineSp: Float, val gapDp: Int)

private val DarkPlan = PlanMetrics(lineSp = 23f, listGapDp = 5f, daySp = 13.5f, macroSp = 12f, macroLineSp = 17f, gapDp = 5)
private val LightPlan = PlanMetrics(lineSp = 22f, listGapDp = 6f, daySp = 15f, macroSp = 13f, macroLineSp = 19f, gapDp = 8)

@Composable
private fun planMetrics() = if (LocalPalette.current.isDark) DarkPlan else LightPlan

/**
 * The reply of a plan, line by line (chatR): a bullet list sits a little apart from the paragraphs
 * around it, and "40P · 38C · 12G" take the macro colours.
 */
@Composable
internal fun PlanText(text: String) {
    val p = LocalPalette.current
    val m = planMetrics()
    val lines = text.trim().lines().filter { it.isNotBlank() }
    // Stitch (Chrome) puts the first line 1 dp lower than Compose's half-leading, and its text box
    // is 2 dp narrower (bubble right edge 329 dp vs 330 dp): same wraps as the gold.
    Column(Modifier.padding(top = 1.dp, end = 2.dp)) {
        lines.forEachIndexed { i, line ->
            val bullet = line.trimStart().startsWith("•") || line.trimStart().startsWith("- ")
            val previousBullet = lines.getOrNull(i - 1)?.trimStart()?.let { it.startsWith("•") || it.startsWith("- ") }
            val gap = when {
                i == 0 -> 0.dp
                bullet && previousBullet == true -> 0.dp
                bullet -> m.listGapDp.dp
                else -> 8.dp
            }
            Text(
                macroColored(line.trim(), p),
                style = DietaBotType.bodyLg.copy(fontSize = 16.sp, lineHeight = m.lineSp.sp, fontWeight = FontWeight.W400, letterSpacing = 0.sp),
                color = p.text,
                modifier = Modifier.padding(top = gap),
            )
        }
    }
}

/** "40P" (gold) or "P 17 g" (how the model often writes it). */
private val MACRO = Regex("""\b(\d+)\s?([PCG])\b|\b([PCG])\s(\d+)\s?g\b""")

private fun macroColored(line: String, p: Palette): AnnotatedString = buildAnnotatedString {
    append(line)
    MACRO.findAll(line).forEach { m ->
        val color = when (m.groupValues[2].ifEmpty { m.groupValues[3] }) {
            "P" -> p.protein
            "C" -> p.carbs
            else -> p.fat
        }
        addStyle(SpanStyle(color = color, fontWeight = FontWeight.W600), m.range.first, m.range.last + 1)
    }
}

/** Day projected with the plan, computed by the app (ADR-023 decision 3). Above the ceiling: `bad`. */
@Composable
internal fun PlanPanel(day: ProjectedDay) {
    val p = LocalPalette.current
    val m = planMetrics()
    val numbers = if (day.over) p.bad else p.text
    val words = if (day.over) p.bad else p.muted
    Column(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(PanelShape)
            .background(if (p.isDark) p.phone.copy(alpha = 0.9f) else p.panel)
            .border(1.dp, p.line, PanelShape)
            .padding(horizontal = 15.dp, vertical = 14.dp)
            .testTag("chat-plan-panel"),
        verticalArrangement = Arrangement.spacedBy(m.gapDp.dp),
    ) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = words)) { append("Dia: ") }
                withStyle(SpanStyle(color = numbers, fontWeight = FontWeight.W600)) { append(formatRemaining(day.eatenKcal)) }
                withStyle(SpanStyle(color = words)) { append(" → ") }
                withStyle(SpanStyle(color = numbers, fontWeight = FontWeight.W600)) { append(formatRemaining(day.projected.kcal)) }
                withStyle(SpanStyle(color = words)) { append(" de ${formatRemaining(day.ceilingKcal)} kcal") }
            },
            style = DietaBotType.bodyMd.copy(fontSize = m.daySp.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
            modifier = Modifier.testTag("chat-plan-day"),
        )
        Text(
            buildAnnotatedString {
                macro("P", day.projected.p, day.targets.p, p.protein)
                append(" · ")
                macro("C", day.projected.c, day.targets.c, p.carbs)
                append(" · ")
                macro("G", day.projected.g, day.targets.g, p.fat)
            },
            style = DietaBotType.bodyMd.copy(fontSize = m.macroSp.sp, lineHeight = m.macroLineSp.sp, letterSpacing = 0.sp),
            color = p.muted,
        )
    }
}

private fun AnnotatedString.Builder.macro(letter: String, value: Int, target: Int, color: Color) {
    append("$letter ")
    withStyle(SpanStyle(color = color, fontWeight = FontWeight.W500)) { append(value.toString()) }
    append("/$target")
}

// ----------------------------------------------------------------------------- memory (chatM)

enum class MemoryChipKind(val label: String, val icon: ImageVector, val tag: String) {
    UPDATED("Memória atualizada", Icons.Outlined.Check, "updated"),
    PERMANENT("Memória permanente", Icons.Outlined.PushPin, "permanent"),
    DYNAMIC("Memória dinâmica", Icons.Outlined.Sync, "dynamic"),
}

/** Informative only: no tap (A29). The check of `Memória atualizada` is the only gold. */
@Composable
internal fun MemoryChip(kind: MemoryChipKind, background: Color = LocalPalette.current.surf, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val updated = kind == MemoryChipKind.UPDATED
    Row(
        modifier
            .height(28.dp)
            .clip(ChipShape)
            .background(background)
            .border(1.dp, p.line, ChipShape)
            .padding(horizontal = 10.dp)
            .testTag("chat-memory-${kind.tag}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(kind.icon, contentDescription = null, tint = if (updated) p.gold else p.muted, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            kind.label,
            style = DietaBotType.labelMd.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.W500, letterSpacing = 0.sp),
            color = if (updated) p.text else p.muted,
            maxLines = 1,
        )
    }
}

/** The chips of a bubble, in the fixed order: updated, permanent, dynamic. Nothing → nothing drawn. */
@Composable
internal fun MemoryChips(notice: MemoryNotice, modifier: Modifier = Modifier, horizontal: Alignment.Horizontal = Alignment.Start) {
    if (!notice.any) return
    Column(modifier.padding(top = 5.dp).testTag("chat-memory"), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = horizontal) {
        if (notice.updated) MemoryChip(MemoryChipKind.UPDATED)
        if (notice.permanent) MemoryChip(MemoryChipKind.PERMANENT)
        if (notice.dynamic) MemoryChip(MemoryChipKind.DYNAMIC)
    }
}

// ----------------------------------------------------------------------------- routine (chatS)

/** `O de sempre no {slot}?` (ADR-023 decision 8). No gold background anywhere. */
@Composable
internal fun RoutineSuggestionCard(s: RoutineSuggestion, onRecord: () -> Unit, onEdit: () -> Unit) {
    val p = LocalPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(p.surf)
            .border(1.dp, p.line, PanelShape)
            .padding(16.dp)
            .testTag("chat-routine"),
    ) {
        Text(
            "O de sempre no ${s.slot.name}?",
            style = DietaBotType.bodyLg.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.W600, letterSpacing = 0.sp),
            color = p.text,
        )
        Text(
            s.text,
            style = DietaBotType.bodyMd.copy(lineHeight = 22.75.sp, letterSpacing = 0.sp),
            color = p.muted,
            maxLines = 2,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = p.text, fontWeight = FontWeight.W600)) { append("${formatRemaining(s.kcal)} kcal") }
                withStyle(SpanStyle(color = p.dim)) { append(" · ") }
                withStyle(SpanStyle(color = p.protein)) { append("${s.p}P") }
                withStyle(SpanStyle(color = p.dim)) { append(" · ") }
                withStyle(SpanStyle(color = p.carbs)) { append("${s.c}C") }
                withStyle(SpanStyle(color = p.dim)) { append(" · ") }
                withStyle(SpanStyle(color = p.fat)) { append("${s.g}G") }
            },
            style = DietaBotType.bodyMd.copy(fontSize = 14.sp, lineHeight = 22.sp, letterSpacing = 0.sp),
            modifier = Modifier.padding(top = 13.dp),
        )
        MemoryChip(
            if (s.permanent) MemoryChipKind.PERMANENT else MemoryChipKind.DYNAMIC,
            background = p.surf2,
            modifier = Modifier.padding(top = 11.dp),
        )
        Row(Modifier.padding(top = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoutineButton("Registrar", primary = true, tag = "chat-routine-record", onClick = onRecord, modifier = Modifier.weight(1f))
            RoutineButton("Quase igual", primary = false, tag = "chat-routine-edit", onClick = onEdit, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun RoutineButton(label: String, primary: Boolean, tag: String, onClick: () -> Unit, modifier: Modifier) {
    val p = LocalPalette.current
    Row(
        modifier
            .height(48.dp)
            .clip(CircleShape)
            .background(if (primary) p.ctaBg else p.surf2)
            .then(if (primary) Modifier else Modifier.border(1.dp, p.line, CircleShape))
            .dietaClick(if (primary) Haptic.Confirm else Haptic.Light, onClick = onClick)
            .testTag(tag),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = DietaBotType.labelLg.copy(fontSize = 15.sp, fontWeight = if (primary) FontWeight.W600 else FontWeight.W500, letterSpacing = 0.sp),
            color = if (primary) p.ctaText else p.text,
        )
    }
}
