package com.nutri.android.core.designsystem.aero

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nutri.android.R

private fun nunito(weight: Int, italic: Boolean = false) = Font(
    resId = if (italic) R.font.nunito_sans_italic else R.font.nunito_sans,
    weight = FontWeight(weight),
    style = if (italic) FontStyle.Italic else FontStyle.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Nunito Sans (OFL, third_party/nunito-sans/OFL.txt), the variable font of the Figma text styles. */
val NunitoSans = FontFamily(
    nunito(400),
    nunito(600),
    nunito(700),
    nunito(400, italic = true),
)

/** Figma line boxes: half-leading on both sides, never trimmed. */
private val figmaLines = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

/** Tabular figures: every number in Aero uses them (ADR-030 § 3). */
private const val TABULAR = "tnum"

fun AeroTextToken.toTextStyle(tabular: Boolean = false) = TextStyle(
    fontFamily = NunitoSans,
    fontWeight = FontWeight(weight),
    fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = (letterSpacingPercent / 100f).em,
    lineHeightStyle = figmaLines,
    fontFeatureSettings = if (tabular) TABULAR else null,
)

/** Applies the style's Figma text case (Compose TextStyle has none). */
fun AeroTextToken.cased(text: String): String = if (uppercase) text.uppercase(java.util.Locale.ROOT) else text

/** The Figma text styles of the file `Design`, one property per style. */
@Immutable
data class AeroType(
    val heroNumber: TextStyle = AeroTextTokens.heroNumber.toTextStyle(tabular = true),
    val fieldNumber: TextStyle = AeroTextTokens.fieldNumber.toTextStyle(tabular = true),
    val title: TextStyle = AeroTextTokens.title.toTextStyle(),
    val body: TextStyle = AeroTextTokens.body.toTextStyle(),
    val bodyStrong: TextStyle = AeroTextTokens.bodyStrong.toTextStyle(),
    val caption: TextStyle = AeroTextTokens.caption.toTextStyle(),
    val captionStrong: TextStyle = AeroTextTokens.captionStrong.toTextStyle(tabular = true),
    val labelSection: TextStyle = AeroTextTokens.labelSection.toTextStyle(),
    val button: TextStyle = AeroTextTokens.button.toTextStyle(),
)
