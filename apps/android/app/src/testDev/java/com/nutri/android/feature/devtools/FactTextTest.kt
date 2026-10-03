package com.nutri.android.feature.devtools

import com.google.common.truth.Truth.assertThat
import com.nutri.android.domain.Fact
import com.nutri.android.domain.Memory
import com.nutri.android.domain.NextIds
import java.time.LocalDate
import org.junit.Test

class FactTextTest {
    private val today = LocalDate.parse("2026-09-30")
    private val leite = Fact("P1", "permanent", "preference", "leite", "Leite semidesnatado", source = "explicit", days = listOf("2026-09-29"), created = "2026-09-20")
    private val cafe = Fact(
        "D2", "dynamic", "routine", "cafe", "2 ovos mexidos, 1 pão", slot = "1", source = "observed",
        days = listOf("2026-09-28", "2026-09-30"), created = "2026-09-28", kcal = 440, p = 25, c = 38, g = 22,
    )
    private val memory = Memory(NextIds(2, 3), listOf(leite, cafe))
    private val slots = setOf("1", "2")

    private fun parse(text: String) = FactText.parse(text, memory, today, slots)

    private fun error(text: String) = (parse(text) as FactText.Result.Error).message

    @Test
    fun format_oneLinePerFact_andBackUnchanged() {
        val text = FactText.format(memory.facts)
        assertThat(text).isEqualTo(
            "P1 | preference | leite | Leite semidesnatado\n" +
                "D2 | routine | cafe | slot=1 | 2 ovos mexidos, 1 pão | 440 kcal 25P 38C 22G",
        )
        assertThat((parse(text) as FactText.Result.Ok).memory).isEqualTo(memory)
    }

    @Test
    fun edits_textKeyCategory_andNewLineIsPermanent() {
        val text = "P1 | portion | leite | 200 ml de leite\n" +
            "D2 | routine | cafe-da-manha | slot=2 | 2 ovos\n" +
            "novo | preference | queijo | Queijo minas"
        val out = (parse(text) as FactText.Result.Ok).memory
        assertThat(out.facts.map { Triple(it.id, it.category, it.text) }).containsExactly(
            Triple("P1", "portion", "200 ml de leite"),
            Triple("D2", "routine", "2 ovos"),
            Triple("P2", "preference", "Queijo minas"),
        ).inOrder()
        assertThat(out.facts[1].key).isEqualTo("cafe-da-manha")
        assertThat(out.facts[1].slot).isEqualTo("2")
        // No macros column: the last record's macros stay.
        assertThat(out.facts[1].kcal).isEqualTo(440)
        assertThat(out.facts[1].days).isEqualTo(cafe.days)
        assertThat(out.facts[2].days).containsExactly("2026-09-30")
        assertThat(out.next).isEqualTo(NextIds(3, 3))
    }

    @Test
    fun routineToPreference_dropsSlotAndMacros() {
        val out = (parse("P1 | preference | leite | Leite\nD2 | preference | cafe | Café preto") as FactText.Result.Ok).memory
        assertThat(out.facts[1].slot).isNull()
        assertThat(out.facts[1].kcal).isNull()
    }

    @Test
    fun refusals_carryTheLineNumber() {
        val ok = FactText.format(memory.facts)
        assertThat(error("$ok\nX9 | preference | a | b")).isEqualTo("Linha 3: id desconhecido X9.")
        assertThat(error("$ok\nP1 | preference | outra | b")).isEqualTo("Linha 3: P1 repetido.")
        assertThat(error("$ok\nnovo | habito | a | b")).isEqualTo("Linha 3: categoria aceita preference, portion ou routine.")
        assertThat(error("$ok\nnovo | preference | LEITE | b")).isEqualTo("Linha 3: chave LEITE repetida (linha 1).")
        assertThat(error("$ok\nnovo | preference | a")).isEqualTo("Linha 3: use id | categoria | chave | texto.")
        assertThat(error("$ok\nnovo | routine | janta | sopa")).isEqualTo("Linha 3: rotina precisa de slot=ID.")
        assertThat(error("$ok\nnovo | routine | janta | slot=9 | sopa")).isEqualTo("Linha 3: slot 9 não existe.")
        assertThat(error("$ok\nnovo | preference | a | ${"x".repeat(161)}")).isEqualTo("Linha 3: texto com até 160 caracteres.")
        assertThat(error("$ok\nnovo | preference | ${"k".repeat(41)} | b")).isEqualTo("Linha 3: chave com até 40 caracteres.")
        assertThat(error("$ok\nnovo | preference | a |  ")).isEqualTo("Linha 3: texto vazio.")
    }

    @Test
    fun slotRemovedInConfig_unchangedRoutineStillSaves() {
        val text = FactText.format(memory.facts)
        val out = (FactText.parse(text, memory, today, setOf("2")) as FactText.Result.Ok).memory
        assertThat(out).isEqualTo(memory)
        assertThat((FactText.parse(text.replace("slot=1", "slot=7"), memory, today, setOf("2")) as FactText.Result.Error).message)
            .isEqualTo("Linha 2: slot 7 não existe.")
    }

    @Test
    fun removedLine_isRefused_withItsOriginalLine() {
        assertThat(error("P1 | preference | leite | Leite semidesnatado")).isEqualTo("Linha 2: remover não é permitido (D2). Esquecer é pelo Chat.")
    }

    @Test
    fun permanentLimit_isChecked() {
        val lines = FactText.format(memory.facts) + (1..30).joinToString("") { "\nnovo | preference | k$it | t$it" }
        assertThat(error(lines)).isEqualTo("Permanente acima de 30.")
    }

    @Test
    fun readOnlyLines() {
        assertThat(FactText.summary(memory.facts)).isEqualTo("Permanente 1/30 · Dinâmica 1/40 · Temporária 0/5")
        assertThat(FactText.seen(cafe)).isEqualTo("D2 · visto 2 dias · último 30/09")
        assertThat(FactText.seen(leite)).isEqualTo("P1 · visto 1 dia · último 29/09")
    }

    // ------------------------------------------------------------------ A38: temp facts

    private val lasanha = Fact("T1", "temp", "portion", "leite", "Lasanha 100 g = 150 kcal", source = "observed", days = listOf("2026-09-29"), created = "2026-09-29")
    private val withTemp = Memory(NextIds(2, 3, 2), listOf(lasanha, leite, cafe))

    @Test
    fun tempLines_formattedLastReadOnly_andCounted() {
        val text = FactText.format(withTemp.facts)
        assertThat(text.lines()).containsExactly(
            "P1 | preference | leite | Leite semidesnatado",
            "D2 | routine | cafe | slot=1 | 2 ovos mexidos, 1 pão | 440 kcal 25P 38C 22G",
            "T1 | temp | portion | leite | Lasanha 100 g = 150 kcal · criado 29/09",
        ).inOrder()
        assertThat(FactText.summary(withTemp.facts)).isEqualTo("Permanente 1/30 · Dinâmica 1/40 · Temporária 1/5")
    }

    @Test
    fun salvarOfAnEditedPermanent_keepsEveryTemp_sameKeyAsTempAccepted_changedTempLineIgnored() {
        val text = FactText.format(withTemp.facts)
            .replace("Leite semidesnatado", "Leite integral")
            .replace("150 kcal · criado 29/09", "999 kcal")
        val out = (FactText.parse(text, withTemp, today, slots) as FactText.Result.Ok).memory
        assertThat(out.facts.single { it.id == "P1" }.text).isEqualTo("Leite integral")
        assertThat(out.facts.single { it.id == "T1" }).isEqualTo(lasanha)
        assertThat(out.next).isEqualTo(withTemp.next)

        // A removed temp line is not an error either: the temp fact stays.
        val withoutTemp = FactText.format(withTemp.facts).lines().filterNot { it.startsWith("T1") }.joinToString("\n")
        assertThat((FactText.parse(withoutTemp, withTemp, today, slots) as FactText.Result.Ok).memory.facts).contains(lasanha)
    }
}
