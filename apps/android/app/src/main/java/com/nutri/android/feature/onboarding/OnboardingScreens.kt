package com.nutri.android.feature.onboarding

// Legacy (Material/Stitch look) controls still used by the Config screens until A44 moves them to Aero.
// The onboarding screens themselves live in OnboardingAeroScreens.kt.

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DinnerDining
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nutri.android.core.designsystem.Haptic
import com.nutri.android.core.designsystem.Inter
import com.nutri.android.core.designsystem.Jakarta
import com.nutri.android.core.designsystem.LocalPalette
import com.nutri.android.core.designsystem.DietaBotType
import com.nutri.android.core.designsystem.dietaClick
import com.nutri.android.core.designsystem.rememberHaptic
import com.nutri.android.core.designsystem.TimeWheelDialog
import com.nutri.android.domain.SlotBand
import com.nutri.android.domain.SlotSuggestions
import kotlin.math.roundToInt

@Composable
internal fun ModeGroup(mode: String, onMode: (String) -> Unit, tag: String = "o1", enabled: Boolean = true) {
    val p = LocalPalette.current
    val items = listOf(
        Triple("same", "Mesma meta todos os dias", "Um valor fixo para a semana inteira."),
        Triple("weekdayWeekend", "Metas separadas (útil e fim de semana)", "Sábado e domingo com limites diferentes."),
        Triple("seven", "Personalizado por dia", "Cada dia da semana com sua própria meta."),
    )
    Column(
        Modifier
            .fillMaxWidth()
            .disabledAlpha(enabled)
            .clip(RoundedCornerShape(16.dp))
            .background(p.card)
            .border(1.dp, p.line, RoundedCornerShape(16.dp))
            .testTag("$tag-modes"),
    ) {
        items.forEachIndexed { i, (value, title, body) ->
            val selected = mode == value
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(p.line))
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (selected) p.cardSel else p.card)
                    .then(if (enabled) Modifier.dietaClick { onMode(value) } else Modifier)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .testTag("$tag-mode-$value"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(title, style = DietaBotType.labelLg.copy(letterSpacing = (-0.025).em, fontWeight = if (selected) FontWeight.W600 else FontWeight.W500), color = p.text)
                    Text(body, style = DietaBotType.labelMd.copy(letterSpacing = 0.sp, fontWeight = FontWeight.W400), color = if (selected) p.muted else p.dim, modifier = Modifier.padding(top = 2.dp))
                }
                GoldRadio(selected, enabled)
            }
        }
    }
}

@Composable
internal fun KcalField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
    compact: Boolean = false,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val p = LocalPalette.current
    BasicTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        keyboardActions = keyboardActions,
        textStyle = TextStyle(
            fontFamily = Jakarta,
            fontSize = if (compact) 22.sp else 32.sp,
            fontWeight = FontWeight.W700,
            letterSpacing = (-0.025).em,
            color = p.text,
        ),
        cursorBrush = SolidColor(p.gold),
        modifier = modifier.disabledAlpha(enabled),
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(p.card)
                    .border(1.dp, p.line, RoundedCornerShape(16.dp))
                    .padding(start = 20.dp, end = if (compact) 14.dp else 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    if (caption != null) Text(caption.uppercase(), style = DietaBotType.labelCaps.copy(fontSize = 9.sp), color = p.dim)
                    Box {
                        if (value.isEmpty()) {
                            Text("—", style = TextStyle(fontFamily = Jakarta, fontSize = if (compact) 22.sp else 32.sp, fontWeight = FontWeight.W700), color = p.dim)
                        }
                        inner()
                    }
                }
                if (!compact) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (enabled) p.gold.copy(alpha = 0.10f) else Color.Transparent)
                            .border(1.dp, if (enabled) p.gold.copy(alpha = 0.20f) else p.line, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("kcal", style = DietaBotType.labelMd.copy(fontWeight = FontWeight.W600, letterSpacing = 0.sp), color = if (enabled) p.gold else p.dim)
                        Icon(Icons.Outlined.Bolt, contentDescription = null, tint = if (enabled) p.gold else p.dim, modifier = Modifier.size(14.dp))
                    }
                } else {
                    Text("kcal", style = DietaBotType.labelMd, color = p.muted)
                }
            }
        },
    )
}

@Composable
internal fun EatCard(
    selected: Boolean,
    title: String,
    body: String,
    tag: String,
    onClick: () -> Unit,
    badge: String? = null,
    extra: @Composable () -> Unit = {},
) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            // Stitch dark: raised surface + translucent gold ring. Light: same card + solid 2 dp gold border.
            .background(if (selected && p.isDark) p.cardSel else p.card)
            .border(
                if (selected) 2.dp else 1.dp,
                if (!selected) p.line else if (p.isDark) p.gold.copy(alpha = 0.6f) else p.gold,
                shape,
            )
            .dietaClick(onClick = onClick)
            .padding(16.dp)
            .testTag(tag),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.padding(top = 2.dp)) { GoldRadio(selected) }
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Stitch: flex row (with badge) = 20 line; plain inline span = 24 line box.
                Text(
                    title,
                    style = DietaBotType.headlineMd.copy(fontSize = 15.sp, lineHeight = if (badge != null) 20.sp else 24.sp, letterSpacing = (-0.025).em),
                    color = p.text,
                    modifier = Modifier.weight(1f),
                )
                if (badge != null) {
                    Text(
                        badge.uppercase(),
                        style = DietaBotType.labelCaps.copy(fontSize = 10.sp, fontWeight = FontWeight.W400, letterSpacing = 0.05.em),
                        color = p.gold,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(p.gold.copy(alpha = 0.15f))
                            .border(1.dp, p.gold.copy(alpha = 0.25f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Text(body, style = DietaBotType.bodyMd.copy(fontSize = 13.sp, lineHeight = 17.875.sp, letterSpacing = 0.sp), color = p.muted, modifier = Modifier.padding(top = 7.dp))
            extra()
        }
    }
}

@Composable
internal fun PctField(value: String, onChange: (String) -> Unit, tag: String = "o2-pct") {
    val p = LocalPalette.current
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = TextStyle(fontFamily = Jakarta, fontSize = 22.sp, fontWeight = FontWeight.W700, color = p.text),
        cursorBrush = SolidColor(p.gold),
        modifier = Modifier.padding(top = 12.dp).testTag(tag),
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(InnerShape)
                    .background(p.card)
                    .border(1.dp, p.line, InnerShape)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) { inner() }
                Text("% do treino", style = DietaBotType.labelMd, color = p.muted)
            }
        },
    )
}

// ---------------------------------------------------------------- Config (legacy look until A44)

@Composable
fun SlotsScreen(
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
    configHeader: (@Composable () -> Unit)? = null,
    ctaLabel: String = "Continuar",
    ctaTag: String = "o3-continue",
    tag: String = "o3",
) {
    androidx.activity.compose.BackHandler(onBack = onBack)
    var picking by remember { mutableIntStateOf(-1) }
    Box(Modifier.then(if (picking in ui.slots.indices) Modifier.blur(8.dp) else Modifier)) {
        OnboardingFrame(
            bar = OnboardingBar.IntakeSegments(filled = 3),
            cta = ctaLabel,
            ctaEnabled = ui.o3Valid,
            onCta = onContinue,
            onBack = onBack,
            ctaTag = ctaTag,
            contentTop = 12.dp,
            header = configHeader,
        ) {
            if (configHeader == null) Eyebrow("ONBOARDING 3/4", "ROTINA", sectionAccent = false)
            ScreenTitle("Distribuição das refeições", "Organize sua rotina para planejar o dia e receber lembretes no horário certo.", titleLine = 37.5f)
            Spacer(Modifier.height(24.dp))
            SlotScheduleControls(ui.slotSchedule, onMode, onCopy, tag)
            // A15 (Stitch gold): stepper 46 dp tall (36 dp pills, 5 dp inset), 10 dp under the label, 28 dp above the cards.
            SectionLabel("Quantidade de refeições", bottom = 10.dp)
            CountStepper(ui.slots.size, onCount, tag = tag, itemHeight = 36.dp, inset = 5.dp)
            Spacer(Modifier.height(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ui.slots.forEachIndexed { i, slot ->
                    SlotCard(
                        index = i,
                        slot = slot,
                        onName = { onName(i, it) },
                        onPickTime = { picking = i },
                        tag = tag,
                        pad = if (ui.slotSchedule.mode == "same") 15.dp else 14.dp, // ST4 group cards use the 14 dp inset.
                    )
                }
            }
            InfoNote(
                Icons.Outlined.NotificationsActive,
                "Você poderá ajustar intervalos, adicionar refeições intermediárias ou desativar alertas a qualquer momento.",
                Modifier.padding(top = 20.dp),
                infoLine = 16.5f,
                infoTracking = 0.05f,
            )
        }
    }
    SlotModeConfirmation(ui.slotSchedule, onConfirmMode, onCancelMode)
    if (picking in ui.slots.indices) {
        TimeWheelDialog(
            title = ui.slots[picking].name.ifBlank { "Refeição ${picking + 1}" },
            minutes = ui.slots[picking].minutes,
            onDismiss = { picking = -1 },
            onConfirm = { onTime(picking, it); picking = -1 },
        )
    }
}

@Composable
internal fun CountStepper(count: Int, onCount: (Int) -> Unit, tag: String = "o3", itemHeight: Dp = 40.dp, inset: Dp = 4.dp) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(p.card)
            .border(1.dp, p.line, RoundedCornerShape(16.dp))
            .padding(inset)
            .testTag("$tag-count"),
    ) {
        (SlotSuggestions.MIN_SLOTS..SlotSuggestions.MAX_SLOTS).forEach { n ->
            val selected = n == count
            Box(
                Modifier
                    .weight(1f)
                    .height(itemHeight)
                    .clip(InnerShape)
                    .background(if (selected) p.segSel else p.card)
                    .dietaClick { onCount(n) }
                    .testTag("$tag-count-$n"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    n.toString(),
                    style = DietaBotType.labelLg.copy(fontWeight = if (selected) FontWeight.W700 else FontWeight.W600),
                    color = if (selected) p.onSegSel else p.muted,
                )
            }
        }
    }
}

@Composable
internal fun SlotCard(index: Int, slot: SlotDraft, onName: (String) -> Unit, onPickTime: () -> Unit, tag: String = "o3", pad: Dp = 14.dp) {
    val p = LocalPalette.current
    val field = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(p.card)
            .border(1.dp, p.line, RoundedCornerShape(16.dp))
            .padding(pad)
            .testTag("$tag-slot-$index"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BasicTextField(
                value = slot.name,
                onValueChange = onName,
                singleLine = true,
                textStyle = DietaBotType.bodyMd.copy(color = p.text),
                cursorBrush = SolidColor(p.gold),
                modifier = Modifier.weight(1f).testTag("$tag-name-$index"),
                decorationBox = { inner ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(field)
                            .background(p.phone)
                            .border(1.dp, p.line, field)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(bandIcon(slot.minutes), contentDescription = null, tint = p.muted, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.weight(1f)) {
                            if (slot.name.isEmpty()) Text("Nome da refeição", style = DietaBotType.bodyMd, color = p.dim)
                            inner()
                        }
                    }
                },
            )
            Row(
                Modifier
                    .width(112.dp)
                    .height(48.dp)
                    .clip(field)
                    .background(p.phone)
                    .border(1.dp, p.line, field)
                    .dietaClick(onClick = onPickTime)
                    .padding(horizontal = 12.dp)
                    .testTag("$tag-time-$index"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(SlotSuggestions.format(slot.minutes), style = DietaBotType.bodyMd.copy(fontWeight = FontWeight.W500), color = p.text)
                Icon(Icons.Outlined.Schedule, contentDescription = "Horário", tint = p.gold, modifier = Modifier.size(18.dp))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Sugestões:", style = DietaBotType.labelCaps.copy(fontSize = 10.sp), color = p.dim, modifier = Modifier.padding(end = 4.dp))
            SlotSuggestions.namesFor(slot.minutes).forEachIndexed { j, name ->
                Text(
                    name,
                    style = DietaBotType.labelMd,
                    color = p.muted,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(p.surf2)
                        .border(1.dp, p.line, CircleShape)
                        .dietaClick { onName(name) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("$tag-chip-$index-$j"),
                )
            }
        }
    }
}

private fun bandIcon(minutes: Int): ImageVector = when (SlotSuggestions.bandOf(minutes)) {
    SlotBand.BREAKFAST -> Icons.Outlined.Restaurant
    SlotBand.MORNING_SNACK, SlotBand.AFTERNOON_SNACK -> Icons.Outlined.BakeryDining
    SlotBand.LUNCH -> Icons.Outlined.LunchDining
    SlotBand.DINNER -> Icons.Outlined.DinnerDining
    SlotBand.NIGHT -> Icons.Outlined.Bedtime
}

@Composable
internal fun MacroCard(
    name: String,
    detail: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    onChange: (String) -> Unit,
    tag: String,
) {
    val p = LocalPalette.current
    val focus = remember { FocusRequester() }
    Row(
        Modifier
            .fillMaxWidth()
            .height(74.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(p.card)
            .border(1.dp, p.line, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(InnerShape)
                .background(color.copy(alpha = 0.10f))
                .border(1.dp, color.copy(alpha = 0.25f), InnerShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(name, style = DietaBotType.labelLg, color = p.text)
            Text(detail, style = DietaBotType.labelMd, color = p.muted)
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = DietaBotType.headlineMd.copy(fontWeight = FontWeight.W700, color = p.text, textAlign = TextAlign.End),
            cursorBrush = SolidColor(p.gold),
            modifier = Modifier.width(72.dp).focusRequester(focus).testTag("$tag-field"),
        )
        Text("g", style = DietaBotType.bodyMd, color = p.muted, modifier = Modifier.padding(start = 6.dp, end = 8.dp))
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(p.surf2)
                .dietaClick { focus.requestFocus() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Tune, contentDescription = "Ajustar $name", tint = p.muted, modifier = Modifier.size(16.dp))
        }
    }
}
