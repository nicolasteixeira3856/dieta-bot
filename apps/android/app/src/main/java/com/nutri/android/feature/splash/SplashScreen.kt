package com.nutri.android.feature.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nutri.android.core.designsystem.LocalPalette
import com.nutri.android.core.designsystem.DietaBotType
import com.nutri.android.core.designsystem.SplashBoot
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    capture: Boolean = false,
    onDone: () -> Unit,
) {
    val p = LocalPalette.current
    LaunchedEffect(capture) {
        if (capture) return@LaunchedEffect
        delay(SplashBoot.DELAY_MS)
        onDone()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(p.phone)
            .drawBehind {
                val r = 200.dp.toPx()
                drawCircle(
                    Brush.radialGradient(listOf(p.gold.copy(alpha = 0.06f), Color.Transparent), center = center, radius = r),
                    radius = r,
                    center = center,
                )
            }
            .testTag("splash"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.offset(y = (-12).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(SplashBoot.WORDMARK, style = DietaBotType.displayLg.copy(lineHeight = 48.sp), color = p.text)
            Box(
                Modifier
                    .padding(top = 16.dp)
                    .width(48.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(p.gold),
            )
            Text(
                SplashBoot.COPY,
                style = DietaBotType.bodyMd.copy(lineHeight = 22.sp, letterSpacing = 0.sp),
                color = p.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 28.dp).widthIn(max = 280.dp).testTag("splash-copy"),
            )
        }
    }
}
