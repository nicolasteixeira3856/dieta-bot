package com.nutri.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CeilingScreen(
    ui: DayUi,
    onMode: (String) -> Unit,
    onSame: (String) -> Unit,
    onWeekday: (String) -> Unit,
    onWeekend: (String) -> Unit,
    onDay: (Int, String) -> Unit,
    onContinue: () -> Unit,
) {
    val p = LocalPalette.current
    val mode = when (ui.ceilingMode) {
        "weekdayWeekend" -> 1
        "seven" -> 2
        else -> 0
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 22.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text("1 / 2", color = p.dim, fontSize = 12.sp)
            Text(
                "Teto do dia.",
                color = p.text,
                fontSize = 22.sp,
                fontWeight = FontWeight(590),
                letterSpacing = (-0.6).sp,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text("Mesmo número ou varia.", color = p.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            NutriGroup(
                options = listOf("Mesmo todos os dias", "Útil / fds", "7 dias"),
                selected = mode,
                onSelect = { onMode(when (it) { 1 -> "weekdayWeekend"; 2 -> "seven"; else -> "same" }) },
                stacked = true,
                modifier = Modifier.padding(top = 18.dp).testTag("o1-modes"),
            )
            when (ui.ceilingMode) {
                "weekdayWeekend" -> {
                    Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField("", ui.weekdayField, onChange = onWeekday, modifier = Modifier.weight(1f))
                        NumberField("", ui.weekendField, onChange = onWeekend, modifier = Modifier.weight(1f))
                    }
                    Text("útil · fds", color = p.dim, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                }
                "seven" -> {
                    val labels = listOf("S", "T", "Q", "Q", "S", "S", "D")
                    Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        labels.forEachIndexed { i, _ ->
                            NumberField(
                                "",
                                ui.dayFields[i],
                                onChange = { onDay(i, it) },
                                modifier = Modifier.weight(1f),
                                compact = true,
                            )
                        }
                    }
                    Text("7 dias", color = p.dim, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                }
                else -> {
                    NumberField("", ui.sameField, onChange = onSame, modifier = Modifier.padding(top = 18.dp), large = true)
                    Text("kcal · todos os dias", color = p.dim, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        NutriCta("Continuar", Modifier.align(Alignment.BottomCenter).testTag("o1-continue"), onContinue)
    }
}

@Composable
fun EatScreen(ui: DayUi, onEat: (String) -> Unit, onPct: (String) -> Unit, onStart: () -> Unit) {
    val p = LocalPalette.current
    val sel = when (ui.eat) {
        "partial" -> 1
        "full" -> 2
        else -> 0
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 22.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text("2 / 2", color = p.dim, fontSize = 12.sp)
            Text(
                "Treino vira crédito?",
                color = p.text,
                fontSize = 22.sp,
                fontWeight = FontWeight(590),
                letterSpacing = (-0.6).sp,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text("Sem número no dia, crédito = 0.", color = p.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            NutriGroup(
                options = listOf("0%", "%", "100%"),
                selected = sel,
                onSelect = { onEat(when (it) { 1 -> "partial"; 2 -> "full"; else -> "zero" }) },
                modifier = Modifier.padding(top = 18.dp).testTag("o2-eat"),
            )
            when (ui.eat) {
                "partial" -> {
                    NumberField("", ui.pct, onChange = onPct, modifier = Modifier.padding(top = 12.dp), large = true)
                    Text("% do treino. default 50. sem cap", color = p.dim, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                }
                "full" -> Text("crédito = kcal do treino", color = p.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
                else -> Text("crédito sempre 0", color = p.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
            }
        }
        NutriCta("Começar", Modifier.align(Alignment.BottomCenter).testTag("o2-start"), onStart)
    }
}

@Composable
fun NumberField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    compact: Boolean = false,
) {
    val p = LocalPalette.current
    val size = when {
        compact -> 12.sp
        large -> NutriMeasure.fieldPt.sp
        else -> 16.sp
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label.isNotBlank()) Text(label, color = p.muted, fontSize = 12.sp)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(
                color = p.text,
                fontSize = size,
                fontWeight = FontWeight(560),
                letterSpacing = (-0.5).sp,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("number-field"),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(p.surf, RoundedCornerShape(16.dp))
                        .border(1.dp, p.line, RoundedCornerShape(16.dp))
                        .padding(horizontal = if (compact) 4.dp else 14.dp, vertical = if (compact) 10.dp else 14.dp),
                    contentAlignment = if (compact) Alignment.Center else Alignment.CenterStart,
                ) { inner() }
            },
        )
    }
}
