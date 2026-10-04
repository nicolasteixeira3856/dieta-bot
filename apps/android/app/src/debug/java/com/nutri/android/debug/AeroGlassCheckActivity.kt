package com.nutri.android.debug

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nutri.android.core.designsystem.aero.Aero
import com.nutri.android.core.designsystem.aero.AeroText
import com.nutri.android.core.designsystem.aero.AeroButtonPrimary
import com.nutri.android.core.designsystem.aero.AeroMealCard
import com.nutri.android.core.designsystem.aero.AeroMealState
import com.nutri.android.core.designsystem.aero.AeroTheme
import com.nutri.android.core.designsystem.aero.aeroBlurSupported
import com.nutri.android.core.designsystem.aero.aeroGlass
import com.nutri.android.core.designsystem.aero.aeroPage

/**
 * A39 validation harness, debug build type only: Aero glass over the page gradient and a few shapes, so the
 * backdrop blur (API 31+) and the opaque fallback (API 26-30) can be captured on an emulator.
 * adb shell am start -n com.nutri.android.dev/com.nutri.android.debug.AeroGlassCheckActivity
 */
class AeroGlassCheckActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AeroTheme {
                val c = Aero.colors
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().aeroPage()) {
                        Box(Modifier.offset(40.dp, 150.dp).size(120.dp).background(c.accentDefault, CircleShape))
                        Box(Modifier.offset(230.dp, 330.dp).size(90.dp).background(c.macroCarbs, CircleShape))
                        Box(Modifier.offset(0.dp, 520.dp).fillMaxWidth().height(24.dp).background(c.macroProtein))
                        AeroText("BLUR", Modifier.offset(60.dp, 600.dp), style = Aero.type.heroNumber.copy(color = c.statusBad))
                    }
                    Column(
                        Modifier.systemBarsPadding().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        AeroText(
                            "API ${Build.VERSION.SDK_INT} · blur ${if (aeroBlurSupported) "on" else "fallback"}",
                            style = Aero.type.bodyStrong.copy(color = c.textPrimary),
                        )
                        Box(Modifier.padding(top = 60.dp).fillMaxWidth().height(140.dp).aeroGlass(Aero.shapes.card)) {
                            AeroText("1.240 kcal", Modifier.padding(20.dp), style = Aero.type.heroNumber.copy(color = c.textPrimary))
                        }
                        AeroMealCard(AeroMealState.Logged, "Almoço", "12:30", "Arroz, feijão e frango", kcal = "640 kcal", log = "640 kcal · 42P · 70C · 18G")
                        Box(Modifier.fillMaxWidth().height(90.dp).aeroGlass(Aero.shapes.card))
                        AeroButtonPrimary("Continuar", {})
                    }
                }
            }
        }
    }
}
