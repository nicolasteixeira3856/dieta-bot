package com.nutri.android.ui

import java.io.File

/**
 * The gold inventory of docs/qa/README.md § Golds: each line starts with its source (`stitch` or `figma`) and
 * lists `<id>.png`. Same rule as parseGoldInventory in tools/check-docs.mjs.
 */
object GoldInventory {
    private val SOURCES = setOf("stitch", "figma")
    private val SECTION = Regex("""(?ms)^## Golds\s*${'$'}(.*?)(?=^## |\z)""")
    private val BLOCK = Regex("""(?ms)^ {0,3}(`{3,}|~{3,})[^\n]*\n(.*?)^ {0,3}\1""")
    private val ID = Regex("""([A-Za-z0-9]+)\.png""")
    private val SOURCE = Regex("""^\s*([a-z]+):""")

    private val sources: Map<String, String> by lazy { parse(File("../../../docs/qa/README.md").readText()) }

    fun source(id: String): String = sources[id] ?: error("gold '$id' is not in the inventory of docs/qa/README.md")

    fun parse(readme: String): Map<String, String> {
        val section = SECTION.find(readme)?.groupValues?.get(1) ?: error("docs/qa/README.md has no '## Golds' section")
        val block = BLOCK.find(section)?.groupValues?.get(2) ?: error("docs/qa/README.md § Golds has no code block")
        val out = LinkedHashMap<String, String>()
        for (line in block.lines()) {
            val ids = ID.findAll(line).map { it.groupValues[1] }.toList()
            if (ids.isEmpty()) continue
            val source = SOURCE.find(line)?.groupValues?.get(1)
            require(source in SOURCES) { "inventory line '${line.trim()}' does not start with a source" }
            ids.forEach { out[it] = source!! }
        }
        return out
    }
}
