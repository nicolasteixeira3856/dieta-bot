package com.nutri.android.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object SplashBoot {
    const val MAX_MS = 2000L
    const val DELAY_MS = 1200L
    const val COPY = "Estimativa nutricional, não substitui consulta médica ou nutricional."
    const val WORDMARK = "Dieta Bot"
}

object DietaBotMeasure {
    const val remainingPt = 34
    const val fieldPt = 28
    const val sheetTopDp = 22
    const val cardDp = 14
    const val barDp = 6
}

object DietaBotShapes {
    val sheetTopRadius = RoundedCornerShape(topStart = DietaBotMeasure.sheetTopDp.dp, topEnd = DietaBotMeasure.sheetTopDp.dp)
    val cardRadius = RoundedCornerShape(DietaBotMeasure.cardDp.dp)
    val continuousBarHeight = DietaBotMeasure.barDp.dp
}

object DietaBotHex {
    // Dark Theme
    const val darkBg = 0xFF0B0D10
    const val darkPanel = 0xFF12151A
    const val darkPhone = 0xFF0E1114
    const val darkSurf = 0xFF171B20
    const val darkSurf2 = 0xFF1E242B
    const val darkLine = 0xFF2A3139
    const val darkText = 0xFFF3F5F7
    const val darkMuted = 0xFF8B939C
    const val darkDim = 0xFF5C6570
    const val darkGold = 0xFFE8B86D
    const val darkGood = 0xFF7DDA9A
    const val darkBad = 0xFFE07A6A
    const val darkCtaBg = 0xFFF3F5F7
    const val darkCtaText = 0xFF111111

    // Dark Semantic Macros
    const val darkProtein = 0xFF4EC994
    const val darkCarbs = 0xFFE58E42
    const val darkFat = 0xFFE8B86D

    // Light Theme
    const val lightBg = 0xFFF4F3F0
    const val lightPanel = 0xFFECEAE6
    const val lightPhone = 0xFFF7F6F3
    const val lightSurf = 0xFFFFFFFF
    const val lightSurf2 = 0xFFE8E6E2
    const val lightLine = 0xFFD5D2CC
    const val lightText = 0xFF14161A
    const val lightMuted = 0xFF5C636B
    const val lightDim = 0xFF8B939C
    const val lightGold = 0xFFB8873D
    const val lightGood = 0xFF1F8A4C
    const val lightBad = 0xFFC14D40
    const val lightCtaBg = 0xFF111111
    const val lightCtaText = 0xFFF3F5F7

    // Light Semantic Macros
    const val lightProtein = 0xFF1B7A4B
    const val lightCarbs = 0xFFC2651E
    const val lightFat = 0xFFB8873D
}

fun formatRemaining(n: Int): String {
    val s = n.toString()
    if (s.length <= 3) return s
    val out = StringBuilder()
    val rev = s.reversed()
    rev.forEachIndexed { i, c ->
        if (i > 0 && i % 3 == 0) out.append('.')
        out.append(c)
    }
    return out.reverse().toString()
}
