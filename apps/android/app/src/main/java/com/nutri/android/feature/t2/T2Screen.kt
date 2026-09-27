package com.nutri.android.feature.t2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nutri.android.core.designsystem.LocalPalette
import com.nutri.android.core.designsystem.NutriCta
import com.nutri.android.core.designsystem.NutriCtaGhost
import com.nutri.android.core.designsystem.NutriGroup
import com.nutri.android.core.designsystem.NutriMeasure
import com.nutri.android.core.designsystem.cardBorder
import com.nutri.android.core.designsystem.formatRemaining

@Composable
fun T2Screen(
    ui: T2UiState,
    onYes: () -> Unit,
    onRevise: () -> Unit,
    onDiscard: () -> Unit,
    onConfirm: () -> Unit,
    onUndo: () -> Unit,
) {
    val p = LocalPalette.current
    val low = ui.question != null || ui.estimate?.confidence != "high"
    val kcal = ui.estimate?.kcal?.toInt() ?: 0
    val proteinG = ui.estimate?.p?.toInt() ?: 0
    val leftover = (ui.windowBudget - kcal).coerceAtLeast(0)
    Box(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 22.dp),
    ) {
        Column(Modifier.fillMaxWidth().testTag("t2-card")) {
            Text(ui.title, color = p.dim, fontSize = 12.sp)
            Column(
                Modifier
                    .padding(top = 18.dp)
                    .fillMaxWidth()
                    .cardBorder()
                    .padding(14.dp),
            ) {
                Text(ui.name, color = p.text, fontSize = 16.sp, fontWeight = FontWeight(560))
                if (low) {
                    if (kcal > 0) {
                        Text(
                            "${formatRemaining(kcal)} kcal · $proteinG g P",
                            color = p.muted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 6.dp).testTag("t2-kcal"),
                        )
                    } else if (!ui.range.isNullOrBlank()) {
                        Text(ui.range, color = p.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                    Text(ui.question ?: "descreve em 1 linha", color = p.text, modifier = Modifier.padding(top = 18.dp).testTag("t2-question"))
                    NutriGroup(
                        options = listOf("Sim", "Forma", "Esquece"),
                        selected = ui.answer,
                        onSelect = { i ->
                            when (i) {
                                1 -> onRevise()
                                2 -> onDiscard()
                                else -> onYes()
                            }
                        },
                        modifier = Modifier.padding(top = 12.dp).testTag("t2-options"),
                    )
                } else {
                    Text(
                        formatRemaining(kcal),
                        color = p.text,
                        fontSize = NutriMeasure.fieldPt.sp,
                        fontWeight = FontWeight(590),
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text("kcal · $proteinG g P", color = p.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                    Text(
                        "Cabe. Sobra ${formatRemaining(leftover)} pra janta.",
                        color = p.good,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 12.dp).testTag("t2-fits"),
                    )
                }
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            NutriCta(
                if (low) "Confirmar" else "Ok",
                Modifier.testTag("t2-confirm"),
                enabled = ui.confirmEnabled,
                onClick = onConfirm,
            )
            if (!low) {
                NutriCtaGhost("Desfazer", Modifier.padding(top = 12.dp).testTag("t2-undo"), onUndo)
            }
        }
    }
}
