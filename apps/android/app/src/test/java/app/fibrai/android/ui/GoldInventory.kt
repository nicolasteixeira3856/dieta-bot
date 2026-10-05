package app.fibrai.android.ui

import java.io.File

/**
 * The gold inventory of docs/qa/README.md § Golds: the fenced block lists `<id>.png`, each gold in
 * docs/qa/figma/{dark,light}/. Same rule as parseGoldInventory in tools/check-docs.mjs.
 */
object GoldInventory {
    private val SECTION = Regex("""(?ms)^## Golds\s*${'$'}(.*?)(?=^## |\z)""")
    private val BLOCK = Regex("""(?ms)^ {0,3}(`{3,}|~{3,})[^\n]*\n(.*?)^ {0,3}\1""")
    private val ID = Regex("""([A-Za-z0-9]+)\.png""")

    private val ids: Set<String> by lazy { parse(File("../../../docs/qa/README.md").readText()) }

    fun contains(id: String): Boolean = id in ids

    fun parse(readme: String): Set<String> {
        val section = SECTION.find(readme)?.groupValues?.get(1) ?: error("docs/qa/README.md has no '## Golds' section")
        val block = BLOCK.find(section)?.groupValues?.get(2) ?: error("docs/qa/README.md § Golds has no code block")
        val out = LinkedHashSet<String>()
        for (line in block.lines()) ID.findAll(line).forEach { out += it.groupValues[1] }
        return out
    }
}
