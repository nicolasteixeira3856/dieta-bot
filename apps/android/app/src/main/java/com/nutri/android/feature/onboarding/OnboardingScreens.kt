package com.nutri.android.feature.onboarding

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import com.nutri.android.domain.SlotBand
import com.nutri.android.domain.SlotSuggestions
import kotlin.math.roundToInt

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
    val p = LocalPalette.current
    OnboardingFrame(
        bar = OnboardingBar.Continuous(0.25f),
        cta = "Continuar",
        ctaEnabled = ui.o1Valid,
        onCta = onContinue,
        onBack = null,
        ctaTag = "o1-continue",
    ) {
        Eyebrow("ONBOARDING 1/4", "METABOLISMO", sectionAccent = true)
        ScreenTitle("Teto do dia", "Defina sua meta diária de calorias. Você pode usar o valor sugerido ou personalizar.")
        Spacer(Modifier.height(25.dp))

        SexToggle(ui.sex, onSex)
        Spacer(Modifier.height(29.dp))

        SectionLabel("Idade, altura e peso")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            UnitField(ui.ageField, "anos", "idade", onAge, Modifier.weight(1f).testTag("o1-age"))
            UnitField(ui.heightField, "cm", "altura", onHeight, Modifier.weight(1f).testTag("o1-height"))
            UnitField(ui.weightField, "kg", "peso", onWeight, Modifier.weight(1f).testTag("o1-weight"), decimal = true)
        }
        Spacer(Modifier.height(30.dp))

        SectionLabel("Modo do teto")
        ModeGroup(ui.ceilingMode, onMode)
        Spacer(Modifier.height(30.dp))

        when (ui.ceilingMode) {
            "weekdayWeekend" -> {
                SectionLabel("Metas diárias")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KcalField(ui.weekdayField, onWeekday, Modifier.weight(1f), caption = "Dias úteis", compact = true)
                    KcalField(ui.weekendField, onWeekend, Modifier.weight(1f), caption = "Fim de semana", compact = true)
                }
            }
            "seven" -> {
                SectionLabel("Meta por dia")
                val labels = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    labels.chunked(2).forEachIndexed { row, pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            pair.forEachIndexed { col, label ->
                                val i = row * 2 + col
                                KcalField(ui.dayFields[i], { onDay(i, it) }, Modifier.weight(1f), caption = label, compact = true)
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            else -> {
                SectionLabel("Meta diária")
                KcalField(ui.sameField, onSame, Modifier.fillMaxWidth().testTag("o1-ceiling"))
            }
        }
        ui.suggestedCeiling?.let { suggested ->
            Row(Modifier.padding(top = 10.dp, start = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = p.gold, modifier = Modifier.padding(top = 1.dp).size(16.dp))
                Text(
                    buildAnnotatedString {
                        append("Sugerido ")
                        withStyle(SpanStyle(color = p.text, fontWeight = FontWeight.W600)) { append("$suggested kcal") }
                        append(" com base no seu perfil. Você pode alterar quando quiser.")
                    },
                    style = DietaBotType.labelMd.copy(letterSpacing = 0.sp, lineHeight = 16.5.sp, fontWeight = FontWeight.W400),
                    color = p.muted,
                    modifier = Modifier.testTag("o1-suggested"),
                )
            }
        }
    }
}

@Composable
private fun SexToggle(sex: String, onSex: (String) -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(p.card)
            .border(1.dp, p.line, RoundedCornerShape(16.dp))
            .padding(4.dp)
            .testTag("o1-sex"),
    ) {
        listOf("male" to "Homem", "female" to "Mulher").forEach { (value, label) ->
            val selected = sex == value
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(InnerShape)
                    .background(if (selected) p.segSel else p.card)
                    .dietaClick { onSex(value) }
                    .testTag("o1-sex-$value"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = DietaBotType.labelLg.copy(fontWeight = if (selected) FontWeight.W600 else FontWeight.W500),
                    color = if (selected) p.onSegSel else p.muted,
                )
            }
        }
    }
}

@Composable
private fun UnitField(
    value: String,
    unit: String,
    placeholder: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    decimal: Boolean = false,
) {
    val p = LocalPalette.current
    val suffix = remember(unit, p) { UnitSuffix(unit, SpanStyle(fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.W400, color = p.muted)) }
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        textStyle = TextStyle(fontFamily = Jakarta, fontSize = 18.sp, fontWeight = FontWeight.W700, color = p.text, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(p.gold),
        visualTransformation = if (value.isEmpty()) VisualTransformation.None else suffix,
        modifier = modifier,
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(p.card)
                    .border(1.dp, p.line, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (value.isEmpty()) Text("$placeholder · $unit", style = DietaBotType.labelMd, color = p.dim)
                inner()
            }
        },
    )
}

/** Draws "  unit" after the typed number without putting it in the value. */
private class UnitSuffix(private val unit: String, private val style: SpanStyle) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val out = buildAnnotatedString {
            append(text)
            withStyle(style) { append("  $unit") }
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = offset
            override fun transformedToOriginal(offset: Int) = offset.coerceAtMost(text.length)
        }
        return TransformedText(out, mapping)
    }

    override fun equals(other: Any?) = other is UnitSuffix && other.unit == unit && other.style == style
    override fun hashCode() = unit.hashCode() * 31 + style.hashCode()
}

@Composable
internal fun ModeGroup(mode: String, onMode: (String) -> Unit, tag: String = "o1") {
    val p = LocalPalette.current
    val items = listOf(
        Triple("same", "Mesma meta todos os dias", "Um valor fixo para a semana inteira."),
        Triple("weekdayWeekend", "Metas separadas (útil e fim de semana)", "Sábado e domingo com limites diferentes."),
        Triple("seven", "Personalizado por dia", "Cada dia da semana com sua própria meta."),
    )
    Column(
        Modifier
            .fillMaxWidth()
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
                    .dietaClick { onMode(value) }
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .testTag("$tag-mode-$value"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(title, style = DietaBotType.labelLg.copy(letterSpacing = (-0.025).em, fontWeight = if (selected) FontWeight.W600 else FontWeight.W500), color = p.text)
                    Text(body, style = DietaBotType.labelMd.copy(letterSpacing = 0.sp, fontWeight = FontWeight.W400), color = if (selected) p.muted else p.dim, modifier = Modifier.padding(top = 2.dp))
                }
                GoldRadio(selected)
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
) {
    val p = LocalPalette.current
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = TextStyle(
            fontFamily = Jakarta,
            fontSize = if (compact) 22.sp else 32.sp,
            fontWeight = FontWeight.W700,
            letterSpacing = (-0.025).em,
            color = p.text,
        ),
        cursorBrush = SolidColor(p.gold),
        modifier = modifier,
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
                    inner()
                }
                if (!compact) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(p.gold.copy(alpha = 0.10f))
                            .border(1.dp, p.gold.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("kcal", style = DietaBotType.labelMd.copy(fontWeight = FontWeight.W600, letterSpacing = 0.sp), color = p.gold)
                        Icon(Icons.Outlined.Bolt, contentDescription = null, tint = p.gold, modifier = Modifier.size(14.dp))
                    }
                } else {
                    Text("kcal", style = DietaBotType.labelMd, color = p.muted)
                }
            }
        },
    )
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
    OnboardingFrame(
        bar = OnboardingBar.IntakeContinuous(0.5f),
        cta = "Continuar",
        ctaEnabled = ui.eat != "partial" || (ui.pct.toIntOrNull() ?: 0) > 0,
        onCta = onContinue,
        onBack = onBack,
        ctaTag = "o2-continue",
        ctaWeight = FontWeight.W600,
        // Stitch light gold sits 4 dp lower than the dark one.
        contentTop = if (LocalPalette.current.isDark) 20.dp else 24.dp,
    ) {
        Eyebrow("ONBOARDING 2/4", "EXERCÍCIOS", sectionAccent = false, tracking = 0.16f)
        ScreenTitle(
            "Compensação de treinos",
            "Escolha se o gasto calórico de exercícios registrados deve aumentar sua meta do dia.",
            titleSize = 28,
            titleLine = 34f,
            // Stitch: semibold in dark, bold in light.
            titleWeight = if (LocalPalette.current.isDark) FontWeight.W600 else FontWeight.W700,
        )
        Spacer(Modifier.height(26.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EatCard(
                selected = ui.eat == "zero",
                title = "0% (Não compensar)",
                body = "O gasto do treino não altera sua meta diária de calorias.",
                badge = "Padrão",
                tag = "o2-zero",
                onClick = { onEat("zero") },
            )
            EatCard(
                selected = ui.eat == "partial",
                title = "Porcentagem personalizada",
                body = "Defina um percentual para somar à meta (ex.: 50%).",
                tag = "o2-partial",
                onClick = { onEat("partial") },
            ) {
                if (ui.eat == "partial") PctField(ui.pct, onPct)
            }
            EatCard(
                selected = ui.eat == "full",
                title = "100% (Compensação total)",
                body = "Adiciona todas as calorias gastas no treino à sua meta.",
                tag = "o2-full",
                onClick = { onEat("full") },
            )
        }
        InfoNote(
            Icons.Outlined.Info,
            "Você poderá registrar ou ajustar os treinos a qualquer momento nas configurações.",
            Modifier.padding(top = 20.dp),
        )
    }
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

// ---------------------------------------------------------------- O3

@Composable
fun SlotsScreen(
    ui: OnboardingUiState,
    onCount: (Int) -> Unit,
    onName: (Int, String) -> Unit,
    onTime: (Int, Int) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    var picking by remember { mutableIntStateOf(-1) }
    OnboardingFrame(
        bar = OnboardingBar.IntakeSegments(filled = 3),
        cta = "Continuar",
        ctaEnabled = ui.o3Valid,
        onCta = onContinue,
        onBack = onBack,
        ctaTag = "o3-continue",
        contentTop = 12.dp,
    ) {
        Eyebrow("ONBOARDING 3/4", "ROTINA", sectionAccent = false)
        ScreenTitle("Distribuição das refeições", "Organize sua rotina para planejar o dia e receber lembretes no horário certo.", titleLine = 37.5f)
        Spacer(Modifier.height(24.dp))
        // A15 (Stitch gold): stepper 46 dp tall (36 dp pills, 5 dp inset), 10 dp under the label, 28 dp above the cards.
        SectionLabel("Quantidade de refeições", bottom = 10.dp)
        CountStepper(ui.slots.size, onCount, itemHeight = 36.dp, inset = 5.dp)
        Spacer(Modifier.height(28.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ui.slots.forEachIndexed { i, slot ->
                SlotCard(
                    index = i,
                    slot = slot,
                    onName = { onName(i, it) },
                    onPickTime = { picking = i },
                    pad = 15.dp, // A15: Stitch gold card is 2 dp taller than the Config one.
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
    if (picking in ui.slots.indices) {
        SlotTimeDialog(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SlotTimeDialog(minutes: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val p = LocalPalette.current
    val state = rememberTimePickerState(initialHour = minutes / 60, initialMinute = minutes % 60, is24Hour = true)
    val haptic = rememberHaptic()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.card,
        confirmButton = {
            TextButton(onClick = { haptic(Haptic.Light); onConfirm(state.hour * 60 + state.minute) }) { Text("OK", color = p.gold) }
        },
        dismissButton = { TextButton(onClick = { haptic(Haptic.Light); onDismiss() }) { Text("Cancelar", color = p.muted) } },
        text = {
            TimePicker(state = state)
        },
    )
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
    val p = LocalPalette.current
    var help by remember { mutableStateOf(false) }
    val haptic = rememberHaptic()
    val grams = listOf(ui.proteinField, ui.carbField, ui.fatField).map { it.toIntOrNull() ?: 0 }
    val kcal = listOf(grams[0] * 4, grams[1] * 4, grams[2] * 9)
    val total = kcal.sum()
    val pct = kcal.map { if (total > 0) (it * 100.0 / total).roundToInt() else 0 }
    OnboardingFrame(
        bar = OnboardingBar.Brand(filled = 4, onHelp = { help = true }),
        cta = "Concluir e começar",
        ctaEnabled = ui.o4Valid,
        onCta = onFinish,
        onBack = onBack,
        ctaTag = "o4-finish",
        ctaJakarta = true,
        contentTop = 12.dp,
    ) {
        Eyebrow("ONBOARDING 4/4", "MACRONUTRIENTES", sectionAccent = false, leadingDot = true, tracking = 0.14f)
        ScreenTitle(
            "Alvos de macronutrientes",
            "Distribuição calculada para a sua meta diária. Você pode ajustar as quantidades.",
            titleSize = 28,
            titleLine = 35f,
            // Stitch dark gold: title falls back to an unstyled 16/20 regular (undefined class).
            titleStyle = if (p.isDark) TextStyle(fontFamily = FontFamily.Default, fontSize = 16.sp, lineHeight = 18.sp, lineHeightStyle = DietaBotType.cssLines) else DietaBotType.headlineMd.copy(fontSize = 26.sp, fontWeight = FontWeight.W700, lineHeight = 30.sp),
            // A15: light gold title is 26 sp and sits 3 dp higher.
            titleTop = if (p.isDark) 8.dp else 5.dp,
            subtitleTop = if (p.isDark) 8.dp else 9.dp,
        )
        Spacer(Modifier.height(if (p.isDark) 21.dp else 20.dp))
        SplitBar(pct)
        Spacer(Modifier.height(if (p.isDark) 21.dp else 20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MacroCard("Proteína", "4 kcal/g • ${pct[0]}%", ui.proteinField, p.protein, onProtein, "o4-protein")
            MacroCard("Carboidrato", "4 kcal/g • ${pct[1]}%", ui.carbField, p.carbs, onCarb, "o4-carb")
            MacroCard("Gordura", "9 kcal/g • ${pct[2]}%", ui.fatField, p.fat, onFat, "o4-fat")
        }
        InfoNote(
            Icons.Outlined.Info,
            "Proporção balanceada: 30% Proteína · 40% Carboidratos · 30% Gorduras.",
            Modifier.padding(top = 16.dp),
            infoLine = 15f,
            infoTracking = 0.05f,
        ) {
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(p.gold))
                Spacer(Modifier.width(6.dp))
                Text(
                    "${ui.day1Ceiling} KCAL TOTAL ESTIMADA",
                    style = DietaBotType.labelCaps.copy(letterSpacing = 0.025.em),
                    color = p.gold,
                    modifier = Modifier.testTag("o4-total"),
                )
            }
        }
    }
    if (help) {
        AlertDialog(
            onDismissRequest = { help = false },
            containerColor = p.card,
            confirmButton = { TextButton(onClick = { haptic(Haptic.Light); help = false }) { Text("OK", color = p.gold) } },
            text = {
                Text(
                    "P e C: 4 kcal/g. G: 9 kcal/g. Sugestão 30/40/30 sobre o teto do dia 1. Estimativa, não consulta.",
                    style = DietaBotType.bodyMd,
                    color = p.muted,
                )
            },
        )
    }
}

@Composable
private fun SplitBar(pct: List<Int>) {
    val p = LocalPalette.current
    val colors = listOf(p.protein, p.carbs, p.fat)
    val labels = listOf("Proteína", "Carbos", "Gorduras")
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(p.card)
            .border(1.dp, p.line, RoundedCornerShape(20.dp))
            // A15: the light gold card is 2 dp taller (bar 1 dp lower, 1 dp more at the bottom).
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = if (p.isDark) 12.dp else 13.dp)
            .testTag("o4-split"),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEachIndexed { i, label ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(colors[i]))
                    Spacer(Modifier.width(4.dp))
                    Text("${pct[i]}% $label", style = DietaBotType.labelMd.copy(fontSize = 11.sp, letterSpacing = 0.sp), color = colors[i])
                }
            }
        }
        Row(
            Modifier
                .padding(top = if (p.isDark) 8.dp else 9.dp)
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape)
                .background(p.phone)
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            pct.forEachIndexed { i, v ->
                if (v > 0) Box(Modifier.weight(v.toFloat()).fillMaxHeight().clip(barShape(i)).background(colors[i]))
            }
        }
    }
}

private fun barShape(i: Int) = when (i) {
    0 -> RoundedCornerShape(topStart = 50f, bottomStart = 50f)
    2 -> RoundedCornerShape(topEnd = 50f, bottomEnd = 50f)
    else -> RoundedCornerShape(0.dp)
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
