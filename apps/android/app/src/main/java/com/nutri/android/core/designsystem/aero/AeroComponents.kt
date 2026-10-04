package com.nutri.android.core.designsystem.aero

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nutri.android.core.designsystem.Haptic
import com.nutri.android.core.designsystem.dietaClick
import androidx.compose.foundation.text.BasicText as Text

/*
 * Compose versions of the Figma `Design` components (page Componentes), same names. Sizes and paddings come from
 * the component properties; colors, radii, type and effects from the tokens. Each takes state and lambdas only.
 * Figma strokes are inside and counted in the layout, so a padding includes the border width.
 */

private val pill = RoundedCornerShape(percent = 50)

/** Disabled look (38 %) by a save layer: the JVM render does not draw Modifier.alpha. */
fun Modifier.aeroDisabled(enabled: Boolean): Modifier = if (enabled) this else aeroLayerAlpha(0.38f)

/** Figma layer opacity by a save layer, inflated so the glass shadow outside the bounds is kept. */
fun Modifier.aeroLayerAlpha(alpha: Float): Modifier =
    if (alpha >= 1f) this else drawWithContent {
        val bleed = AeroEffects.glassShadowRadius.toPx() * 2
        drawContext.canvas.saveLayer(
            Rect(Offset(-bleed, -bleed), Offset(size.width + bleed, size.height + bleed)),
            Paint().apply { this.alpha = alpha },
        )
        drawContent()
        drawContext.canvas.restore()
    }

/** Button/Primary: the glossy pill CTA, one per screen. */
@Composable
fun AeroButtonPrimary(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: AeroIconName? = null,
    enabled: Boolean = true,
    fillWidth: Boolean = true,
) {
    val c = Aero.colors
    Row(
        modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .height(58.dp)
            .aeroDisabled(enabled)
            .aeroShadow(pill, c.shadowGlass, radius = 18.dp, offsetY = 6.dp)
            .clip(pill)
            .background(c.accentDefault)
            .aeroGloss(pill)
            .border(1.dp, c.borderGlass, pill)
            .dietaClick(Haptic.Confirm, enabled = enabled, onClick = onClick)
            .padding(horizontal = 25.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Aero.type.button.copy(color = c.accentOn), maxLines = 1)
        if (icon != null) AeroIcon(icon, c.accentOn)
    }
}

/** IconButton/Glass: 44 dp glass circle with a 24 dp icon. */
@Composable
fun AeroIconButton(
    icon: AeroIconName,
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Aero.colors.iconPrimary,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .size(44.dp)
            .aeroGlass(CircleShape)
            .dietaClick(Haptic.Light, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AeroIcon(icon, tint, contentDescription = contentDescription)
    }
}

enum class AeroChipTone { Accent, Neutral, Bad }

/** Chip/Log: one-line pill. Accent = consolidated meal log, Neutral = day meta, Bad = over the meta (with a dot). */
@Composable
fun AeroChipLog(
    text: String,
    modifier: Modifier = Modifier,
    tone: AeroChipTone = AeroChipTone.Accent,
    dot: Boolean = tone == AeroChipTone.Bad,
) {
    val c = Aero.colors
    val (fill, stroke, ink) = when (tone) {
        AeroChipTone.Accent -> Triple(c.surfaceTint, c.borderGlass, c.accentDefault)
        AeroChipTone.Neutral -> Triple(c.surface2, c.borderGlass, c.textMuted)
        AeroChipTone.Bad -> Triple(c.statusBadTint, c.statusBad, c.statusBad)
    }
    Row(
        modifier
            .height(32.dp)
            .clip(pill)
            .background(fill)
            .border(1.dp, stroke, pill)
            .padding(horizontal = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot) Box(Modifier.size(6.dp).background(c.statusBad, CircleShape))
        Text(text, style = Aero.type.captionStrong.copy(color = ink), maxLines = 1)
    }
}

/** A glossy bar fill on a surface/2 track, [progress] 0..1. */
@Composable
private fun AeroBar(progress: Float, color: Color, modifier: Modifier = Modifier) {
    val c = Aero.colors
    val bar = RoundedCornerShape(AeroDimens.sizeBar / 2)
    Box(modifier.fillMaxWidth().height(AeroDimens.sizeBar).clip(bar).background(c.surface2)) {
        if (progress > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(bar)
                    .background(color)
                    .aeroGloss(bar),
            )
        }
    }
}

/** Progress/Bar: onboarding step progress. */
@Composable
fun AeroProgressBar(progress: Float, modifier: Modifier = Modifier) {
    AeroBar(progress, Aero.colors.accentDefault, modifier)
}

enum class AeroMacro { Protein, Carbs, Fat }

/**
 * Macro/Row: dot, label, consumed / target and a glossy bar in the semantic macro color. Over the target the dot,
 * bar and consumed value turn status/bad.
 */
@Composable
fun AeroMacroRow(
    macro: AeroMacro,
    label: String,
    consumed: String,
    target: String,
    progress: Float,
    over: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = Aero.colors
    val semantic = when (macro) {
        AeroMacro.Protein -> c.macroProtein
        AeroMacro.Carbs -> c.macroCarbs
        AeroMacro.Fat -> c.macroFat
    }
    val tone = if (over) c.statusBad else semantic
    val type = Aero.type
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().height(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(tone, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(label, Modifier.weight(1f), style = type.captionStrong.copy(color = c.textPrimary), maxLines = 1)
            Spacer(Modifier.width(8.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = if (over) c.statusBad else c.textPrimary)) { append(consumed) }
                    withStyle(SpanStyle(color = c.textMuted, fontWeight = type.caption.fontWeight)) { append(" / $target") }
                },
                style = type.captionStrong,
                maxLines = 1,
            )
        }
        AeroBar(progress, tone)
    }
}

enum class AeroNodeState { Done, Photo, Skipped, Active, Over, Empty }

/** Timeline/Node: 24 dp marker on the continuous timeline guide. */
@Composable
fun AeroTimelineNode(state: AeroNodeState, modifier: Modifier = Modifier) {
    val c = Aero.colors
    val ring = when (state) {
        AeroNodeState.Done -> c.statusGood
        AeroNodeState.Photo, AeroNodeState.Active -> c.accentDefault
        AeroNodeState.Skipped, AeroNodeState.Empty -> c.textDim
        AeroNodeState.Over -> c.statusBad
    }
    val fill = if (state == AeroNodeState.Over) c.statusBadTint else c.bgPage
    val glow = if (state == AeroNodeState.Active) {
        Modifier.dropShadow(CircleShape, Shadow(radius = 8.dp, color = c.accentDefault))
    } else {
        Modifier
    }
    Box(modifier.size(24.dp).then(glow), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(24.dp)) {
            val stroke = 2.dp.toPx()
            drawCircle(c.bgPage)
            drawCircle(fill)
            drawCircle(
                ring,
                radius = size.minDimension / 2 - stroke / 2,
                style = Stroke(
                    width = stroke,
                    pathEffect = if (state == AeroNodeState.Skipped) PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())) else null,
                ),
            )
        }
        when (state) {
            AeroNodeState.Done -> AeroIcon(AeroIconName.Check, ring, size = 14.dp)
            AeroNodeState.Photo -> AeroIcon(AeroIconName.Camera, ring, size = 14.dp)
            AeroNodeState.Skipped -> AeroIcon(AeroIconName.Minus, ring, size = 14.dp)
            AeroNodeState.Over -> AeroIcon(AeroIconName.ExclamationMark, ring, size = 14.dp)
            AeroNodeState.Active, AeroNodeState.Empty -> Box(Modifier.size(10.dp).background(ring, CircleShape))
        }
    }
}

enum class AeroMealState { Logged, Over, Skipped, Pending, Empty }

/** One log line of a meal card: description and its kcal ("520 kcal"). */
@androidx.compose.runtime.Immutable
data class AeroMealLine(val text: String, val kcal: String)

/**
 * Card/Meal: one timeline card per slot. Logged/Over show the description, kcal and one consolidated [log] chip;
 * Skipped and Pending/Empty show [description] as their one-line note.
 */
@Composable
fun AeroMealCard(
    state: AeroMealState,
    meal: String,
    time: String,
    description: String,
    modifier: Modifier = Modifier,
    kcal: String = "",
    log: String = "",
    onClick: (() -> Unit)? = null,
    lines: List<AeroMealLine> = listOf(AeroMealLine(description, kcal)),
    logTag: String? = null,
) {
    val c = Aero.colors
    val type = Aero.type
    val shape = Aero.shapes.card
    val border = when (state) {
        AeroMealState.Over -> c.statusBad
        AeroMealState.Pending -> c.accentDefault
        else -> c.borderGlass
    }
    val logged = state == AeroMealState.Logged || state == AeroMealState.Over
    Column(
        modifier
            .fillMaxWidth()
            .aeroLayerAlpha(if (state == AeroMealState.Skipped) 0.6f else 1f)
            .aeroGlass(shape, border = border)
            .then(if (onClick != null) Modifier.dietaClick(Haptic.Light, onClick = onClick) else Modifier)
            .padding(horizontal = 21.dp, vertical = 17.dp),
        verticalArrangement = Arrangement.spacedBy(if (logged) 12.dp else 8.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(28.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                meal,
                Modifier.weight(1f),
                style = type.title.copy(color = if (state == AeroMealState.Skipped) c.textMuted else c.textPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            Text(time, style = type.caption.copy(color = c.textDim))
            if (state == AeroMealState.Pending || state == AeroMealState.Empty) {
                Spacer(Modifier.width(8.dp))
                AeroIcon(
                    AeroIconName.CaretRight,
                    if (state == AeroMealState.Pending) c.accentDefault else c.textDim,
                    size = 20.dp,
                )
            }
        }
        when (state) {
            AeroMealState.Logged, AeroMealState.Over -> {
                lines.forEach { line ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(line.text, Modifier.weight(1f), style = type.body.copy(color = c.textPrimary))
                        Text(line.kcal, style = type.captionStrong.copy(color = if (state == AeroMealState.Over) c.statusBad else c.textMuted))
                    }
                }
                AeroChipLog(
                    log,
                    modifier = if (logTag != null) Modifier.testTag(logTag) else Modifier,
                    tone = if (state == AeroMealState.Over) AeroChipTone.Bad else AeroChipTone.Accent,
                    dot = false,
                )
            }
            AeroMealState.Skipped -> Text(
                description,
                style = type.caption.copy(color = c.textDim, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
            )
            AeroMealState.Pending, AeroMealState.Empty -> Text(
                description,
                style = type.caption.copy(color = if (state == AeroMealState.Pending) c.accentDefault else c.textMuted),
            )
        }
    }
}

/** Option/Card: radio card. [extra] holds an optional nested control (the typed % field). */
@Composable
fun AeroOptionCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    extra: (@Composable () -> Unit)? = null,
) {
    val c = Aero.colors
    val type = Aero.type
    val stroke = if (selected) 1.5.dp else 1.dp
    Row(
        modifier
            .fillMaxWidth()
            .aeroGlass(Aero.shapes.card, border = if (selected) c.accentDefault else c.borderGlass, borderWidth = stroke)
            .dietaClick(Haptic.Light, onClick = onClick)
            .padding(20.dp + stroke),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .then(
                    if (selected) {
                        Modifier.background(c.accentDefault).aeroGloss(CircleShape)
                    } else {
                        Modifier.border(1.5.dp, c.borderLine, CircleShape)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(8.dp).background(c.accentOn, CircleShape))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = type.bodyStrong.copy(color = c.textPrimary))
                if (badge != null) {
                    Box(
                        Modifier
                            .clip(pill)
                            .background(c.surfaceTint)
                            .border(1.dp, c.borderGlass, pill)
                            .padding(horizontal = 11.dp, vertical = 4.dp),
                    ) {
                        Text(AeroTextTokens.labelSection.cased(badge), style = type.labelSection.copy(color = c.accentDefault))
                    }
                }
            }
            Text(description, style = type.caption.copy(color = c.textMuted))
            if (extra != null) extra()
        }
    }
}

/** Chat/Bubble: user (tinted glass, read ticks) or bot (glass), max 280 dp wide. */
@Composable
fun AeroChatBubble(
    text: String,
    time: String,
    fromUser: Boolean,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 280.dp,
) {
    val c = Aero.colors
    val type = Aero.type
    val r = AeroDimens.radiusCard
    val tail = 6.dp
    val shape = if (fromUser) {
        RoundedCornerShape(topStart = r, topEnd = r, bottomEnd = tail, bottomStart = r)
    } else {
        RoundedCornerShape(topStart = r, topEnd = r, bottomEnd = r, bottomStart = tail)
    }
    Column(
        modifier
            .widthIn(max = maxWidth)
            .aeroGlass(shape, fill = if (fromUser) c.surfaceTint else c.surfaceGlass)
            .padding(horizontal = 19.dp, vertical = 15.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text, style = type.body.copy(color = c.textPrimary))
        Row(
            Modifier.align(Alignment.End).height(18.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(time, style = type.caption.copy(color = c.textDim))
            if (fromUser) AeroIcon(AeroIconName.Checks, c.accentDefault, size = 14.dp)
        }
    }
}

/**
 * Chat/Composer: camera, text field and send. Default is a glass pill; it becomes a 20 dp card when the text wraps
 * or a photo is attached; [error] (TooLong) turns the border status/bad, mutes camera and send and shows the error
 * under the box.
 */
@Composable
fun AeroComposer(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onCamera: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    sendEnabled: Boolean = value.isNotBlank(),
    error: String? = null,
    attachment: ImageBitmap? = null,
    onRemoveAttachment: () -> Unit = {},
) {
    val c = Aero.colors
    val type = Aero.type
    var lines by remember { mutableIntStateOf(1) }
    val tall = lines > 1 || error != null || attachment != null
    val shape = if (tall) Aero.shapes.card else pill
    val muted = error != null
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .aeroGlass(shape, border = if (muted) c.statusBad else c.borderGlass, borderWidth = if (muted) 1.5.dp else 1.dp)
                .padding(
                    PaddingValues(
                        start = if (muted) 13.5.dp else 9.dp,
                        end = if (muted) 13.5.dp else 9.dp,
                        top = if (attachment != null) 15.dp else if (muted) 13.5.dp else 9.dp,
                        bottom = if (muted) 13.5.dp else 9.dp,
                    ),
                ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (attachment != null) {
                Box(Modifier.size(82.dp, 72.dp)) {
                    Image(
                        attachment,
                        contentDescription = null,
                        modifier = Modifier
                            .offset(6.dp, 8.dp)
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, c.borderLine, RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        Modifier
                            .offset(56.dp, 0.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(c.surface2)
                            .border(1.dp, c.borderLine, CircleShape)
                            .dietaClick(Haptic.Light, onClick = onRemoveAttachment)
                            .testTag("composer-remove-attachment"),
                        contentAlignment = Alignment.Center,
                    ) {
                        AeroIcon(AeroIconName.X, c.iconPrimary, size = 14.dp)
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = if (tall) Alignment.Bottom else Alignment.CenterVertically,
            ) {
                AeroIconButton(
                    AeroIconName.Camera,
                    onCamera,
                    contentDescription = "Foto",
                    tint = if (muted) c.textDim else c.iconPrimary,
                )
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 24.dp, max = 120.dp)
                        .align(Alignment.CenterVertically)
                        .testTag("composer-field"),
                    textStyle = type.body.copy(color = c.textPrimary),
                    cursorBrush = SolidColor(c.accentDefault),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    onTextLayout = { lines = it.lineCount },
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty()) {
                                Text(placeholder, style = type.body.copy(color = c.textDim), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            inner()
                        }
                    },
                )
                val sendOn = sendEnabled && !muted
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .then(if (sendOn) Modifier.background(c.accentDefault).aeroGloss(CircleShape) else Modifier.background(c.surface2))
                        .dietaClick(Haptic.Confirm, enabled = sendOn, onClick = onSend)
                        .testTag("composer-send"),
                    contentAlignment = Alignment.Center,
                ) {
                    AeroIcon(AeroIconName.ArrowUp, if (sendOn) c.accentOn else c.textDim, size = 20.dp)
                }
            }
        }
        if (error != null) {
            Text(error, Modifier.padding(start = 66.dp), style = type.caption.copy(color = c.statusBad))
        }
    }
}
