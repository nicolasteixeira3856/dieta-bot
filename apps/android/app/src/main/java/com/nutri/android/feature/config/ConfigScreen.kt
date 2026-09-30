package com.nutri.android.feature.config

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nutri.android.core.designsystem.Haptic
import com.nutri.android.core.designsystem.LocalPalette
import com.nutri.android.core.designsystem.DietaBotMeasure
import com.nutri.android.core.designsystem.DietaBotType
import com.nutri.android.core.designsystem.Palette
import com.nutri.android.core.designsystem.SheetActions
import com.nutri.android.core.designsystem.dietaClick
import com.nutri.android.feature.onboarding.SlotsScreen
import com.nutri.android.feature.onboarding.OnboardingUiState
import com.nutri.android.domain.SlotModes
import com.nutri.android.feature.onboarding.EatCard
import com.nutri.android.feature.onboarding.KcalField
import com.nutri.android.feature.onboarding.MacroCard
import com.nutri.android.feature.onboarding.ModeGroup
import com.nutri.android.feature.onboarding.PctField
import com.nutri.android.feature.workout.WorkoutField

private val CardShape = RoundedCornerShape(16.dp)

/** Stitch cfg: secondary copy is text at ~78% in dark (#c0c7d0), the muted token in light. */
private fun Palette.secondary(): Color = if (isDark) text.copy(alpha = 0.78f) else muted

/** Config (ADR-012 cfg + wipe). Rows open edit sheets; a new ceiling asks before wiping today. */
@Composable
fun ConfigScreen(ui: ConfigUiState, actions: ConfigActions) {
    val p = LocalPalette.current
    Box(
        Modifier
            .fillMaxSize()
            .background(p.phone)
            .drawBehind {
                // Stitch: warm glow behind the header.
                val r = 260.dp.toPx()
                drawCircle(Brush.radialGradient(listOf(p.gold.copy(alpha = if (p.isDark) 0.08f else 0.10f), Color.Transparent), Offset(size.width / 2f, 0f), r), r, Offset(size.width / 2f, 0f))
            }
            .testTag("cfg"),
    ) {
        val overlay = ui.editor != null || ui.wipeConfirm
        // Stitch wipe: backdrop-blur behind the dialog.
        Column(Modifier.fillMaxSize().then(if (overlay) Modifier.blur(8.dp) else Modifier).statusBarsPadding()) {
            Header(actions.onBack)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(start = 25.dp, end = 25.dp, bottom = 32.dp),
            ) {
                SectionLabel("Metas e limites", top = 17.dp)
                Group {
                    SettingRow("Meta de calorias", ui.ceilingValue, "cfg-ceiling", detail = ui.ceilingDetail, accent = true) { actions.onOpen(ConfigEditor.CEILING) }
                    Divider()
                    SettingRow("Compensação de treinos", ui.eatBackValue, "cfg-eat") { actions.onOpen(ConfigEditor.EAT_BACK) }
                    Divider()
                    SettingRow("Macronutrientes (P · C · G)", ui.macrosValue, "cfg-macros") { actions.onOpen(ConfigEditor.MACROS) }
                }
                if (ui.slotMode == "same") SectionLabel("Horários das refeições")
                else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    SectionLabel("Horários das refeições")
                    Text(SlotModes.labels.first { it.first == ui.slotMode }.second, style = DietaBotType.labelMd.copy(fontSize = 12.sp), color = p.dim, modifier = Modifier.padding(top = 26.dp, bottom = 11.dp))
                }
                Group {
                    if (ui.slotMode != "same") ui.slotGroups.forEachIndexed { i, group ->
                        if (i > 0) Divider()
                        SettingRow(group.name, "", "cfg-group-$i", detail = group.time) { actions.onOpenSlotGroup(i) }
                    }
                    else ui.slots.forEachIndexed { i, slot ->
                        if (i > 0) Divider()
                        SettingRow(slot.name, slot.time, "cfg-slot-$i") { actions.onOpen(ConfigEditor.SLOTS) }
                    }
                }
                SectionLabel("Treino de hoje")
                Group {
                    SettingRow(
                        "Gasto calórico do treino",
                        ui.workoutValue,
                        "cfg-workout",
                        detail = "Crédito atual: ${ui.creditKcal} kcal",
                    ) { actions.onOpen(ConfigEditor.WORKOUT) }
                }
                WipeNote()
            }
        }
        if (ui.editor == ConfigEditor.SLOTS) {
            val d = ui.draft
            // Same O3 editor, Config header and final save; no new destination.
            SlotsScreen(
                ui = OnboardingUiState(slots = d.slots, slotSchedule = d.slotSchedule),
                onCount = actions.onSlotCount, onName = actions.onSlotName, onTime = actions.onSlotTime,
                onBack = actions.onPreviousSlots, onContinue = actions.onSave,
                onMode = actions.onSlotMode, onCopy = actions.onCopySlots,
                onConfirmMode = actions.onConfirmSlotMode, onCancelMode = actions.onCancelSlotMode,
                configHeader = { Header(actions.onPreviousSlots) },
                ctaLabel = if (d.slotSchedule.last) "Salvar" else "Continuar",
                ctaTag = "cfg-save",
                tag = "cfg",
            )
        } else ui.editor?.let { EditSheet(it, ui, actions) }
        if (ui.wipeConfirm) WipeDialog(actions.onConfirmWipe, actions.onCancelWipe)
    }
}

// ----------------------------------------------------------------------------- list

@Composable
private fun Header(onBack: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().padding(start = 25.dp, end = 25.dp, top = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(37.dp)
                .clip(CircleShape)
                .background(p.surf2)
                .border(1.dp, p.line, CircleShape)
                .dietaClick(onClick = onBack)
                .testTag("cfg-back"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Voltar", tint = p.text, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(17.dp))
        Text(
            "Configurações",
            style = DietaBotType.headlineLg.copy(fontSize = 24.5.sp, lineHeight = 32.sp, letterSpacing = (-0.02).em),
            color = p.text,
        )
    }
}

@Composable
private fun SectionLabel(text: String, top: Dp = 26.dp) {
    val p = LocalPalette.current
    Text(
        text.uppercase(),
        style = DietaBotType.labelCaps.copy(fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.W600, letterSpacing = 0.04.em),
        color = p.secondary(),
        modifier = Modifier.padding(top = top, bottom = 11.dp),
    )
}

@Composable
private fun Group(content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(if (p.isDark) p.surf2 else p.panel)
            .border(1.dp, p.line, CardShape),
        content = content,
    )
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(LocalPalette.current.line))
}

@Composable
private fun SettingRow(
    title: String,
    value: String,
    tag: String,
    detail: String? = null,
    accent: Boolean = false,
    onClick: () -> Unit,
) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .dietaClick(onClick = onClick)
            .padding(start = 17.dp, end = 15.dp, top = 16.dp, bottom = 16.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = DietaBotType.bodyLg.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.W500), color = p.text)
            if (detail != null) {
                Text(detail, style = DietaBotType.labelMd.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.W400, letterSpacing = 0.06.em), color = p.secondary())
            }
        }
        Text(
            value,
            style = DietaBotType.bodyLg.copy(fontSize = 14.sp, lineHeight = 22.sp, fontWeight = if (accent) FontWeight.W600 else FontWeight.W400, letterSpacing = 0.sp),
            color = if (accent) p.gold else p.secondary(),
            modifier = Modifier.testTag("$tag-value"),
        )
        Spacer(Modifier.width(6.dp))
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = p.secondary(), modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun WipeNote() {
    val p = LocalPalette.current
    Row(
        Modifier
            .padding(top = 25.dp)
            .fillMaxWidth()
            .clip(CardShape)
            .background(if (p.isDark) p.surf else p.surf2)
            .border(1.dp, p.line, CardShape)
            .padding(start = 17.dp, end = 17.dp, top = 16.dp, bottom = 16.dp),
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = p.secondary(), modifier = Modifier.padding(top = 2.dp).size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "Alterar a meta de calorias reinicia os registros do dia atual. O histórico da conversa será mantido.",
            style = DietaBotType.bodyMd.copy(fontSize = 11.5.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
            color = p.secondary(),
        )
    }
}

// ----------------------------------------------------------------------------- sheets

@Composable
private fun Scrim(color: Color, onDismiss: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(color)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
    )
}

@Composable
private fun BoxScope.EditSheet(editor: ConfigEditor, ui: ConfigUiState, a: ConfigActions) {
    val p = LocalPalette.current
    BackHandler(onBack = a.onClose)
    Scrim(Color.Black.copy(alpha = if (p.isDark) 0.6f else 0.35f), a.onClose)
    val shape = RoundedCornerShape(topStart = DietaBotMeasure.sheetTopDp.dp, topEnd = DietaBotMeasure.sheetTopDp.dp)
    val (title, subtitle) = when (editor) {
        ConfigEditor.CEILING -> "Meta de calorias" to "Salvar um novo valor reinicia os registros de hoje."
        ConfigEditor.EAT_BACK -> "Compensação de treinos" to "Quanto do treino de hoje volta para a meta."
        ConfigEditor.MACROS -> "Macronutrientes" to "Alvos diários em gramas."
        ConfigEditor.SLOTS -> "Horários das refeições" to "Mudar nome ou horário não apaga o que você já registrou hoje."
        ConfigEditor.WORKOUT -> "Treino de hoje" to null
    }
    Column(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.9f)
            .clip(shape)
            .background(if (p.isDark) p.panel else p.phone)
            .border(1.dp, p.line.copy(alpha = 0.7f), shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .imePadding()
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets(bottom = 24.dp)))
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp)
            .testTag("cfg-sheet"),
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).width(44.dp).height(5.dp).clip(CircleShape).background(p.dim))
        Text(title, style = DietaBotType.headlineMd.copy(fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.W700, letterSpacing = 0.sp), color = p.text, modifier = Modifier.padding(top = 16.dp))
        if (subtitle != null) {
            Text(subtitle, style = DietaBotType.bodyMd.copy(fontSize = 13.sp, letterSpacing = 0.sp), color = p.muted, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
        } else {
            Spacer(Modifier.height(16.dp))
        }
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            when (editor) {
                ConfigEditor.CEILING -> CeilingEditor(ui.draft, a)
                ConfigEditor.EAT_BACK -> EatBackEditor(ui.draft, a)
                ConfigEditor.MACROS -> MacrosEditor(ui.draft, a)
                ConfigEditor.SLOTS -> Unit // Full-screen editor is rendered by ConfigScreen.
                // A22: same editor as the Home sheet (homeW).
                ConfigEditor.WORKOUT -> WorkoutField(ui.draft.workoutEditor, a.onWorkout, tag = "cfg-workout", autoFocus = true)
            }
        }
        SheetActions(
            primary = "Salvar",
            onPrimary = a.onSave,
            secondary = "Cancelar",
            onSecondary = a.onClose,
            primaryEnabled = ui.canSave,
            primaryTag = "cfg-save",
            secondaryTag = "cfg-cancel",
            // A20: >= 24 dp between the last scrolled item and the actions.
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}

@Composable
private fun CeilingEditor(d: ConfigDraft, a: ConfigActions) {
    ModeGroup(d.ceilingMode, a.onCeilingMode, tag = "cfg")
    Spacer(Modifier.height(16.dp))
    when (d.ceilingMode) {
        "weekdayWeekend" -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KcalField(d.weekdayField, a.onWeekday, Modifier.weight(1f).testTag("cfg-weekday"), caption = "Dias úteis", compact = true)
            KcalField(d.weekendField, a.onWeekend, Modifier.weight(1f).testTag("cfg-weekend"), caption = "Fim de semana", compact = true)
        }
        "seven" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom").chunked(2).forEachIndexed { row, pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEachIndexed { col, label ->
                        val i = row * 2 + col
                        KcalField(d.dayFields[i], { a.onDay(i, it) }, Modifier.weight(1f).testTag("cfg-day-$i"), caption = label, compact = true)
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        else -> KcalField(d.sameField, a.onSame, Modifier.fillMaxWidth().testTag("cfg-same"))
    }
}

@Composable
private fun EatBackEditor(d: ConfigDraft, a: ConfigActions) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EatCard(d.eat == "zero", "0% (Não compensar)", "O gasto do treino não altera a meta do dia.", "cfg-eat-zero", { a.onEat("zero") })
        EatCard(d.eat == "partial", "Porcentagem personalizada", "Percentual do treino somado à meta.", "cfg-eat-partial", { a.onEat("partial") }) {
            if (d.eat == "partial") PctField(d.pct, a.onPct, tag = "cfg-pct")
        }
        EatCard(d.eat == "full", "100% (Compensação total)", "Todo o gasto do treino volta para a meta.", "cfg-eat-full", { a.onEat("full") })
    }
}

@Composable
private fun MacrosEditor(d: ConfigDraft, a: ConfigActions) {
    val p = LocalPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MacroCard("Proteína", "4 kcal/g", d.proteinField, p.protein, a.onProtein, "cfg-protein")
        MacroCard("Carboidrato", "4 kcal/g", d.carbField, p.carbs, a.onCarb, "cfg-carb")
        MacroCard("Gordura", "9 kcal/g", d.fatField, p.fat, a.onFat, "cfg-fat")
    }
}

// ----------------------------------------------------------------------------- wipe

@Composable
private fun BoxScope.WipeDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    val p = LocalPalette.current
    BackHandler(onBack = onCancel)
    // Stitch wipe: near-black/80 dark, text colour/40 light (same scrim as chatP).
    Scrim(if (p.isDark) Color(0xFF07090D).copy(alpha = 0.8f) else p.text.copy(alpha = 0.4f), onCancel)
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .align(Alignment.Center)
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(if (p.isDark) p.surf2 else p.surf)
            .border(1.dp, p.line, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(24.dp)
            .testTag("cfg-wipe"),
    ) {
        // Stitch: rounded square with a ring in dark, plain circle in light.
        val badge = if (p.isDark) RoundedCornerShape(16.dp) else CircleShape
        Box(
            Modifier
                .size(if (p.isDark) 44.dp else 48.dp)
                .clip(badge)
                .background(p.bad.copy(alpha = if (p.isDark) 0.16f else 0.12f))
                .then(if (p.isDark) Modifier.border(1.dp, p.bad.copy(alpha = 0.3f), badge) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.RestartAlt, contentDescription = null, tint = p.bad, modifier = Modifier.size(22.dp))
        }
        Text(
            "Reiniciar registros de hoje?",
            style = DietaBotType.headlineMd.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.W700, letterSpacing = (-0.03).em),
            color = p.text,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            "Ao atualizar sua meta calórica, as refeições de hoje serão reiniciadas para o novo cálculo de saldo. O histórico da conversa e os dias anteriores serão preservados.",
            style = DietaBotType.bodyLg.copy(fontSize = 13.7.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
            color = p.muted,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            Modifier
                .padding(top = 26.dp)
                .fillMaxWidth()
                .height(52.dp)
                .shadow(12.dp, CircleShape, ambientColor = p.bad, spotColor = p.bad)
                .clip(CircleShape)
                .background(p.bad)
                .dietaClick(Haptic.Confirm, onClick = onConfirm)
                .testTag("cfg-wipe-confirm"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val on = if (p.isDark) p.ctaText else p.surf
            Icon(Icons.Outlined.RestartAlt, contentDescription = null, tint = on, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Confirmar e reiniciar dia", style = DietaBotType.labelLg.copy(fontSize = 15.sp, fontWeight = FontWeight.W700, letterSpacing = 0.sp), color = on)
        }
        Box(
            Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .height(52.dp)
                .clip(CircleShape)
                .background(if (p.isDark) p.surf2 else p.panel)
                .then(if (p.isDark) Modifier.border(1.dp, p.line, CircleShape) else Modifier)
                .dietaClick(onClick = onCancel)
                .testTag("cfg-wipe-cancel"),
            contentAlignment = Alignment.Center,
        ) {
            Text("Cancelar", style = DietaBotType.labelLg.copy(fontSize = 15.sp, fontWeight = FontWeight.W700, letterSpacing = 0.sp), color = p.text)
        }
    }
}
