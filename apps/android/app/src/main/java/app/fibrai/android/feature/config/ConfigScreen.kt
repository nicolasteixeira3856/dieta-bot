package app.fibrai.android.feature.config

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import app.fibrai.android.core.designsystem.dietaClick
import app.fibrai.android.core.designsystem.aero.Aero
import app.fibrai.android.core.designsystem.aero.AeroBubbleSpec
import app.fibrai.android.core.designsystem.aero.AeroConfirmDialog
import app.fibrai.android.core.designsystem.aero.AeroFieldNumber
import app.fibrai.android.core.designsystem.aero.AeroIcon
import app.fibrai.android.core.designsystem.aero.AeroIconButton
import app.fibrai.android.core.designsystem.aero.AeroIconName
import app.fibrai.android.core.designsystem.aero.AeroMacro
import app.fibrai.android.core.designsystem.aero.AeroMacroTargetCard
import app.fibrai.android.core.designsystem.aero.AeroNoteCard
import app.fibrai.android.core.designsystem.aero.AeroNumberField
import app.fibrai.android.core.designsystem.aero.AeroOptionCard
import app.fibrai.android.core.designsystem.aero.AeroPage
import app.fibrai.android.core.designsystem.aero.AeroPageBubbles
import app.fibrai.android.core.designsystem.aero.AeroScrim
import app.fibrai.android.core.designsystem.aero.AeroSheet
import app.fibrai.android.feature.onboarding.ToneOptions
import app.fibrai.android.core.designsystem.aero.AeroText
import app.fibrai.android.core.designsystem.aero.AeroTextTokens
import app.fibrai.android.core.designsystem.aero.aeroGlass
import app.fibrai.android.core.designsystem.aero.cased
import app.fibrai.android.domain.SlotModes
import app.fibrai.android.feature.onboarding.AeroOnboardingBar
import app.fibrai.android.feature.onboarding.OnboardingSlotsScreen
import app.fibrai.android.feature.onboarding.OnboardingUiState
import app.fibrai.android.feature.workout.WorkoutEditorState

/*
 * Config on Aero (A44): Figma `Design`, Release 1, section "Config e push · D7". Frame: 20 dp margins, 24 dp from
 * the top and between blocks, 32 dp under the note; each block is a Label/Section and a glass group 12 dp below.
 */

/** The edge bubbles of the cfg frames. */
private val Bubbles = listOf(
    AeroBubbleSpec((-50).dp, (-60).dp, 80.dp),
    AeroBubbleSpec(372.dp, 520.dp, 46.dp),
    AeroBubbleSpec((-22).dp, 820.dp, 40.dp),
)

/**
 * Config (ADR-012 cfg + wipe). Rows open edit sheets; a new ceiling asks before wiping today.
 * [extra] goes after the wipe note: flavor rows (A23 dev tools, ADR-019), empty in prod.
 */
@Composable
fun ConfigScreen(ui: ConfigUiState, actions: ConfigActions, extra: @Composable ColumnScope.() -> Unit = {}) {
    val scroll = rememberScrollState()
    val overlay = ui.editor != null || ui.wipeConfirm
    Box(Modifier.fillMaxSize().testTag("cfg")) {
        // wipe: the page behind a dialog or sheet is blurred (Figma layer blur 8) under overlay/scrim.
        AeroPage(Modifier.fillMaxSize().then(if (overlay) Modifier.blur(8.dp) else Modifier), scroll) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(scroll)
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                ConfigHeader(actions.onBack)
                Block("Metas e limites") {
                    Group {
                        SettingRow("Meta de calorias", ui.ceilingValue, "cfg-ceiling", detail = ui.ceilingDetail, accent = true) { actions.onOpen(ConfigEditor.CEILING) }
                        Divider()
                        SettingRow("Compensação de treinos", ui.eatBackValue, "cfg-eat") { actions.onOpen(ConfigEditor.EAT_BACK) }
                        Divider()
                        SettingRow("Macronutrientes (P · C · G)", ui.macrosValue, "cfg-macros") { actions.onOpen(ConfigEditor.MACROS) }
                        Divider()
                        SettingRow("Tom da Tali", ui.toneValue, "cfg-tone") { actions.onOpen(ConfigEditor.TONE) }
                    }
                }
                Block("Horários das refeições", trailing = SlotModes.labels.first { it.first == ui.slotMode }.second.takeIf { ui.slotMode != "same" }) {
                    Group {
                        if (ui.slotMode != "same") {
                            ui.slotGroups.forEachIndexed { i, group ->
                                if (i > 0) Divider()
                                SettingRow(group.name, "", "cfg-group-$i", detail = group.time) { actions.onOpenSlotGroup(i) }
                            }
                        } else {
                            ui.slots.forEachIndexed { i, slot ->
                                if (i > 0) Divider()
                                SettingRow(slot.name, slot.time, "cfg-slot-$i") { actions.onOpen(ConfigEditor.SLOTS) }
                            }
                        }
                    }
                }
                Block("Treino de hoje") {
                    Group {
                        SettingRow(
                            "Gasto calórico do treino",
                            ui.workoutValue,
                            "cfg-workout",
                            detail = "Crédito atual: ${ui.creditKcal} kcal",
                        ) { actions.onOpen(ConfigEditor.WORKOUT) }
                    }
                }
                AeroNoteCard(AeroIconName.Info, "Alterar a meta de calorias reinicia os registros do dia atual. O histórico da conversa será mantido.")
                Block("Dados") {
                    Group {
                        SettingRow("Resetar app", "", "cfg-reset", detail = "Apaga tudo e refaz o onboarding") { actions.onOpenReset() }
                    }
                }
                extra()
            }
            AeroPageBubbles(Bubbles, scroll, Modifier.statusBarsPadding())
        }
        if (ui.editor == ConfigEditor.SLOTS) {
            val d = ui.draft
            // The O3 editor with Header/Page and the final save; no new destination.
            OnboardingSlotsScreen(
                ui = OnboardingUiState(slots = d.slots, slotSchedule = d.slotSchedule),
                onCount = actions.onSlotCount, onName = actions.onSlotName, onTime = actions.onSlotTime,
                onBack = actions.onPreviousSlots, onContinue = actions.onSave,
                onMode = actions.onSlotMode, onCopy = actions.onCopySlots,
                onConfirmMode = actions.onConfirmSlotMode, onCancelMode = actions.onCancelSlotMode,
                bar = AeroOnboardingBar.Header { ConfigHeader(actions.onPreviousSlots) },
                cta = if (d.slotSchedule.last) "Salvar" else "Continuar",
                ctaTag = "cfg-save",
                tag = "cfg",
            )
        } else {
            ui.editor?.let { EditSheet(it, ui, actions) }
        }
        if (ui.wipeConfirm) WipeDialog(actions.onConfirmWipe, actions.onCancelWipe)
        if (ui.resetConfirm) ResetDialog(actions.onConfirmReset, actions.onCancelReset)
    }
}

// ----------------------------------------------------------------------------- list

/** Header/Page: glass back button (caret-left) and the Title, 12 dp apart. */
@Composable
private fun ConfigHeader(onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(44.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        AeroIconButton(AeroIconName.CaretLeft, onBack, contentDescription = "Voltar", modifier = Modifier.testTag("cfg-back"))
        AeroText("Configurações", style = Aero.type.title.copy(color = Aero.colors.textPrimary))
    }
}

/** A block: Label/Section in text/muted (an optional Caption on the right), then the group 12 dp below. */
@Composable
private fun Block(label: String, trailing: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val c = Aero.colors
    val type = Aero.type
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 16 dp for the label alone, 18 with the Caption (cfgS).
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            AeroText(AeroTextTokens.labelSection.cased(label), style = type.labelSection.copy(color = c.textMuted))
            if (trailing != null) AeroText(trailing, style = type.caption.copy(color = c.textMuted), maxLines = 1)
        }
        content()
    }
}

/** Glass group card of Row/Setting, divided by border/line. Also hosts the dev rows (ADR-019). */
@Composable
internal fun Group(content: @Composable ColumnScope.() -> Unit) {
    // The rows sit inside the 1 dp border.
    Column(Modifier.fillMaxWidth().aeroGlass(Aero.shapes.card).padding(1.dp), content = content)
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Aero.colors.borderLine))
}

/**
 * Row/Setting: Body title with an optional Caption detail, the value (Body text/muted, or Body/Strong accent) and
 * the muted caret-right, 16 dp padding and 8 dp gaps; no fill, the row sits in the glass group.
 */
@Composable
internal fun SettingRow(
    title: String,
    value: String,
    tag: String,
    detail: String? = null,
    accent: Boolean = false,
    onClick: () -> Unit,
) {
    val c = Aero.colors
    val type = Aero.type
    Row(
        Modifier
            .fillMaxWidth()
            .dietaClick(onClick = onClick)
            .padding(16.dp)
            .testTag(tag),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AeroText(title, style = type.body.copy(color = c.textPrimary))
            if (detail != null) AeroText(detail, style = type.caption.copy(color = c.textMuted))
        }
        if (value.isNotEmpty()) {
            AeroText(
                value,
                Modifier.testTag("$tag-value"),
                style = if (accent) type.bodyStrong.copy(color = c.accentDefault) else type.body.copy(color = c.textMuted),
                maxLines = 1,
            )
        }
        AeroIcon(AeroIconName.CaretRight, c.iconMuted, size = 20.dp)
    }
}

// ----------------------------------------------------------------------------- sheets (no gold)

/** Sheet/Bottom over the blurred list: title, the subtitle and the editor (scrolled past half the screen), Salvar + Cancelar. */
@Composable
private fun BoxScope.EditSheet(editor: ConfigEditor, ui: ConfigUiState, a: ConfigActions) {
    BackHandler(onBack = a.onClose)
    AeroScrim(a.onClose)
    val (title, subtitle) = when (editor) {
        ConfigEditor.CEILING -> "Meta de calorias" to "Salvar um novo valor reinicia os registros de hoje."
        ConfigEditor.EAT_BACK -> "Compensação de treinos" to "Quanto do treino de hoje volta para a meta."
        ConfigEditor.MACROS -> "Macronutrientes" to "Alvos diários em gramas."
        ConfigEditor.SLOTS -> "Horários das refeições" to "Mudar nome ou horário não apaga o que você já registrou hoje."
        ConfigEditor.WORKOUT -> "Treino de hoje" to null
        ConfigEditor.TONE -> "Tom da Tali" to null
    }
    AeroSheet(
        title = title,
        primary = "Salvar",
        onPrimary = a.onSave,
        secondary = "Cancelar",
        onSecondary = a.onClose,
        primaryEnabled = ui.canSave,
        primaryTag = "cfg-save",
        secondaryTag = "cfg-cancel",
        bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        modifier = Modifier.align(Alignment.BottomCenter).statusBarsPadding().imePadding().testTag("cfg-sheet"),
    ) {
        Column(
            Modifier
                .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.5f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (subtitle != null) AeroText(subtitle, style = Aero.type.body.copy(color = Aero.colors.textMuted))
            when (editor) {
                ConfigEditor.CEILING -> CeilingEditor(ui.draft, a)
                ConfigEditor.EAT_BACK -> EatBackEditor(ui.draft, a)
                ConfigEditor.MACROS -> MacrosEditor(ui.draft, a)
                ConfigEditor.SLOTS -> Unit // Full-screen editor is rendered by ConfigScreen.
                // A22: the field of the Home sheet (homeW).
                ConfigEditor.WORKOUT -> WorkoutEditor(ui.draft.workoutEditor, a.onWorkout)
                // A60 part B (cfgT): the two options of O5; Salvar stores, the next turn uses it.
                ConfigEditor.TONE -> ToneOptions(ui.draft.tone, a.onTone, "cfg-tone", onGlass = true)
            }
        }
    }
}

private val CeilingModes = listOf(
    Triple("same", "Mesma meta todos os dias", "Um valor fixo para a semana inteira."),
    Triple("weekdayWeekend", "Metas separadas (útil e fim de semana)", "Sábado e domingo com limites diferentes."),
    Triple("seven", "Personalizado por dia", "Cada dia da semana com sua própria meta."),
)

@Composable
private fun CeilingEditor(d: ConfigDraft, a: ConfigActions) {
    val focus = LocalFocusManager.current
    val done = KeyboardActions(onDone = { focus.clearFocus() })
    Column(Modifier.testTag("cfg-modes"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CeilingModes.forEach { (value, title, body) ->
            AeroOptionCard(title, body, selected = d.ceilingMode == value, onClick = { a.onCeilingMode(value) }, onGlass = true, modifier = Modifier.testTag("cfg-mode-$value"))
        }
    }
    when (d.ceilingMode) {
        "weekdayWeekend" -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CaptionedKcal("Dias úteis", d.weekdayField, a.onWeekday, "cfg-weekday", Modifier.weight(1f))
            CaptionedKcal("Fim de semana", d.weekendField, a.onWeekend, "cfg-weekend", Modifier.weight(1f))
        }
        "seven" -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom").chunked(2).forEachIndexed { row, pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEachIndexed { col, label ->
                        val i = row * 2 + col
                        CaptionedKcal(label, d.dayFields[i], { a.onDay(i, it) }, "cfg-day-$i", Modifier.weight(1f))
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        else -> AeroNumberField(
            d.sameField, a.onSame, "kcal", Modifier.fillMaxWidth(),
            fieldModifier = Modifier.testTag("cfg-same"),
            placeholder = "—", icon = AeroIconName.Lightning, imeAction = ImeAction.Done, keyboardActions = done, onGlass = true,
        )
    }
}

/** A compact kcal field with its day caption above, as O1. */
@Composable
private fun CaptionedKcal(caption: String, value: String, onChange: (String) -> Unit, tag: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AeroText(AeroTextTokens.labelSection.cased(caption), style = Aero.type.labelSection.copy(color = Aero.colors.textDim))
        AeroNumberField(value, onChange, "kcal", fieldModifier = Modifier.testTag(tag), compact = true, placeholder = "—", onGlass = true)
    }
}

@Composable
private fun EatBackEditor(d: ConfigDraft, a: ConfigActions) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AeroOptionCard("0% (Não compensar)", "O gasto do treino não altera a meta do dia.", selected = d.eat == "zero", onClick = { a.onEat("zero") }, onGlass = true, modifier = Modifier.testTag("cfg-eat-zero"))
        AeroOptionCard(
            "Porcentagem personalizada",
            "Percentual do treino somado à meta.",
            selected = d.eat == "partial",
            onClick = { a.onEat("partial") },
            onGlass = true,
            modifier = Modifier.testTag("cfg-eat-partial"),
            extra = if (d.eat == "partial") {
                { AeroNumberField(d.pct, a.onPct, "% do treino", Modifier.padding(top = 4.dp).fillMaxWidth(), fieldModifier = Modifier.testTag("cfg-pct"), compact = true, onGlass = true) }
            } else {
                null
            },
        )
        AeroOptionCard("100% (Compensação total)", "Todo o gasto do treino volta para a meta.", selected = d.eat == "full", onClick = { a.onEat("full") }, onGlass = true, modifier = Modifier.testTag("cfg-eat-full"))
    }
}

@Composable
private fun MacrosEditor(d: ConfigDraft, a: ConfigActions) {
    val focus = remember { List(3) { FocusRequester() } }
    val rows = listOf(
        Triple("Proteína", AeroMacro.Protein, "cfg-protein"),
        Triple("Carboidrato", AeroMacro.Carbs, "cfg-carb"),
        Triple("Gordura", AeroMacro.Fat, "cfg-fat"),
    )
    val values = listOf(d.proteinField, d.carbField, d.fatField)
    val changes = listOf(a.onProtein, a.onCarb, a.onFat)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEachIndexed { i, (name, macro, tag) ->
            AeroMacroTargetCard(
                name, if (macro == AeroMacro.Fat) "9 kcal/g" else "4 kcal/g", macro, values[i], changes[i],
                onAdjust = { runCatching { focus[i].requestFocus() } },
                onGlass = true,
                modifier = Modifier.testTag(tag),
                fieldModifier = Modifier.focusRequester(focus[i]).testTag("$tag-field"),
            )
        }
    }
}

/** Field/Number of the homeW sheet with the live credit line; focused on open, cursor at the end. */
@Composable
private fun WorkoutEditor(state: WorkoutEditorState, onChange: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    var text by remember { mutableStateOf(TextFieldValue(state.input, TextRange(state.input.length))) }
    if (text.text != state.input) text = TextFieldValue(state.input, TextRange(state.input.length))
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    AeroFieldNumber(
        value = text,
        onValueChange = { v ->
            val clean = WorkoutEditorState.clean(v.text)
            text = if (clean == v.text) v else TextFieldValue(clean, TextRange(clean.length))
            if (clean != state.input) onChange(clean)
        },
        unit = "kcal",
        helper = state.creditLine,
        icon = AeroIconName.Barbell,
        focusRequester = focus,
        fieldTag = "cfg-workout-field",
        helperTag = "cfg-workout-credit",
        onGlass = true,
    )
}

// ----------------------------------------------------------------------------- wipe

/** cfgR: Dialog/Confirm Tone=Danger over the blurred Config (ADR-040). */
@Composable
internal fun BoxScope.ResetDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    AeroConfirmDialog(
        title = "Resetar o app?",
        body = "Apaga perfil, metas, refeições, registros, conversa e memória deste aparelho. Depois você refaz o onboarding. Não dá para desfazer.",
        primary = "Apagar tudo",
        onPrimary = onConfirm,
        secondary = "Cancelar",
        onSecondary = onCancel,
        dangerIcon = AeroIconName.ArrowCounterClockwise,
        primaryTag = "cfg-reset-confirm",
        secondaryTag = "cfg-reset-cancel",
        modifier = Modifier.testTag("cfg-reset-dialog"),
    )
}

/** wipe: Dialog/Confirm Tone=Danger over the blurred Config. Also used by the dev memory tool (ADR-019). */
@Composable
internal fun BoxScope.WipeDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    AeroConfirmDialog(
        title = "Reiniciar registros de hoje?",
        body = "Ao atualizar sua meta calórica, as refeições de hoje serão reiniciadas para o novo cálculo de saldo. O histórico da conversa e os dias anteriores serão preservados.",
        primary = "Confirmar e reiniciar dia",
        onPrimary = onConfirm,
        secondary = "Cancelar",
        onSecondary = onCancel,
        dangerIcon = AeroIconName.ArrowCounterClockwise,
        primaryTag = "cfg-wipe-confirm",
        secondaryTag = "cfg-wipe-cancel",
        modifier = Modifier.testTag("cfg-wipe"),
    )
}
