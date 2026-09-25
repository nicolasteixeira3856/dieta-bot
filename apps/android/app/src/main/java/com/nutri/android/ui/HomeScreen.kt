package com.nutri.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    ui: DayUi,
    onLog: () -> Unit,
    onFit: () -> Unit,
    onRemoveChip: () -> Unit,
) {
    val p = LocalPalette.current
    Column(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 22.dp),
    ) {
        Text(ui.shortDate, color = p.dim, fontSize = 12.sp)
        Text(
            formatRemaining(ui.remaining),
            modifier = Modifier.padding(top = 12.dp).testTag("saldo"),
            color = p.text,
            fontSize = NutriMeasure.remainingPt.sp,
            fontWeight = FontWeight(590),
            letterSpacing = (-1.4).sp,
            lineHeight = 36.sp,
        )
        Text(ui.remainingLabel, color = p.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
        Column(
            Modifier
                .padding(top = 18.dp)
                .fillMaxWidth()
                .background(p.surf2, RoundedCornerShape(20.dp))
                .padding(16.dp)
                .testTag("proxima-janela"),
        ) {
            Text("PRÓXIMA", color = p.dim, fontSize = 10.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.W600)
            Text(ui.nextTitle, color = p.text, fontSize = 16.sp, fontWeight = FontWeight(560), modifier = Modifier.padding(top = 6.dp))
            Text(ui.nextDetail, color = p.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            Box(
                Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .height(NutriMeasure.barDp.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(p.line),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(ui.bar.coerceIn(0f, 1f))
                        .background(p.gold),
                )
            }
        }
        if (ui.appDay > 1 && ui.chipLabel != null) {
            Row(
                Modifier
                    .padding(top = 18.dp)
                    .background(p.surf2, RoundedCornerShape(NutriMeasure.cardDp.dp))
                    .border(1.dp, p.line, RoundedCornerShape(NutriMeasure.cardDp.dp))
                    .padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
                    .testTag("chip"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(ui.chipLabel, color = p.text, fontSize = 13.sp)
                Text("×", color = p.dim, fontSize = 16.sp, modifier = Modifier.clickable(onClick = onRemoveChip).testTag("chip-remover"))
            }
            if (ui.chipNote != null) {
                Text(ui.chipNote, color = p.dim, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
        Row(
            Modifier
                .padding(top = 18.dp)
                .fillMaxWidth()
                .background(p.surf, RoundedCornerShape(18.dp))
                .border(1.dp, p.line, RoundedCornerShape(18.dp))
                .clickable(onClick = onLog)
                .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)
                .testTag("home-composer"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("o que comeu", color = p.muted, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .size(36.dp)
                    .background(p.surf2, RoundedCornerShape(12.dp))
                    .border(1.dp, p.line, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("▣", color = p.muted, fontSize = 14.sp)
            }
        }
        NutriCta("o que cabe agora", Modifier.padding(top = 12.dp).testTag("home-cta"), onFit)
        Text(
            "Estimativa, não consulta.",
            color = p.dim,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 12.dp).testTag("disclaimer"),
        )
    }
}
