package app.fibrai.android.feature.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.fibrai.android.R
import app.fibrai.android.core.designsystem.SplashBoot
import app.fibrai.android.core.designsystem.aero.Aero
import app.fibrai.android.core.designsystem.aero.AeroText
import app.fibrai.android.core.designsystem.aero.AeroBubble
import app.fibrai.android.core.designsystem.aero.AeroPage
import kotlinx.coroutines.delay

/**
 * Cold-start splash on Aero (Figma `Design`, splash · D4): page gradient, the brand mark in its 120 dp box, the
 * wordmark in Hero/Number, a 48 x 4 accent bar and the disclaimer. Not a freeze: it leaves after SplashBoot.DELAY_MS.
 */
@Composable
fun SplashScreen(
    capture: Boolean = false,
    onDone: () -> Unit,
) {
    val c = Aero.colors
    val type = Aero.type
    LaunchedEffect(capture) {
        if (capture) return@LaunchedEffect
        delay(SplashBoot.DELAY_MS)
        onDone()
    }
    AeroPage(Modifier.fillMaxSize().testTag("splash")) {
        AeroBubble(80.dp, Modifier.offset(250.dp, (-30).dp))
        AeroBubble(46.dp, Modifier.offset(340.dp, 520.dp))
        AeroBubble(40.dp, Modifier.offset((-20).dp, 300.dp))
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            // A14: logo from design/brand (tools/brand-icons.ps1). Decorative: the wordmark carries the name.
            Box(Modifier.size(SplashBoot.LOGO_BOX_DP.dp).testTag("splash-logo"), contentAlignment = Alignment.Center) {
                Image(painter = painterResource(R.drawable.logo_mark), contentDescription = null, modifier = Modifier.size(SplashBoot.LOGO_DP.dp))
            }
            AeroText(SplashBoot.WORDMARK, Modifier.padding(top = 16.dp), style = type.heroNumber.copy(color = c.textPrimary))
            Box(Modifier.padding(top = 10.dp).width(48.dp).height(4.dp).clip(CircleShape).background(c.accentDefault))
            AeroText(
                SplashBoot.COPY,
                Modifier.padding(top = 28.dp).width(280.dp).testTag("splash-copy"),
                style = type.body.copy(color = c.textMuted, textAlign = TextAlign.Center),
            )
        }
    }
}
