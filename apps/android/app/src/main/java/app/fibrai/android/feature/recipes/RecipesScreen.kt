package app.fibrai.android.feature.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
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
import app.fibrai.android.core.designsystem.aero.AeroPortionsTable
import app.fibrai.android.core.designsystem.aero.AeroText
import app.fibrai.android.core.designsystem.aero.AeroTextTokens
import app.fibrai.android.core.designsystem.aero.aeroGlass
import app.fibrai.android.core.designsystem.aero.cased
import app.fibrai.android.core.designsystem.dietaClick
import app.fibrai.android.domain.ReplyBlock

/** rcpL (D23, A68): Config → Receitas, most recent first, each row with its name and `{kcal} kcal · {p}P · {c}C · {g}G`. */
@Composable
fun RecipesScreen(ui: RecipesUiState, onBack: () -> Unit, onOpen: (Long) -> Unit) {
    RecipePage("Receitas", onBack, "rcpL") {
        if (ui.loaded && ui.rows.isEmpty()) {
            AeroText("Nenhuma receita salva. Peça uma receita no Chat e toque em Salvar receita.", Modifier.testTag("rcp-empty"), style = Aero.type.body.copy(color = Aero.colors.textMuted))
        } else if (ui.rows.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().aeroGlass(Aero.shapes.card).padding(1.dp)) {
                ui.rows.forEachIndexed { i, row ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Aero.colors.borderLine))
                    RecipeListRow(row) { onOpen(row.id) }
                }
            }
        }
    }
}

@Composable
private fun RecipeListRow(row: RecipeRow, onClick: () -> Unit) {
    val c = Aero.colors
    val type = Aero.type
    Row(
        Modifier.fillMaxWidth().dietaClick(onClick = onClick).padding(16.dp).testTag("rcp-row-${row.id}"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AeroText(row.name, style = type.body.copy(color = c.textPrimary))
            AeroText(macros(row.kcal, row.p, row.c, row.g), style = type.caption)
        }
        AeroIcon(AeroIconName.CaretRight, c.iconMuted, size = 20.dp)
    }
}

/**
 * rcpD (D23, A68): the current version of a recipe: name, `Versão {n} · salva em {data}` and totals; INGREDIENTES as the
 * portions table; MODO DE PREPARO as numbered steps; Excluir receita (danger) asks first.
 */
@Composable
fun RecipeScreen(ui: RecipesUiState, onBack: () -> Unit, onDelete: () -> Unit, onConfirmDelete: () -> Unit, onCancelDelete: () -> Unit) {
    val d = ui.detail
    Box(Modifier.fillMaxSize()) {
        RecipePage("Receita", onBack, "rcpD", Modifier.then(if (ui.confirmDelete) Modifier.blur(8.dp) else Modifier)) {
            if (d != null) {
                val c = Aero.colors
                val type = Aero.type
                Column(
                    Modifier.fillMaxWidth().aeroGlass(Aero.shapes.card).padding(start = 17.dp, end = 17.dp, top = 17.dp, bottom = 17.dp).testTag("rcp-header"),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    AeroText(d.name, style = type.title.copy(color = c.textPrimary))
                    AeroText(d.versionLine, Modifier.padding(bottom = 8.dp), style = type.caption.copy(color = c.textMuted))
                    AeroText(macros(d.kcal, d.p, d.c, d.g), style = type.body)
                }
                Section("Ingredientes") {
                    Column(Modifier.fillMaxWidth().aeroGlass(Aero.shapes.card).padding(start = 17.dp, end = 17.dp, top = 21.dp, bottom = 17.dp)) {
                        AeroPortionsTable(ReplyBlock.Table("Item" to "Gramas", d.ingredients))
                    }
                }
                if (d.steps.isNotEmpty()) {
                    Section("Modo de preparo") {
                        Column(
                            Modifier.fillMaxWidth().aeroGlass(Aero.shapes.card).padding(horizontal = 17.dp, vertical = 17.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            d.steps.forEachIndexed { i, step ->
                                Row(Modifier.fillMaxWidth().testTag("rcp-step")) {
                                    AeroText("${i + 1}.", Modifier.width(STEP_COLUMN), style = type.body.copy(color = c.textPrimary))
                                    AeroText(step, Modifier.weight(1f), style = type.body.copy(color = c.textPrimary))
                                }
                            }
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().height(44.dp).aeroGlass(Aero.shapes.card).dietaClick(Haptic.Confirm, onClick = onDelete)
                        .padding(horizontal = 15.dp).testTag("rcp-delete"),
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AeroIcon(AeroIconName.Trash, c.iconPrimary, size = 22.dp)
                    AeroText("Excluir receita", style = type.button.copy(color = c.statusBad), maxLines = 1)
                }
            }
        }
        if (ui.confirmDelete && d != null) {
            AeroConfirmDialog(
                title = "Excluir receita?",
                body = "${d.name} sai da sua lista e a Tali deixa de conhecê-la. Os registros feitos com ela continuam.",
                primary = "Excluir",
                onPrimary = onConfirmDelete,
                secondary = "Cancelar",
                onSecondary = onCancelDelete,
                dangerIcon = AeroIconName.Trash,
                primaryTag = "rcp-delete-confirm",
                secondaryTag = "rcp-delete-cancel",
                modifier = Modifier.testTag("rcp-delete-dialog"),
            )
        }
    }
}

/** Header/Page and the cfg page frame (gradient, 20 dp margins, 24 dp gaps, the page bubbles). */
@Composable
private fun RecipePage(title: String, onBack: () -> Unit, tag: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberScrollState()
    AeroPage(modifier.fillMaxSize().testTag(tag), scroll) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(scroll).navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(Modifier.fillMaxWidth().height(44.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AeroIconButton(AeroIconName.CaretLeft, onBack, contentDescription = "Voltar", modifier = Modifier.testTag("$tag-back"))
                AeroText(title, style = Aero.type.title.copy(color = Aero.colors.textPrimary))
            }
            content()
        }
        AeroPageBubbles(Bubbles, scroll, Modifier.statusBarsPadding())
    }
}

/** A block label (Label/Section, text/muted) and its card 12 dp below. */
@Composable
private fun Section(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AeroText(AeroTextTokens.labelSection.cased(label), style = Aero.type.labelSection.copy(color = Aero.colors.textMuted))
        content()
    }
}

@Composable
private fun macros(kcal: Int, p: Int, c: Int, g: Int): AnnotatedString {
    val colors = Aero.colors
    return buildAnnotatedString {
        withStyle(SpanStyle(color = colors.textPrimary)) { append("$kcal kcal") }
        withStyle(SpanStyle(color = colors.textDim)) { append(" · ") }
        withStyle(SpanStyle(color = colors.macroProtein)) { append("${p}P") }
        withStyle(SpanStyle(color = colors.textDim)) { append(" · ") }
        withStyle(SpanStyle(color = colors.macroCarbs)) { append("${c}C") }
        withStyle(SpanStyle(color = colors.textDim)) { append(" · ") }
        withStyle(SpanStyle(color = colors.macroFat)) { append("${g}G") }
    }
}

private val STEP_COLUMN = 21.dp

/** The cfg frame's page bubbles (rcpL and rcpD reuse the cfg frame). */
private val Bubbles = listOf(
    AeroBubbleSpec((-50).dp, (-60).dp, 80.dp),
    AeroBubbleSpec(372.dp, 520.dp, 46.dp),
    AeroBubbleSpec((-22).dp, 820.dp, 40.dp),
)
