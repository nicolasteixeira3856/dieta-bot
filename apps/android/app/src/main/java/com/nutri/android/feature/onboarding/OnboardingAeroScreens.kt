package com.nutri.android.feature.onboarding

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.nutri.android.core.designsystem.Haptic
import com.nutri.android.core.designsystem.dietaClick
import com.nutri.android.core.designsystem.rememberHaptic
import com.nutri.android.core.designsystem.aero.Aero
import com.nutri.android.core.designsystem.aero.AeroText
import com.nutri.android.core.designsystem.aero.AeroBarBalance
import com.nutri.android.core.designsystem.aero.AeroBubbleSpec
import com.nutri.android.core.designsystem.aero.AeroButtonPrimary
import com.nutri.android.core.designsystem.aero.AeroChoice
import com.nutri.android.core.designsystem.aero.AeroIcon
import com.nutri.android.core.designsystem.aero.AeroIconButton
import com.nutri.android.core.designsystem.aero.AeroIconName
import com.nutri.android.core.designsystem.aero.AeroMacro
import com.nutri.android.core.designsystem.aero.AeroMacroTargetCard
import com.nutri.android.core.designsystem.aero.AeroMealSlotRow
import com.nutri.android.core.designsystem.aero.AeroNoteCard
import com.nutri.android.core.designsystem.aero.AeroNumberField
import com.nutri.android.core.designsystem.aero.AeroOptionCard
import com.nutri.android.core.designsystem.aero.AeroPage
import com.nutri.android.core.designsystem.aero.AeroPageBubbles
import com.nutri.android.core.designsystem.aero.AeroSegmented
import com.nutri.android.core.designsystem.aero.AeroStepper
import com.nutri.android.core.designsystem.aero.AeroTabs
import com.nutri.android.core.designsystem.aero.AeroTextTokens
import com.nutri.android.core.designsystem.aero.AeroTimeWheelDialog
import com.nutri.android.core.designsystem.aero.aeroAccentSheen
import com.nutri.android.core.designsystem.aero.aeroGlass
import com.nutri.android.core.designsystem.aero.aeroLayerAlpha
import com.nutri.android.core.designsystem.aero.cased
import com.nutri.android.domain.SlotBand
import com.nutri.android.domain.SlotModes
import com.nutri.android.domain.SlotSuggestions
import kotlin.math.roundToInt

/*
 * Splash and onboarding on Aero (A41): Figma `Design`, Release 1, section "Splash e onboarding · D4".
 * Frames: 20 dp margins, 24 dp from the top, 24 dp between blocks (o1e: 20), the CTA 32 dp above the bottom.
 */

private val CtaHeight = 58.dp
private val CtaBottom = 32.dp

/** The edge bubbles shared by the onboarding frames: one above the top-left corner, two on the side margins. */
private fun bubbles(rightY: Int, leftY: Int) = listOf(
    AeroBubbleSpec((-50).dp, (-60).dp, 80.dp),
    AeroBubbleSpec(372.dp, rightY.dp, 46.dp),
    AeroBubbleSpec((-22).dp, leftY.dp, 40.dp),
)

/** Top of an onboarding frame: stepper alone (O1), intake bar (O2/O3) or brand bar with help (O4). */
sealed interface AeroOnboardingBar {
    data class Stepper(val step: Int) : AeroOnboardingBar
    data class Intake(val step: Int) : AeroOnboardingBar
    data class Brand(val step: Int, val onHelp: () -> Unit) : AeroOnboardingBar
}

/**
 * Onboarding page: the top bar and the scrolled content on the page gradient, the edge bubbles, and the CTA
 * (Button/Primary with an arrow) fixed 32 dp above the bottom, 24 dp under the last block once scrolled.
 */
@Composable
private fun AeroOnboardingFrame(
    bar: AeroOnboardingBar,
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
                .verticalScroll(scroll)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = gap + CtaHeight + CtaBottom),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            TopBar(bar, onBack)
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

@Composable
private fun TopBar(bar: AeroOnboardingBar, onBack: (() -> Unit)?) {
    val c = Aero.colors
    val type = Aero.type
    when (bar) {
        is AeroOnboardingBar.Stepper -> AeroStepper(bar.step)
        is AeroOnboardingBar.Intake, is AeroOnboardingBar.Brand -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth().height(44.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AeroIconButton(
                    AeroIconName.ArrowLeft,
                    { onBack?.invoke() },
                    contentDescription = "Voltar",
                    enabled = onBack != null,
                    modifier = Modifier.testTag("onboarding-back"),
                )
                if (bar is AeroOnboardingBar.Brand) {
                    AeroText("Dieta Bot", Modifier.weight(1f), style = type.title.copy(color = c.textPrimary, textAlign = TextAlign.Center))
                    AeroIconButton(AeroIconName.Question, bar.onHelp, contentDescription = "Ajuda", modifier = Modifier.testTag("onboarding-help"))
                } else {
                    AeroText(
                        AeroTextTokens.labelSection.cased("Dieta Bot Intake"),
                        Modifier.weight(1f),
                        style = type.labelSection.copy(color = c.textPrimary, textAlign = TextAlign.Center),
                    )
                    AeroBarBalance()
                }
            }
            AeroStepper(if (bar is AeroOnboardingBar.Intake) bar.step else (bar as AeroOnboardingBar.Brand).step)
        }
    }
}

/** Eyebrow "ONBOARDING n/4 • SECTION", title and subtitle (Intro frame, 8 dp apart). */
@Composable
private fun Intro(step: String, section: String, title: String, subtitle: String, leadingDot: Boolean = false) {
    val c = Aero.colors
    val type = Aero.type
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (leadingDot) Box(Modifier.size(6.dp).background(c.accentDefault, CircleShape))
            AeroText(AeroTextTokens.labelSection.cased(step), style = type.labelSection.copy(color = c.textMuted))
            Box(Modifier.size(4.dp).background(c.borderLine, CircleShape))
            AeroText(AeroTextTokens.labelSection.cased(section), style = type.labelSection.copy(color = c.accentDefault))
        }
        AeroText(title, style = type.title.copy(color = c.textPrimary))
        AeroText(subtitle, style = type.body.copy(color = c.textMuted))
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

// ---------------------------------------------------------------- O1

@Composable
fun CeilingScreen(
    ui: OnboardingUiState,
    onSex: (String) -> Unit,
    onAge: (String) -> Unit,
    onHeight: (String) -> Unit,
    onWeight: (String) -> Unit,
    onMode: (String) -> Unit,
    onSame: (String) -> Unit,
    onWeekday: (String) -> Unit,
    onWeekend: (String) -> Unit,
    onDay: (Int, String) -> Unit,
    onContinue: () -> Unit,
) {
    val c = Aero.colors
    val type = Aero.type
    val focus = LocalFocusManager.current
    val done = KeyboardActions(onDone = { focus.clearFocus() })
    val height = remember { FocusRequester() }
    val weight = remember { FocusRequester() }
    val ceiling = remember { List(7) { FocusRequester() } }
    val enabled = ui.profileValid
    AeroOnboardingFrame(
        bar = AeroOnboardingBar.Stepper(1),
        cta = "Continuar",
        ctaEnabled = ui.o1Valid,
        onCta = onContinue,
        onBack = null,
        ctaTag = "o1-continue",
        bubbles = bubbles(600, 860),
        // o1e (ST8 / D4) keeps its own, tighter spacing.
        gap = if (enabled) 24.dp else 20.dp,
    ) {
        Intro("Onboarding 1/4", "Metabolismo", "Teto do dia", "Defina sua meta diária de calorias. Você pode usar o valor sugerido ou personalizar.")
        val sexes = listOf("male" to "Homem", "female" to "Mulher")
        AeroSegmented(
            sexes.map { (value, label) -> AeroChoice(label, "o1-sex-$value") },
            selected = sexes.indexOfFirst { it.first == ui.sex },
            onSelect = { onSex(sexes[it].first) },
            modifier = Modifier.testTag("o1-sex"),
        )
        Group("Idade, altura e peso") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AeroNumberField(
                    ui.ageField, onAge, "anos", Modifier.weight(1f), fieldModifier = Modifier.testTag("o1-age"),
                    compact = true, placeholder = "idade",
                    imeAction = ImeAction.Next, keyboardActions = KeyboardActions(onNext = { height.requestFocus() }),
                )
                AeroNumberField(
                    ui.heightField, onHeight, "cm", Modifier.weight(1f), fieldModifier = Modifier.focusRequester(height).testTag("o1-height"),
                    compact = true, placeholder = "altura",
                    imeAction = ImeAction.Next, keyboardActions = KeyboardActions(onNext = { weight.requestFocus() }),
                )
                AeroNumberField(
                    ui.weightField, onWeight, "kg", Modifier.weight(1f), fieldModifier = Modifier.focusRequester(weight).testTag("o1-weight"),
                    compact = true, placeholder = "peso", decimal = true,
                    imeAction = ImeAction.Done, keyboardActions = done,
                )
            }
            if (!enabled) {
                Row(Modifier.testTag("o1-profile-hint"), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AeroIcon(AeroIconName.Info, c.iconMuted, size = 16.dp)
                    AeroText("Preencha idade, altura e peso para ver a meta sugerida.", style = type.caption.copy(color = c.textMuted))
                }
            }
        }
        Group("Modo do teto") {
            Column(
                Modifier.aeroLayerAlpha(if (enabled) 1f else DISABLED_ALPHA).testTag("o1-modes"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CeilingModes.forEach { (value, title, body) ->
                    AeroOptionCard(
                        title,
                        body,
                        selected = ui.ceilingMode == value,
                        onClick = { onMode(value) },
                        enabled = enabled,
                        modifier = Modifier.testTag("o1-mode-$value"),
                    )
                }
            }
        }
        when (ui.ceilingMode) {
            "weekdayWeekend" -> Group("Metas diárias") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CaptionedKcal("Dias úteis", ui.weekdayField, onWeekday, Modifier.weight(1f), Modifier.focusRequester(ceiling[0]).testTag("o1-weekday"), enabled, ImeAction.Next, KeyboardActions(onNext = { ceiling[1].requestFocus() }))
                    CaptionedKcal("Fim de semana", ui.weekendField, onWeekend, Modifier.weight(1f), Modifier.focusRequester(ceiling[1]).testTag("o1-weekend"), enabled, ImeAction.Done, done)
                }
            }
            "seven" -> Group("Meta por dia") {
                val labels = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    labels.chunked(2).forEachIndexed { row, pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEachIndexed { col, label ->
                                val i = row * 2 + col
                                val last = i == labels.lastIndex
                                CaptionedKcal(
                                    label, ui.dayFields[i], { onDay(i, it) }, Modifier.weight(1f), Modifier.focusRequester(ceiling[i]).testTag("o1-day-$i"), enabled,
                                    if (last) ImeAction.Done else ImeAction.Next,
                                    if (last) done else KeyboardActions(onNext = { ceiling[i + 1].requestFocus() }),
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            else -> Group("Meta diária") {
                AeroNumberField(
                    ui.sameField, onSame, "kcal",
                    Modifier.fillMaxWidth().aeroLayerAlpha(if (enabled) 1f else DISABLED_ALPHA),
                    fieldModifier = Modifier.testTag("o1-ceiling"),
                    placeholder = "—", icon = AeroIconName.Lightning, enabled = enabled,
                    imeAction = ImeAction.Done, keyboardActions = done,
                )
                ui.suggestedCeiling?.takeIf { enabled }?.let { suggested ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AeroIcon(AeroIconName.Sparkle, c.accentDefault, size = 16.dp)
                        AeroText(
                            buildAnnotatedString {
                                append("Sugerido ")
                                withStyle(SpanStyle(color = c.textPrimary, fontWeight = type.captionStrong.fontWeight)) { append("$suggested kcal") }
                                append(" com base no seu perfil. Você pode alterar quando quiser.")
                            },
                            style = type.caption.copy(color = c.textMuted),
                            modifier = Modifier.testTag("o1-suggested"),
                        )
                    }
                }
            }
        }
    }
}

private val CeilingModes = listOf(
    Triple("same", "Mesma meta todos os dias", "Um valor fixo para a semana inteira."),
    Triple("weekdayWeekend", "Metas separadas (útil e fim de semana)", "Sábado e domingo com limites diferentes."),
    Triple("seven", "Personalizado por dia", "Cada dia da semana com sua própria meta."),
)

/** A compact kcal field with its day caption above (no gold: the O1 split and per-day modes). */
@Composable
private fun CaptionedKcal(
    caption: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier,
    fieldModifier: Modifier,
    enabled: Boolean,
    imeAction: ImeAction,
    keyboardActions: KeyboardActions,
) {
    Column(modifier.aeroLayerAlpha(if (enabled) 1f else DISABLED_ALPHA), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AeroText(AeroTextTokens.labelSection.cased(caption), style = Aero.type.labelSection.copy(color = Aero.colors.textDim))
        AeroNumberField(value, onChange, "kcal", fieldModifier = fieldModifier, compact = true, placeholder = "—", enabled = enabled, imeAction = imeAction, keyboardActions = keyboardActions)
    }
}

// ---------------------------------------------------------------- O2

@Composable
fun EatScreen(
    ui: OnboardingUiState,
    onEat: (String) -> Unit,
    onPct: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    AeroOnboardingFrame(
        bar = AeroOnboardingBar.Intake(2),
        cta = "Continuar",
        ctaEnabled = ui.eat != "partial" || (ui.pct.toIntOrNull() ?: 0) > 0,
        onCta = onContinue,
        onBack = onBack,
        ctaTag = "o2-continue",
        bubbles = bubbles(430, 690),
    ) {
        Intro("Onboarding 2/4", "Exercícios", "Compensação de treinos", "Escolha se o gasto calórico de exercícios registrados deve aumentar sua meta do dia.")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AeroOptionCard(
                "0% (Não compensar)",
                "O gasto do treino não altera sua meta diária de calorias.",
                selected = ui.eat == "zero",
                onClick = { onEat("zero") },
                badge = "Padrão",
                modifier = Modifier.testTag("o2-zero"),
            )
            AeroOptionCard(
                "Porcentagem personalizada",
                "Defina um percentual para somar à meta (ex.: 50%).",
                selected = ui.eat == "partial",
                onClick = { onEat("partial") },
                modifier = Modifier.testTag("o2-partial"),
                extra = if (ui.eat == "partial") {
                    {
                        AeroNumberField(
                            ui.pct, onPct, "% do treino", Modifier.padding(top = 4.dp).fillMaxWidth(),
                            fieldModifier = Modifier.testTag("o2-pct"), compact = true,
                        )
                    }
                } else {
                    null
                },
            )
            AeroOptionCard(
                "100% (Compensação total)",
                "Adiciona todas as calorias gastas no treino à sua meta.",
                selected = ui.eat == "full",
                onClick = { onEat("full") },
                modifier = Modifier.testTag("o2-full"),
            )
        }
        AeroNoteCard(AeroIconName.Info, "Você poderá registrar ou ajustar os treinos a qualquer momento nas configurações.")
    }
}

// ---------------------------------------------------------------- O3

/**
 * O3 meal distribution on Aero. The Config meal editor keeps [SlotsScreen] until its own plan (A44).
 */
@Composable
fun OnboardingSlotsScreen(
    ui: OnboardingUiState,
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
) {
    BackHandler(onBack = onBack)
    var picking by remember { mutableIntStateOf(initialPicking) }
    val schedule = ui.slotSchedule
    Box(Modifier.fillMaxSize()) {
        AeroOnboardingFrame(
            bar = AeroOnboardingBar.Intake(3),
            cta = "Continuar",
            ctaEnabled = ui.o3Valid,
            onCta = onContinue,
            onBack = onBack,
            ctaTag = "o3-continue",
            bubbles = if (schedule.mode == "same") bubbles(520, 820) else bubbles(560, 880),
            // o3t: the page behind the time dialog is blurred (Figma layer blur 8) under overlay/scrim.
            modifier = if (picking in ui.slots.indices) Modifier.blur(8.dp) else Modifier,
        ) {
            Intro("Onboarding 3/4", "Rotina", "Distribuição das refeições", "Organize sua rotina para planejar o dia e receber lembretes no horário certo.")
            Group("Dias da semana") {
                val modes = SlotModes.labels
                AeroTabs(
                    modes.map { (mode, label) -> AeroChoice(label, "o3-mode-$mode") },
                    selected = modes.indexOfFirst { it.first == schedule.mode },
                    onSelect = { onMode(modes[it].first) },
                )
            }
            if (schedule.mode != "same") {
                GroupStep(schedule)
                if (schedule.index > 0) CopyPill("Copiar de ${schedule.groups[schedule.index - 1].label}", onCopy)
            }
            Group("Quantidade de refeições") {
                val counts = (SlotSuggestions.MIN_SLOTS..SlotSuggestions.MAX_SLOTS).toList()
                AeroSegmented(
                    counts.map { AeroChoice(it.toString(), "o3-count-$it") },
                    selected = counts.indexOf(ui.slots.size),
                    onSelect = { onCount(counts[it]) },
                    modifier = Modifier.testTag("o3-count"),
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
    }
    SlotModeConfirmation(schedule, onConfirmMode, onCancelMode)
}

/** o3s: group name, "Etapa n de m" and one segment per group. */
@Composable
private fun GroupStep(schedule: SlotScheduleDraft) {
    val c = Aero.colors
    val type = Aero.type
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AeroText(schedule.group.label, Modifier.weight(1f).testTag("o3-group"), style = type.title.copy(color = c.textPrimary))
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
private fun CopyPill(label: String, onCopy: () -> Unit) {
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
            .testTag("o3-copy"),
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

// ---------------------------------------------------------------- O4

@Composable
fun MacrosScreen(
    ui: OnboardingUiState,
    onProtein: (String) -> Unit,
    onCarb: (String) -> Unit,
    onFat: (String) -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    val c = Aero.colors
    var help by remember { mutableStateOf(false) }
    val haptic = rememberHaptic()
    val grams = listOf(ui.proteinField, ui.carbField, ui.fatField).map { it.toIntOrNull() ?: 0 }
    val kcal = listOf(grams[0] * 4, grams[1] * 4, grams[2] * 9)
    val total = kcal.sum()
    val pct = kcal.map { if (total > 0) (it * 100.0 / total).roundToInt() else 0 }
    val focus = remember { List(3) { FocusRequester() } }
    AeroOnboardingFrame(
        bar = AeroOnboardingBar.Brand(4, onHelp = { help = true }),
        cta = "Concluir e começar",
        ctaEnabled = ui.o4Valid,
        onCta = onFinish,
        onBack = onBack,
        ctaTag = "o4-finish",
        bubbles = bubbles(470, 760),
    ) {
        Intro(
            "Onboarding 4/4",
            "Macronutrientes",
            "Alvos de macronutrientes",
            "Distribuição calculada para a sua meta diária. Você pode ajustar as quantidades.",
            leadingDot = true,
        )
        SplitCard(pct)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val rows = listOf(
                Triple("Proteína", "4 kcal/g • ${pct[0]}%", AeroMacro.Protein),
                Triple("Carboidrato", "4 kcal/g • ${pct[1]}%", AeroMacro.Carbs),
                Triple("Gordura", "9 kcal/g • ${pct[2]}%", AeroMacro.Fat),
            )
            val values = listOf(ui.proteinField, ui.carbField, ui.fatField)
            val changes = listOf(onProtein, onCarb, onFat)
            val tags = listOf("o4-protein", "o4-carb", "o4-fat")
            rows.forEachIndexed { i, (name, detail, macro) ->
                AeroMacroTargetCard(
                    name, detail, macro, values[i], changes[i],
                    onAdjust = { runCatching { focus[i].requestFocus() } },
                    modifier = Modifier.testTag(tags[i]),
                    fieldModifier = Modifier.focusRequester(focus[i]).testTag("${tags[i]}-field"),
                )
            }
        }
        AeroNoteCard(
            AeroIconName.Info,
            "Proporção balanceada: 30% Proteína · 40% Carboidratos · 30% Gorduras.",
            footer = "${ui.day1Ceiling} kcal total estimada",
            footerTag = "o4-total",
        )
    }
    if (help) {
        AlertDialog(
            onDismissRequest = { help = false },
            containerColor = c.bgPage,
            confirmButton = { TextButton(onClick = { haptic(Haptic.Light); help = false }) { Text("OK", color = c.accentDefault) } },
            text = {
                Text(
                    "P e C: 4 kcal/g. G: 9 kcal/g. Sugestão 30/40/30 sobre o teto do dia 1. Estimativa, não consulta.",
                    style = Aero.type.body,
                    color = c.textMuted,
                )
            },
        )
    }
}

/** O4 split: legend (dot + "30% Proteína" in the macro colour) and the three-part bar, 4 dp apart. */
@Composable
private fun SplitCard(pct: List<Int>) {
    val c = Aero.colors
    val colors = listOf(c.macroProtein, c.macroCarbs, c.macroFat)
    val labels = listOf("Proteína", "Carbos", "Gorduras")
    Column(
        Modifier
            .fillMaxWidth()
            .aeroGlass(Aero.shapes.card)
            .padding(horizontal = 17.dp, vertical = 15.dp)
            .testTag("o4-split"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEachIndexed { i, label ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(colors[i], CircleShape))
                    AeroText("${pct[i]}% $label", style = Aero.type.captionStrong.copy(color = colors[i]))
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            pct.forEachIndexed { i, v ->
                if (v > 0) Box(Modifier.weight(v.toFloat()).fillMaxHeight().clip(CircleShape).background(colors[i]))
            }
        }
    }
}
