package com.nutri.android.ui

import com.google.common.truth.Truth.assertThat
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
        assertThat(darkPalette.ctaBg).isEqualTo(androidx.compose.ui.graphics.Color(NutriHex.darkCtaBg))
        assertThat(lightPalette.gold).isEqualTo(androidx.compose.ui.graphics.Color(NutriHex.lightGold))
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
