package app.fibrai.android.core.designsystem.aero

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import app.fibrai.android.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.fibrai.android.core.designsystem.Haptic
import app.fibrai.android.core.designsystem.dietaClick

/*
 * D5 components of the Figma `Design` (Componentes, section "Chat"): Chat/BotLabel, Chip/Date, Chat/ActionBar,
 * Loader/Aero, Row/SlotPick and Dialog/Confirm (Tone=Default). D20: Chat/SelectionBar, Chat/CopyToast and the selection.
 */

private val pill = RoundedCornerShape(percent = 50)

/** Chat/BotLabel (D10): the Label avatar + "Tali" (Caption/Strong), 6 dp apart, 20 dp high. */
@Composable
fun AeroBotLabel(modifier: Modifier = Modifier) {
    val c = Aero.colors
    Row(
        modifier.padding(start = 4.dp).height(20.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TaliAvatar(TaliAvatarSize.Label)
        AeroText("Tali", style = Aero.type.captionStrong.copy(color = c.textPrimary))
    }
}

/** Chat/TaliAvatar sizes (D10): Header 32 dp, Label 20 dp. */
enum class TaliAvatarSize(val dp: Dp) { Header(32.dp), Label(20.dp) }

/**
 * Chat/TaliAvatar (D10, ADR-035): the assistant image in a circle with a 1 dp border/glass ring inside. Decorative: the
 * "Tali" text next to it carries the name. The temporary art is R.drawable.tali_avatar; the final art replaces only it.
 */
@Composable
fun TaliAvatar(size: TaliAvatarSize, modifier: Modifier = Modifier) {
    Image(
        painterResource(R.drawable.tali_avatar),
        contentDescription = null,
        modifier = modifier.size(size.dp).clip(CircleShape).border(1.dp, Aero.colors.borderGlass, CircleShape),
        contentScale = ContentScale.Crop,
    )
}

/** Chip/Date: centred glass pill with the day ("Hoje, 25 de setembro"). */
@Composable
fun AeroDateChip(label: String, modifier: Modifier = Modifier) {
    val c = Aero.colors
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.height(32.dp).aeroGlass(pill).padding(horizontal = 15.dp), contentAlignment = Alignment.Center) {
            AeroText(label, style = Aero.type.captionStrong.copy(color = c.textMuted), maxLines = 1)
        }
    }
}

/** Chat/ActionBar: the 48 dp glass pill of the actions slot (Registrar, Forçar estimativa, Registrar assim). */
@Composable
fun AeroActionBar(
    label: String,
    icon: AeroIconName,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    haptic: Haptic = Haptic.Confirm,
) {
    val c = Aero.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .aeroGlass(pill)
            .dietaClick(haptic, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AeroIcon(icon, c.iconPrimary, size = 20.dp)
        AeroText(label, style = Aero.type.button.copy(color = c.textPrimary), maxLines = 1)
    }
}

/** Loader/Aero: border/line track ring (inner radius 72 %) and a 225° accent arc that turns. */
@Composable
fun AeroLoader(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val c = Aero.colors
    val turn by rememberInfiniteTransition(label = "loader").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(LOADER_TURN_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "turn",
    )
    Canvas(modifier.size(size)) {
        val thickness = this.size.minDimension / 2 * (1 - LOADER_INNER)
        val inset = thickness / 2
        val arcSize = androidx.compose.ui.geometry.Size(this.size.width - thickness, this.size.height - thickness)
        val stroke = Stroke(thickness)
        drawArc(c.borderLine, 0f, 360f, false, androidx.compose.ui.geometry.Offset(inset, inset), arcSize, style = stroke)
        rotate(turn) {
            drawArc(c.accentDefault, -90f, LOADER_SWEEP, false, androidx.compose.ui.geometry.Offset(inset, inset), arcSize, style = stroke)
        }
    }
}

private const val LOADER_INNER = 0.72f
private const val LOADER_SWEEP = 225f
private const val LOADER_TURN_MS = 1_000

/** Row/SlotPick: slot row of the meal picker sheet, glass when idle, tinted with an accent border when picked. */
@Composable
fun AeroSlotPickRow(
    name: String,
    time: String,
    icon: AeroIconName,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Aero.colors
    val type = Aero.type
    val shape = Aero.shapes.card
    Row(
        modifier
            .fillMaxWidth()
            .height(if (selected) 72.dp else 70.dp)
            .then(
                if (selected) {
                    Modifier.clip(shape).background(c.surfaceTint).border(2.dp, c.accentDefault, shape)
                } else {
                    // Rows sit on the sheet's glass: the fill alone, as a background blur of a blurred layer looks.
                    Modifier.aeroGlass(shape, backdropBlurred = true)
                },
            )
            .dietaClick(Haptic.Light, onClick = onClick)
            .padding(horizontal = if (selected) 18.dp else 17.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(if (selected) c.accentDefault else c.surface2),
            contentAlignment = Alignment.Center,
        ) { AeroIcon(icon, if (selected) c.accentOn else c.iconMuted, size = 22.dp) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AeroText(name, style = (if (selected) type.bodyStrong else type.body).copy(color = c.textPrimary), maxLines = 1)
            AeroText(time, style = if (selected) type.captionStrong.copy(color = c.accentDefault) else type.caption.copy(color = c.textMuted), maxLines = 1)
        }
        if (selected) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(c.accentDefault), contentAlignment = Alignment.Center) {
                AeroIcon(AeroIconName.Check, c.accentOn, size = 14.dp)
            }
        } else {
            Box(Modifier.size(20.dp).border(1.5.dp, c.borderLine, CircleShape))
        }
    }
}

/**
 * Dialog/Confirm: centred glass dialog (radius 28) drawn in the screen over overlay/scrim (the screen blurs itself
 * behind it); back and the scrim cancel. Tone=Default: Title and the action pair (Button/Primary + secondary pill).
 * Tone=Danger ([dangerIcon] set): a 48 dp status/bad-tint badge with the icon, Title and [body], and a status/bad
 * primary with the same icon.
 */
@Composable
fun BoxScope.AeroConfirmDialog(
    title: String,
    primary: String,
    onPrimary: () -> Unit,
    secondary: String,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
    primaryTag: String? = null,
    secondaryTag: String? = null,
    body: String? = null,
    dangerIcon: AeroIconName? = null,
) {
    val c = Aero.colors
    val type = Aero.type
    BackHandler(onBack = onSecondary)
    AeroScrim(onSecondary)
    Column(
        modifier
            .align(Alignment.Center)
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .aeroGlass(RoundedCornerShape(AeroDimens.radiusSheet), backdropBlurred = true)
            .aeroSwallowTaps()
            .padding(25.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (dangerIcon != null) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(c.statusBadTint), contentAlignment = Alignment.Center) {
                    AeroIcon(dangerIcon, c.statusBad)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AeroText(title, style = type.title.copy(color = c.textPrimary))
                if (body != null) AeroText(body, style = type.body.copy(color = c.textMuted))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val primaryModifier = if (primaryTag != null) Modifier.testTag(primaryTag) else Modifier
            if (dangerIcon != null) {
                AeroDangerButton(primary, dangerIcon, onPrimary, primaryModifier)
            } else {
                AeroButtonPrimary(primary, onPrimary, modifier = primaryModifier)
            }
            AeroSecondaryPill(secondary, onSecondary, if (secondaryTag != null) Modifier.testTag(secondaryTag) else Modifier)
        }
    }
}

/** Dialog/Confirm with one action (no gold): a Body note and Button/Primary; back and the scrim close it. */
@Composable
fun BoxScope.AeroNoticeDialog(body: String, button: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val c = Aero.colors
    BackHandler(onBack = onDismiss)
    AeroScrim(onDismiss)
    Column(
        modifier
            .align(Alignment.Center)
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .aeroGlass(RoundedCornerShape(AeroDimens.radiusSheet), backdropBlurred = true)
            .aeroSwallowTaps()
            .padding(25.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        AeroText(body, style = Aero.type.body.copy(color = c.textMuted))
        AeroButtonPrimary(button, onDismiss)
    }
}

/**
 * Dialog/Confirm Tone=Danger primary: 56 dp status/bad pill, 24 dp icon and the Button label in [content] (accent/on in
 * the dialog; status/on-bad, white in both themes, in the Chat's Excluir e pular, chatSD).
 */
@Composable
fun AeroDangerButton(
    label: String,
    icon: AeroIconName,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: Color = Aero.colors.accentOn,
) {
    val c = Aero.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(pill)
            .background(c.statusBad)
            .dietaClick(Haptic.Confirm, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AeroIcon(icon, content)
        AeroText(label, style = Aero.type.button.copy(color = content), maxLines = 1)
    }
}

/** The surface/2 secondary pill of sheets and dialogs (Cancelar), 58 dp. */
@Composable
fun AeroSecondaryPill(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Aero.colors
    Box(
        modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(pill)
            .background(c.surface2)
            .border(1.dp, c.borderLine, pill)
            .dietaClick(Haptic.Light, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AeroText(label, style = Aero.type.button.copy(color = c.textPrimary))
    }
}

/**
 * Chat/ChoiceBar (D12, chatRB): two 48 dp pills side by side in the actions slot, the glass [left] and the accent [right]
 * (the action that sends). A label that does not fit (large text) wraps to two lines instead of being cut.
 */
@Composable
fun AeroChoiceBar(
    left: String,
    right: String,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    modifier: Modifier = Modifier,
    leftTag: String? = null,
    rightTag: String? = null,
) {
    val c = Aero.colors
    val type = Aero.type
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .aeroGlass(pill)
                .dietaClick(Haptic.Confirm, onClick = onLeft)
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .then(if (leftTag != null) Modifier.testTag(leftTag) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            AeroText(left, style = type.button.copy(color = c.textPrimary, textAlign = TextAlign.Center), maxLines = 2)
        }
        Box(
            Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clip(pill)
                .background(c.accentDefault)
                .aeroGloss(pill)
                .border(1.dp, c.borderGlass, pill)
                .dietaClick(Haptic.Confirm, onClick = onRight)
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .then(if (rightTag != null) Modifier.testTag(rightTag) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            AeroText(right, style = type.button.copy(color = c.accentOn, textAlign = TextAlign.Center), maxLines = 2)
        }
    }
}

/**
 * Chat/SelectionBar (D20, ADR-048): in place of Chat/Header while messages are selected. ✕ (ends the selection), the
 * count in the header title style, and one Copy icon button labelled Copiar.
 */
@Composable
fun AeroSelectionBar(count: Int, onClose: () -> Unit, onCopy: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 24.dp).height(44.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AeroIconButton(AeroIconName.X, onClose, contentDescription = "Cancelar seleção", modifier = Modifier.testTag("chat-selection-close"))
        AeroText(
            "$count",
            Modifier.weight(1f).testTag("chat-selection-count"),
            style = Aero.type.bodyStrong.copy(color = Aero.colors.textPrimary),
            maxLines = 1,
        )
        AeroIconButton(AeroIconName.Copy, onCopy, contentDescription = "Copiar", modifier = Modifier.testTag("chat-copy"))
    }
}

/**
 * Chat/CopyToast (D20): the app's own copy confirmation (Android 12 and earlier; 13+ shows the system overlay). The
 * Chip/Date glass pill, a check in the accent and the label, centred.
 */
@Composable
fun AeroCopyToast(label: String, modifier: Modifier = Modifier) {
    val c = Aero.colors
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            Modifier.height(36.dp).aeroGlass(pill).padding(horizontal = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AeroIcon(AeroIconName.Check, c.accentDefault, size = 16.dp)
            AeroText(label, style = Aero.type.captionStrong.copy(color = c.textPrimary), maxLines = 1)
        }
    }
}

/**
 * A selected message row (D20): the surface/selected band across the screen width, 6 dp over and under the row. The
 * thread pads its items by [inset] on each side; the band is drawn past that padding, behind the row.
 */
fun Modifier.aeroSelectedRow(selected: Boolean, color: Color, inset: Dp = 20.dp): Modifier = if (!selected) this else drawBehind {
    val x = inset.toPx()
    val y = 6.dp.toPx()
    drawRect(color, topLeft = Offset(-x, -y), size = Size(size.width + 2 * x, size.height + 2 * y))
}

/**
 * A selected bubble (D20): an opaque bg/page backing under the glass, so the band does not show through it, and the
 * 2 dp border/selected ring inside its edge, over the glass border. The bubble keeps its size.
 */
@Composable
fun Modifier.aeroSelectedBubble(selected: Boolean, shape: Shape): Modifier {
    if (!selected) return this
    val c = Aero.colors
    return background(c.bgPage, shape).drawWithContent {
        drawContent()
        val w = 2.dp.toPx()
        val outline = shape.createOutline(Size(size.width - w, size.height - w), layoutDirection, this)
        translate(w / 2, w / 2) { drawOutline(outline, c.borderSelected, style = Stroke(w)) }
    }
}
