package com.nutri.android.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nutri.android.core.designsystem.Haptic
import com.nutri.android.core.designsystem.Inter
import com.nutri.android.core.designsystem.Jakarta
import com.nutri.android.core.designsystem.LocalPalette
import com.nutri.android.core.designsystem.DietaBotMeasure
import com.nutri.android.core.designsystem.DietaBotType
import com.nutri.android.core.designsystem.dietaClick

internal val CardShape = RoundedCornerShape(DietaBotMeasure.cardDp.dp)
internal val InnerShape = RoundedCornerShape(12.dp)

/** CtaFooter: 24 top + 56 pill + 28 bottom, above the navigation inset. */
private val CtaFooterHeight = 108.dp

/** A20: >= 24 dp between the last item and the CTA once scrolled to the end. */
private val ScrollBreath = 24.dp

/** Top bar variants as drawn in each Stitch gold. */
sealed interface OnboardingBar {
    /** O1: continuous progress only. */
    data class Continuous(val fraction: Float) : OnboardingBar

    /** O2: back + "DIETA BOT INTAKE", continuous thin progress. */
    data class IntakeContinuous(val fraction: Float) : OnboardingBar

    /** O3: back + "DIETA BOT INTAKE", 4 segments. */
    data class IntakeSegments(val filled: Int) : OnboardingBar

    /** O4: round back + wordmark + help, 4 segments. */
    data class Brand(val filled: Int, val onHelp: () -> Unit) : OnboardingBar
}

@Composable
fun OnboardingFrame(
    bar: OnboardingBar,
    cta: String,
    ctaEnabled: Boolean,
    onCta: () -> Unit,
    onBack: (() -> Unit)?,
    ctaTag: String,
    contentTop: Dp = 16.dp,
    ctaJakarta: Boolean = false,
    ctaWeight: FontWeight? = null,
    header: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = LocalPalette.current
    Box(
        Modifier
            .fillMaxSize()
            .background(p.phone)
            .topGlow(p.gold),
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            if (header != null) header() else TopBar(bar, onBack)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = contentTop, bottom = CtaFooterHeight + ScrollBreath),
                content = content,
            )
        }
        CtaFooter(cta, ctaEnabled, onCta, ctaTag, ctaJakarta, ctaWeight)
    }
}

private fun Modifier.topGlow(gold: Color): Modifier = drawBehind {
    val radius = 160.dp.toPx()
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(gold.copy(alpha = 0.07f), Color.Transparent),
            center = Offset(size.width / 2f, 0f),
            radius = radius,
        ),
        radius = radius,
        center = Offset(size.width / 2f, 0f),
    )
}

@Composable
private fun TopBar(bar: OnboardingBar, onBack: (() -> Unit)?) {
    val p = LocalPalette.current
    when (bar) {
        is OnboardingBar.Continuous -> {
            ContinuousProgress(bar.fraction, height = 4.dp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
        }
        is OnboardingBar.IntakeContinuous -> {
            Column(Modifier.padding(horizontal = 24.dp)) {
                IntakeRow(onBack, Modifier.padding(top = 8.dp, bottom = 4.dp))
                ContinuousProgress(bar.fraction, height = 3.dp, modifier = Modifier.padding(top = if (p.isDark) 8.dp else 10.dp))
            }
        }
        is OnboardingBar.IntakeSegments -> {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 3.dp, bottom = 8.dp)) {
                IntakeRow(onBack)
                SegmentProgress(bar.filled, gap = 6.dp, modifier = Modifier.padding(top = 16.dp))
            }
        }
        is OnboardingBar.Brand -> {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 7.dp, bottom = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(p.card)
                            .border(1.dp, p.line, CircleShape)
                            .dietaClick(enabled = onBack != null) { onBack?.invoke() }
                            .testTag("onboarding-back"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar", tint = p.text, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        "Dieta Bot",
                        style = DietaBotType.headlineMd.copy(fontWeight = FontWeight.W700),
                        color = if (p.isDark) p.gold else p.text,
                    )
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).dietaClick(onClick = bar.onHelp).testTag("onboarding-help"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = "Ajuda", tint = p.muted, modifier = Modifier.size(20.dp))
                    }
                }
                SegmentProgress(bar.filled, gap = 8.dp, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            }
        }
    }
}

@Composable
private fun IntakeRow(onBack: (() -> Unit)?, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Box(modifier.fillMaxWidth().height(36.dp)) {
        // Stitch: plain arrow in dark, arrow in a circle in light.
        val circle = if (p.isDark) Modifier else Modifier.background(p.card, CircleShape).border(1.dp, p.line, CircleShape)
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .size(36.dp)
                .clip(CircleShape)
                .then(circle)
                .dietaClick(enabled = onBack != null) { onBack?.invoke() }
                .testTag("onboarding-back"),
            contentAlignment = if (p.isDark) Alignment.CenterStart else Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Voltar",
                tint = if (p.isDark) p.muted else p.text,
                modifier = Modifier.size(if (p.isDark) 22.dp else 20.dp),
            )
        }
        Text(
            "DIETA BOT INTAKE",
            style = DietaBotType.labelCaps.copy(fontWeight = FontWeight.W700, letterSpacing = 0.2.em),
            color = if (p.isDark) p.dim else p.text,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun ContinuousProgress(fraction: Float, height: Dp, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(p.card),
    ) {
        Box(Modifier.fillMaxWidth(fraction).height(height).clip(CircleShape).background(p.gold))
    }
}

@Composable
private fun SegmentProgress(filled: Int, gap: Dp, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
        repeat(4) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(if (i < filled) p.gold else p.surf2),
            )
        }
    }
}

@Composable
private fun BoxScope.CtaFooter(text: String, enabled: Boolean, onClick: () -> Unit, tag: String, jakarta: Boolean, weight: FontWeight?) {
    val p = LocalPalette.current
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, p.phone.copy(alpha = 0.95f), p.phone)))
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 28.dp),
    ) {
        PillCta(text, enabled, onClick, Modifier.testTag(tag), jakarta, weight)
    }
}

@Composable
fun PillCta(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    jakarta: Boolean = false,
    weight: FontWeight? = null,
) {
    val p = LocalPalette.current
    val alpha = if (enabled) 1f else DISABLED_ALPHA
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(CircleShape)
            .background(p.ctaBg.copy(alpha = alpha))
            .dietaClick(Haptic.Confirm, enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = DietaBotType.bodyLg.copy(fontFamily = if (jakarta) Jakarta else Inter, fontWeight = weight ?: if (jakarta) FontWeight.W600 else FontWeight.W700, fontSize = 16.sp),
            color = p.ctaText.copy(alpha = if (enabled) 1f else 0.7f),
        )
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = p.ctaText, modifier = Modifier.size(20.dp))
    }
}

/** "ONBOARDING n/4 • SECTION". */
@Composable
fun Eyebrow(step: String, section: String, sectionAccent: Boolean, leadingDot: Boolean = false, tracking: Float = 0.1f) {
    val p = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (leadingDot) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(p.gold))
            Spacer(Modifier.width(6.dp))
        }
        // Stitch: a non-accent eyebrow is dim in dark and gold in light.
        val plain = if (sectionAccent || p.isDark) p.dim else p.gold
        Text(step, style = DietaBotType.labelCaps.copy(letterSpacing = tracking.em), color = plain)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(4.dp).clip(CircleShape).background(if (sectionAccent) p.line else plain))
        Spacer(Modifier.width(8.dp))
        Text(section, style = DietaBotType.labelCaps.copy(letterSpacing = if (sectionAccent) 0.14.em else tracking.em), color = if (sectionAccent) p.gold else plain)
    }
}

@Composable
fun ScreenTitle(
    title: String,
    subtitle: String,
    titleSize: Int = 30,
    titleLine: Float = 38f,
    titleStyle: TextStyle? = null,
    titleWeight: FontWeight = FontWeight.W600,
    subtitleTop: Dp = 8.dp,
    titleTop: Dp = 8.dp,
) {
    val p = LocalPalette.current
    Text(
        title,
        style = titleStyle ?: DietaBotType.headlineLg.copy(fontSize = titleSize.sp, lineHeight = titleLine.sp, letterSpacing = (-0.025).em, fontWeight = titleWeight),
        color = p.text,
        modifier = Modifier.padding(top = titleTop),
    )
    Text(
        subtitle,
        style = DietaBotType.bodyMd.copy(lineHeight = 23.sp),
        color = p.muted,
        modifier = Modifier.padding(top = subtitleTop),
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, bottom: Dp = 14.dp) {
    val p = LocalPalette.current
    Text(
        text.uppercase(),
        style = DietaBotType.labelCaps.copy(fontWeight = FontWeight.W500, letterSpacing = 0.05.em),
        color = p.muted,
        modifier = modifier.padding(bottom = bottom),
    )
}

@Composable
fun InfoNote(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    infoLine: Float = 19.5f,
    infoTracking: Float = 0f,
    footer: (@Composable () -> Unit)? = null,
) {
    val p = LocalPalette.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(p.card)
            .border(1.dp, p.line, CardShape)
            .padding(horizontal = 14.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = p.gold, modifier = Modifier.padding(top = 1.dp).size(18.dp))
        Column(Modifier.weight(1f)) {
            Text(text, style = DietaBotType.labelMd.copy(lineHeight = infoLine.sp, letterSpacing = infoTracking.em, fontWeight = FontWeight.W400), color = p.muted)
            footer?.invoke()
        }
    }
}

/** Gold radio used by O1 modes and O2 cards. */
@Composable
fun GoldRadio(selected: Boolean, enabled: Boolean = true) {
    val p = LocalPalette.current
    // Disabled (o1e): the selected radio is a dim ring, no gold fill.
    val accent = if (enabled) p.gold else p.dim
    Box(
        Modifier
            .size(20.dp)
            .border(2.dp, if (selected) accent else p.line, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(accent))
    }
}

/** Disabled O1 controls (o1e): 38 % opacity, like the disabled PillCta. */
internal const val DISABLED_ALPHA = 0.38f

/** Composites the content at [DISABLED_ALPHA] through one save layer when [enabled] is false. */
internal fun Modifier.disabledAlpha(enabled: Boolean): Modifier =
    if (enabled) this else drawWithContent {
        drawContext.canvas.saveLayer(Rect(Offset.Zero, size), Paint().apply { alpha = DISABLED_ALPHA })
        drawContent()
        drawContext.canvas.restore()
    }
