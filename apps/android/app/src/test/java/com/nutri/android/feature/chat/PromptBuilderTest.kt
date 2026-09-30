package com.nutri.android.feature.chat

import com.google.common.truth.Truth.assertThat
import com.nutri.android.core.database.ChatMessageEntity
import com.nutri.android.core.database.DayDigestEntity
import com.nutri.android.core.database.MealLogEntity
import com.nutri.android.feature.home.HomeFixtures
import java.time.Instant
import org.junit.Test

class PromptBuilderTest {
    private val now = Instant.parse("2026-09-25T21:10:00-03:00")

    private fun msgs(n: Int, date: String = "2026-09-25", from: Long = 1_000) = (1..n).map { i ->
        ChatMessageEntity(
            id = i.toLong(),
            date = date,
            role = if (i % 2 == 1) "user" else "assistant",
            text = "m$i",
            createdAtEpochMs = from + i,
        )
    }

    @Test
    fun `12 raw messages ask for compact when enabled`() {
        val turn = PromptBuilder.build(HomeFixtures.home0, msgs(12), emptyList(), "jantar", now, compactEnabled = true)
        assertThat(turn.needsCompact).isTrue()
        assertThat(turn.body.compact).isFalse()
        val compact = PromptBuilder.compact(turn)
        assertThat(compact.compact).isTrue()
        assertThat(compact.messages).hasSize(12)
    }

    @Test
    fun `compact is on by default since S3 - 12 raw ask for it, 11 do not`() {
        assertThat(PromptBuilder.COMPACT_ENABLED).isTrue()
        assertThat(PromptBuilder.build(HomeFixtures.home0, msgs(12), emptyList(), "jantar", now).needsCompact).isTrue()
        assertThat(PromptBuilder.build(HomeFixtures.home0, msgs(11), emptyList(), "jantar", now).needsCompact).isFalse()
    }

    @Test
    fun `compact off - 13th raw message is not in the IN, oldest leaves`() {
        val turn = PromptBuilder.build(HomeFixtures.home0, msgs(13), emptyList(), "jantar", now, compactEnabled = false)
        assertThat(turn.needsCompact).isFalse()
        assertThat(turn.body.messages).hasSize(12)
        assertThat(turn.body.messages.first().text).isEqualTo("m2")
        assertThat(turn.body.messages.last().text).isEqualTo("m13")
        assertThat(turn.body.compact).isFalse()
    }

    @Test
    fun `digest cuts older raw, max 2 digests, receipts never sent`() {
        val digests = listOf(
            DayDigestEntity("2026-09-25", 1, "d1", createdAtEpochMs = 1_004),
            DayDigestEntity("2026-09-25", 2, "d2", createdAtEpochMs = 1_006),
        )
        val all = msgs(8) + ChatMessageEntity(id = 99, date = "2026-09-25", role = "logged", text = "Almoço", createdAtEpochMs = 1_007)
        val turn = PromptBuilder.build(HomeFixtures.home0, all, digests, "oi", now, compactEnabled = true)
        assertThat(turn.body.digests).containsExactly("d1", "d2").inOrder()
        assertThat(turn.body.messages.map { it.text }).containsExactly("m7", "m8").inOrder()
        assertThat(turn.body.messages.map { it.role }.toSet()).containsExactly("user", "assistant")
        assertThat(turn.needsCompact).isFalse()
    }

    @Test
    fun `profile and day snapshot carry meta, slots and statuses`() {
        val day = HomeFixtures.home1.copy(eat = "partial", pct = 40, workoutKcal = 300)
        val body = PromptBuilder.build(day, emptyList(), emptyList(), "2 ovos", now).body
        assertThat(body.localTime).isEqualTo("2026-09-25T21:10:00-03:00")
        assertThat(body.profile.ceilingKcal).isEqualTo(2120)
        assertThat(body.profile.eatBack).isEqualTo("partial 40%")
        assertThat(body.profile.slots.map { it.id to it.time })
            .containsExactly("1" to "07:30", "2" to "12:30", "3" to "16:00", "4" to "20:00").inOrder()
        assertThat(body.day.eatenKcal).isEqualTo(1300)
        assertThat(body.day.slots.map { it.status }).containsExactly("eaten", "eaten", "skipped", "empty").inOrder()
        assertThat(body.day.slots.first().kcal).isEqualTo(520)
        assertThat(body.memory).isEmpty()
        assertThat(body.imageB64).isNull()
        assertThat(body.text).isEqualTo("2 ovos")
    }

    @Test
    fun `wipe marker restarts the prompt, thread before it is not sent`() {
        val before = msgs(4)
        val wiped = ChatMessageEntity(id = 5, date = "2026-09-25", role = "wiped", createdAtEpochMs = 1_005)
        val after = msgs(2, from = 1_005).map { it.copy(id = it.id + 5) }
        val turn = PromptBuilder.build(HomeFixtures.home0, before + wiped + after, emptyList(), "oi", now)
        assertThat(turn.body.messages.map { it.text }).containsExactly("m1", "m2").inOrder()
        assertThat(turn.body.messages.map { it.role }).doesNotContain("wiped")
    }

    @Test
    fun `text is capped at 2000 code points, 1500 goes whole`() {
        val whole = PromptBuilder.build(HomeFixtures.home0, emptyList(), emptyList(), "x".repeat(1500), now).body
        assertThat(whole.text).hasLength(1500)
        val capped = PromptBuilder.build(HomeFixtures.home0, emptyList(), emptyList(), "x".repeat(2500), now).body
        assertThat(capped.text).hasLength(2000)
        // 2000 emojis are 4000 UTF-16 chars: nothing is cut (ADR-022 counts code points).
        val emoji = PromptBuilder.build(HomeFixtures.home0, emptyList(), emptyList(), "🍚".repeat(2000), now).body
        assertThat(emoji.text).isEqualTo("🍚".repeat(2000))
    }

    @Test fun saturdayProfileAndSnapshotOnlyContainWeekendSlots() {
        val day = HomeFixtures.home1.copy(
            slotMode = "split",
            slots = listOf(
                com.nutri.android.core.database.MealSlot(1, "Útil", 450, 31),
                com.nutri.android.core.database.MealSlot(2, "Fim de semana", 570, 96),
            ),
        )
        val body = PromptBuilder.build(day, emptyList(), emptyList(), "café", Instant.parse("2026-09-26T12:00:00Z")).body
        assertThat(body.profile.slots.map { it.id }).containsExactly("2")
        assertThat(body.day.slots.map { it.id }).containsExactly("2")
        assertThat(body.day.eatenKcal).isEqualTo(day.logs.sumOf { it.kcal })
    }

    private fun log(id: Long, date: String, slotId: Long?, text: String = "r$id", kcal: Int = 100) =
        MealLogEntity(id = id, date = date, text = text, kcal = kcal, p = 10, slotId = slotId, carbs = 12, fat = 4)

    @Test
    fun `recent - 7 days before today, oldest first, slot time order, Outros last`() {
        val logs = listOf(
            log(1, "2026-09-25", 1), // today: out
            log(2, "2026-09-17", 1), // 8 days ago: out
            log(3, "2026-09-18", 4, "jantar 18"),
            log(4, "2026-09-24", 4, "jantar ontem"),
            log(5, "2026-09-24", null, "sem slot"),
            log(6, "2026-09-24", 1, "café ontem"),
            log(7, "2026-09-24", 99, "slot apagado"),
            log(8, "2026-09-18", 2, "almoço 18"),
        )
        val recent = PromptBuilder.build(HomeFixtures.home1, emptyList(), emptyList(), "igual ontem", now, recentLogs = logs).body.recent
        assertThat(recent.map { it.text })
            .containsExactly("almoço 18", "jantar 18", "café ontem", "jantar ontem", "sem slot", "slot apagado").inOrder()
        val cafe = recent[2]
        assertThat(cafe.date).isEqualTo("2026-09-24")
        assertThat(cafe.slotId).isEqualTo("1")
        assertThat(cafe.slotName).isEqualTo(HomeFixtures.home1.slots.first { it.id == 1L }.name)
        assertThat(listOf(cafe.kcal, cafe.p, cafe.c, cafe.g)).containsExactly(100, 10, 12, 4).inOrder()
        assertThat(recent.takeLast(2).map { it.slotId to it.slotName }).containsExactly(null to "Outros", null to "Outros")
    }

    @Test
    fun `recent - text cut at 240 code points, at most 42 items and the newest stay`() {
        val long = PromptBuilder.build(
            HomeFixtures.home0, emptyList(), emptyList(), "x", now,
            recentLogs = listOf(log(1, "2026-09-24", 1, "🍚".repeat(300))),
        ).body.recent.single()
        assertThat(long.text).isEqualTo("🍚".repeat(240))

        val many = (1..50).map { i -> log(i.toLong(), "2026-09-${18 + (i - 1) / 8}", null, "r$i") }
        val recent = PromptBuilder.build(HomeFixtures.home0, emptyList(), emptyList(), "x", now, recentLogs = many).body.recent
        assertThat(recent).hasSize(42)
        assertThat(recent.first().text).isEqualTo("r9")
        assertThat(recent.last().text).isEqualTo("r50")
    }

    @Test
    fun `remaining_kcal is effective ceiling minus eaten, with and without workout, may be negative`() {
        fun remaining(eat: String, workout: Int?) = PromptBuilder.build(
            HomeFixtures.home1.copy(kcalSame = 2000, eat = eat, pct = 40, workoutKcal = workout), emptyList(), emptyList(), "x", now,
        ).body.day.remainingKcal
        // home1 eats 1300.
        assertThat(remaining("zero", 300)).isEqualTo(700)
        assertThat(remaining("partial", 300)).isEqualTo(820)
        assertThat(remaining("full", 300)).isEqualTo(1000)
        assertThat(remaining("full", null)).isEqualTo(700)
        val over = PromptBuilder.build(HomeFixtures.homeX.copy(kcalSame = 2000), emptyList(), emptyList(), "x", now).body.day
        assertThat(over.remainingKcal).isEqualTo(2000 - 2280)
        assertThat(PromptBuilder.build(HomeFixtures.home0, emptyList(), emptyList(), "x", now).body.recent).isEmpty()
    }
}
