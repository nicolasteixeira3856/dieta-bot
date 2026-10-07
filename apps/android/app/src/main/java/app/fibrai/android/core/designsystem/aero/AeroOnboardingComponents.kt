package app.fibrai.android.core.designsystem.aero

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import app.fibrai.android.core.designsystem.Haptic
import app.fibrai.android.core.designsystem.dietaClick

/*
 * D4 components of the Figma `Design` (Componentes, section "Onboarding"): Stepper/Progress, Choice/Segmented,
 * Tabs/Weekday, Field/Number (Compact), Row/MealSlot, Card/MacroTarget, Card/Note, plus the selected-pill fill.
 */

private val pill = RoundedCornerShape(percent = 50)

/** Selected pill fill: accent with the glass sheen (paint Surface/Glass gradient), as in Stepper/Choice/Tabs. */
@Composable
fun Modifier.aeroAccentSheen(shape: Shape = pill): Modifier {
    val c = Aero.colors
    val brush = remember(c) { Brush.verticalGradient(*AeroPaints.surfaceGlassStops(c).map { it.position to it.color }.toTypedArray()) }
    return clip(shape).background(c.accentDefault).drawBehind { drawOutline(shape.createOutline(size, layoutDirection, this), brush) }
}

/** Stepper/Progress: five 8 dp segments (one per onboarding step, D20), the first [step] filled. */
@Composable
fun AeroStepper(step: Int, modifier: Modifier = Modifier, count: Int = 5) {
    val c = Aero.colors
    Row(modifier.fillMaxWidth().height(AeroDimens.sizeBar), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(AeroDimens.sizeBar)
                    .then(if (i < step) Modifier.aeroAccentSheen() else Modifier.clip(pill).background(c.surface2)),
            )
        }
    }
}

/** One option of a segmented control or a tab row. */
@Immutable
data class AeroChoice(val label: String, val tag: String? = null)

/**
 * Choice/Segmented: glass pill track (4 dp inset) with equal segments; the selected one is the accent pill with the
 * accent glow.
 */
@Composable
fun AeroSegmented(
    options: List<AeroChoice>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Aero.colors
    val type = Aero.type
    Row(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .aeroGlass(pill)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { i, option ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .then(
                        if (on) {
                            Modifier
                                .dropShadow(
                                    pill,
                                    Shadow(
                                        radius = AeroEffects.glowAccentShadowRadius,
                                        color = AeroEffects.glowAccentShadowColor(c),
                                        offset = DpOffset(AeroEffects.glowAccentShadowOffsetX, AeroEffects.glowAccentShadowOffsetY),
                                    ),
                                )
                                .aeroAccentSheen()
                        } else {
                            Modifier.clip(pill)
                        },
                    )
                    .dietaClick(Haptic.Light) { onSelect(i) }
                    .then(if (option.tag != null) Modifier.testTag(option.tag) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                AeroText(
                    option.label,
                    style = (if (on) type.bodyStrong.copy(color = c.accentOn) else type.body.copy(color = c.textMuted)).copy(textAlign = TextAlign.Center),
                    maxLines = 1,
                )
            }
        }
    }
}

/** Tabs/Weekday: wrapping pills, 8 dp apart; selected = accent pill, others = glass pills. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AeroTabs(
    options: List<AeroChoice>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Aero.colors
    val type = Aero.type
    FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, option ->
            val on = i == selected
            Box(
                Modifier
                    .height(if (on) 44.dp else 46.dp)
                    .then(if (on) Modifier.aeroAccentSheen() else Modifier.aeroGlass(pill, shadow = false))
                    .dietaClick(Haptic.Light) { onSelect(i) }
                    .then(if (option.tag != null) Modifier.testTag(option.tag) else Modifier)
                    .padding(horizontal = if (on) 16.dp else 17.dp),
                contentAlignment = Alignment.Center,
            ) {
                AeroText(option.label, style = if (on) type.bodyStrong.copy(color = c.accentOn) else type.body.copy(color = c.textPrimary), maxLines = 1)
            }
        }
    }
}

/** Card/Note: glass note with a 20 dp accent icon, Caption text and an optional accent footer (dot + Label/Section). */
@Composable
fun AeroNoteCard(
    icon: AeroIconName,
    text: String,
    modifier: Modifier = Modifier,
    footer: String? = null,
    footerTag: String? = null,
) {
    val c = Aero.colors
    val type = Aero.type
    Row(
        modifier
            .fillMaxWidth()
            .aeroGlass(Aero.shapes.card)
            .padding(horizontal = 17.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AeroIcon(icon, c.accentDefault, size = 20.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AeroText(text, style = type.caption.copy(color = c.textMuted))
            if (footer != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).background(c.accentDefault, CircleShape))
                    AeroText(
                        AeroTextTokens.labelSection.cased(footer),
                        if (footerTag != null) Modifier.testTag(footerTag) else Modifier,
                        style = type.labelSection.copy(color = c.accentDefault),
                    )
                }
            }
        }
    }
}

/**
 * Field/Number as a text field with a String value. Large: Field/Number style, 58 dp, optional trailing icon.
 * Compact: Title style, 52 dp. Empty: [placeholder] in text/dim (Compact: "placeholder · unit", unit hidden).
 */
@Composable
fun AeroNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
    compact: Boolean = false,
    placeholder: String = "0",
    icon: AeroIconName? = null,
    enabled: Boolean = true,
    decimal: Boolean = false,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    /** On a sheet or card that is already glass: the glass fill alone. */
    onGlass: Boolean = false,
) {
    val c = Aero.colors
    val type = Aero.type
    val style = if (compact) type.title else type.fieldNumber
    // A46: an edit starts at the end of the value.
    val cursor = rememberEndCursorField(value)
    BasicTextField(
        value = cursor.field,
        onValueChange = { cursor.onValueChange(it, onValueChange) },
        enabled = enabled,
        singleLine = true,
        textStyle = style.copy(color = c.textPrimary),
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number, imeAction = imeAction),
        keyboardActions = keyboardActions,
        cursorBrush = SolidColor(c.accentDefault),
        modifier = modifier.then(fieldModifier).endCursorOnFocus(cursor),
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(if (compact) 52.dp else 58.dp)
                    .aeroGlass(Aero.shapes.card, border = c.borderLine, shadow = false, backdropBlurred = onGlass)
                    .padding(horizontal = if (compact) 15.dp else 19.dp),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(IntrinsicSize.Max)) {
                    if (value.isEmpty()) {
                        AeroText(
                            if (compact) "$placeholder · $unit" else placeholder,
                            style = (if (compact) type.body else style).copy(color = c.textDim),
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                    inner()
                }
                if (!(compact && value.isEmpty())) AeroText(unit, style = type.body.copy(color = c.textMuted), maxLines = 1)
                if (icon != null) {
                    Spacer(Modifier.weight(1f))
                    AeroIcon(icon, c.iconPrimary)
                }
            }
        },
    )
}

/**
 * Row/MealSlot: name field (band icon), time field (clock) and two suggestion chips. Tags: "$tag-slot-$index",
 * "$tag-name-$index", "$tag-time-$index", "$tag-chip-$index-$j".
 */
@Composable
fun AeroMealSlotRow(
    index: Int,
    name: String,
    time: String,
    icon: AeroIconName,
    suggestions: List<String>,
    onName: (String) -> Unit,
    onPickTime: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String = "o3",
) {
    val c = Aero.colors
    val type = Aero.type
    Column(
        modifier
            .fillMaxWidth()
            .aeroGlass(Aero.shapes.card)
            .padding(17.dp)
            .testTag("$tag-slot-$index"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // A46: renaming starts at the end of the name.
            val cursor = rememberEndCursorField(name)
            BasicTextField(
                value = cursor.field,
                onValueChange = { cursor.onValueChange(it, onName) },
                singleLine = true,
                textStyle = type.body.copy(color = c.textPrimary),
                cursorBrush = SolidColor(c.accentDefault),
                modifier = Modifier.weight(1f).endCursorOnFocus(cursor).testTag("$tag-name-$index"),
                decorationBox = { inner ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(pill)
                            .background(c.surface2)
                            .border(1.dp, c.borderLine, pill)
                            .padding(horizontal = 15.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AeroIcon(icon, c.iconMuted, size = 20.dp)
                        Box(Modifier.weight(1f)) {
                            if (name.isEmpty()) AeroText("Nome da refeição", style = type.body.copy(color = c.textDim), maxLines = 1)
                            inner()
                        }
                    }
                },
            )
            Row(
                Modifier
                    .width(104.dp)
                    .height(48.dp)
                    .clip(pill)
                    .background(c.surface2)
                    .border(1.dp, c.borderLine, pill)
                    .dietaClick(Haptic.Light, onClick = onPickTime)
                    .padding(horizontal = 15.dp)
                    .testTag("$tag-time-$index"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AeroText(time, style = type.bodyStrong.copy(color = c.textPrimary), maxLines = 1)
                AeroIcon(AeroIconName.Clock, c.accentDefault, size = 20.dp, contentDescription = "Horário")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            AeroText("Sugestões:", style = type.caption.copy(color = c.textDim))
            suggestions.forEachIndexed { j, suggestion ->
                AeroChipLog(
                    suggestion,
                    Modifier.clip(pill).dietaClick(Haptic.Light) { onName(suggestion) }.testTag("$tag-chip-$index-$j"),
                    tone = AeroChipTone.Neutral,
                )
            }
        }
    }
}

/**
 * Card/MacroTarget: colour well, name and detail, the grams field (Compact, unit "g") and the adjust glass button
 * that focuses the field.
 */
@Composable
fun AeroMacroTargetCard(
    name: String,
    detail: String,
    macro: AeroMacro,
    value: String,
    onValueChange: (String) -> Unit,
    onAdjust: () -> Unit,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
    /** On a sheet that is already glass: the glass fill alone. */
    onGlass: Boolean = false,
) {
    val c = Aero.colors
    val type = Aero.type
    val color = when (macro) {
        AeroMacro.Protein -> c.macroProtein
        AeroMacro.Carbs -> c.macroCarbs
        AeroMacro.Fat -> c.macroFat
    }
    Row(
        modifier
            .fillMaxWidth()
            .aeroGlass(Aero.shapes.card, backdropBlurred = onGlass)
            .padding(horizontal = 17.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(c.surface2).border(1.dp, c.borderGlass, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Box(Modifier.size(10.dp).background(color, CircleShape)) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AeroText(name, style = type.bodyStrong.copy(color = c.textPrimary), maxLines = 1)
            AeroText(detail, style = type.caption.copy(color = c.textMuted), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        AeroNumberField(value, onValueChange, "g", Modifier.width(84.dp), fieldModifier = fieldModifier, compact = true, placeholder = "0")
        AeroIconButton(AeroIconName.SlidersHorizontal, onAdjust, contentDescription = "Ajustar $name")
    }
}

/** An edge bubble of a Release 1 frame, at frame coordinates. */
@Immutable
data class AeroBubbleSpec(val x: Dp, val y: Dp, val size: Dp)

/**
 * The edge bubbles of a scrolling page, drawn above the content at frame coordinates and moved with [scroll].
 * Decoration only: they never take touches.
 */
@Composable
fun BoxScope.AeroPageBubbles(bubbles: List<AeroBubbleSpec>, scroll: ScrollState?, modifier: Modifier = Modifier) {
    Box(modifier.matchParentSize().graphicsLayer { translationY = -(scroll?.value ?: 0).toFloat() }) {
        bubbles.forEach { AeroBubble(it.size, Modifier.offset(it.x, it.y)) }
    }
}

/** Scrim + no-op catcher used under an Aero dialog drawn in the screen (Figma: content layer blur + overlay/scrim). */
@Composable
fun BoxScope.AeroScrim(onDismiss: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Aero.colors.overlayScrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
    )
}

/** Canvas-free spacer kept for symmetric bars (Figma "Balance" frame). */
@Composable
fun AeroBarBalance() = Spacer(Modifier.size(44.dp))
