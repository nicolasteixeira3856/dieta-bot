package app.fibrai.android.feature.config

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.fibrai.android.core.designsystem.dietaClick
import app.fibrai.android.core.designsystem.aero.Aero
import app.fibrai.android.core.designsystem.aero.AeroText
import app.fibrai.android.core.designsystem.aero.AeroBarBalance
import app.fibrai.android.core.designsystem.aero.AeroBubbleSpec
import app.fibrai.android.core.designsystem.aero.AeroButtonPrimary
import app.fibrai.android.core.designsystem.aero.AeroChoice
import app.fibrai.android.core.designsystem.aero.AeroIcon
import app.fibrai.android.core.designsystem.aero.AeroIconButton
import app.fibrai.android.core.designsystem.aero.AeroIconName
import app.fibrai.android.core.designsystem.aero.AeroMacro
import app.fibrai.android.core.designsystem.aero.AeroMacroTargetCard
import app.fibrai.android.core.designsystem.aero.AeroMealSlotRow
import app.fibrai.android.core.designsystem.aero.AeroNoteCard
import app.fibrai.android.core.designsystem.aero.AeroNoticeDialog
import app.fibrai.android.core.designsystem.aero.AeroNumberField
import app.fibrai.android.core.designsystem.aero.AeroOptionCard
import app.fibrai.android.core.designsystem.aero.AeroPage
import app.fibrai.android.core.designsystem.aero.AeroPageBubbles
import app.fibrai.android.core.designsystem.aero.AeroSegmented
import app.fibrai.android.core.designsystem.aero.AeroStepper
import app.fibrai.android.core.designsystem.aero.AeroTabs
import app.fibrai.android.core.designsystem.aero.AeroTextTokens
import app.fibrai.android.core.designsystem.aero.AeroTimeWheelDialog
import app.fibrai.android.core.designsystem.aero.aeroAccentSheen
import app.fibrai.android.core.designsystem.aero.aeroGlass
import app.fibrai.android.core.designsystem.aero.aeroLayerAlpha
import app.fibrai.android.core.designsystem.aero.cased
import app.fibrai.android.domain.SlotBand
import app.fibrai.android.domain.SlotModes
import app.fibrai.android.domain.SlotSuggestions
import kotlin.math.roundToInt

/*
 * The Config meal editor (A44, gold cfgS): the step-by-step meal distribution of the retired O3 (A71 moved it here,
 * ADR-057). Figma `Design`, Release 1: 20 dp margins, 24 dp from the top and between blocks, the CTA 32 dp above the bottom.
 */

private val CtaHeight = 58.dp

private val CtaBottom = 32.dp

/** The edge bubbles shared by the onboarding frames: one above the top-left corner, two on the side margins. */
private fun bubbles(rightY: Int, leftY: Int) = listOf(
    AeroBubbleSpec((-50).dp, (-60).dp, 80.dp),
    AeroBubbleSpec(372.dp, rightY.dp, 46.dp),
    AeroBubbleSpec((-22).dp, leftY.dp, 40.dp),
)

/**
 * Editor page: the header and the scrolled content on the page gradient, the edge bubbles, and the CTA
 * (Button/Primary with an arrow) fixed 32 dp above the bottom, 24 dp under the last block once scrolled.
 */
@Composable
private fun EditorFrame(
    header: @Composable () -> Unit,
    cta: String,
    ctaEnabled: Boolean,
    onCta: () -> Unit,
    onBack: (() -> Unit)?,
    ctaTag: String,
    bubbles: List<AeroBubbleSpec>,
    modifier: Modifier = Modifier,
    gap: Dp = 24.dp,
    scroll: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    AeroPage(modifier.fillMaxSize(), scroll) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                // A46: the scroll viewport ends at the top of the keyboard, so the focused field is brought above it.
                // The CTA below stays behind the keyboard while it is open.
                .imePadding()
                .verticalScroll(scroll)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = gap + CtaHeight + CtaBottom),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            header()
            content()
        }
        AeroPageBubbles(bubbles, scroll, Modifier.statusBarsPadding())
        AeroButtonPrimary(
            cta,
            onCta,
            icon = AeroIconName.ArrowRight,
            enabled = ctaEnabled,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = CtaBottom)
                .testTag(ctaTag),
        )
    }
}

/** Title and subtitle (Intro frame, 8 dp apart). */
@Composable
private fun Intro(title: String, subtitle: String?) {
    val c = Aero.colors
    val type = Aero.type
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AeroText(title, style = type.title.copy(color = c.textPrimary))
        if (subtitle != null) AeroText(subtitle, style = type.body.copy(color = c.textMuted))
    }
}

/** A labelled group: Label/Section muted, 12 dp, then the content. */
@Composable
private fun Group(label: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AeroText(AeroTextTokens.labelSection.cased(label), style = Aero.type.labelSection.copy(color = Aero.colors.textMuted))
        content()
    }
}


/**
 * The meal distribution editor of the Config (A44): Header/Page from [header], the step per group, the count, the rows.
 */
@Composable
fun MealScheduleEditor(
    ui: MealScheduleState,
    onCount: (Int) -> Unit,
    onName: (Int, String) -> Unit,
    onTime: (Int, Int) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onMode: (String) -> Unit = {},
    onCopy: () -> Unit = {},
    onConfirmMode: () -> Unit = {},
    onCancelMode: () -> Unit = {},
    initialPicking: Int = -1,
    header: @Composable () -> Unit = {},
    cta: String = "Continuar",
    ctaTag: String = "cfg-save",
    tag: String = "cfg",
) {
    BackHandler(onBack = onBack)
    var picking by remember { mutableIntStateOf(initialPicking) }
    val schedule = ui.slotSchedule
    Box(Modifier.fillMaxSize()) {
        EditorFrame(
            header = header,
            cta = cta,
            ctaEnabled = ui.valid,
            onCta = onContinue,
            onBack = onBack,
            ctaTag = ctaTag,
            bubbles = if (schedule.mode == "same") bubbles(520, 820) else bubbles(560, 880),
            // o3t: the page behind a dialog is blurred (Figma layer blur 8) under overlay/scrim.
            modifier = if (picking in ui.slots.indices || schedule.pendingMode != null) Modifier.blur(8.dp) else Modifier,
        ) {
            Intro("Distribuição das refeições", "Organize sua rotina para planejar o dia e receber lembretes no horário certo.")
            Group("Dias da semana") {
                val modes = SlotModes.labels
                AeroTabs(
                    modes.map { (mode, label) -> AeroChoice(label, "$tag-mode-$mode") },
                    selected = modes.indexOfFirst { it.first == schedule.mode },
                    onSelect = { onMode(modes[it].first) },
                )
            }
            if (schedule.mode != "same") {
                GroupStep(schedule, tag)
                if (schedule.index > 0) CopyPill("Copiar de ${schedule.groups[schedule.index - 1].label}", onCopy, tag)
            }
            Group("Quantidade de refeições") {
                val counts = (SlotSuggestions.MIN_SLOTS..SlotSuggestions.MAX_SLOTS).toList()
                AeroSegmented(
                    counts.map { AeroChoice(it.toString(), "$tag-count-$it") },
                    selected = counts.indexOf(ui.slots.size),
                    onSelect = { onCount(counts[it]) },
                    modifier = Modifier.testTag("$tag-count"),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ui.slots.forEachIndexed { i, slot ->
                    AeroMealSlotRow(
                        index = i,
                        name = slot.name,
                        time = SlotSuggestions.format(slot.minutes),
                        icon = bandIconAero(slot.minutes),
                        suggestions = SlotSuggestions.namesFor(slot.minutes),
                        onName = { onName(i, it) },
                        onPickTime = { picking = i },
                        tag = tag,
                    )
                }
            }
            AeroNoteCard(
                AeroIconName.BellRinging,
                "Você poderá ajustar intervalos, adicionar refeições intermediárias ou desativar alertas a qualquer momento.",
            )
        }
        if (picking in ui.slots.indices) {
            AeroTimeWheelDialog(
                title = ui.slots[picking].name.ifBlank { "Refeição ${picking + 1}" },
                minutes = ui.slots[picking].minutes,
                onDismiss = { picking = -1 },
                onConfirm = { onTime(picking, it); picking = -1 },
            )
        }
        SlotModeConfirmation(schedule, onConfirmMode, onCancelMode)
    }
}

/** o3s: group name, "Etapa n de m" and one segment per group. */
@Composable
private fun GroupStep(schedule: SlotScheduleDraft, tag: String) {
    val c = Aero.colors
    val type = Aero.type
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AeroText(schedule.group.label, Modifier.weight(1f).testTag("$tag-group"), style = type.title.copy(color = c.textPrimary))
            AeroText("Etapa ${schedule.index + 1} de ${schedule.groups.size}", style = type.caption.copy(color = c.textMuted))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            schedule.groups.forEachIndexed { i, _ ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(8.dp)
                        .then(if (i <= schedule.index) Modifier.aeroAccentSheen() else Modifier.clip(CircleShape).background(c.surface2)),
                )
            }
        }
    }
}

/** o3s "Copiar de {grupo}": tinted pill with the copy icon. */
@Composable
private fun CopyPill(label: String, onCopy: () -> Unit, tag: String) {
    val c = Aero.colors
    val pill = RoundedCornerShape(percent = 50)
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(pill)
            .background(c.surfaceTint)
            .border(1.dp, c.borderLine, pill)
            .dietaClick(onClick = onCopy)
            .testTag("$tag-copy"),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AeroIcon(AeroIconName.Copy, c.accentDefault, size = 20.dp)
        AeroText(label, style = Aero.type.bodyStrong.copy(color = c.textPrimary))
    }
}

private fun bandIconAero(minutes: Int): AeroIconName = when (SlotSuggestions.bandOf(minutes)) {
    SlotBand.BREAKFAST -> AeroIconName.Coffee
    SlotBand.MORNING_SNACK, SlotBand.AFTERNOON_SNACK -> AeroIconName.Cookie
    SlotBand.LUNCH -> AeroIconName.ForkKnife
    SlotBand.DINNER -> AeroIconName.BowlFood
    SlotBand.NIGHT -> AeroIconName.Moon
}

/** The two tones of the Tali (the Config sheet cfgT): Seco preselected and marked PADRÃO. */
@Composable
fun ToneOptions(tone: String, onTone: (String) -> Unit, tagPrefix: String, onGlass: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AeroOptionCard(
            "Seco",
            "Só os números. Sem opinião.",
            selected = tone != "duro",
            onClick = { onTone("seco") },
            badge = "Padrão",
            onGlass = onGlass,
            modifier = Modifier.testTag("$tagPrefix-seco"),
        )
        AeroOptionCard(
            "Duro",
            "Cobra o que estourou e o que faltou. Sem rodeio.",
            selected = tone == "duro",
            onClick = { onTone("duro") },
            onGlass = onGlass,
            modifier = Modifier.testTag("$tagPrefix-duro"),
        )
    }
}

/** The editor's input: the rows of the current group and the schedule draft. */
@androidx.compose.runtime.Immutable
data class MealScheduleState(
    val slots: List<SlotDraft> = SlotScheduleDraft.defaults(),
    val slotSchedule: SlotScheduleDraft = SlotScheduleDraft(),
) {
    val valid: Boolean
        get() = slotSchedule.pendingMode == null && SlotScheduleDraft.validSlots(slots)
}
