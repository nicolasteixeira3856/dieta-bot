package app.fibrai.android.core.designsystem

object SplashBoot {
    const val MAX_MS = 2000L
    const val DELAY_MS = 1200L
    /** Disclaimer shown on the Home (perfil-onboarding rule 7). The splash no longer shows it (A51). */
    const val COPY = "Estimativa nutricional, não substitui consulta médica ou nutricional."
    /** Accessibility label of the splash logo: the name is not drawn on the splash since A51. */
    const val LOGO_DESCRIPTION = "Fibrai"
    /** Logo alone, centered, 160 dp tall on the splash (Figma gold `splash`, D13). logo_mark is the tight square crop. */
    const val LOGO_DP = 160
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
