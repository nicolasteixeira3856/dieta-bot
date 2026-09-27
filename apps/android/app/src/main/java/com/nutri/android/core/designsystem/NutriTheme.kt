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
)

val darkPalette = Palette(
    bg = Color(NutriHex.darkBg),
    panel = Color(NutriHex.darkPanel),
    phone = Color(NutriHex.darkPhone),
    surf = Color(NutriHex.darkSurf),
    surf2 = Color(NutriHex.darkSurf2),
    line = Color(NutriHex.darkLine),
    text = Color(NutriHex.darkText),
    muted = Color(NutriHex.darkMuted),
    dim = Color(NutriHex.darkDim),
    gold = Color(NutriHex.darkGold),
    good = Color(NutriHex.darkGood),
    bad = Color(NutriHex.darkBad),
    ctaBg = Color(NutriHex.darkCtaBg),
    ctaText = Color(NutriHex.darkCtaText),
    handle = Color(0xFF3A424C),
    protein = Color(NutriHex.darkProtein),
    carbs = Color(NutriHex.darkCarbs),
    fat = Color(NutriHex.darkFat),
)

val lightPalette = Palette(
    bg = Color(NutriHex.lightBg),
    panel = Color(NutriHex.lightPanel),
    phone = Color(NutriHex.lightPhone),
    surf = Color(NutriHex.lightSurf),
    surf2 = Color(NutriHex.lightSurf2),
    line = Color(NutriHex.lightLine),
    text = Color(NutriHex.lightText),
    muted = Color(NutriHex.lightMuted),
    dim = Color(NutriHex.lightDim),
    gold = Color(NutriHex.lightGold),
    good = Color(NutriHex.lightGood),
    bad = Color(NutriHex.lightBad),
    ctaBg = Color(NutriHex.lightCtaBg),
    ctaText = Color(NutriHex.lightCtaText),
    handle = Color(0xFFC8C4BC),
    protein = Color(NutriHex.lightProtein),
    carbs = Color(NutriHex.lightCarbs),
    fat = Color(NutriHex.lightFat),
)

val LocalPalette = staticCompositionLocalOf { darkPalette }
val LocalNutriColors = LocalPalette

val shapes = Shapes(
    extraSmall = RoundedCornerShape(NutriMeasure.cardDp.dp),
    small = RoundedCornerShape(NutriMeasure.cardDp.dp),
    medium = RoundedCornerShape(NutriMeasure.cardDp.dp),
    large = RoundedCornerShape(NutriMeasure.cardDp.dp),
    extraLarge = RoundedCornerShape(NutriMeasure.sheetTopDp.dp),
    largeIncreased = RoundedCornerShape(NutriMeasure.cardDp.dp),
    extraLargeIncreased = RoundedCornerShape(NutriMeasure.sheetTopDp.dp),
    extraExtraLarge = RoundedCornerShape(NutriMeasure.sheetTopDp.dp),
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
        fontSize = NutriMeasure.remainingPt.sp,
        fontWeight = FontWeight(590),
        letterSpacing = (-0.8).sp,
    ),
    headlineMedium = TextStyle(
        color = p.text,
        fontSize = NutriMeasure.fieldPt.sp,
        fontWeight = FontWeight(560),
    ),
    titleLarge = TextStyle(color = p.text, fontSize = 22.sp, fontWeight = FontWeight(590)),
    bodyLarge = TextStyle(color = p.text, fontSize = 16.sp),
    bodyMedium = TextStyle(color = p.muted, fontSize = 13.sp),
    labelSmall = TextStyle(color = p.gold, fontSize = 11.sp, fontWeight = FontWeight.W600, letterSpacing = 0.8.sp),
)

object NutriTheme {
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
    operator fun invoke(content: @Composable () -> Unit) {
        val dark = isSystemInDarkTheme()
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
fun NutriGroup(
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
fun NutriCta(
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
fun NutriCtaGhost(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
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
        .background(p.surf, RoundedCornerShape(NutriMeasure.cardDp.dp))
        .border(1.dp, p.line, RoundedCornerShape(NutriMeasure.cardDp.dp))
}
