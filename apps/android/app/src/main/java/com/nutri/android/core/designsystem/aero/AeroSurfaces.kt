package com.nutri.android.core.designsystem.aero

import android.os.Build
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/** Backdrop blur needs RenderEffect (API 31+). Below it glass falls back to a more opaque fill (ADR-030 § 6). */
val aeroBlurSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** Fallback glass alpha below API 31 (A39 plan): light white 85 %, dark surface/glass at 88 %. */
private const val FALLBACK_ALPHA_LIGHT = 0.85f
private const val FALLBACK_ALPHA_DARK = 0.88f

private fun List<AeroStop>.verticalBrush(): Brush = Brush.verticalGradient(*map { it.position to it.color }.toTypedArray())

/**
 * The page gradient (paint style Background/Page) and the backdrop that glass blurs. Put it on a layer behind the
 * content, not on the content's parent: use [AeroPage]. With [scroll], the gradient spans the whole scrolled page
 * and moves with it, as the fill of a Figma page frame does.
 */
@Composable
fun Modifier.aeroPage(scroll: ScrollState? = null): Modifier {
    val c = Aero.colors
    val haze = LocalAeroHaze.current
    val stops = remember(c) { AeroPaints.backgroundPageStops(c).map { it.position to it.color }.toTypedArray() }
    return (if (haze != null) hazeSource(haze) else this).drawBehind {
        val top = -(scroll?.value ?: 0).toFloat()
        val bottom = top + size.height + (scroll?.maxValue?.takeIf { it != Int.MAX_VALUE } ?: 0)
        drawRect(Brush.verticalGradient(*stops, startY = top, endY = bottom))
    }
}

/** A screen root: the page gradient as a backdrop layer, the content on top of it. */
@Composable
fun AeroPage(
    modifier: Modifier = Modifier,
    scroll: ScrollState? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier) {
        Box(Modifier.matchParentSize().aeroPage(scroll))
        content()
    }
}

/**
 * Drop shadow outside the shape only: Figma does not draw a shadow behind a translucent fill.
 */
@Composable
fun Modifier.aeroShadow(
    shape: Shape,
    color: Color,
    radius: Dp = AeroEffects.glassShadowRadius,
    offsetY: Dp = AeroEffects.glassShadowOffsetY,
): Modifier {
    val context = LocalGraphicsContext.current
    val painter = remember(context, shape, color, radius, offsetY) {
        context.shadowContext.createDropShadowPainter(
            shape,
            Shadow(radius = radius, color = color, offset = DpOffset(AeroEffects.glassShadowOffsetX, offsetY)),
        )
    }
    return drawBehind {
        val hole = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawBehind)) }
        clipPath(hole, ClipOp.Difference) {
            with(painter) { draw(size) }
        }
    }
}

/**
 * Frosted glass (paint Surface/Glass + effect Glass): backdrop blur 16 dp on API 31+, the glass fill, the top
 * sheen, a hairline border and the soft shadow. [fill] and [border] default to the glass tokens; tinted glass
 * (user bubble, chips) passes its own. [backdropBlurred]: the content behind is already blurred (a sheet over a
 * Modifier.blur screen), so the glass is its fill over it, as Figma's background blur of a blurred layer looks.
 */
@Composable
fun Modifier.aeroGlass(
    shape: Shape,
    fill: Color = Aero.colors.surfaceGlass,
    border: Color = Aero.colors.borderGlass,
    borderWidth: Dp = 1.dp,
    sheen: Boolean = true,
    shadow: Boolean = true,
    backdropBlurred: Boolean = false,
): Modifier {
    val c = Aero.colors
    val haze = LocalAeroHaze.current
    val sheenBrush = remember(c) { AeroPaints.surfaceGlassStops(c).verticalBrush() }
    val blur = aeroBlurSupported && haze != null && !backdropBlurred
    val base = if (shadow) aeroShadow(shape, AeroEffects.glassShadowColor(c)) else this
    val backdrop = if (blur) {
        val style = remember(c, fill) {
            HazeStyle(
                backgroundColor = c.bgPage,
                tint = HazeTint(fill),
                blurRadius = AeroEffects.glassBlur,
                noiseFactor = 0f,
            )
        }
        Modifier.hazeEffect(haze!!, style)
    } else if (backdropBlurred && aeroBlurSupported) {
        // What is behind is already blurred (a sheet over a blurred screen): the glass fill alone.
        Modifier.background(fill)
    } else {
        val alpha = if (c.isDark) FALLBACK_ALPHA_DARK else FALLBACK_ALPHA_LIGHT
        Modifier.background(fill.copy(alpha = maxOf(fill.alpha, alpha)))
    }
    return base
        .clip(shape)
        .then(backdrop)
        .then(if (sheen) Modifier.drawBehind { drawOutline(shape.createOutline(size, layoutDirection, this), sheenBrush) } else Modifier)
        .border(borderWidth, border, shape)
}

/** The button gloss (paint style Gloss/Button) over a solid accent or macro fill, under the label. */
@Composable
fun Modifier.aeroGloss(shape: Shape): Modifier {
    val c = Aero.colors
    val brush = remember(c) { AeroPaints.glossButtonStops(c).verticalBrush() }
    return drawBehind { drawOutline(shape.createOutline(size, layoutDirection, this), brush) }
}
