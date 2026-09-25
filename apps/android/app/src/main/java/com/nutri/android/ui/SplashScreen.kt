package com.nutri.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(ui: DayUi, onDone: () -> Unit) {
    val p = LocalPalette.current
    LaunchedEffect(ui.capture) {
        if (ui.capture) return@LaunchedEffect
        delay(SplashBoot.DELAY_MS)
        onDone()
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .testTag("splash"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(p.gold),
        )
        Text(
            SplashBoot.WORDMARK,
            color = p.text,
            fontSize = 22.sp,
            fontWeight = FontWeight(590),
            letterSpacing = (-0.6).sp,
            modifier = Modifier.padding(top = 18.dp),
        )
        Box(
            Modifier
                .padding(top = 18.dp)
                .width(80.dp)
                .height(NutriMeasure.barDp.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(p.gold),
        )
        Text(
            SplashBoot.COPY,
            color = p.dim,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 18.dp).testTag("splash-copy"),
        )
    }
}
