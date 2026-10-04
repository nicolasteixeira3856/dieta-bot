package com.nutri.android.feature.chat

import androidx.compose.foundation.layout.Box
import com.nutri.android.core.designsystem.aero.Aero
import com.nutri.android.core.designsystem.aero.AeroButtonPrimary
import com.nutri.android.core.designsystem.aero.AeroIcon
import com.nutri.android.core.designsystem.aero.AeroIconName
import com.nutri.android.core.designsystem.aero.AeroText
import com.nutri.android.core.designsystem.aero.aeroGlass
import com.nutri.android.core.designsystem.aero.AeroColors
import com.nutri.android.core.designsystem.aero.AeroSecondaryPill
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

// ----------------------------------------------------------------------------- plan (chatR)

/** The reply of a plan as one Body text (chatR); "40P · 38C · 12G" in Body/Strong and the macro colours. */
@Composable
internal fun PlanText(text: String) {
    val c = Aero.colors
    val lines = text.trim().lines().filter { it.isNotBlank() }.joinToString("\n") { it.trim() }
    AeroText(macroColored(lines, c), style = Aero.type.body.copy(color = c.textPrimary))
}

/** "40P" (gold) or "P 17 g" (how the model often writes it). */
private val MACRO = Regex("""\b(\d+)\s?([PCG])\b|\b([PCG])\s(\d+)\s?g\b""")

private fun macroColored(line: String, c: AeroColors): AnnotatedString = buildAnnotatedString {
    append(line)
    MACRO.findAll(line).forEach { m ->
        val color = when (m.groupValues[2].ifEmpty { m.groupValues[3] }) {
            "P" -> c.macroProtein
            "C" -> c.macroCarbs
            else -> c.macroFat
        }
        addStyle(SpanStyle(color = color, fontWeight = FontWeight.W600), m.range.first, m.range.last + 1)
    }
}

/** Card/MealPlan: the day projected with the plan, computed by the app (ADR-023 decision 3); above the ceiling: status/bad. */
@Composable
internal fun PlanPanel(day: ProjectedDay) {
    val c = Aero.colors
    val type = Aero.type
    val numbers = if (day.over) c.statusBad else c.textPrimary
    val words = if (day.over) c.statusBad else c.textMuted
    val strong = type.captionStrong.fontWeight
    val shape = Aero.shapes.card
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface2)
            .border(1.dp, c.borderLine, shape)
            .padding(horizontal = 15.dp, vertical = 13.dp)
            .testTag("chat-plan-panel"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AeroText(
            buildAnnotatedString {
                withStyle(SpanStyle(color = words)) { append("Dia: ") }
                withStyle(SpanStyle(color = numbers, fontWeight = strong)) { append(formatRemaining(day.eatenKcal)) }
                withStyle(SpanStyle(color = words)) { append(" → ") }
                withStyle(SpanStyle(color = numbers, fontWeight = strong)) { append(formatRemaining(day.projected.kcal)) }
                withStyle(SpanStyle(color = words)) { append(" de ${formatRemaining(day.ceilingKcal)} kcal") }
            },
            Modifier.testTag("chat-plan-day"),
            style = type.caption,
        )
        AeroText(
            buildAnnotatedString {
                macro("P", day.projected.p, day.targets.p, c.macroProtein, strong)
                append(" · ")
                macro("C", day.projected.c, day.targets.c, c.macroCarbs, strong)
                append(" · ")
                macro("G", day.projected.g, day.targets.g, c.macroFat, strong)
            },
            style = type.caption.copy(color = c.textMuted),
        )
    }
}

private fun AnnotatedString.Builder.macro(letter: String, value: Int, target: Int, color: Color, weight: FontWeight?) {
    append("$letter ")
    withStyle(SpanStyle(color = color, fontWeight = weight)) { append(value.toString()) }
    append("/$target")
}

// ----------------------------------------------------------------------------- memory (chatM)

enum class MemoryChipKind(val label: String, val icon: AeroIconName, val tag: String) {
    UPDATED("Memória atualizada", AeroIconName.Check, "updated"),
    PERMANENT("Memória permanente", AeroIconName.PushPin, "permanent"),
    DYNAMIC("Memória dinâmica", AeroIconName.ArrowsClockwise, "dynamic"),
}

/** Chip/Memory: informative only, no tap (A29); the accent check of `Memória atualizada` is the only accent. */
@Composable
internal fun MemoryChip(kind: MemoryChipKind, modifier: Modifier = Modifier) {
    val c = Aero.colors
    val updated = kind == MemoryChipKind.UPDATED
    val pill = RoundedCornerShape(percent = 50)
    Row(
        modifier
            .height(28.dp)
            .clip(pill)
            .background(c.surface2)
            .border(1.dp, c.borderLine, pill)
            .padding(start = 11.dp, end = 13.dp)
            .testTag("chat-memory-${kind.tag}"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AeroIcon(kind.icon, if (updated) c.accentDefault else c.iconMuted, size = 14.dp)
        AeroText(kind.label, style = Aero.type.caption.copy(color = if (updated) c.textPrimary else c.textMuted), maxLines = 1)
    }
}

/** The chips of a bubble, 8 dp under it and 8 dp apart, in the fixed order: updated, permanent, dynamic. */
@Composable
internal fun MemoryChips(notice: MemoryNotice, modifier: Modifier = Modifier, horizontal: Alignment.Horizontal = Alignment.Start) {
    if (!notice.any) return
    Column(modifier.padding(top = 8.dp).testTag("chat-memory"), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = horizontal) {
        if (notice.updated) MemoryChip(MemoryChipKind.UPDATED)
        if (notice.permanent) MemoryChip(MemoryChipKind.PERMANENT)
        if (notice.dynamic) MemoryChip(MemoryChipKind.DYNAMIC)
    }
}

// ----------------------------------------------------------------------------- routine (chatS)

/** Card/Routine: `O de sempre no {slot}?` (ADR-023 decision 8) with Registrar and Quase igual. */
@Composable
internal fun RoutineSuggestionCard(s: RoutineSuggestion, onRecord: () -> Unit, onEdit: () -> Unit) {
    val c = Aero.colors
    val type = Aero.type
    val strong = type.bodyStrong.fontWeight
    Column(
        Modifier
            .fillMaxWidth()
            .aeroGlass(Aero.shapes.card)
            .padding(17.dp)
            .testTag("chat-routine"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AeroText("O de sempre no ${s.slot.name}?", style = type.bodyStrong.copy(color = c.textPrimary))
        AeroText(s.text, style = type.body.copy(color = c.textMuted), maxLines = 2)
        AeroText(
            buildAnnotatedString {
                withStyle(SpanStyle(color = c.textPrimary, fontWeight = strong)) { append("${formatRemaining(s.kcal)} kcal") }
                withStyle(SpanStyle(color = c.textDim)) { append(" · ") }
                withStyle(SpanStyle(color = c.macroProtein, fontWeight = strong)) { append("${s.p}P") }
                withStyle(SpanStyle(color = c.textDim)) { append(" · ") }
                withStyle(SpanStyle(color = c.macroCarbs, fontWeight = strong)) { append("${s.c}C") }
                withStyle(SpanStyle(color = c.textDim)) { append(" · ") }
                withStyle(SpanStyle(color = c.macroFat, fontWeight = strong)) { append("${s.g}G") }
            },
            style = type.body,
        )
        MemoryChip(if (s.permanent) MemoryChipKind.PERMANENT else MemoryChipKind.DYNAMIC)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AeroButtonPrimary("Registrar", onRecord, Modifier.weight(1f).testTag("chat-routine-record"))
            Box(Modifier.weight(QUASE_WEIGHT)) { AeroSecondaryPill("Quase igual", onEdit, Modifier.testTag("chat-routine-edit")) }
        }
    }
}

/** Card/Routine buttons: 166 and 142 dp of the 316 dp row. */
private const val QUASE_WEIGHT = 142f / 166f
