package app.fibrai.android.feature.chat

import app.fibrai.android.core.closure.DayTotals
import app.fibrai.android.domain.ClosureMeal
import app.fibrai.android.domain.DayBalance
import app.fibrai.android.domain.Fact
import app.fibrai.android.domain.Macros
import app.fibrai.android.feature.home.HomeFixtures
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Test

/** A64 (S33): routine macros in `facts`, the `recent_days` totals, the day balance and its projection. */
class ChatContextFieldsTest {
    private val now = Instant.parse("2026-09-25T21:10:00-03:00")
    private val today = LocalDate.parse("2026-09-25")
    private val json = Json { encodeDefaults = false }

    private fun fact(id: String, category: String, kcal: Int? = null, p: Int? = null, c: Int? = null, g: Int? = null) =
        Fact(id, "dynamic", category, "k$id", "texto $id", slot = null, source = "observed", days = listOf("2026-09-24"), created = "2026-09-24", kcal = kcal, p = p, c = c, g = g)

    @Test
    fun `a routine with all four numbers carries them, one without or a preference does not`() {
        val facts = PromptBuilder.chatFacts(
            listOf(fact("D1", "routine", 480, 27, 40, 24), fact("D2", "routine", 480, 27, null, 24), fact("D3", "preference", 100, 1, 1, 1)),
            emptySet(),
        )
        assertThat(facts.map { listOf(it.kcal, it.p, it.c, it.g) }).containsExactly(
            listOf(480, 27, 40, 24),
            listOf(null, null, null, null),
            listOf(null, null, null, null),
        ).inOrder()
        val encoded = facts.map { json.encodeToString(app.fibrai.android.core.network.ChatFact.serializer(), it) }
        assertThat(encoded[0]).contains("\"kcal\":480")
        assertThat(encoded[1]).doesNotContain("kcal")
    }

    private fun meal(slot: Long, kcal: Int, eaten: Boolean = kcal > 0, skipped: Boolean = false, planned: Int? = null) =
        ClosureMeal(slot, "s$slot", kcal, skipped, planned, eaten)

    @Test
    fun `recent days go newest first with the slot that went furthest over and the slots without record`() {
        val days = listOf(
            DayTotals(today.minusDays(2), listOf(meal(1, 300), meal(2, 900), meal(3, 0, skipped = true), meal(4, 1200)), Macros(2400, 120, 230, 70), 2000, null),
            DayTotals(today.minusDays(1), listOf(meal(1, 0), meal(2, 0), meal(3, 0), meal(9, 0)), Macros(0, 0, 0, 0), 2300, 300),
            // Today and anything older than 7 days never go.
            DayTotals(today, listOf(meal(1, 100)), Macros(100, 5, 5, 5), 2000, null),
            DayTotals(today.minusDays(8), listOf(meal(1, 100)), Macros(100, 5, 5, 5), 2000, null),
        )
        val sent = PromptBuilder.recentDays(days, setOf("1", "2", "3", "4"), today)
        assertThat(sent.map { it.date }).containsExactly("2026-09-24", "2026-09-23").inOrder()
        val unrecorded = sent[0]
        assertThat(unrecorded.recorded).isFalse()
        assertThat(unrecorded.ceilingKcal).isEqualTo(2300)
        // Slot 9 is not a slot of today: left out.
        assertThat(unrecorded.missingSlots).containsExactly("1", "2", "3").inOrder()
        val over = sent[1]
        assertThat(over.recorded).isTrue()
        assertThat(listOf(over.kcal, over.p, over.c, over.g)).containsExactly(2400, 120, 230, 70).inOrder()
        // Share 500: the dinner (1200) is 700 over, the lunch (900) 400.
        assertThat(over.overSlot).isEqualTo("4")
        // Skipped is not missing.
        assertThat(over.missingSlots).isEmpty()
    }

    @Test
    fun `the request carries recent_days and a compact request does not`() {
        val days = listOf(DayTotals(today.minusDays(1), emptyList(), Macros(1800, 100, 200, 60), 2000, null))
        val msgs = (1..12).map { i ->
            app.fibrai.android.core.database.ChatMessageEntity(id = i.toLong(), date = "2026-09-25", role = if (i % 2 == 1) "user" else "assistant", text = "m$i", createdAtEpochMs = 1_000L + i)
        }
        val turn = PromptBuilder.build(HomeFixtures.home0, msgs, emptyList(), "jantar", now, pastDays = days)
        val body = Json.parseToJsonElement(json.encodeToString(app.fibrai.android.core.network.ChatIn.serializer(), turn.body)).jsonObject
        assertThat(body["recent_days"]!!.jsonArray).hasSize(1)
        val compact = PromptBuilder.compact(turn, turn.blocks.first())
        assertThat(json.encodeToString(app.fibrai.android.core.network.ChatIn.serializer(), compact)).doesNotContain("recent_days")
        val none = PromptBuilder.build(HomeFixtures.home0, emptyList(), emptyList(), "jantar", now)
        assertThat(json.encodeToString(app.fibrai.android.core.network.ChatIn.serializer(), none.body)).doesNotContain("recent_days")
    }

    @Test
    fun `day balance and projection`() {
        assertThat(DayBalance.line(Macros(1240, 98, 120, 40), 2000, 160)).isEqualTo("1.240 de 2.000 kcal · faltam 62 g de proteína")
        assertThat(DayBalance.line(Macros(2100, 170, 120, 40), 2000, 160)).isEqualTo("2.100 de 2.000 kcal · meta de proteína atingida")
        assertThat(DayBalance.projection(Macros(1240, 98, 120, 40), Macros(380, 22, 36, 16), 2000, 160))
            .isEqualTo("Projeção: 1.620 de 2.000 kcal · faltam 40 g de proteína")
    }
}
