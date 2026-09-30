package com.nutri.android.domain

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class MemoryRulesTest {
    private val today = LocalDate.parse("2026-09-30")
    private val d = today.toString()

    private fun add(kind: String, key: String, text: String, category: String = "preference", slot: String? = null) =
        MemoryUpdate("add", null, kind, category, key, text, slot)

    private fun op(op: String, id: String, text: String = "", kind: String = "dynamic", category: String = "preference") =
        MemoryUpdate(op, id, kind, category, "k", text)

    private fun fact(
        id: String,
        key: String,
        days: List<String> = listOf(d),
        kind: String = if (id.startsWith("P")) "permanent" else "dynamic",
        category: String = "preference",
        text: String = "texto $key",
    ) = Fact(id, kind, category, key, text, source = if (kind == "permanent") "explicit" else "observed", days = days, created = days.firstOrNull() ?: d)

    private fun memory(vararg facts: Fact, nextP: Int = 10, nextD: Int = 10) = Memory(NextIds(nextP, nextD), facts.toList())

    private fun daysBack(vararg n: Long) = n.map { today.minusDays(it).toString() }.sorted()

    @Test
    fun addPermanent_new_getsPnExplicit() {
        val r = MemoryRules.apply(Memory(), listOf(add("permanent", "leite", "Leite semidesnatado")), today)
        val f = r.memory.facts.single()
        assertThat(f.id).isEqualTo("P1")
        assertThat(f.kind).isEqualTo("permanent")
        assertThat(f.source).isEqualTo("explicit")
        assertThat(f.days).containsExactly(d)
        assertThat(f.created).isEqualTo(d)
        assertThat(r.memory.next).isEqualTo(NextIds(2, 1))
        assertThat(r.changed).isTrue()
        assertThat(r.counts["add"]).isEqualTo(1)
    }

    @Test
    fun addPermanent_sameKey_contradictionKeepsIdAndTakesNewText() {
        val m = memory(fact("P1", "leite", text = "Leite semidesnatado", days = daysBack(3)))
        val r = MemoryRules.apply(m, listOf(add("permanent", "Leite", "Leite integral")), today)
        val f = r.memory.facts.single()
        assertThat(f.id).isEqualTo("P1")
        assertThat(f.text).isEqualTo("Leite integral")
        assertThat(f.days).containsExactly(today.minusDays(3).toString(), d).inOrder()
    }

    @Test
    fun addPermanent_sameKeyOfDynamic_becomesPermanentWithNewId() {
        val m = memory(fact("D4", "iogurte", days = daysBack(1)))
        val r = MemoryRules.apply(m, listOf(add("permanent", "iogurte", "Iogurte natural sem açúcar")), today)
        val f = r.memory.facts.single()
        assertThat(f.id).isEqualTo("P10")
        assertThat(f.kind).isEqualTo("permanent")
        assertThat(f.source).isEqualTo("explicit")
        assertThat(f.text).isEqualTo("Iogurte natural sem açúcar")
    }

    @Test
    fun addPermanent_full_isIgnored() {
        val full = memory(*Array(30) { fact("P$it", "k$it") })
        val r = MemoryRules.apply(full, listOf(add("permanent", "queijo", "Queijo minas")), today)
        assertThat(r.memory.facts).hasSize(30)
        assertThat(r.memory.facts.none { it.key == "queijo" }).isTrue()
        assertThat(r.changed).isFalse()
    }

    @Test
    fun addDynamic_new_getsDnObserved_sameKeyReinforces() {
        val r1 = MemoryRules.apply(Memory(), listOf(add("dynamic", "pao", "Pão francês")), today)
        assertThat(r1.memory.facts.single().id).isEqualTo("D1")
        assertThat(r1.memory.facts.single().source).isEqualTo("observed")

        val tomorrow = today.plusDays(1)
        val r2 = MemoryRules.apply(r1.memory, listOf(add("dynamic", "pao", "Pão de forma")), tomorrow)
        val f = r2.memory.facts.single()
        assertThat(f.id).isEqualTo("D1")
        assertThat(f.text).isEqualTo("Pão de forma")
        assertThat(f.days).containsExactly(d, tomorrow.toString()).inOrder()
        assertThat(r2.counts["reinforce"]).isEqualTo(1)
    }

    @Test
    fun addDynamic_sameKeyOfPermanent_reinforcesWithoutTouchingText() {
        val m = memory(fact("P1", "leite", text = "Leite semidesnatado", days = daysBack(2)))
        val r = MemoryRules.apply(m, listOf(add("dynamic", "leite", "Leite")), today)
        val f = r.memory.facts.single()
        assertThat(f.text).isEqualTo("Leite semidesnatado")
        assertThat(f.days).hasSize(2)
    }

    @Test
    fun addDynamic_over40_oldestLastSeenLeaves() {
        val facts = (1..40).map { fact("D$it", "k$it", days = daysBack(if (it == 7) 15 else 1)) }
        val r = MemoryRules.apply(memory(*facts.toTypedArray(), nextD = 41), listOf(add("dynamic", "novo", "Fato novo")), today)
        assertThat(r.memory.facts.count { !it.permanent }).isEqualTo(40)
        assertThat(r.memory.facts.map { it.id }).doesNotContain("D7")
        assertThat(r.memory.facts.last().id).isEqualTo("D41")
    }

    @Test
    fun reinforce_addsTodayOnce_dynamicTakesText_permanentKeepsText() {
        val m = memory(fact("D1", "pao", days = daysBack(2)), fact("P1", "leite", text = "Leite semidesnatado"))
        val r = MemoryRules.apply(
            m,
            listOf(op("reinforce", "D1", "Pão integral"), op("reinforce", "D1"), op("reinforce", "P1", "Leite")),
            today,
        )
        val (dyn, perm) = r.memory.facts
        assertThat(dyn.text).isEqualTo("Pão integral")
        assertThat(dyn.days).containsExactly(today.minusDays(2).toString(), d).inOrder()
        assertThat(perm.text).isEqualTo("Leite semidesnatado")
        assertThat(perm.days).containsExactly(d)
    }

    @Test
    fun reinforce_unknownId_isIgnoredAndDoesNotMark() {
        val r = MemoryRules.apply(memory(fact("D1", "pao")), listOf(op("reinforce", "D99")), today)
        assertThat(r.changed).isFalse()
    }

    @Test
    fun replace_changesText_permanentKindPromotesDynamicWhenRoom() {
        val m = memory(fact("P1", "leite"), fact("D2", "cafe"))
        val r = MemoryRules.apply(
            m,
            listOf(op("replace", "P1", "Leite integral", kind = "permanent"), op("replace", "D2", "Café sem açúcar", kind = "permanent")),
            today,
        )
        val (leite, cafe) = r.memory.facts
        assertThat(leite.id).isEqualTo("P1")
        assertThat(leite.text).isEqualTo("Leite integral")
        assertThat(cafe.id).isEqualTo("P10")
        assertThat(cafe.kind).isEqualTo("permanent")
        assertThat(cafe.source).isEqualTo("explicit")
        assertThat(cafe.text).isEqualTo("Café sem açúcar")
    }

    @Test
    fun replace_permanentKindWithoutRoom_keepsDynamicButChangesText() {
        val m = memory(*(Array(30) { fact("P$it", "k$it") } + fact("D1", "cafe")))
        val r = MemoryRules.apply(m, listOf(op("replace", "D1", "Café sem açúcar", kind = "permanent")), today)
        val cafe = r.memory.facts.last()
        assertThat(cafe.id).isEqualTo("D1")
        assertThat(cafe.text).isEqualTo("Café sem açúcar")
    }

    @Test
    fun remove_deletesFact_unknownIgnored() {
        val m = memory(fact("P1", "leite"), fact("D1", "pao"))
        val r = MemoryRules.apply(m, listOf(op("remove", "P1"), op("remove", "P77")), today)
        assertThat(r.memory.facts.map { it.id }).containsExactly("D1")
        assertThat(r.counts["remove"]).isEqualTo(1)
        assertThat(r.changed).isTrue()
    }

    @Test
    fun promotion_fifthDayWithRoom_becomesPromotedPermanentKeepingDays() {
        val m = memory(fact("D3", "cafe", days = daysBack(1, 3, 6, 10)))
        val r = MemoryRules.apply(m, listOf(op("reinforce", "D3")), today)
        val f = r.memory.facts.single()
        assertThat(f.id).isEqualTo("P10")
        assertThat(f.kind).isEqualTo("permanent")
        assertThat(f.source).isEqualTo("promoted")
        assertThat(f.days).hasSize(5)
        assertThat(r.counts["promote"]).isEqualTo(1)
    }

    @Test
    fun promotion_withoutRoom_waits_thenHappensWhenRoomOpens() {
        val full = Array(30) { fact("P$it", "k$it") }
        val m = memory(*(full + fact("D3", "cafe", days = daysBack(1, 3, 6, 10))), nextP = 100)
        val r = MemoryRules.apply(m, listOf(op("reinforce", "D3")), today)
        assertThat(r.memory.facts.last().id).isEqualTo("D3")
        assertThat(r.memory.facts.last().days).hasSize(5)

        val r2 = MemoryRules.apply(r.memory, listOf(op("remove", "P0")), today)
        assertThat(r2.memory.facts.last().id).isEqualTo("P100")
        assertThat(r2.memory.facts.last().source).isEqualTo("promoted")
    }

    @Test
    fun expiration_dynamicSeenOnDay1_goneOnDay22_permanentStays() {
        val day1 = today
        val m = memory(fact("D1", "pao", days = listOf(day1.toString())), fact("P1", "leite", days = listOf(day1.toString())))

        val day21 = MemoryRules.expire(m, day1.plusDays(20))
        assertThat(day21.memory.facts.map { it.id }).containsExactly("D1", "P1")

        val day22 = MemoryRules.expire(m, day1.plusDays(21))
        assertThat(day22.memory.facts.map { it.id }).containsExactly("P1")
        assertThat(day22.memory.facts.single().days).isEmpty()
        assertThat(day22.counts["expire"]).isEqualTo(1)
    }

    @Test
    fun expiration_dropsOldDaysOfALiveFact() {
        val m = memory(fact("D1", "pao", days = daysBack(0, 25, 30)))
        val f = MemoryRules.expire(m, today).memory.facts.single()
        assertThat(f.days).containsExactly(d)
    }

    @Test
    fun routine_takesSlotAndMacrosOfTheRecord() {
        val meal = RecordedMeal("1", 440, 25, 38, 22)
        val r = MemoryRules.apply(Memory(), listOf(add("dynamic", "cafe", "2 ovos mexidos", category = "routine", slot = "2")), today, meal)
        val f = r.memory.facts.single()
        assertThat(f.slot).isEqualTo("1")
        assertThat(listOf(f.kcal, f.p, f.c, f.g)).containsExactly(440, 25, 38, 22).inOrder()
    }

    @Test
    fun strongRoutine_permanentOrThreeDays_withSlotAndKcal() {
        val base = fact("D1", "cafe", category = "routine", days = daysBack(0, 1)).copy(slot = "1", kcal = 440)
        assertThat(MemoryRules.isStrongRoutine(base)).isFalse()
        assertThat(MemoryRules.isStrongRoutine(base.copy(days = daysBack(0, 1, 2)))).isTrue()
        assertThat(MemoryRules.isStrongRoutine(base.copy(kind = "permanent", id = "P1"))).isTrue()
        assertThat(MemoryRules.isStrongRoutine(base.copy(days = daysBack(0, 1, 2), kcal = null))).isFalse()
        assertThat(MemoryRules.isStrongRoutine(base.copy(days = daysBack(0, 1, 2), slot = null))).isFalse()
        assertThat(MemoryRules.isStrongRoutine(base.copy(days = daysBack(0, 1, 2), category = "preference"))).isFalse()
    }

    @Test
    fun textAndKeyAreCleanedAndCut() {
        val r = MemoryRules.apply(Memory(), listOf(add("permanent", "  k".padEnd(60, 'x'), "a  b\n" + "y".repeat(300))), today)
        val f = r.memory.facts.single()
        assertThat(f.key.length).isEqualTo(MemoryRules.KEY_MAX)
        assertThat(f.text).startsWith("a b y")
        assertThat(f.text.length).isEqualTo(MemoryRules.TEXT_MAX)
    }

    @Test
    fun blankAdd_isIgnored() {
        val r = MemoryRules.apply(Memory(), listOf(add("dynamic", " ", "x"), add("dynamic", "k", "  ")), today)
        assertThat(r.memory.facts).isEmpty()
        assertThat(r.changed).isFalse()
    }
}
