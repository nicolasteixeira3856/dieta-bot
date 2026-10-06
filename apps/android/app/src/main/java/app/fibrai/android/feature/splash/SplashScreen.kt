package app.fibrai.android.feature.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.fibrai.android.R
import app.fibrai.android.core.designsystem.SplashBoot
import app.fibrai.android.core.designsystem.aero.AeroBubble
import app.fibrai.android.core.designsystem.aero.AeroPage
import kotlinx.coroutines.delay

/**
 * Cold-start splash on Aero (Figma `Design`, splash · D13): page gradient, the three bubbles and the logo alone,
 * centered, SplashBoot.LOGO_DP tall. No wordmark and no disclaimer here (the Home carries the disclaimer).
 * Not a freeze: it leaves after SplashBoot.DELAY_MS.
 */
@Composable
fun SplashScreen(
    capture: Boolean = false,
    onDone: () -> Unit,
) {
    LaunchedEffect(capture) {
        if (capture) return@LaunchedEffect
        delay(SplashBoot.DELAY_MS)
        onDone()
    }
    AeroPage(Modifier.fillMaxSize().testTag("splash")) {
        AeroBubble(80.dp, Modifier.offset(250.dp, (-30).dp))
        AeroBubble(46.dp, Modifier.offset(340.dp, 520.dp))
        AeroBubble(40.dp, Modifier.offset((-20).dp, 300.dp))
        // Logo from design/brand (tools/brand-icons.ps1): logo_mark is the tight square crop, so the seed is LOGO_DP tall.
        Image(
            painter = painterResource(R.drawable.logo_mark),
            contentDescription = SplashBoot.LOGO_DESCRIPTION,
            modifier = Modifier.align(Alignment.Center).size(SplashBoot.LOGO_DP.dp).testTag("splash-logo"),
        )
    }
}
