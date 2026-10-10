package app.fibrai.android.core.designsystem.aero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.fibrai.android.core.designsystem.Haptic
import app.fibrai.android.core.designsystem.dietaClick

/*
 * D27 components of the Figma `Design` (Componentes, section "Onboarding v2 · D27"): Onboarding/Progress,
 * Onboarding/QuickReply, Onboarding/SummaryBlock and Onboarding/FullScreenState (ADR-057).
 */

private val pill = RoundedCornerShape(percent = 50)

/**
 * Onboarding/Progress: the section label (Label/Section, accent/default), `{n} de {total}` (Caption, text/muted) and the
 * 6 dp bar (track surface/2, fill accent/default, n/total of the width), 8 dp apart.
 */
@Composable
fun AeroOnboardingProgress(section: String, step: Int, total: Int, modifier: Modifier = Modifier) {
    val c = Aero.colors
    val type = Aero.type
    Column(modifier.fillMaxWidth().testTag("ob-progress"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            AeroText(AeroTextTokens.labelSection.cased(section), style = type.labelSection.copy(color = c.accentDefault))
            AeroText("$step de $total", style = type.caption.copy(color = c.textMuted))
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(c.surface2)) {
            Box(
                Modifier
                    .fillMaxWidth(step.coerceIn(0, total).toFloat() / total.coerceAtLeast(1))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(c.accentDefault),
            )
        }
    }
}

/**
 * Onboarding/QuickReply: a closed answer under the active question. Selected=false: glass pill (border/glass). Selected=true:
 * surface/selected with the 2 dp border/selected ring and a check. Label Body/Strong, text/primary; 44 dp inside the border.
 */
@Composable
fun AeroQuickReply(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Aero.colors
    val base = if (selected) {
        Modifier.clip(pill).background(c.surfaceSelected).border(2.dp, c.borderSelected, pill).padding(horizontal = 20.dp, vertical = 12.dp)
    } else {
        Modifier.aeroGlass(pill).padding(horizontal = 19.dp, vertical = 11.dp)
    }
    Row(
        modifier
            .semantics {
                role = Role.Button
                this.selected = selected
            }
            .then(base)
            .dietaClick(Haptic.Confirm, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) AeroIcon(AeroIconName.Check, c.textPrimary, size = 18.dp)
        AeroText(label, style = Aero.type.bodyStrong.copy(color = c.textPrimary), maxLines = 1)
    }
}

/**
 * Onboarding/SummaryBlock: a glass card with the title (Label/Section, text/muted), the lines (Body, text/primary, one per
 * line) and the pencil that reopens those questions. The whole card takes the tap.
 */
@Composable
fun AeroSummaryBlock(title: String, lines: List<String>, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    val c = Aero.colors
    val type = Aero.type
    Row(
        modifier
            .fillMaxWidth()
            .aeroGlass(Aero.shapes.card)
            .dietaClick(Haptic.Light, onClick = onEdit)
            .semantics { contentDescription = "Corrigir $title" }
            .padding(start = 21.dp, end = 17.dp, top = 17.dp, bottom = 17.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AeroText(AeroTextTokens.labelSection.cased(title), style = type.labelSection.copy(color = c.textMuted))
            AeroText(lines.joinToString("\n"), style = type.body.copy(color = c.textPrimary))
        }
        AeroIcon(AeroIconName.PencilSimple, c.iconPrimary, size = 24.dp)
    }
}

/** The two status surfaces of Onboarding/FullScreenState (Success, Error). */
enum class AeroStatusTone { Good, Bad }

/**
 * Onboarding/FullScreenState, State=Success | Error: the status surface, the 96 dp mark (accent/on) with the 48 dp icon in
 * the surface colour, Title and Body in accent/on, then the stacked actions 32 dp above the bottom: the inverse primary
 * (accent/on fill, the surface colour label) and, on Error, the outline secondary (1.5 dp accent/on) 12 dp below.
 */
@Composable
fun AeroStatusScreen(
    tone: AeroStatusTone,
    title: String,
    body: String,
    primary: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    secondary: String? = null,
    onSecondary: () -> Unit = {},
    primaryTag: String = "status-primary",
    secondaryTag: String = "status-secondary",
) {
    val c = Aero.colors
    val type = Aero.type
    val surface = if (tone == AeroStatusTone.Good) c.statusGood else c.statusBad
    val on = c.accentOn
    // Figma: Spacer, Content (16 dp gaps inside), Spacer and Actions with no gap between them.
    Column(
        // The surface runs under the system bars (edge to edge); the content keeps clear of them.
        modifier.background(surface).statusBarsPadding().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(96.dp).clip(CircleShape).background(on), contentAlignment = Alignment.Center) {
                AeroIcon(if (tone == AeroStatusTone.Good) AeroIconName.Check else AeroIconName.ExclamationMark, surface, size = 48.dp)
            }
            AeroText(title, Modifier.fillMaxWidth(), style = type.title.copy(color = on, textAlign = TextAlign.Center))
            AeroText(body, Modifier.fillMaxWidth(), style = type.body.copy(color = on, textAlign = TextAlign.Center))
        }
        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusButton(primary, fill = on, label = surface, border = null, onClick = onPrimary, tag = primaryTag)
            if (secondary != null) StatusButton(secondary, fill = Color.Transparent, label = on, border = on, onClick = onSecondary, tag = secondaryTag)
        }
    }
}

@Composable
private fun StatusButton(text: String, fill: Color, label: Color, border: Color?, onClick: () -> Unit, tag: String) {
    Box(
        Modifier
            .fillMaxWidth()
            // 16 dp padding around the 24 dp label; the outline's 1.5 dp stroke is counted in the layout (59 dp).
            .height(if (border != null) 59.dp else 56.dp)
            .clip(pill)
            .background(fill)
            .then(if (border != null) Modifier.border(1.5.dp, border, pill) else Modifier)
            .dietaClick(Haptic.Confirm, onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        AeroText(text, style = Aero.type.button.copy(color = label), maxLines = 1)
    }
}
