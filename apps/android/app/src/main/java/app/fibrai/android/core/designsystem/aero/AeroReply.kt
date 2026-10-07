package app.fibrai.android.core.designsystem.aero

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.fibrai.android.domain.ReplyBlock
import app.fibrai.android.domain.ReplySpan

/** Extra styles laid over a block's plain text (the macro colours of a plan, rule 16): ranges of that text. */
fun interface AeroReplyDecor {
    fun styles(text: String): List<Pair<IntRange, SpanStyle>>
}

/**
 * Chat/Reply (D17, ADR-045): the blocks of a formatted reply, each one a child of the bubble's column (its 12 dp gap).
 * Paragraphs with bold runs in the strong body weight, bullets and steps as rows with a marker column, the portions table
 * as two aligned columns. Tokens only; no Material.
 */
@Composable
fun AeroReplyBlocks(blocks: List<ReplyBlock>, decor: AeroReplyDecor = AeroReplyDecor { emptyList() }) {
    val c = Aero.colors
    val type = Aero.type
    val style = type.body.copy(color = c.textPrimary)
    blocks.forEach { block ->
        when (block) {
            is ReplyBlock.Paragraph -> AeroText(annotate(block.spans, decor), style = style)
            is ReplyBlock.Bullet -> Row(Modifier.fillMaxWidth().testTag("chat-reply-bullet")) {
                Box(Modifier.width(BulletColumn).padding(start = 4.dp)) { AeroText("•", style = style) }
                AeroText(annotate(block.spans, decor), Modifier.weight(1f), style = style)
            }
            is ReplyBlock.Step -> Row(Modifier.fillMaxWidth().testTag("chat-reply-step")) {
                AeroText("${block.number}.", Modifier.width(StepColumn), style = style)
                AeroText(annotate(block.spans, decor), Modifier.weight(1f), style = style)
            }
            is ReplyBlock.Table -> AeroPortionsTable(block)
        }
    }
}

/** The portions table (chatRK): `Item` and `Gramas` muted over a line, then rows of 32 dp with the grams at the end. */
@Composable
fun AeroPortionsTable(table: ReplyBlock.Table) {
    val c = Aero.colors
    val type = Aero.type
    Column(Modifier.fillMaxWidth().testTag("chat-reply-table")) {
        Row(Modifier.fillMaxWidth().padding(bottom = 5.dp)) {
            AeroText(table.header.first, Modifier.weight(1f), style = type.caption.copy(color = c.textMuted), maxLines = 1)
            AeroText(table.header.second, style = type.caption.copy(color = c.textMuted, textAlign = TextAlign.End), maxLines = 1)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.borderLine))
        table.rows.forEach { (item, grams) ->
            Row(Modifier.fillMaxWidth().heightIn(min = RowHeight), verticalAlignment = Alignment.CenterVertically) {
                AeroText(item, Modifier.weight(1f), style = type.body.copy(color = c.textPrimary))
                AeroText(grams, Modifier.padding(start = 12.dp), style = type.body.copy(color = c.textPrimary, textAlign = TextAlign.End), maxLines = 1)
            }
        }
    }
}

/** Bold runs, then [decor] over the whole text (a macro colour stays on a bold number). */
@Composable
private fun annotate(spans: List<ReplySpan>, decor: AeroReplyDecor): AnnotatedString {
    val strong = Aero.type.bodyStrong.fontWeight
    val text = spans.joinToString("") { it.text }
    return buildAnnotatedString {
        spans.forEach { span -> if (span.bold) withStyle(SpanStyle(fontWeight = strong)) { append(span.text) } else append(span.text) }
        decor.styles(text).forEach { (range, style) -> addStyle(style, range.first, range.last + 1) }
    }
}

/** The marker columns of D17: the bullet 17 dp, the step number 21 dp. */
private val BulletColumn = 17.dp
private val StepColumn = 21.dp
private val RowHeight = 32.dp
