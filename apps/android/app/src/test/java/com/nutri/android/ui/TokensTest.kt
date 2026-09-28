package com.nutri.android.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.nutri.android.core.designsystem.DietaBotHex
import com.nutri.android.core.designsystem.DietaBotMeasure
import com.nutri.android.core.designsystem.DietaBotShapes
import com.nutri.android.core.designsystem.SplashBoot
import com.nutri.android.core.designsystem.darkPalette
import com.nutri.android.core.designsystem.formatRemaining
import com.nutri.android.core.designsystem.lightPalette
import org.junit.Test

class TokensTest {
    @Test
    fun remainingAndFieldMatchWire() {
        assertThat(DietaBotMeasure.remainingPt).isEqualTo(34)
        assertThat(DietaBotMeasure.fieldPt).isEqualTo(28)
        assertThat(DietaBotMeasure.sheetTopDp).isEqualTo(22)
        assertThat(DietaBotMeasure.cardDp).isEqualTo(14)
        assertThat(DietaBotMeasure.barDp).isEqualTo(6)
    }

    @Test
    fun shapesAndGeometry() {
        assertThat(DietaBotShapes.continuousBarHeight).isEqualTo(6.dp)
        assertThat(DietaBotShapes.sheetTopRadius.topStart).isNotNull()
        assertThat(DietaBotShapes.sheetTopRadius.topEnd).isNotNull()
        assertThat(DietaBotShapes.cardRadius).isNotNull()
    }

    @Test
    fun ctaIsNotGold() {
        assertThat(DietaBotHex.darkCtaBg).isNotEqualTo(DietaBotHex.darkGold)
        assertThat(DietaBotHex.lightCtaBg).isNotEqualTo(DietaBotHex.lightGold)
        assertThat(DietaBotHex.darkCtaBg).isEqualTo(0xFFF3F5F7)
        assertThat(DietaBotHex.darkCtaText).isEqualTo(0xFF111111)
        assertThat(DietaBotHex.lightCtaBg).isEqualTo(0xFF111111)
        assertThat(DietaBotHex.lightCtaText).isEqualTo(0xFFF3F5F7)
    }

    @Test
    fun darkAndLightHexes() {
        assertThat(DietaBotHex.darkBg).isEqualTo(0xFF0B0D10)
        assertThat(DietaBotHex.darkGold).isEqualTo(0xFFE8B86D)
        assertThat(DietaBotHex.lightBg).isEqualTo(0xFFF4F3F0)
        assertThat(DietaBotHex.lightGold).isEqualTo(0xFFB8873D)
        assertThat(darkPalette.ctaBg).isEqualTo(Color(DietaBotHex.darkCtaBg))
        assertThat(lightPalette.gold).isEqualTo(Color(DietaBotHex.lightGold))
    }

    @Test
    fun semanticMacros() {
        // Dark semantic macros
        assertThat(DietaBotHex.darkProtein).isEqualTo(0xFF4EC994)
        assertThat(DietaBotHex.darkCarbs).isEqualTo(0xFFE58E42)
        assertThat(DietaBotHex.darkFat).isEqualTo(0xFFE8B86D)
        assertThat(DietaBotHex.darkBad).isEqualTo(0xFFE07A6A)
        assertThat(DietaBotHex.darkGood).isEqualTo(0xFF7DDA9A)

        // Light semantic macros
        assertThat(DietaBotHex.lightProtein).isEqualTo(0xFF1B7A4B)
        assertThat(DietaBotHex.lightCarbs).isEqualTo(0xFFC2651E)
        assertThat(DietaBotHex.lightFat).isEqualTo(0xFFB8873D)
        assertThat(DietaBotHex.lightBad).isEqualTo(0xFFC14D40)
        assertThat(DietaBotHex.lightGood).isEqualTo(0xFF1F8A4C)

        // Palette bindings
        assertThat(darkPalette.protein).isEqualTo(Color(DietaBotHex.darkProtein))
        assertThat(darkPalette.carbs).isEqualTo(Color(DietaBotHex.darkCarbs))
        assertThat(darkPalette.fat).isEqualTo(Color(DietaBotHex.darkFat))
        assertThat(darkPalette.bad).isEqualTo(Color(DietaBotHex.darkBad))
        assertThat(darkPalette.good).isEqualTo(Color(DietaBotHex.darkGood))

        assertThat(lightPalette.protein).isEqualTo(Color(DietaBotHex.lightProtein))
        assertThat(lightPalette.carbs).isEqualTo(Color(DietaBotHex.lightCarbs))
        assertThat(lightPalette.fat).isEqualTo(Color(DietaBotHex.lightFat))
        assertThat(lightPalette.bad).isEqualTo(Color(DietaBotHex.lightBad))
        assertThat(lightPalette.good).isEqualTo(Color(DietaBotHex.lightGood))
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
        assertThat(SplashBoot.COPY).isEqualTo("Estimativa nutricional, não substitui consulta médica ou nutricional.")
        assertThat(SplashBoot.WORDMARK).isEqualTo("Dieta Bot")
    }
}
