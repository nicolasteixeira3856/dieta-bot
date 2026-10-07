package app.fibrai.android.domain

/** A run of reply text, bold or not (ADR-045). */
data class ReplySpan(val text: String, val bold: Boolean = false)

/** A block of a formatted reply: what the bubble draws, in order. */
sealed interface ReplyBlock {
    data class Paragraph(val spans: List<ReplySpan>) : ReplyBlock

    /** `- ` item: an option, an ingredient, a food to avoid. */
    data class Bullet(val spans: List<ReplySpan>) : ReplyBlock

    /** `1. ` item: a preparation step. */
    data class Step(val number: Int, val spans: List<ReplySpan>) : ReplyBlock

    /** The one two-column table: its header and one to six rows. */
    data class Table(val header: Pair<String, String>, val rows: List<Pair<String, String>>) : ReplyBlock
}

/**
 * The reply formatting subset (A60 part C, ADR-045): `**bold**`, `- ` bullets, `1. ` steps and one two-column table of at
 * most six rows. Pure Kotlin, no library, no recursion, linear in the reply. Anything outside the subset is literal text:
 * an unbalanced `**` keeps its asterisks, a broken or second table stays as its lines.
 */
object ReplyMarkup {
    const val TABLE_MAX_ROWS = 6

    private val STEP = Regex("""^(\d{1,2})\. (.*)$""")
    private val SEPARATOR = Regex("""^\|?\s*:?-{2,}:?\s*\|\s*:?-{2,}:?\s*\|?$""")

    fun parse(reply: String): List<ReplyBlock> {
        val lines = reply.split('\n').map { it.trimEnd() }
        val out = mutableListOf<ReplyBlock>()
        var tableUsed = false
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (line.isBlank()) {
                i++
                continue
            }
            if (line.startsWith("|")) {
                val end = (i until lines.size).firstOrNull { !lines[it].startsWith("|") } ?: lines.size
                val table = if (tableUsed) null else table(lines.subList(i, end))
                if (table != null) {
                    out += table
                    tableUsed = true
                } else {
                    lines.subList(i, end).forEach { out += ReplyBlock.Paragraph(listOf(ReplySpan(it))) }
                }
                i = end
                continue
            }
            out += when {
                line.startsWith("- ") -> ReplyBlock.Bullet(spans(line.substring(2)))
                STEP.matches(line) -> STEP.find(line)!!.let { ReplyBlock.Step(it.groupValues[1].toInt(), spans(it.groupValues[2])) }
                else -> ReplyBlock.Paragraph(spans(line))
            }
            i++
        }
        return out
    }

    /** A header, a separator and one to six rows, two cells each; anything else is not a table. */
    private fun table(rows: List<String>): ReplyBlock.Table? {
        if (rows.size < 3 || !SEPARATOR.matches(rows[1].trim())) return null
        val cells = rows.map { row -> row.trim().removePrefix("|").removeSuffix("|").split('|').map { it.trim() } }
        val body = cells.drop(2)
        if (cells[0].size != 2 || body.isEmpty() || body.size > TABLE_MAX_ROWS || body.any { it.size != 2 }) return null
        return ReplyBlock.Table(cells[0][0] to cells[0][1], body.map { it[0] to it[1] })
    }

    /** Balanced `**` pairs become bold runs; an odd count leaves the line literal. */
    fun spans(line: String): List<ReplySpan> {
        val parts = line.split("**")
        if (parts.size % 2 == 0) return listOf(ReplySpan(line))
        return parts.mapIndexedNotNull { i, text -> text.takeIf { it.isNotEmpty() }?.let { ReplySpan(it, bold = i % 2 == 1) } }
    }

    /**
     * The reply without the markers, for every place that keeps or sends text (history, receipts, the composer): bold
     * markers dropped, a bullet's `- ` dropped, steps keep their numbers, a table becomes `{item}: {grams}` lines.
     */
    fun plain(reply: String): String = parse(reply).joinToString("\n") { block ->
        when (block) {
            is ReplyBlock.Paragraph -> block.spans.joinToString("") { it.text }
            is ReplyBlock.Bullet -> block.spans.joinToString("") { it.text }
            is ReplyBlock.Step -> "${block.number}. " + block.spans.joinToString("") { it.text }
            is ReplyBlock.Table -> block.rows.joinToString("\n") { (item, grams) -> "$item: $grams" }
        }
    }

    /** True when [reply] carries any marker of the subset (a bubble that renders blocks, not plain prose). */
    fun formatted(reply: String): Boolean = parse(reply).any { block ->
        block !is ReplyBlock.Paragraph || block.spans.any { it.bold }
    }
}
