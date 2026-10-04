package com.nutri.android.core.designsystem.aero

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.isSpecified
import kotlin.math.roundToInt

/**
 * Figma line box: a text block is exactly lines × line height, the glyphs centred in it. Compose keeps the font's
 * natural height when the line height is smaller than it (Nunito Sans Hero/Number 34/40 measures 47 dp, Title
 * 22/28 measures 30 dp), which would push every block below a title down.
 */
fun Modifier.aeroLineBox(style: TextStyle): Modifier = if (!style.lineHeight.isSpecified) this else layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val line = style.lineHeight.toPx()
    val lines = (placeable.height / line).roundToInt().coerceAtLeast(1)
    val box = (lines * line).roundToInt()
    val extra = placeable.height - box
    if (extra <= 0) {
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    } else {
        layout(placeable.width, box) { placeable.place(0, -extra / 2) }
    }
}

/** BasicText with the Figma line box ([aeroLineBox]). */
@Composable
fun AeroText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
) {
    BasicText(text, modifier.aeroLineBox(style), style, onTextLayout, overflow, softWrap, maxLines)
}

/** BasicText with the Figma line box ([aeroLineBox]), for styled spans. */
@Composable
fun AeroText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
) {
    BasicText(text, modifier.aeroLineBox(style), style, onTextLayout, overflow, softWrap, maxLines)
}
