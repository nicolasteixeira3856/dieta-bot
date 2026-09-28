package com.nutri.android.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Palette(
    val bg: Color,
    val panel: Color,
    val phone: Color,
    val surf: Color,
    val surf2: Color,
    val line: Color,
    val text: Color,
    val muted: Color,
    val dim: Color,
    val gold: Color,
    val good: Color,
    val bad: Color,
    val ctaBg: Color,
    val ctaText: Color,
    val handle: Color,
    val protein: Color,
    val carbs: Color,
    val fat: Color,
    /** Roles derived from the tokens above (Stitch gold mapping). */
    val card: Color = surf,
    val cardSel: Color = surf2,
    val segSel: Color = text,
    val onSegSel: Color = ctaText,
    val isDark: Boolean = true,
    /** Text on the gold FAB. */
    val onGold: Color = ctaText,
)

val darkPalette = Palette(
    bg = Color(DietaBotHex.darkBg),
    panel = Color(DietaBotHex.darkPanel),
    phone = Color(DietaBotHex.darkPhone),
    surf = Color(DietaBotHex.darkSurf),
    surf2 = Color(DietaBotHex.darkSurf2),
    line = Color(DietaBotHex.darkLine),
    text = Color(DietaBotHex.darkText),
    muted = Color(DietaBotHex.darkMuted),
    dim = Color(DietaBotHex.darkDim),
    gold = Color(DietaBotHex.darkGold),
    good = Color(DietaBotHex.darkGood),
    bad = Color(DietaBotHex.darkBad),
    ctaBg = Color(DietaBotHex.darkCtaBg),
    ctaText = Color(DietaBotHex.darkCtaText),
    handle = Color(0xFF3A424C),
    protein = Color(DietaBotHex.darkProtein),
    carbs = Color(DietaBotHex.darkCarbs),
    fat = Color(DietaBotHex.darkFat),
)

val lightPalette = Palette(
    bg = Color(DietaBotHex.lightBg),
    panel = Color(DietaBotHex.lightPanel),
    phone = Color(DietaBotHex.lightPhone),
    surf = Color(DietaBotHex.lightSurf),
    surf2 = Color(DietaBotHex.lightSurf2),
    line = Color(DietaBotHex.lightLine),
    text = Color(DietaBotHex.lightText),
    muted = Color(DietaBotHex.lightMuted),
    dim = Color(DietaBotHex.lightDim),
    gold = Color(DietaBotHex.lightGold),
    good = Color(DietaBotHex.lightGood),
    bad = Color(DietaBotHex.lightBad),
    ctaBg = Color(DietaBotHex.lightCtaBg),
    ctaText = Color(DietaBotHex.lightCtaText),
    handle = Color(0xFFC8C4BC),
    protein = Color(DietaBotHex.lightProtein),
    carbs = Color(DietaBotHex.lightCarbs),
    fat = Color(DietaBotHex.lightFat),
    card = Color(DietaBotHex.lightPanel),
    cardSel = Color(DietaBotHex.lightSurf),
    segSel = Color(DietaBotHex.lightSurf),
    onSegSel = Color(DietaBotHex.lightText),
    isDark = false,
    onGold = Color(DietaBotHex.lightSurf),
)

val LocalPalette = staticCompositionLocalOf { darkPalette }
val LocalNutriColors = LocalPalette

val shapes = Shapes(
    extraSmall = RoundedCornerShape(DietaBotMeasure.cardDp.dp),
    small = RoundedCornerShape(DietaBotMeasure.cardDp.dp),
    medium = RoundedCornerShape(DietaBotMeasure.cardDp.dp),
    large = RoundedCornerShape(DietaBotMeasure.cardDp.dp),
    extraLarge = RoundedCornerShape(DietaBotMeasure.sheetTopDp.dp),
    largeIncreased = RoundedCornerShape(DietaBotMeasure.cardDp.dp),
    extraLargeIncreased = RoundedCornerShape(DietaBotMeasure.sheetTopDp.dp),
    extraExtraLarge = RoundedCornerShape(DietaBotMeasure.sheetTopDp.dp),
)

private fun scheme(p: Palette, dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        background = p.bg,
        surface = p.surf,
        surfaceVariant = p.surf2,
        surfaceContainerLowest = p.bg,
        surfaceContainerLow = p.panel,
        surfaceContainer = p.surf,
        surfaceContainerHigh = p.surf2,
        surfaceContainerHighest = p.surf2,
        surfaceBright = p.surf2,
        surfaceDim = p.bg,
        primary = p.ctaBg,
        onPrimary = p.ctaText,
        primaryContainer = p.surf,
        onPrimaryContainer = p.text,
        secondary = p.gold,
        onSecondary = p.ctaText,
        secondaryContainer = p.gold.copy(alpha = 0.16f),
        onSecondaryContainer = p.gold,
        tertiary = p.good,
        onTertiary = p.bg,
        tertiaryContainer = p.surf,
        onTertiaryContainer = p.good,
        onBackground = p.text,
        onSurface = p.text,
        onSurfaceVariant = p.handle,
        outline = p.line,
        outlineVariant = p.line,
        error = p.bad,
        onError = p.text,
        errorContainer = p.surf,
        onErrorContainer = p.bad,
        surfaceTint = Color.Transparent,
        inverseSurface = p.text,
        inverseOnSurface = p.bg,
        inversePrimary = p.gold,
        scrim = p.bg,
    )
}

private fun type(p: Palette) = Typography(
    headlineLarge = TextStyle(
        color = p.text,
        fontSize = DietaBotMeasure.remainingPt.sp,
        fontWeight = FontWeight(590),
        letterSpacing = (-0.8).sp,
    ),
    headlineMedium = TextStyle(
        color = p.text,
        fontSize = DietaBotMeasure.fieldPt.sp,
        fontWeight = FontWeight(560),
    ),
    titleLarge = TextStyle(color = p.text, fontSize = 22.sp, fontWeight = FontWeight(590)),
    bodyLarge = TextStyle(color = p.text, fontSize = 16.sp),
    bodyMedium = TextStyle(color = p.muted, fontSize = 13.sp),
    labelSmall = TextStyle(color = p.gold, fontSize = 11.sp, fontWeight = FontWeight.W600, letterSpacing = 0.8.sp),
)

object DietaBotTheme {
    val colors: Palette
        @Composable
        get() = LocalPalette.current

    val typography: Typography
        @Composable
        get() = MaterialTheme.typography

    val shapes: Shapes
        @Composable
        get() = MaterialTheme.shapes

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    operator fun invoke(
        darkTheme: Boolean = isSystemInDarkTheme(),
        content: @Composable () -> Unit,
    ) {
        val dark = darkTheme
        val palette = if (dark) darkPalette else lightPalette
        CompositionLocalProvider(
            LocalPalette provides palette,
            LocalNutriColors provides palette,
        ) {
            MaterialExpressiveTheme(
                colorScheme = scheme(palette, dark),
                motionScheme = MotionScheme.expressive(),
                shapes = com.nutri.android.core.designsystem.shapes,
                typography = type(palette),
                content = content,
            )
        }
    }
}

@Composable
fun DietaBotGroup(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    stacked: Boolean = false,
) {
    ExpressiveButtonGroup(
        options = options,
        selected = selected,
        onSelect = onSelect,
        modifier = modifier,
        stacked = stacked,
    )
}

@Composable
fun DietaBotCta(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val p = LocalPalette.current
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = p.ctaBg, contentColor = p.ctaText),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        Text(text, color = p.ctaText, fontWeight = FontWeight.W600, fontSize = 15.sp)
    }
}

@Composable
fun DietaBotCtaGhost(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = p.text),
        border = BorderStroke(1.dp, p.line),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        Text(text, color = p.text, fontWeight = FontWeight.W600, fontSize = 15.sp)
    }
}

@Composable
fun Modifier.cardBorder(): Modifier {
    val p = LocalPalette.current
    return this
        .background(p.surf, RoundedCornerShape(DietaBotMeasure.cardDp.dp))
        .border(1.dp, p.line, RoundedCornerShape(DietaBotMeasure.cardDp.dp))
}
