package com.nutri.android.core.designsystem.aero

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState

/**
 * Aero design system (ADR-030), built next to the Material theme until A45. Token values are generated from
 * docs/design/tokens.json (AeroTokens.kt); this file only wires them into CompositionLocals.
 * The theme follows the system (isSystemInDarkTheme), no dynamic color.
 */
@Immutable
data class AeroShapes(
    val card: Shape = RoundedCornerShape(AeroDimens.radiusCard),
    val sheet: Shape = RoundedCornerShape(topStart = AeroDimens.radiusSheet, topEnd = AeroDimens.radiusSheet),
    val pill: Shape = RoundedCornerShape(percent = 50),
)

val LocalAeroColors = staticCompositionLocalOf { AeroLightColors }
val LocalAeroType = staticCompositionLocalOf { AeroType() }
val LocalAeroShapes = staticCompositionLocalOf { AeroShapes() }

/** The backdrop that glass surfaces blur (API 31+). Written by Modifier.aeroPage(), read by Modifier.aeroGlass(). */
val LocalAeroHaze = staticCompositionLocalOf<HazeState?> { null }

object Aero {
    val colors: AeroColors
        @Composable @ReadOnlyComposable
        get() = LocalAeroColors.current

    val type: AeroType
        @Composable @ReadOnlyComposable
        get() = LocalAeroType.current

    val shapes: AeroShapes
        @Composable @ReadOnlyComposable
        get() = LocalAeroShapes.current

    /** Durations and easings (Figma `Motion`). */
    val motion: AeroMotion get() = AeroMotion
}

@Composable
fun AeroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAeroColors provides if (darkTheme) AeroDarkColors else AeroLightColors,
        LocalAeroType provides AeroType(),
        LocalAeroShapes provides AeroShapes(),
        LocalAeroHaze provides rememberHazeState(),
        content = content,
    )
}
