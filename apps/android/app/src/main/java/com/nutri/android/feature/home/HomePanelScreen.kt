package com.nutri.android.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nutri.android.core.designsystem.LocalPalette
import com.nutri.android.core.designsystem.DietaBotType
import com.nutri.android.core.designsystem.SplashBoot

private val CardRadius = RoundedCornerShape(16.dp)

@Composable
fun HomePanelScreen(
    ui: HomePanelUiState,
    onSkip: (Long) -> Unit,
    onConfig: () -> Unit,
    onChat: () -> Unit,
) {
    val p = LocalPalette.current
    var confirmSkip by remember { mutableStateOf<TimelineSlot?>(null) }
    Box(Modifier.fillMaxSize().background(p.phone).testTag("home")) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Header(ui, onConfig)
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 116.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Ring(ui)
                MacroCard(ui)
                Timeline(ui) { confirmSkip = it }
                Text(
                    SplashBoot.COPY,
                    style = DietaBotType.labelCaps.copy(fontWeight = FontWeight.W400, letterSpacing = 0.sp, lineHeight = 18.sp),
                    color = p.dim,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).testTag("home-disclaimer"),
                )
            }
        }
        Fab(onChat, Modifier.align(Alignment.BottomEnd))
    }
    confirmSkip?.let { slot ->
        AlertDialog(
            onDismissRequest = { confirmSkip = null },
            containerColor = p.card,
            title = { Text("Pular ${slot.name}?", style = DietaBotType.headlineMd.copy(fontSize = 18.sp), color = p.text) },
            confirmButton = {
                TextButton(
                    onClick = {
                        slot.slotId?.let(onSkip)
                        confirmSkip = null
                    },
                    modifier = Modifier.testTag("home-skip-confirm"),
                ) { Text("Pular", color = p.gold) }
            },
            dismissButton = { TextButton(onClick = { confirmSkip = null }) { Text("Cancelar", color = p.muted) } },
        )
    }
}

@Composable
private fun Header(ui: HomePanelUiState, onConfig: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(ui.dayLabel, style = DietaBotType.labelCaps.copy(letterSpacing = 0.1.em), color = p.dim, modifier = Modifier.padding(bottom = 2.dp))
            Text(
                ui.dateLabel,
                style = DietaBotType.headlineMd.copy(fontSize = 18.sp, lineHeight = 22.5.sp, fontWeight = FontWeight.W700, letterSpacing = (-0.025).em),
                color = p.text,
                modifier = Modifier.testTag("home-date"),
            )
        }
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(p.card)
                .border(1.dp, p.line, CircleShape)
                .clickable(onClick = onConfig)
                .testTag("home-config"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Settings, contentDescription = "Configurações", tint = p.muted, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun Ring(ui: HomePanelUiState) {
    val p = LocalPalette.current
    val over = ui.over > 0
    val arc = if (over) p.bad else p.gold
    Column(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 1.5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(204.dp)
                .drawBehind {
                    val r = 144.dp.toPx()
                    drawCircle(Brush.radialGradient(listOf(arc.copy(alpha = 0.14f), Color.Transparent), center, r), r, center)
                }
                .testTag("home-ring"),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 16.dp.toPx()
                val inset = (size.minDimension - 172.dp.toPx()) / 2f
                val topLeft = Offset(inset, inset)
                val arcSize = androidx.compose.ui.geometry.Size(172.dp.toPx(), 172.dp.toPx())
                drawArc(p.surf2, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                if (ui.ringFraction > 0f) {
                    drawArc(arc, -90f, 360f * ui.ringFraction, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    ui.consumed.toString(),
                    style = DietaBotType.displayLg.copy(fontSize = 36.sp, lineHeight = 36.sp, fontWeight = FontWeight.W700, letterSpacing = (-0.025).em),
                    color = p.text,
                    modifier = Modifier.padding(bottom = 2.dp).testTag("home-consumed"),
                )
                Text("kcal consumidas", style = DietaBotType.labelMd.copy(letterSpacing = 0.sp), color = p.muted, modifier = Modifier.padding(bottom = 8.dp))
                Text(
                    "Meta ${ui.meta} kcal",
                    style = DietaBotType.labelMd.copy(fontSize = 11.sp, letterSpacing = 0.025.em),
                    color = p.dim,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(p.card)
                        .border(1.dp, p.line.copy(alpha = 0.6f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                        .testTag("home-meta"),
                )
            }
        }
        if (over) {
            Row(
                Modifier
                    .padding(top = 16.dp)
                    .clip(CircleShape)
                    .background(p.bad.copy(alpha = 0.15f))
                    .border(1.dp, p.bad.copy(alpha = 0.25f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .testTag("home-over"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(p.bad))
                Text("Meta excedida (+${ui.over} kcal)", style = DietaBotType.labelMd.copy(letterSpacing = 0.sp), color = p.bad)
            }
        }
    }
}

@Composable
private fun MacroCard(ui: HomePanelUiState) {
    val p = LocalPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(CardRadius)
            .background(p.card)
            .border(1.dp, p.line.copy(alpha = 0.4f), CardRadius)
            .padding(16.dp)
            .testTag("home-macros"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MacroRow("P", "Proteína", ui.protein, p.protein)
        MacroRow("C", "Carboidratos", ui.carbs, p.carbs)
        MacroRow("G", "Gorduras", ui.fat, p.fat)
    }
}

@Composable
private fun MacroRow(letter: String, name: String, line: MacroLine, color: Color) {
    val p = LocalPalette.current
    val tone = if (line.over) p.bad else color
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(tone))
            Spacer(Modifier.width(8.dp))
            Text(
                "$letter $name",
                style = DietaBotType.labelLg.copy(fontSize = 13.sp, letterSpacing = (-0.025).em),
                color = p.text,
                modifier = Modifier.weight(1f),
            )
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = if (line.over) p.bad else p.text, fontWeight = FontWeight.W600)) { append(line.consumed.toString()) }
                    append(" / ${line.target} g")
                },
                style = DietaBotType.labelLg.copy(fontSize = 13.sp, fontWeight = FontWeight.W500, letterSpacing = 0.sp),
                color = p.muted,
            )
        }
        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(p.surf2)) {
            if (line.fraction > 0f) Box(Modifier.fillMaxWidth(line.fraction).fillMaxHeight().clip(CircleShape).background(tone))
        }
    }
}

@Composable
private fun Timeline(ui: HomePanelUiState, onEmptyTap: (TimelineSlot) -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 37.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "LINHA DO TEMPO NUTRICIONAL",
                style = DietaBotType.labelCaps.copy(letterSpacing = 0.1.em),
                color = p.muted,
                modifier = Modifier.weight(1f),
            )
            Text("${ui.slotCount} Refeições", style = DietaBotType.labelMd, color = p.dim)
        }
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            ui.timeline.forEachIndexed { i, slot ->
                TimelineRow(slot, first = i == 0, last = i == ui.timeline.lastIndex, onEmptyTap)
            }
        }
    }
}

@Composable
private fun TimelineRow(slot: TimelineSlot, first: Boolean, last: Boolean, onEmptyTap: (TimelineSlot) -> Unit) {
    val p = LocalPalette.current
    val tag = "home-slot-${slot.slotId ?: "outros"}"
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .then(if (slot.state == SlotState.SKIPPED) Modifier.alpha(0.75f) else Modifier)
            .testTag(tag),
    ) {
        // Node + continuous guide. The guide runs through the 20 dp gap to the next row.
        Box(Modifier.width(32.dp).fillMaxHeight()) {
            Canvas(Modifier.fillMaxSize()) {
                val x = size.width / 2f
                val top = if (first) 16.dp.toPx() else -20.dp.toPx()
                val bottom = if (last) size.height - 20.dp.toPx() else size.height
                drawLine(p.line, Offset(x, top), Offset(x, bottom), strokeWidth = 2.dp.toPx())
            }
            Node(slot.state, slot.fromPhoto)
        }
        Spacer(Modifier.width(14.dp))
        SlotCard(slot, Modifier.weight(1f), onEmptyTap)
    }
}

@Composable
private fun Node(state: SlotState, fromPhoto: Boolean) {
    val p = LocalPalette.current
    Box(
        // ring-4 in the page colour: hides the guide around the node.
        Modifier.size(32.dp).drawBehind { drawCircle(p.phone, radius = size.minDimension / 2f + 4.dp.toPx()) },
        contentAlignment = Alignment.Center,
    ) {
        val node = Modifier.size(32.dp).clip(CircleShape)
        when {
            state == SlotState.LOGGED && !fromPhoto -> Box(node.background(p.gold), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Check, null, tint = p.phone, modifier = Modifier.size(18.dp))
            }
            state == SlotState.LOGGED -> Box(node.background(p.card).border(1.dp, p.gold, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.PhotoCamera, null, tint = p.gold, modifier = Modifier.size(17.dp))
            }
            state == SlotState.OVER -> Box(node.background(p.bad.copy(alpha = 0.15f)).border(1.dp, p.bad, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.PriorityHigh, null, tint = p.bad, modifier = Modifier.size(16.dp))
            }
            state == SlotState.SKIPPED -> Box(
                node.drawBehind {
                    drawCircle(
                        p.dim,
                        radius = size.minDimension / 2f - 1.dp.toPx(),
                        style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                    )
                },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Remove, null, tint = p.dim, modifier = Modifier.size(16.dp))
            }
            state == SlotState.NEXT -> Box(node.background(p.surf2).border(2.dp, p.gold, CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(p.gold))
            }
            else -> Box(node.border(2.dp, p.dim, CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(p.dim))
            }
        }
    }
}

@Composable
private fun SlotCard(slot: TimelineSlot, modifier: Modifier, onEmptyTap: (TimelineSlot) -> Unit) {
    val p = LocalPalette.current
    val empty = slot.state == SlotState.NEXT || slot.state == SlotState.EMPTY
    val skipped = slot.state == SlotState.SKIPPED
    val over = slot.state == SlotState.OVER
    val border = when {
        over -> p.bad.copy(alpha = 0.4f)
        skipped -> p.line.copy(alpha = 0.3f)
        else -> p.line.copy(alpha = 0.5f)
    }
    Column(
        modifier
            .clip(CardRadius)
            .background(if (skipped) p.card.copy(alpha = 0.5f) else p.card)
            .border(1.dp, border, CardRadius)
            .then(if (empty && slot.slotId != null) Modifier.clickable { onEmptyTap(slot) } else Modifier)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(if (empty || skipped) 4.dp else 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                slot.name,
                style = DietaBotType.headlineMd.copy(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = if (skipped) FontWeight.W500 else FontWeight.W700, letterSpacing = 0.sp),
                color = if (skipped) p.muted else p.text,
                modifier = Modifier.weight(1f),
            )
            slot.time?.let { Text(it, style = DietaBotType.labelMd, color = p.dim) }
            if (empty) Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = p.dim, modifier = Modifier.padding(start = 4.dp).size(18.dp))
        }
        when {
            empty -> Text(
                "Nenhum registro · Toque para pular",
                style = DietaBotType.bodyMd.copy(fontSize = 13.sp, letterSpacing = 0.sp),
                color = p.muted,
            )
            skipped -> Text(
                "Refeição pulada",
                style = DietaBotType.bodyMd.copy(fontSize = 13.sp, fontStyle = FontStyle.Italic, letterSpacing = 0.sp),
                color = p.dim,
            )
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    slot.lines.forEach { line ->
                        Row {
                            Text(
                                line.text,
                                style = DietaBotType.bodyMd.copy(fontSize = 13.sp, lineHeight = 16.sp),
                                color = p.text,
                                // Stitch wraps the description ~24 dp before the kcal column.
                                modifier = Modifier.weight(1f).padding(end = 24.dp),
                            )
                            Text(
                                "${line.kcal} kcal",
                                style = DietaBotType.bodyMd.copy(fontSize = 13.sp, fontWeight = if (over) FontWeight.W700 else FontWeight.W500),
                                color = if (over) p.bad else p.muted,
                            )
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(p.line.copy(alpha = 0.4f)))
                Text(
                    slot.summary,
                    style = DietaBotType.labelMd.copy(fontSize = 11.sp, letterSpacing = 0.sp),
                    color = if (over) p.bad else p.muted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(p.surf2)
                        .then(if (over) Modifier.border(1.dp, p.bad.copy(alpha = 0.3f), RoundedCornerShape(6.dp)) else Modifier)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .testTag("home-summary-${slot.slotId ?: "outros"}"),
                )
            }
        }
    }
}

@Composable
private fun Fab(onClick: () -> Unit, modifier: Modifier) {
    val p = LocalPalette.current
    Box(
        modifier
            .navigationBarsPadding()
            .padding(end = 22.dp, bottom = 28.dp)
            .size(64.dp)
            .clip(CircleShape)
            .background(p.gold)
            .clickable(onClick = onClick)
            .testTag("home-fab"),
        contentAlignment = Alignment.Center,
    ) {
        Text("Chat", style = DietaBotType.headlineMd.copy(fontSize = 15.sp, fontWeight = FontWeight.W700, letterSpacing = 0.sp), color = p.onGold)
    }
}
