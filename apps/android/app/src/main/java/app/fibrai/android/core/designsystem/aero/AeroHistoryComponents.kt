package app.fibrai.android.core.designsystem.aero

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.fibrai.android.core.designsystem.Haptic
import app.fibrai.android.core.designsystem.dietaClick

/*
 * D28 components of the Figma `Design` (Componentes, section "Home history · D28"): Home/DayCircle, Home/DayStrip and
 * Card/Extra (ADR-058).
 */

/** The five states of Home/DayCircle. */
enum class AeroDayState { TodaySelected, Today, PastSelected, Past, NoRecord }

/** One item of Home/DayStrip: the day number, the month above it where it changes, its state and its label. */
data class AeroStripDay(val key: String, val day: String, val month: String?, val state: AeroDayState, val label: String)

/**
 * Home/DayCircle: a 36 dp circle with the day (Caption/Strong) under 20 dp kept for the month abbreviation (Label/Section,
 * text/muted, centred). today-selected: accent fill; today: glass with a 2 dp accent ring; past-selected: surface/selected with
 * the 2 dp border/selected ring; past: glass; no-record: glass with a dashed border/line ring and a text/dim number.
 */
@Composable
fun AeroDayCircle(day: AeroStripDay, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Aero.colors
    val type = Aero.type
    Column(
        modifier
            .width(36.dp)
            .semantics {
                contentDescription = day.label
                selected = day.state == AeroDayState.TodaySelected || day.state == AeroDayState.PastSelected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.height(20.dp).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            if (day.month != null) {
                AeroText(
                    AeroTextTokens.labelSection.cased(day.month),
                    style = type.labelSection.copy(color = c.textMuted, textAlign = TextAlign.Center),
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
        val circle = when (day.state) {
            AeroDayState.TodaySelected -> Modifier.clip(CircleShape).background(c.accentDefault)
            AeroDayState.Today -> Modifier.aeroGlass(CircleShape).border(2.dp, c.accentDefault, CircleShape)
            AeroDayState.PastSelected -> Modifier.clip(CircleShape).background(c.surfaceSelected).border(2.dp, c.borderSelected, CircleShape)
            AeroDayState.Past -> Modifier.aeroGlass(CircleShape)
            AeroDayState.NoRecord -> Modifier.aeroGlass(CircleShape, border = c.borderLine.copy(alpha = 0f))
        }
        Box(
            Modifier.size(36.dp).then(circle).dietaClick(Haptic.Light, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (day.state == AeroDayState.NoRecord) {
                Canvas(Modifier.size(36.dp)) {
                    val stroke = 1.5.dp.toPx()
                    drawCircle(
                        c.borderLine,
                        radius = size.minDimension / 2 - stroke / 2,
                        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))),
                    )
                }
            }
            AeroText(
                day.day,
                style = type.captionStrong.copy(
                    color = when (day.state) {
                        AeroDayState.TodaySelected -> c.accentOn
                        AeroDayState.NoRecord -> c.textDim
                        else -> c.textPrimary
                    },
                ),
                maxLines = 1,
            )
        }
    }
}

/**
 * Home/DayStrip: the days 10 dp apart, packed to the right with today at the right end; it scrolls to the left. Items are
 * given oldest first.
 */
@Composable
fun AeroDayStrip(days: List<AeroStripDay>, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier.fillMaxWidth().height(56.dp).testTag("home-strip"),
        reverseLayout = true,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
    ) {
        items(days.asReversed(), key = { it.key }) { day ->
            AeroDayCircle(day, onClick = { onSelect(day.key) }, modifier = Modifier.testTag("home-day-${day.key}"))
        }
    }
}

/**
 * Card/Extra: `Extra · {HH:mm}` (Body/Strong), the text (Body) and `{kcal} kcal · {p}P · {c}C · {g}G` (Caption/Strong, the
 * macros in their colours). A glass card, radius/card, 20 dp padding across, 16 dp down.
 */
@Composable
fun AeroExtraCard(title: String, text: String, kcal: Int, p: Int, cG: Int, g: Int, modifier: Modifier = Modifier) {
    val c = Aero.colors
    val type = Aero.type
    Column(
        modifier.fillMaxWidth().aeroGlass(Aero.shapes.card).padding(horizontal = 21.dp, vertical = 17.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AeroText(title, style = type.bodyStrong.copy(color = c.textPrimary), maxLines = 1)
        AeroText(text, style = type.body.copy(color = c.textPrimary))
        AeroText(
            buildAnnotatedString {
                withStyle(SpanStyle(color = c.textPrimary)) { append("$kcal kcal") }
                withStyle(SpanStyle(color = c.textDim)) { append(" · ") }
                withStyle(SpanStyle(color = c.macroProtein)) { append("${p}P") }
                withStyle(SpanStyle(color = c.textDim)) { append(" · ") }
                withStyle(SpanStyle(color = c.macroCarbs)) { append("${cG}C") }
                withStyle(SpanStyle(color = c.textDim)) { append(" · ") }
                withStyle(SpanStyle(color = c.macroFat)) { append("${g}G") }
            },
            style = type.captionStrong,
            maxLines = 1,
        )
    }
}
