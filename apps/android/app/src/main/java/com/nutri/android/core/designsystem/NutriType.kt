package com.nutri.android.core.designsystem

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nutri.android.R

private fun variable(res: Int, weight: Int) = Font(
    resId = res,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Titles and display numbers. Stitch `Nutri`: Plus Jakarta Sans. */
val Jakarta = FontFamily(
    variable(R.font.plus_jakarta_sans, 400),
    variable(R.font.plus_jakarta_sans, 500),
    variable(R.font.plus_jakarta_sans, 600),
    variable(R.font.plus_jakarta_sans, 700),
    variable(R.font.plus_jakarta_sans, 800),
)

/** Body, labels and data. Stitch `Nutri`: Inter. */
val Inter = FontFamily(
    variable(R.font.inter, 400),
    variable(R.font.inter, 500),
    variable(R.font.inter, 600),
    variable(R.font.inter, 700),
)

/** Type scale from the Stitch `Nutri` design system (tailwind fontSize). */
object NutriType {
    /** CSS-like line boxes: half-leading on both sides, never trimmed. */
    val cssLines = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

    val displayLg = TextStyle(fontFamily = Jakarta, fontSize = 48.sp, lineHeight = 54.sp, letterSpacing = (-0.03).em, fontWeight = FontWeight.W600, lineHeightStyle = cssLines)
    val headlineLg = TextStyle(fontFamily = Jakarta, fontSize = 30.sp, lineHeight = 38.sp, letterSpacing = (-0.02).em, fontWeight = FontWeight.W600, lineHeightStyle = cssLines)
    val headlineMd = TextStyle(fontFamily = Jakarta, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.015).em, fontWeight = FontWeight.W600, lineHeightStyle = cssLines)
    val bodyLg = TextStyle(fontFamily = Inter, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.W400, lineHeightStyle = cssLines)
    val bodyMd = TextStyle(fontFamily = Inter, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.01.em, fontWeight = FontWeight.W400, lineHeightStyle = cssLines)
    val labelLg = TextStyle(fontFamily = Inter, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.02.em, fontWeight = FontWeight.W600, lineHeightStyle = cssLines)
    val labelMd = TextStyle(fontFamily = Inter, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.05.em, fontWeight = FontWeight.W500, lineHeightStyle = cssLines)
    val labelCaps = TextStyle(fontFamily = Inter, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.14.em, fontWeight = FontWeight.W600, lineHeightStyle = cssLines)
}
