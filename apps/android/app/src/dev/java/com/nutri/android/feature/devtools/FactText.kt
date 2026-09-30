package com.nutri.android.feature.devtools

import com.nutri.android.domain.Fact
import com.nutri.android.domain.Memory
import com.nutri.android.domain.MemoryRules
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * A28: the memory facts as one editable line each, and back.
 * `P1 | preference | leite | Leite semidesnatado`
 * `D2 | routine | cafe | slot=1 | 2 ovos mexidos | 440 kcal 25P 38C 22G` (macros optional)
 * Text, key and category can change; `novo | …` adds a permanent fact. Nothing is deleted (ADR-019).
 */
object FactText {
    const val NEW = "novo"
    private const val SEP = " | "
    private val MACROS = Regex("""^(\d{1,5}) kcal (\d{1,4})P (\d{1,4})C (\d{1,4})G$""")
    private val DATE = DateTimeFormatter.ofPattern("dd/MM")

    sealed interface Result {
        data class Ok(val memory: Memory) : Result
        data class Error(val message: String) : Result
    }

    fun format(facts: List<Fact>): String = facts.joinToString("\n", transform = ::line)

    fun line(f: Fact): String = buildList {
        add(f.id)
        add(f.category)
        add(f.key)
        if (f.category == MemoryRules.ROUTINE) add("slot=${f.slot ?: ""}")
        add(f.text)
        if (f.category == MemoryRules.ROUTINE && f.kcal != null) add("${f.kcal} kcal ${f.p ?: 0}P ${f.c ?: 0}C ${f.g ?: 0}G")
    }.joinToString(SEP)

    /** Read-only header: `Permanente 3/30 · Dinâmica 5/40`. */
    fun summary(facts: List<Fact>): String =
        "Permanente ${facts.count { it.permanent }}/${MemoryRules.PERMANENT_MAX} · " +
            "Dinâmica ${facts.count { !it.permanent }}/${MemoryRules.DYNAMIC_MAX}"

    /** Read-only, per fact: `P1 · visto 3 dias · último 29/09`. */
    fun seen(f: Fact): String {
        val last = f.lastSeen?.let { LocalDate.parse(it).format(DATE) } ?: "-"
        val days = if (f.days.size == 1) "1 dia" else "${f.days.size} dias"
        return "${f.id} · visto $days · último $last"
    }

    /**
     * [current] is the memory shown on screen: its order gives the number of a removed line.
     * [slotIds]: every stored slot id, the only values a new or changed `slot=` accepts.
     */
    fun parse(text: String, current: Memory, today: LocalDate, slotIds: Set<String>): Result {
        val byId = current.facts.associateBy { it.id }
        val seenIds = mutableSetOf<String>()
        val keys = mutableMapOf<String, Int>()
        val facts = mutableListOf<Fact>()
        var nextP = current.next.p
        text.lines().forEachIndexed { index, raw ->
            val n = index + 1
            if (raw.isBlank()) return@forEachIndexed
            val parts = raw.split('|').map { it.trim() }
            if (parts.size < 4) return Result.Error("Linha $n: use id | categoria | chave | texto.")
            val id = parts[0]
            val category = parts[1]
            val key = parts[2]
            val old = byId[id]
            when {
                id != NEW && old == null -> return Result.Error("Linha $n: id desconhecido $id.")
                id != NEW && !seenIds.add(id) -> return Result.Error("Linha $n: $id repetido.")
                category !in MemoryRules.CATEGORIES -> return Result.Error("Linha $n: categoria aceita preference, portion ou routine.")
            }
            checkSize(n, "chave", "vazia", key, MemoryRules.KEY_MAX)?.let { return Result.Error(it) }
            val normalized = key.lowercase()
            keys[normalized]?.let { return Result.Error("Linha $n: chave $key repetida (linha $it).") }
            keys[normalized] = n

            val routine = category == MemoryRules.ROUTINE
            var slot: String? = null
            var macros: List<Int>? = null
            val body: List<String>
            if (routine) {
                if (parts.size < 5 || !parts[3].startsWith("slot=")) return Result.Error("Linha $n: rotina precisa de slot=ID.")
                slot = parts[3].removePrefix("slot=").trim()
                // A slot removed in Config after the fact was stored stays as it is; only a new or changed one is checked.
                if (slot !in slotIds && slot != old?.slot) return Result.Error("Linha $n: slot $slot não existe.")
                val m = MACROS.find(parts.last())
                if (m != null && parts.size >= 6) macros = m.groupValues.drop(1).map { it.toInt() }
                body = parts.subList(4, if (macros != null) parts.size - 1 else parts.size)
            } else {
                body = parts.subList(3, parts.size)
            }
            val factText = body.joinToString(SEP)
            checkSize(n, "texto", "vazio", factText, MemoryRules.TEXT_MAX)?.let { return Result.Error(it) }

            val base = old ?: Fact(
                id = "P${nextP++}",
                kind = MemoryRules.PERMANENT,
                category = category,
                key = key,
                text = factText,
                source = MemoryRules.EXPLICIT,
                days = listOf(today.toString()),
                created = today.toString(),
            )
            facts += base.copy(
                category = category,
                key = key,
                text = factText,
                slot = slot,
                kcal = if (routine) macros?.get(0) ?: base.kcal else null,
                p = if (routine) macros?.get(1) ?: base.p else null,
                c = if (routine) macros?.get(2) ?: base.c else null,
                g = if (routine) macros?.get(3) ?: base.g else null,
            )
        }
        current.facts.forEachIndexed { i, f ->
            if (f.id !in seenIds) return Result.Error("Linha ${i + 1}: remover não é permitido (${f.id}). Esquecer é pelo Chat.")
        }
        if (facts.count { it.permanent } > MemoryRules.PERMANENT_MAX) return Result.Error("Permanente acima de ${MemoryRules.PERMANENT_MAX}.")
        if (facts.count { !it.permanent } > MemoryRules.DYNAMIC_MAX) return Result.Error("Dinâmica acima de ${MemoryRules.DYNAMIC_MAX}.")
        return Result.Ok(Memory(current.next.copy(p = nextP), facts))
    }

    private fun checkSize(n: Int, name: String, empty: String, value: String, max: Int): String? = when {
        value.isBlank() -> "Linha $n: $name $empty."
        value.codePointCount(0, value.length) > max -> "Linha $n: $name com até $max caracteres."
        else -> null
    }
}
