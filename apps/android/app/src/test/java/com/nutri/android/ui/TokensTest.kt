package com.nutri.android.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.nutri.android.core.designsystem.NutriHex
import com.nutri.android.core.designsystem.NutriMeasure
import com.nutri.android.core.designsystem.NutriShapes
import com.nutri.android.core.designsystem.SplashBoot
import com.nutri.android.core.designsystem.darkPalette
import com.nutri.android.core.designsystem.formatRemaining
import com.nutri.android.core.designsystem.lightPalette
import org.junit.Test

class TokensTest {
    @Test
    fun remainingAndFieldMatchWire() {
        assertThat(NutriMeasure.remainingPt).isEqualTo(34)
        assertThat(NutriMeasure.fieldPt).isEqualTo(28)
        assertThat(NutriMeasure.sheetTopDp).isEqualTo(22)
        assertThat(NutriMeasure.cardDp).isEqualTo(14)
        assertThat(NutriMeasure.barDp).isEqualTo(6)
    }

    @Test
    fun shapesAndGeometry() {
        assertThat(NutriShapes.continuousBarHeight).isEqualTo(6.dp)
        assertThat(NutriShapes.sheetTopRadius.topStart).isNotNull()
        assertThat(NutriShapes.sheetTopRadius.topEnd).isNotNull()
        assertThat(NutriShapes.cardRadius).isNotNull()
    }

    @Test
    fun ctaIsNotGold() {
        assertThat(NutriHex.darkCtaBg).isNotEqualTo(NutriHex.darkGold)
        assertThat(NutriHex.lightCtaBg).isNotEqualTo(NutriHex.lightGold)
        assertThat(NutriHex.darkCtaBg).isEqualTo(0xFFF3F5F7)
        assertThat(NutriHex.darkCtaText).isEqualTo(0xFF111111)
        assertThat(NutriHex.lightCtaBg).isEqualTo(0xFF111111)
        assertThat(NutriHex.lightCtaText).isEqualTo(0xFFF3F5F7)
    }

    @Test
    fun darkAndLightHexes() {
        assertThat(NutriHex.darkBg).isEqualTo(0xFF0B0D10)
        assertThat(NutriHex.darkGold).isEqualTo(0xFFE8B86D)
        assertThat(NutriHex.lightBg).isEqualTo(0xFFF4F3F0)
        assertThat(NutriHex.lightGold).isEqualTo(0xFFB8873D)
        assertThat(darkPalette.ctaBg).isEqualTo(Color(NutriHex.darkCtaBg))
        assertThat(lightPalette.gold).isEqualTo(Color(NutriHex.lightGold))
    }

    @Test
    fun semanticMacros() {
        // Dark semantic macros
        assertThat(NutriHex.darkProtein).isEqualTo(0xFF4EC994)
        assertThat(NutriHex.darkCarbs).isEqualTo(0xFFE58E42)
        assertThat(NutriHex.darkFat).isEqualTo(0xFFE8B86D)
        assertThat(NutriHex.darkBad).isEqualTo(0xFFE07A6A)
        assertThat(NutriHex.darkGood).isEqualTo(0xFF7DDA9A)

        // Light semantic macros
        assertThat(NutriHex.lightProtein).isEqualTo(0xFF1B7A4B)
        assertThat(NutriHex.lightCarbs).isEqualTo(0xFFC2651E)
        assertThat(NutriHex.lightFat).isEqualTo(0xFFB8873D)
        assertThat(NutriHex.lightBad).isEqualTo(0xFFC14D40)
        assertThat(NutriHex.lightGood).isEqualTo(0xFF1F8A4C)

        // Palette bindings
        assertThat(darkPalette.protein).isEqualTo(Color(NutriHex.darkProtein))
        assertThat(darkPalette.carbs).isEqualTo(Color(NutriHex.darkCarbs))
        assertThat(darkPalette.fat).isEqualTo(Color(NutriHex.darkFat))
        assertThat(darkPalette.bad).isEqualTo(Color(NutriHex.darkBad))
        assertThat(darkPalette.good).isEqualTo(Color(NutriHex.darkGood))

        assertThat(lightPalette.protein).isEqualTo(Color(NutriHex.lightProtein))
        assertThat(lightPalette.carbs).isEqualTo(Color(NutriHex.lightCarbs))
        assertThat(lightPalette.fat).isEqualTo(Color(NutriHex.lightFat))
        assertThat(lightPalette.bad).isEqualTo(Color(NutriHex.lightBad))
        assertThat(lightPalette.good).isEqualTo(Color(NutriHex.lightGood))
    }

    @Test
    fun wcagContrastVerification() {
        // Relative luminance helper
        fun luminance(color: Color): Double {
            fun channel(c: Float): Double {
                return if (c <= 0.03928f) (c / 12.92).toDouble() else Math.pow(((c + 0.055) / 1.055).toDouble(), 2.4)
            }
            return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
        }

        fun contrast(c1: Color, c2: Color): Double {
            val l1 = luminance(c1)
            val l2 = luminance(c2)
            val lighter = maxOf(l1, l2)
            val darker = minOf(l1, l2)
            return (lighter + 0.05) / (darker + 0.05)
        }

        // Dark background contrast (minimum 3.0:1 for graphical/badges, >4.5:1 for body)
        val darkBg = darkPalette.bg
        assertThat(contrast(darkPalette.text, darkBg)).isAtLeast(10.0)
        assertThat(contrast(darkPalette.protein, darkBg)).isAtLeast(4.5)
        assertThat(contrast(darkPalette.carbs, darkBg)).isAtLeast(4.5)
        assertThat(contrast(darkPalette.fat, darkBg)).isAtLeast(4.5)
        assertThat(contrast(darkPalette.bad, darkBg)).isAtLeast(4.5)

        // Light background contrast
        val lightBg = lightPalette.bg
        assertThat(contrast(lightPalette.text, lightBg)).isAtLeast(10.0)
        assertThat(contrast(lightPalette.protein, lightBg)).isAtLeast(4.5)
        assertThat(contrast(lightPalette.carbs, lightBg)).isAtLeast(3.0)
        assertThat(contrast(lightPalette.bad, lightBg)).isAtLeast(3.0)
        assertThat(contrast(lightPalette.fat, lightBg)).isAtLeast(2.8)
    }

    @Test
    fun formatRemainingUsesThousandsDot() {
        assertThat(formatRemaining(1840)).isEqualTo("1.840")
        assertThat(formatRemaining(2000)).isEqualTo("2.000")
        assertThat(formatRemaining(420)).isEqualTo("420")
    }

    @Test
    fun splashIsBootNotFreeze() {
        assertThat(SplashBoot.DELAY_MS).isAtMost(SplashBoot.MAX_MS)
        assertThat(SplashBoot.MAX_MS).isEqualTo(2000L)
        assertThat(SplashBoot.COPY).isEqualTo("estimativa, não consulta")
        assertThat(SplashBoot.WORDMARK).isEqualTo("Nutri")
    }
}
