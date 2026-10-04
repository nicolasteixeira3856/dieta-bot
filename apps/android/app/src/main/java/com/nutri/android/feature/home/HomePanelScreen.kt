package com.nutri.android.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.nutri.android.core.designsystem.Haptic
import com.nutri.android.core.designsystem.SplashBoot
import com.nutri.android.core.designsystem.aero.Aero
import com.nutri.android.core.designsystem.aero.AeroText
import com.nutri.android.core.designsystem.aero.AeroBubble
import com.nutri.android.core.designsystem.aero.AeroButtonPrimary
import com.nutri.android.core.designsystem.aero.AeroChipLog
import com.nutri.android.core.designsystem.aero.AeroChipTone
import com.nutri.android.core.designsystem.aero.AeroConfirmDialog
import com.nutri.android.core.designsystem.aero.AeroHeaderDay
import com.nutri.android.core.designsystem.aero.AeroIcon
import com.nutri.android.core.designsystem.aero.AeroIconName
import com.nutri.android.core.designsystem.aero.AeroMacro
import com.nutri.android.core.designsystem.aero.AeroMacroRow
import com.nutri.android.core.designsystem.aero.AeroMealCard
import com.nutri.android.core.designsystem.aero.AeroMealLine
import com.nutri.android.core.designsystem.aero.AeroMealState
import com.nutri.android.core.designsystem.aero.AeroNodeState
import com.nutri.android.core.designsystem.aero.AeroPage
import com.nutri.android.core.designsystem.aero.AeroRingDay
import com.nutri.android.core.designsystem.aero.AeroRingState
import com.nutri.android.core.designsystem.aero.AeroTextTokens
import com.nutri.android.core.designsystem.aero.AeroTimelineNode
import com.nutri.android.core.designsystem.aero.aeroGlass
import com.nutri.android.core.designsystem.aero.cased
import com.nutri.android.core.designsystem.dietaClick
import com.nutri.android.feature.workout.WorkoutSheet

/* Figma `Design`, Release 1, Home · D3 (home0/home1/homeX/homeW): 20 dp margins, 24 dp top, 20 dp between blocks. */
private val FabHeight = 58.dp
private val FabBottom = 32.dp

/** At the end of the scroll the disclaimer sits 20 dp above the FAB, as on the gold page. */
private val ContentBottom = FabBottom + FabHeight + 20.dp

@Composable
fun HomePanelScreen(
    ui: HomePanelUiState,
    onSkip: (Long) -> Unit,
    onConfig: () -> Unit,
    onChat: () -> Unit,
    onWorkoutOpen: () -> Unit = {},
    onWorkoutChange: (String) -> Unit = {},
    onWorkoutSave: () -> Unit = {},
    onWorkoutCancel: () -> Unit = {},
    /** Opens the skip confirmation of this slot (QA renders of chatP). */
    initialSkip: Long? = null,
) {
    var confirmSkip by remember { mutableStateOf(initialSkip?.let { id -> ui.timeline.firstOrNull { it.slotId == id } }) }
    val workout = ui.workoutEditor
    val scroll = rememberScrollState()
    AeroPage(Modifier.fillMaxSize().testTag("home"), scroll) {
        Box(
            Modifier
                .fillMaxSize()
                // homeW: the Home behind the sheet is blurred (Figma layer blur 8) and dimmed by overlay/scrim.
                .then(if (workout != null || confirmSkip != null) Modifier.blur(8.dp) else Modifier)
                .statusBarsPadding()
                .verticalScroll(scroll),
        ) {
            Column(
                Modifier
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = ContentBottom),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                AeroHeaderDay(ui.dayLabel, ui.dateLabel, onConfig, dateTag = "home-date", configTag = "home-config")
                Hero(ui)
                MacroCard(ui)
                WorkoutRow(ui, onWorkoutOpen)
                Timeline(ui) { confirmSkip = it }
                AeroText(
                    SplashBoot.COPY,
                    style = Aero.type.caption.copy(color = Aero.colors.textDim, textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth().testTag("home-disclaimer"),
                )
            }
            // Edge bubbles of the gold frame (decoration, never behind text), placed on the page coordinates.
            AeroBubble(80.dp, Modifier.offset(200.dp, (-50).dp))
            AeroBubble(46.dp, Modifier.offset(352.dp, 290.dp))
            AeroBubble(40.dp, Modifier.offset((-22).dp, 434.dp))
        }
        if (workout == null) {
            AeroButtonPrimary(
                "Chat",
                onChat,
                icon = AeroIconName.ChatCircle,
                fillWidth = false,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = FabBottom)
                    .testTag("home-fab"),
            )
        } else {
            WorkoutSheet(workout, onWorkoutChange, onWorkoutSave, onWorkoutCancel)
        }
        // chatP: Dialog/Confirm over the blurred Home and overlay/scrim.
        confirmSkip?.let { slot ->
            AeroConfirmDialog(
                title = "Pular ${slot.name}?",
                primary = "Pular",
                onPrimary = {
                    slot.slotId?.let(onSkip)
                    confirmSkip = null
                },
                secondary = "Cancelar",
                onSecondary = { confirmSkip = null },
                modifier = Modifier.testTag("home-skip-dialog"),
                primaryTag = "home-skip-confirm",
                secondaryTag = "home-skip-cancel",
            )
        }
    }
}

/** Hero card: Ring/Day, and the "Meta excedida" pill under it when the day is over the meta (homeX). */
@Composable
private fun Hero(ui: HomePanelUiState) {
    val over = ui.over > 0
    Column(
        Modifier
            .fillMaxWidth()
            .aeroGlass(Aero.shapes.card)
            .padding(vertical = 29.dp)
            .testTag("home-ring"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AeroRingDay(
            consumed = ui.consumed.toString(),
            meta = "Meta ${ui.meta} kcal",
            progress = ui.ringFraction,
            state = when {
                over -> AeroRingState.Exceeded
                ui.consumed == 0 -> AeroRingState.Empty
                else -> AeroRingState.Normal
            },
            consumedTag = "home-consumed",
            metaTag = "home-meta",
        )
        if (over) {
            AeroChipLog("Meta excedida (+${ui.over} kcal)", Modifier.testTag("home-over"), tone = AeroChipTone.Bad)
        }
    }
}

@Composable
private fun MacroCard(ui: HomePanelUiState) {
    Column(
        Modifier
            .fillMaxWidth()
            .aeroGlass(Aero.shapes.card)
            .padding(21.dp)
            .testTag("home-macros"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Macro(AeroMacro.Protein, "P Proteína", ui.protein)
        Macro(AeroMacro.Carbs, "C Carboidratos", ui.carbs)
        Macro(AeroMacro.Fat, "G Gorduras", ui.fat)
    }
}

@Composable
private fun Macro(macro: AeroMacro, label: String, line: MacroLine) {
    AeroMacroRow(macro, label, line.consumed.toString(), "${line.target} g", line.fraction, line.over)
}

/** A22 "Treino de hoje": "Informar" in the accent, or "350 kcal · +175 na meta". Tap opens homeW. */
@Composable
private fun WorkoutRow(ui: HomePanelUiState, onClick: () -> Unit) {
    val c = Aero.colors
    val type = Aero.type
    Row(
        Modifier
            .fillMaxWidth()
            .aeroGlass(Aero.shapes.card)
            .dietaClick(Haptic.Light, onClick = onClick)
            .padding(horizontal = 17.dp, vertical = 15.dp)
            .testTag("home-workout"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(c.surfaceTint), contentAlignment = Alignment.Center) {
            AeroIcon(AeroIconName.Barbell, c.iconPrimary)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AeroText("Treino de hoje", style = type.bodyStrong.copy(color = c.textPrimary), maxLines = 1)
            val kcal = ui.workoutKcal
            AeroText(
                if (kcal == null) {
                    buildAnnotatedString { withStyle(SpanStyle(color = c.accentDefault)) { append("Informar") } }
                } else {
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = c.textPrimary)) { append("$kcal kcal") }
                        withStyle(SpanStyle(color = c.textMuted, fontWeight = type.caption.fontWeight)) { append(" · +${ui.workoutCredit} na meta") }
                    }
                },
                style = type.captionStrong,
                maxLines = 1,
                modifier = Modifier.testTag("home-workout-value"),
            )
        }
        AeroIcon(AeroIconName.CaretRight, c.iconPrimary, size = 20.dp)
    }
}

@Composable
private fun Timeline(ui: HomePanelUiState, onEmptyTap: (TimelineSlot) -> Unit) {
    val c = Aero.colors
    val type = Aero.type
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth().height(18.dp), verticalAlignment = Alignment.CenterVertically) {
            AeroText(
                AeroTextTokens.labelSection.cased("Linha do tempo nutricional"),
                style = type.labelSection.copy(color = c.textMuted),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            AeroText("${ui.slotCount} Refeições", style = type.caption.copy(color = c.textDim))
        }
        Column(verticalArrangement = Arrangement.spacedBy(SlotGap)) {
            ui.timeline.forEachIndexed { i, slot ->
                TimelineRow(slot, last = i == ui.timeline.lastIndex, onEmptyTap)
            }
        }
    }
}

private val SlotGap = 16.dp
private val NodeTop = 18.dp
private val NodeSize = 24.dp

/** One slot: node column (24 dp, node 18 dp down) and the meal card. The guide runs from node centre to node centre. */
@Composable
private fun TimelineRow(slot: TimelineSlot, last: Boolean, onEmptyTap: (TimelineSlot) -> Unit) {
    val c = Aero.colors
    val tag = "home-slot-${slot.slotId ?: "outros"}"
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (last) {
                    Modifier
                } else {
                    Modifier.drawBehind {
                        val x = NodeSize.toPx() / 2
                        val top = (NodeTop + NodeSize / 2).toPx()
                        drawLine(c.borderLine, Offset(x, top), Offset(x, size.height + (SlotGap + NodeTop + NodeSize / 2).toPx()), strokeWidth = 2.dp.toPx())
                    }
                },
            )
            .testTag(tag),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AeroTimelineNode(
            when (slot.state) {
                SlotState.LOGGED -> if (slot.fromPhoto) AeroNodeState.Photo else AeroNodeState.Done
                SlotState.OVER -> AeroNodeState.Over
                SlotState.SKIPPED -> AeroNodeState.Skipped
                SlotState.NEXT -> AeroNodeState.Active
                SlotState.EMPTY -> AeroNodeState.Empty
            },
            Modifier.padding(top = NodeTop),
        )
        val tappable = (slot.state == SlotState.NEXT || slot.state == SlotState.EMPTY) && slot.slotId != null
        AeroMealCard(
            state = when (slot.state) {
                SlotState.LOGGED -> AeroMealState.Logged
                SlotState.OVER -> AeroMealState.Over
                SlotState.SKIPPED -> AeroMealState.Skipped
                SlotState.NEXT -> AeroMealState.Pending
                SlotState.EMPTY -> AeroMealState.Empty
            },
            meal = slot.name,
            time = slot.time.orEmpty(),
            description = when (slot.state) {
                SlotState.SKIPPED -> "Refeição pulada"
                SlotState.NEXT, SlotState.EMPTY -> "Nenhum registro · Toque para pular"
                else -> ""
            },
            lines = slot.lines.map { AeroMealLine(it.text, "${it.kcal} kcal") },
            log = slot.summary,
            logTag = "home-summary-${slot.slotId ?: "outros"}",
            onClick = if (tappable) ({ onEmptyTap(slot) }) else null,
            modifier = Modifier.weight(1f),
        )
    }
}
