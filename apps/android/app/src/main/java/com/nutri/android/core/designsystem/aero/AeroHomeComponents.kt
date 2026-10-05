package com.nutri.android.core.designsystem.aero

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nutri.android.core.designsystem.Haptic
import com.nutri.android.core.designsystem.dietaClick

/*
 * D3 components of the Figma `Design` (Componentes, sections "Cabeçalho e anel" and "Campos e sheets"):
 * Header/Day, Ring/Day, Field/Number, Sheet/Bottom, plus the edge bubble decoration of the Release 1 frames.
 */

/** Header/Day: day label, date and the Config glass button. */
@Composable
fun AeroHeaderDay(
    day: String,
    date: String,
    onConfig: () -> Unit,
    modifier: Modifier = Modifier,
    dateTag: String? = null,
    configTag: String? = null,
) {
    val c = Aero.colors
    val type = Aero.type
    Row(modifier.fillMaxWidth().height(46.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AeroText(AeroTextTokens.labelSection.cased(day), style = type.labelSection.copy(color = c.textMuted))
            AeroText(date, if (dateTag != null) Modifier.testTag(dateTag) else Modifier, style = type.title.copy(color = c.textPrimary))
        }
        Spacer(Modifier.width(12.dp))
        AeroIconButton(
            AeroIconName.Gear,
            onConfig,
            contentDescription = "Configurações",
            modifier = if (configTag != null) Modifier.testTag(configTag) else Modifier,
        )
    }
}

enum class AeroRingState { Normal, Exceeded, Empty }

/**
 * Ring/Day: 196 dp ring (inner radius 88 %) over the day's meta, the consumed kcal in Hero/Number, "kcal consumidas"
 * and the meta pill. Exceeded draws the full ring in status/bad.
 */
@Composable
fun AeroRingDay(
    consumed: String,
    meta: String,
    progress: Float,
    state: AeroRingState,
    modifier: Modifier = Modifier,
    consumedTag: String? = null,
    metaTag: String? = null,
) {
    val c = Aero.colors
    val type = Aero.type
    Box(modifier.size(RING), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(RING)) {
            val thickness = size.minDimension / 2 * (1 - RING_INNER)
            val inset = thickness / 2
            val arcSize = Size(size.width - thickness, size.height - thickness)
            val stroke = Stroke(thickness, cap = StrokeCap.Butt)
            drawArc(c.surface2, 0f, 360f, false, Offset(inset, inset), arcSize, style = stroke)
            val sweep = when (state) {
                AeroRingState.Exceeded -> 360f
                AeroRingState.Empty -> 0f
                AeroRingState.Normal -> 360f * progress.coerceIn(0f, 1f)
            }
            if (sweep > 0f) {
                drawArc(
                    if (state == AeroRingState.Exceeded) c.statusBad else c.accentDefault,
                    -90f, sweep, false, Offset(inset, inset), arcSize, style = stroke,
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AeroText(consumed, if (consumedTag != null) Modifier.testTag(consumedTag) else Modifier, style = type.heroNumber.copy(color = c.textPrimary))
            AeroText("kcal consumidas", style = type.caption.copy(color = c.textMuted))
            AeroChipLog(meta, if (metaTag != null) Modifier.testTag(metaTag) else Modifier, tone = AeroChipTone.Neutral)
        }
    }
}

private val RING = 196.dp
private const val RING_INNER = 0.88f

/**
 * Field/Number (Large): glass box with the number in Field/Number, its unit, a trailing icon and a helper line.
 * The border turns accent while focused. The caller owns filtering; an empty value shows a dim "0".
 */
@Composable
fun AeroFieldNumber(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    unit: String,
    helper: String,
    modifier: Modifier = Modifier,
    icon: AeroIconName? = null,
    focusRequester: FocusRequester = remember { FocusRequester() },
    fieldTag: String? = null,
    helperTag: String? = null,
    /** On a sheet or card that is already glass: the glass fill alone. */
    onGlass: Boolean = false,
) {
    val c = Aero.colors
    val type = Aero.type
    var focused by remember { mutableStateOf(false) }
    val shape = Aero.shapes.card
    val stroke = if (focused) 1.5.dp else 1.dp
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(58.dp + (stroke - 1.dp))
                .aeroGlass(shape, border = if (focused) c.accentDefault else c.borderLine, borderWidth = stroke, shadow = false, backdropBlurred = onGlass)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    runCatching { focusRequester.requestFocus() }
                }
                .padding(horizontal = 18.dp + stroke),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = type.fieldNumber.copy(color = c.textPrimary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                cursorBrush = SolidColor(c.accentDefault),
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused }
                    .then(if (fieldTag != null) Modifier.testTag(fieldTag) else Modifier),
                decorationBox = { inner ->
                    Box {
                        if (value.text.isEmpty()) AeroText("0", style = type.fieldNumber.copy(color = c.textDim))
                        inner()
                    }
                },
            )
            AeroText(unit, style = type.body.copy(color = c.textMuted))
            Spacer(Modifier.weight(1f))
            if (icon != null) AeroIcon(icon, c.iconPrimary)
        }
        AeroText(helper, if (helperTag != null) Modifier.testTag(helperTag) else Modifier, style = type.caption.copy(color = c.textMuted))
    }
}

/**
 * Sheet/Bottom: glass sheet (top radius radius/sheet) with a grabber, a Title, the content and the action pair
 * (Button/Primary, absent when [primary] is null, + a surface/2 secondary pill). It sits over a blurred screen, so its glass is the fill alone.
 */
@Composable
fun AeroSheet(
    title: String,
    primary: String?,
    onPrimary: () -> Unit,
    secondary: String,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
    primaryEnabled: Boolean = true,
    primaryTag: String? = null,
    secondaryTag: String? = null,
    bottomInset: Dp = 0.dp,
    primaryIcon: AeroIconName? = null,
    content: @Composable () -> Unit,
) {
    val c = Aero.colors
    val type = Aero.type
    Column(
        modifier
            .fillMaxWidth()
            .aeroGlass(Aero.shapes.sheet, backdropBlurred = true)
            .aeroSwallowTaps()
            .padding(start = 25.dp, end = 25.dp, top = 13.dp, bottom = 32.dp + bottomInset),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(40.dp, 5.dp)
                .clip(CircleShape)
                .background(c.borderLine),
        )
        AeroText(title, style = type.title.copy(color = c.textPrimary))
        // A46: when the room above the keyboard is short, the content shrinks (and scrolls, if it can) so the title
        // and the actions stay on screen.
        Box(Modifier.weight(1f, fill = false)) { content() }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (primary != null) {
                AeroButtonPrimary(
                    primary,
                    onPrimary,
                    modifier = if (primaryTag != null) Modifier.testTag(primaryTag) else Modifier,
                    icon = primaryIcon,
                    enabled = primaryEnabled,
                )
            }
            AeroSecondaryPill(secondary, onSecondary, if (secondaryTag != null) Modifier.testTag(secondaryTag) else Modifier)
        }
    }
}

/** Edge bubble of the Release 1 frames: radial sheen and a glass border, decoration only, never behind text. */
@Composable
fun AeroBubble(size: Dp, modifier: Modifier = Modifier) {
    val c = Aero.colors
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2
        drawCircle(Brush.radialGradient(listOf(c.sheenStart, c.sheenEnd), center, r), r)
        drawCircle(c.borderGlass, r - 0.75.dp.toPx(), style = Stroke(1.5.dp.toPx()))
    }
}
