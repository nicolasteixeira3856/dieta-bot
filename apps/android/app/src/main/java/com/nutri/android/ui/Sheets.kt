package com.nutri.android.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentSheet(
    ui: DayUi,
    onClose: () -> Unit,
    onText: (String) -> Unit,
    onPhoto: (android.net.Uri) -> Unit,
    onSubmit: () -> Unit,
    onMode: (String) -> Unit,
    onFitText: (String) -> Unit,
    onFit: () -> Unit,
    onAlreadyAte: () -> Unit,
) {
    val sheet = ui.sheet ?: return
    val p = LocalPalette.current
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = state,
        containerColor = p.panel,
        scrimColor = Color.Black.copy(alpha = 0.46f),
        shape = RoundedCornerShape(topStart = NutriMeasure.sheetTopDp.dp, topEnd = NutriMeasure.sheetTopDp.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 14.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(p.line, RoundedCornerShape(99.dp)),
            )
        },
    ) {
        when (sheet) {
            SheetKind.T1 -> LogSheet(ui, onText, onPhoto, onSubmit)
            SheetKind.T3 -> FitSheet(ui, onMode, onFitText, onFit, onAlreadyAte)
        }
    }
}

@Composable
private fun LogSheet(
    ui: DayUi,
    onText: (String) -> Unit,
    onPhoto: (android.net.Uri) -> Unit,
    onSubmit: () -> Unit,
) {
    val p = LocalPalette.current
    val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) onPhoto(uri)
    }
    Column(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 22.dp)) {
        Text("Registrar", color = p.text, fontSize = 18.sp, fontWeight = FontWeight(590))
        TextBox(
            value = ui.text,
            onChange = onText,
            placeholder = "2 pães, ovo, café com leite",
            modifier = Modifier.padding(top = 12.dp).fillMaxWidth().testTag("t1-text"),
        )
        if (ui.loading) {
            Column(
                Modifier.fillMaxWidth().padding(top = 18.dp).testTag("t1-load"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                WaitIndicator(Modifier.testTag("t1-submit"))
                Text("estimando", color = p.dim, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        } else {
            Row(
                Modifier.padding(top = 12.dp).testTag("t1-photo").clickable {
                    picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier
                        .size(56.dp)
                        .background(p.surf2, RoundedCornerShape(14.dp))
                        .border(1.dp, p.line, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("foto", color = p.muted, fontSize = 12.sp)
                }
                Text("JPEG 70 · ≤1280", color = p.dim, fontSize = 12.sp)
            }
        }
        NutriCta("Enviar", Modifier.padding(top = 18.dp).testTag("t1-send"), onSubmit)
    }
}

@Composable
private fun FitSheet(
    ui: DayUi,
    onMode: (String) -> Unit,
    onFitText: (String) -> Unit,
    onFit: () -> Unit,
    onAlreadyAte: () -> Unit,
) {
    val p = LocalPalette.current
    val sel = when (ui.fitMode) {
        "have" -> 1
        "idea" -> 2
        else -> 0
    }
    Column(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 22.dp).testTag("t3-fit")) {
        Text("O que cabe agora", color = p.text, fontSize = 18.sp, fontWeight = FontWeight(590))
        NutriGroup(
            options = listOf("Quero", "Tenho", "Sem ideia"),
            selected = sel,
            onSelect = { onMode(when (it) { 1 -> "have"; 2 -> "idea"; else -> "want" }) },
            modifier = Modifier.padding(top = 12.dp).testTag("t3-modes"),
        )
        if (ui.fitMode != "idea") {
            TextBox(
                value = ui.fitText,
                onChange = onFitText,
                placeholder = if (ui.fitMode == "want") "lasanha" else "ovo, arroz, alface",
                modifier = Modifier.padding(top = 12.dp).fillMaxWidth().testTag("t3-text"),
                lines = 1,
            )
        }
        if (ui.fitMode == "idea" && ui.fit != null) {
            val dishes = ui.fit.options.ifEmpty { listOf(ui.fit.dish).filter { it.name.isNotBlank() } }
            dishes.forEachIndexed { i, dish ->
                Column(
                    Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .cardBorder()
                        .padding(14.dp),
                ) {
                    val line = "${dish.name} · ${dish.kcal.toInt()} kcal · ${dish.p.toInt()} g P"
                    Text(line, color = if (i == 0) p.good else p.text, fontSize = 14.sp)
                    Text("preserva a janta", color = p.dim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        } else if (ui.t3Headline != null || ui.fit != null) {
            Column(
                Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .cardBorder()
                    .padding(14.dp),
            ) {
                val headline = ui.t3Headline
                if (headline != null) {
                    Text(
                        headline,
                        color = if (headline.startsWith("Cabe")) p.good else p.bad,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W600,
                    )
                }
                val line = ui.t3Line
                if (line != null) {
                    Text(line, color = p.text, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                }
                val sub = ui.t3Sub
                if (sub != null) {
                    Text(sub, color = p.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        if (ui.loading) {
            Column(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                WaitIndicator(Modifier.testTag("t3-send"))
                Text("estimando", color = p.dim, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        } else {
            val cta = ui.t3Cta
            NutriCta(
                cta,
                Modifier.padding(top = 18.dp).testTag("t3-send"),
                if (ui.fitMode == "idea" && ui.fit != null) onAlreadyAte else onFit,
            )
        }
    }
}

@Composable
fun TextBox(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    lines: Int = 2,
) {
    val p = LocalPalette.current
    BasicTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        textStyle = TextStyle(color = p.text, fontSize = 16.sp, lineHeight = 22.sp),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height((lines * 28).dp.coerceAtLeast(48.dp))
                    .background(p.surf, RoundedCornerShape(16.dp))
                    .border(1.dp, p.line, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp),
            ) {
                if (value.isEmpty()) Text(placeholder, color = p.muted, fontSize = 16.sp)
                inner()
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WaitIndicator(modifier: Modifier = Modifier) {
    LoadingIndicator(modifier = modifier.size(28.dp), color = LocalPalette.current.gold)
}
