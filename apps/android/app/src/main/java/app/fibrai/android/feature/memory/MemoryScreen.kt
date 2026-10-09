package app.fibrai.android.feature.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.fibrai.android.core.designsystem.Haptic
import app.fibrai.android.core.designsystem.aero.Aero
import app.fibrai.android.core.designsystem.aero.AeroBubbleSpec
import app.fibrai.android.core.designsystem.aero.AeroConfirmDialog
import app.fibrai.android.core.designsystem.aero.AeroIcon
import app.fibrai.android.core.designsystem.aero.AeroIconButton
import app.fibrai.android.core.designsystem.aero.AeroIconName
import app.fibrai.android.core.designsystem.aero.AeroPage
import app.fibrai.android.core.designsystem.aero.AeroPageBubbles
import app.fibrai.android.core.designsystem.aero.AeroText
import app.fibrai.android.core.designsystem.aero.AeroTextTokens
import app.fibrai.android.core.designsystem.aero.aeroGlass
import app.fibrai.android.core.designsystem.aero.cased
import app.fibrai.android.core.designsystem.dietaClick

/** The taps of memL. */
class MemoryActions(
    val onBack: () -> Unit = {},
    val onEdit: (String) -> Unit = {},
    val onDraft: (String) -> Unit = {},
    val onCancelEdit: () -> Unit = {},
    val onSaveEdit: () -> Unit = {},
    val onDelete: (String) -> Unit = {},
    val onConfirmDelete: () -> Unit = {},
    val onCancelDelete: () -> Unit = {},
)

/**
 * memL (D24, A69): "O que a Tali sabe". Groups `FIXAS`, `ROTINAS`, `TEMPORÁRIAS`, each with its explanation and a glass card of
 * facts: the category chip with correct (pencil) and delete (trash), the text, a routine's numbers, the origin. Correcting puts
 * the text in a field with Cancelar | Salvar; deleting asks first.
 */
@Composable
fun MemoryScreen(ui: MemoryUiState, actions: MemoryActions) {
    val scroll = rememberScrollState()
    Box(Modifier.fillMaxSize().testTag("memL")) {
        AeroPage(Modifier.fillMaxSize().then(if (ui.confirmDelete != null) Modifier.blur(8.dp) else Modifier), scroll) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().verticalScroll(scroll).navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Row(Modifier.fillMaxWidth().height(44.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AeroIconButton(AeroIconName.CaretLeft, actions.onBack, contentDescription = "Voltar", modifier = Modifier.testTag("mem-back"))
                    AeroText("O que a Tali sabe", style = Aero.type.title.copy(color = Aero.colors.textPrimary))
                }
                if (ui.loaded && ui.groups.isEmpty()) {
                    AeroText("A Tali ainda não guardou nada sobre você.", Modifier.testTag("mem-empty"), style = Aero.type.body.copy(color = Aero.colors.textMuted))
                }
                ui.groups.forEach { group -> Group(group, ui, actions) }
            }
            AeroPageBubbles(Bubbles, scroll, Modifier.statusBarsPadding())
        }
        val deleting = ui.confirmDelete?.let { id -> ui.groups.flatMap { it.facts }.firstOrNull { it.id == id } }
        if (deleting != null) {
            AeroConfirmDialog(
                title = "Apagar da memória?",
                body = "A Tali esquece: ${deleting.text}",
                primary = "Apagar",
                onPrimary = actions.onConfirmDelete,
                secondary = "Cancelar",
                onSecondary = actions.onCancelDelete,
                dangerIcon = AeroIconName.Trash,
                primaryTag = "mem-delete-confirm",
                secondaryTag = "mem-delete-cancel",
                modifier = Modifier.testTag("mem-delete-dialog"),
            )
        }
    }
}

@Composable
private fun Group(group: FactGroup, ui: MemoryUiState, actions: MemoryActions) {
    val c = Aero.colors
    val type = Aero.type
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AeroText(AeroTextTokens.labelSection.cased(group.label), style = type.labelSection.copy(color = c.textMuted))
            AeroText(group.detail, style = type.caption.copy(color = c.textDim))
        }
        Column(Modifier.fillMaxWidth().aeroGlass(Aero.shapes.card).padding(1.dp)) {
            group.facts.forEachIndexed { i, fact ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(c.borderLine))
                FactRow(fact, ui.editing == fact.id, ui.draft, actions)
            }
        }
    }
}

@Composable
private fun FactRow(fact: FactView, editing: Boolean, draft: String, actions: MemoryActions) {
    val c = Aero.colors
    val type = Aero.type
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp).testTag("mem-fact-${fact.id}"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AeroText(
                fact.chip,
                Modifier.clip(RoundedCornerShape(50)).background(c.surface2).border(1.dp, c.borderLine, RoundedCornerShape(50))
                    .padding(horizontal = 13.dp, vertical = 7.dp),
                style = type.caption.copy(color = c.textMuted),
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            if (!editing) {
                Box(Modifier.size(32.dp).dietaClick(Haptic.Light) { actions.onEdit(fact.id) }.testTag("mem-edit-${fact.id}"), contentAlignment = Alignment.Center) {
                    AeroIcon(AeroIconName.PencilSimple, c.iconPrimary, size = 20.dp)
                }
                Spacer(Modifier.size(4.dp))
                Box(Modifier.size(32.dp).dietaClick(Haptic.Light) { actions.onDelete(fact.id) }.testTag("mem-delete-${fact.id}"), contentAlignment = Alignment.Center) {
                    AeroIcon(AeroIconName.Trash, c.iconPrimary, size = 20.dp)
                }
            }
        }
        if (editing) {
            val shape = RoundedCornerShape(24.dp)
            BasicTextField(
                value = draft,
                onValueChange = actions.onDraft,
                textStyle = type.body.copy(color = c.textPrimary),
                cursorBrush = SolidColor(c.accentDefault),
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).clip(shape).background(c.surface2).border(1.5.dp, c.accentDefault, shape)
                    .padding(horizontal = 16.dp, vertical = 13.dp).testTag("mem-field"),
            )
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(25.dp, Alignment.End)) {
                AeroText("Cancelar", Modifier.dietaClick(Haptic.Light, onClick = actions.onCancelEdit).testTag("mem-cancel"), style = type.button.copy(color = c.textMuted))
                AeroText("Salvar", Modifier.dietaClick(Haptic.Confirm, onClick = actions.onSaveEdit).testTag("mem-save"), style = type.button.copy(color = c.accentDefault))
            }
        } else {
            AeroText(fact.text, style = type.body.copy(color = c.textPrimary))
            fact.macros?.let { (kcal, p, carbs, g) ->
                AeroText(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = c.textPrimary)) { append("$kcal kcal") }
                        withStyle(SpanStyle(color = c.textDim)) { append(" · ") }
                        withStyle(SpanStyle(color = c.macroProtein)) { append("${p}P") }
                        withStyle(SpanStyle(color = c.textDim)) { append(" · ") }
                        withStyle(SpanStyle(color = c.macroCarbs)) { append("${carbs}C") }
                        withStyle(SpanStyle(color = c.textDim)) { append(" · ") }
                        withStyle(SpanStyle(color = c.macroFat)) { append("${g}G") }
                    },
                    style = type.caption,
                )
            }
            AeroText(fact.origin, style = type.caption.copy(color = c.textMuted))
        }
    }
}

/** The cfg frame's page bubbles (memL reuses the cfg frame). */
private val Bubbles = listOf(
    AeroBubbleSpec((-50).dp, (-60).dp, 80.dp),
    AeroBubbleSpec(372.dp, 520.dp, 46.dp),
    AeroBubbleSpec((-22).dp, 820.dp, 40.dp),
)
