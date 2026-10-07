package app.fibrai.android.feature.devtools

import app.fibrai.android.core.network.ChatProfile
import app.fibrai.android.core.network.ChatSlot

/**
 * A23: the PROFILE block of the next POST as `chave=valor` lines, and back. Values follow the
 * Config rules. Slots can be renamed and retimed, never added or removed; no key can be removed.
 */
object ProfileText {
    const val CEILING = "teto_kcal"
    const val PROTEIN = "proteina_g"
    const val CARB = "carbo_g"
    const val FAT = "gordura_g"
    const val EAT_BACK = "compensacao"
    const val SLOT = "refeicao."
    private const val MAX_NAME = 40
    private val TIME = Regex("""^(.+?)\s+(\d{1,2}):(\d{2})$""")
    private val PARTIAL = Regex("""^partial\s+(\d{1,3})\s*%$""")

    sealed interface Result {
        data class Ok(val profile: ChatProfile) : Result
        data class Error(val message: String) : Result
    }

    fun format(profile: ChatProfile): String = buildList {
        add("$CEILING=${profile.ceilingKcal}")
        add("$PROTEIN=${profile.pTarget}")
        add("$CARB=${profile.cTarget}")
        add("$FAT=${profile.gTarget}")
        add("$EAT_BACK=${profile.eatBack}")
        profile.slots.forEachIndexed { i, slot -> add("$SLOT${i + 1}=${slot.name} ${slot.time}") }
    }.joinToString("\n")

    /** [current] gives the slot ids (by position) and the set of keys that must stay. */
    fun parse(text: String, current: ChatProfile): Result {
        val values = mutableMapOf<String, String>()
        text.lines().forEachIndexed { index, raw ->
            val n = index + 1
            val line = raw.trim()
            if (line.isEmpty()) return@forEachIndexed
            val eq = line.indexOf('=')
            if (eq <= 0) return Result.Error("Linha $n: esperado chave=valor.")
            val key = line.substring(0, eq).trim()
            val value = line.substring(eq + 1).trim()
            if (key in values) return Result.Error("Linha $n: $key repetida.")
            error(key, value, n, current.slots.size)?.let { return Result.Error(it) }
            values[key] = value
        }
        keys(current.slots.size).firstOrNull { it !in values }?.let {
            return Result.Error("Falta $it: remover não é permitido.")
        }
        return Result.Ok(
            ChatProfile(
                ceilingKcal = values.getValue(CEILING).toInt(),
                pTarget = values.getValue(PROTEIN).toInt(),
                cTarget = values.getValue(CARB).toInt(),
                gTarget = values.getValue(FAT).toInt(),
                eatBack = eatBack(values.getValue(EAT_BACK))!!,
                slots = current.slots.mapIndexed { i, slot ->
                    val m = TIME.find(values.getValue("$SLOT${i + 1}"))!!
                    val (h, min) = m.groupValues[2].toInt() to m.groupValues[3].toInt()
                    ChatSlot(slot.id, m.groupValues[1].trim(), "%02d:%02d".format(h, min))
                },
                // A60: the tone is not part of the dev text; it stays as the app sent it.
                tone = current.tone,
            ),
        )
    }

    /** "zero" | "partial N%" | "full" → the eat-back string of the POST; null when invalid. */
    fun eatBack(value: String): String? {
        val v = value.trim().lowercase()
        if (v == "zero" || v == "full") return v
        val pct = PARTIAL.find(v)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        return if (pct > 0) "partial $pct%" else null
    }

    /** Stored (eat, pct) of a POST eat-back string. pct is null when the mode has none. */
    fun eatAndPct(eatBack: String): Pair<String, Int?> = when {
        eatBack.startsWith("partial") -> "partial" to PARTIAL.find(eatBack)!!.groupValues[1].toInt()
        else -> eatBack to null
    }

    private fun keys(slots: Int) = listOf(CEILING, PROTEIN, CARB, FAT, EAT_BACK) + (1..slots).map { "$SLOT$it" }

    private fun error(key: String, value: String, n: Int, slots: Int): String? = when {
        key == CEILING ->
            if ((value.toIntOrNull() ?: 0) in 1..99_999) null else "Linha $n: $key precisa ser um número maior que 0."
        key == PROTEIN || key == CARB || key == FAT ->
            if (value.toIntOrNull() in 0..9_999) null else "Linha $n: $key precisa ser um número de 0 a 9999."
        key == EAT_BACK ->
            if (eatBack(value) != null) null else "Linha $n: $key aceita zero, partial N% ou full."
        key.startsWith(SLOT) -> {
            val i = key.removePrefix(SLOT).toIntOrNull()
            val m = TIME.find(value)
            when {
                i == null || i < 1 -> "Linha $n: chave desconhecida $key."
                i > slots -> "Linha $n: adicionar refeição não é permitido."
                m == null -> "Linha $n: use Nome HH:MM."
                m.groupValues[2].toInt() > 23 || m.groupValues[3].toInt() > 59 -> "Linha $n: horário inválido."
                m.groupValues[1].trim().length > MAX_NAME -> "Linha $n: nome com até $MAX_NAME caracteres."
                else -> null
            }
        }
        else -> "Linha $n: chave desconhecida $key."
    }
}
