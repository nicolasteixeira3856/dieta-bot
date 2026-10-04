package com.nutri.android.core.designsystem

object SplashBoot {
    const val MAX_MS = 2000L
    const val DELAY_MS = 1200L
    const val COPY = "Estimativa nutricional, não substitui consulta médica ou nutricional."
    const val WORDMARK = "Dieta Bot"
    /**
     * A14: logo above the wordmark (design/brand, tools/brand-icons.ps1). Stitch gold: a 120 dp box with the
     * uploaded icon.png (symbol ≈ half of it), 16 dp above the wordmark line box (A15, measured on the gold PNG). logo_mark is the tight crop, so it is
     * drawn at LOGO_DP inside the LOGO_BOX_DP box.
     */
    const val LOGO_BOX_DP = 120
    const val LOGO_DP = 60
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
