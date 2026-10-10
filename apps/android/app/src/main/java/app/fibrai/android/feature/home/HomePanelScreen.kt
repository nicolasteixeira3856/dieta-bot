package app.fibrai.android.feature.home

import app.fibrai.android.core.designsystem.aero.AeroDayState
import app.fibrai.android.core.designsystem.aero.AeroDayStrip
import app.fibrai.android.core.designsystem.aero.AeroExtraCard
import app.fibrai.android.core.designsystem.aero.AeroStripDay

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
import app.fibrai.android.core.designsystem.Haptic
import app.fibrai.android.core.designsystem.SplashBoot
import app.fibrai.android.core.designsystem.aero.Aero
import app.fibrai.android.core.designsystem.aero.AeroText
import app.fibrai.android.core.designsystem.aero.AeroBubble
import app.fibrai.android.core.designsystem.aero.AeroButtonPrimary
import app.fibrai.android.core.designsystem.aero.AeroChipLog
import app.fibrai.android.core.designsystem.aero.AeroChipTone
import app.fibrai.android.core.designsystem.aero.AeroConfirmDialog
import app.fibrai.android.core.designsystem.aero.AeroHeaderDay
import app.fibrai.android.core.designsystem.aero.AeroIcon
import app.fibrai.android.core.designsystem.aero.AeroIconName
import app.fibrai.android.core.designsystem.aero.AeroMacro
import app.fibrai.android.core.designsystem.aero.AeroMacroRow
import app.fibrai.android.core.designsystem.aero.AeroMealCard
import app.fibrai.android.core.designsystem.aero.AeroMealLine
import app.fibrai.android.core.designsystem.aero.AeroMealState
import app.fibrai.android.core.designsystem.aero.AeroNodeState
import app.fibrai.android.core.designsystem.aero.AeroPage
import app.fibrai.android.core.designsystem.aero.AeroRingDay
import app.fibrai.android.core.designsystem.aero.AeroRingState
import app.fibrai.android.core.designsystem.aero.AeroTextTokens
import app.fibrai.android.core.designsystem.aero.AeroTimelineNode
import app.fibrai.android.core.designsystem.aero.aeroGlass
import app.fibrai.android.core.designsystem.aero.cased
import app.fibrai.android.core.designsystem.dietaClick
import app.fibrai.android.feature.workout.WorkoutSheet

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
    /** A60 part B: a collapsed closure card tapped. */
    onClosureExpand: (String) -> Unit = {},
    /** Opens the skip confirmation of this slot (QA renders of chatP). */
    initialSkip: Long? = null,
    /** A72: a day of the strip tapped (ISO date). */
    onSelectDay: (String) -> Unit = {},
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
                // A72 (ADR-058 decision 3): the last 30 days, today at the right end.
                if (ui.strip.isNotEmpty()) {
                    AeroDayStrip(ui.strip.map { it.toAero() }, onSelectDay)
                }
                Hero(ui)
                MacroCard(ui)
                WorkoutRow(ui, if (ui.past) null else onWorkoutOpen)
                ui.closures.forEach { ClosureCardView(it) { onClosureExpand(it.key) } }
                Timeline(ui, onRecord = onChat, onSkipAsk = { confirmSkip = it }, readOnly = ui.past)
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

/**
 * A22 "Treino de hoje": "Informar" in the accent, or "350 kcal · +175 na meta". Tap opens homeW. A72 (homeH): a past day reads
 * "Treino do dia", without the chevron or the tap ([onClick] null).
 */
@Composable
private fun WorkoutRow(ui: HomePanelUiState, onClick: (() -> Unit)?) {
    val c = Aero.colors
    val type = Aero.type
    Row(
        Modifier
            .fillMaxWidth()
            .aeroGlass(Aero.shapes.card)
            .then(if (onClick != null) Modifier.dietaClick(Haptic.Light, onClick = onClick) else Modifier)
            .padding(horizontal = 17.dp, vertical = 15.dp)
            .testTag("home-workout"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(c.surfaceTint), contentAlignment = Alignment.Center) {
            AeroIcon(AeroIconName.Barbell, c.iconPrimary)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AeroText(if (ui.past) "Treino do dia" else "Treino de hoje", style = type.bodyStrong.copy(color = c.textPrimary), maxLines = 1)
            val kcal = ui.workoutKcal
            AeroText(
                if (kcal == null && ui.past) {
                    buildAnnotatedString { withStyle(SpanStyle(color = c.textMuted)) { append("Sem treino") } }
                } else if (kcal == null) {
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
        if (onClick != null) AeroIcon(AeroIconName.CaretRight, c.iconPrimary, size = 20.dp)
    }
}

@Composable
private fun Timeline(ui: HomePanelUiState, onRecord: () -> Unit, onSkipAsk: (TimelineSlot) -> Unit, readOnly: Boolean = false) {
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
                TimelineRow(slot, last = i == ui.timeline.lastIndex, onRecord, onSkipAsk, readOnly)
            }
        }
    }
}

private val SlotGap = 16.dp
private val NodeTop = 18.dp
private val NodeSize = 24.dp

/**
 * One slot: node column (24 dp, node 18 dp down) and the meal card. The guide runs from node centre to node centre.
 * An empty card opens the Chat on tap and asks to skip on long press (ADR-040).
 */
@Composable
private fun TimelineRow(slot: TimelineSlot, last: Boolean, onRecord: () -> Unit, onSkipAsk: (TimelineSlot) -> Unit, readOnly: Boolean = false) {
    val c = Aero.colors
    val tag = if (slot.state == SlotState.EXTRA) "home-extra-${slot.time}" else "home-slot-${slot.slotId ?: "outros"}"
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
                SlotState.PLANNED -> AeroNodeState.Planned
                SlotState.EXTRA -> AeroNodeState.Extra
            },
            Modifier.padding(top = NodeTop),
        )
        // A72 (D28 Card/Extra): an extra at its time, no gesture.
        if (slot.state == SlotState.EXTRA) {
            AeroExtraCard(slot.name, slot.extraText.orEmpty(), slot.kcal, slot.p, slot.c, slot.g, Modifier.weight(1f))
            return@Row
        }
        // A planned meal opens the Chat like an empty one; a long press skips it (and clears the reservation).
        // A72: a past day's cards take no tap and no long press.
        val tappable = !readOnly && (slot.state == SlotState.NEXT || slot.state == SlotState.EMPTY || slot.state == SlotState.PLANNED) && slot.slotId != null
        AeroMealCard(
            state = when (slot.state) {
                SlotState.LOGGED -> AeroMealState.Logged
                SlotState.OVER -> AeroMealState.Over
                SlotState.SKIPPED -> AeroMealState.Skipped
                SlotState.NEXT -> AeroMealState.Pending
                SlotState.EMPTY -> AeroMealState.Empty
                SlotState.PLANNED -> AeroMealState.Planned
                SlotState.EXTRA -> AeroMealState.Logged
            },
            meal = slot.name,
            time = slot.time.orEmpty(),
            description = when (slot.state) {
                SlotState.SKIPPED -> "Refeição pulada"
                SlotState.NEXT, SlotState.EMPTY -> if (readOnly) "Nenhum registro" else "Nenhum registro · Toque para registrar, segura para pular"
                else -> ""
            },
            lines = slot.lines.map { AeroMealLine(it.text, if (slot.state == SlotState.PLANNED) "planejado · ${it.kcal} kcal" else "${it.kcal} kcal") },
            log = slot.summary,
            logTag = "home-summary-${slot.slotId ?: "outros"}",
            onClick = if (tappable) onRecord else null,
            onClickLabel = "Registrar",
            onLongClick = if (tappable) ({ onSkipAsk(slot) }) else null,
            onLongClickLabel = "Pular",
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Card/Closure (D16, homeC / homeK): title, the kcal line, the macros in their colours (day), the detail lines and the
 * server text; collapsed, one line that expands on tap.
 */
@Composable
private fun ClosureCardView(card: ClosureCard, onExpand: () -> Unit) {
    val c = Aero.colors
    val type = Aero.type
    val tag = "home-closure-${card.period}"
    if (!card.expanded) {
        Row(
            Modifier
                .fillMaxWidth()
                .aeroGlass(Aero.shapes.card)
                .dietaClick(Haptic.Light, onClick = onExpand)
                .padding(horizontal = 17.dp, vertical = 16.dp)
                .testTag("$tag-collapsed"),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AeroText(card.line, Modifier.weight(1f), style = type.body.copy(color = c.textPrimary), maxLines = 1)
            AeroIcon(AeroIconName.CaretRight, c.iconMuted, size = 20.dp)
        }
        return
    }
    Column(
        Modifier
            .fillMaxWidth()
            .aeroGlass(Aero.shapes.card)
            .padding(horizontal = 17.dp, vertical = 16.dp)
            .testTag(tag),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AeroText(card.title, style = type.bodyStrong.copy(color = c.textPrimary))
        AeroText(card.kcalLine, style = type.body.copy(color = c.textPrimary), modifier = Modifier.testTag("$tag-kcal"))
        card.macros?.let { m ->
            AeroText(
                buildAnnotatedString {
                    val colors = listOf(c.macroProtein, c.macroCarbs, c.macroFat)
                    listOf("P", "C", "G").forEachIndexed { i, label ->
                        if (i > 0) withStyle(SpanStyle(color = c.textMuted)) { append(" · ") }
                        withStyle(SpanStyle(color = colors[i])) { append("$label ${m[i].consumed}/${m[i].target}") }
                    }
                },
                style = type.caption,
            )
        }
        card.detail?.let { AeroText(it, style = type.caption.copy(color = c.textMuted)) }
        card.extra?.let { AeroText(it, style = type.caption.copy(color = c.textMuted)) }
        AeroText(card.text, style = type.body.copy(color = c.textPrimary), modifier = Modifier.testTag("$tag-text"))
    }
}

/** A72: a strip day as the Aero component draws it. */
private fun StripDay.toAero() = AeroStripDay(
    key = date.toString(),
    day = day,
    month = month,
    state = when (state) {
        DayCircleState.TODAY_SELECTED -> AeroDayState.TodaySelected
        DayCircleState.TODAY -> AeroDayState.Today
        DayCircleState.PAST_SELECTED -> AeroDayState.PastSelected
        DayCircleState.PAST -> AeroDayState.Past
        DayCircleState.NO_RECORD -> AeroDayState.NoRecord
    },
    label = label,
)
