package com.nutri.android.core.designsystem.aero

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nutri.android.core.designsystem.Haptic
import com.nutri.android.core.designsystem.dietaClick

/*
 * D5 components of the Figma `Design` (Componentes, section "Chat"): Chat/BotLabel, Chip/Date, Chat/ActionBar,
 * Loader/Aero, Row/SlotPick and Dialog/Confirm (Tone=Default).
 */

private val pill = RoundedCornerShape(percent = 50)

/** Chat/BotLabel: lightning in a tinted well + "Dieta Bot AI". */
@Composable
fun AeroBotLabel(modifier: Modifier = Modifier) {
    val c = Aero.colors
    Row(
        modifier.padding(start = 4.dp).height(20.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(20.dp).clip(CircleShape).background(c.surfaceTint).border(1.dp, c.borderGlass, CircleShape),
            contentAlignment = Alignment.Center,
        ) { AeroIcon(AeroIconName.Lightning, c.iconPrimary, size = 12.dp) }
        AeroText("Dieta Bot AI", style = Aero.type.captionStrong.copy(color = c.textPrimary))
    }
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

/** Dialog/Confirm Tone=Danger primary: 56 dp status/bad pill, 24 dp icon and the Button label in accent/on. */
@Composable
private fun AeroDangerButton(label: String, icon: AeroIconName, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
        AeroIcon(icon, c.accentOn)
        AeroText(label, style = Aero.type.button.copy(color = c.accentOn), maxLines = 1)
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
