package com.nutri.android.feature.chat

import com.google.common.truth.Truth.assertThat
import com.nutri.android.core.database.ChatMessageEntity
import com.nutri.android.core.database.DayDigestEntity
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
    fun `text is capped at 1000 chars`() {
        val body = PromptBuilder.build(HomeFixtures.home0, emptyList(), emptyList(), "x".repeat(1500), now).body
        assertThat(body.text).hasLength(1000)
    }
}
