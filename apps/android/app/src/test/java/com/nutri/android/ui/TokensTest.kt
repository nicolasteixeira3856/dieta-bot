package com.nutri.android.ui

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.nutri.android.core.designsystem.SplashBoot
import com.nutri.android.core.designsystem.aero.AeroColors
import com.nutri.android.core.designsystem.aero.AeroDarkColors
import com.nutri.android.core.designsystem.aero.AeroLightColors
import com.nutri.android.core.designsystem.formatRemaining
import org.junit.Test

/** Aero tokens (generated from docs/design/tokens.json) and the shared formatters. */
class TokensTest {
    @Test
    fun accentIsNotAMacroColour() {
        for (c in listOf(AeroDarkColors, AeroLightColors)) {
            assertThat(listOf(c.macroProtein, c.macroCarbs, c.macroFat)).doesNotContain(c.accentDefault)
        }
    }

    /** Every number and label on the page clears WCAG AA 4.5:1 against bg/page in both themes. */
    @Test
    fun wcagContrastOnPage() {
        fun luminance(color: Color): Double {
            fun channel(c: Float): Double = if (c <= 0.03928f) (c / 12.92).toDouble() else Math.pow(((c + 0.055) / 1.055).toDouble(), 2.4)
            return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
        }

        fun contrast(c1: Color, c2: Color): Double {
            val l1 = luminance(c1)
            val l2 = luminance(c2)
            return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
        }

        fun check(c: AeroColors) {
            assertThat(contrast(c.textPrimary, c.bgPage)).isAtLeast(10.0)
            listOf(c.textMuted, c.textDim, c.accentDefault, c.statusBad, c.statusGood, c.macroProtein, c.macroCarbs, c.macroFat)
                .forEach { assertThat(contrast(it, c.bgPage)).isAtLeast(4.5) }
        }
        check(AeroDarkColors)
        check(AeroLightColors)
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
