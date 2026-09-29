package com.nutri.android.feature.chat

import com.nutri.android.core.database.ChatMessageEntity
import com.nutri.android.core.database.DayDigestEntity
import com.nutri.android.core.database.DayRepository
import com.nutri.android.core.database.DaySnapshot
import com.nutri.android.core.database.metaOn
import com.nutri.android.core.network.ChatDay
import com.nutri.android.core.network.ChatDaySlot
import com.nutri.android.core.network.ChatIn
import com.nutri.android.core.network.ChatProfile
import com.nutri.android.core.network.ChatSlot
import com.nutri.android.core.network.ChatTurn
import com.nutri.android.domain.ChatText
import com.nutri.android.domain.SaoPaulo
import com.nutri.android.domain.SlotSuggestions
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Turn prompt (spec chat rule 8): profile + memory + day snapshot + <= 2 digests + <= 12 raw
 * messages of today since the last digest. The snapshot never compacts.
 */
object PromptBuilder {
    const val MAX_RAW = 12
    const val MAX_DIGESTS = 2
    private val ROLES = setOf("user", "assistant")

    /**
     * Server compact is live since S3 (A5b). 12 raw since the last digest: the next send asks
     * for compact=true first. False turns compaction into a no-op (oldest raw just leave).
     */
    const val COMPACT_ENABLED = true

    data class Turn(val body: ChatIn, val needsCompact: Boolean)

    fun build(
        day: DaySnapshot,
        todayMessages: List<ChatMessageEntity>,
        digests: List<DayDigestEntity>,
        text: String,
        now: Instant,
        compactEnabled: Boolean = COMPACT_ENABLED,
        /** MemoryStore text (A8). Never the profile: that goes in its own block. */
        memory: String = "",
    ): Turn {
        val today = SaoPaulo.date(now)
        val raw = rawSinceDigest(todayMessages, digests)
        return Turn(
            body = ChatIn(
                localTime = now.atZone(SaoPaulo.zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                profile = profile(day, today),
                memory = memory,
                day = snapshot(day, today),
                digests = digests.sortedBy { it.createdAtEpochMs }.takeLast(MAX_DIGESTS).map { it.text },
                messages = raw.takeLast(MAX_RAW).map { ChatTurn(it.role, ChatText.clip(turnText(it))) },
                text = ChatText.clip(text),
                compact = false,
            ),
            needsCompact = compactEnabled && raw.size >= MAX_RAW,
        )
    }

    /** A photo message keeps a marker in the history; the image itself is sent only once (A6). */
    private fun turnText(m: ChatMessageEntity): String = when {
        m.photoPath == null -> m.text
        m.text.isBlank() -> "[foto]"
        else -> "[foto] ${m.text}"
    }

    /** compact=true request: only the raw block to summarise (spec rule 9). */
    fun compact(turn: Turn): ChatIn = turn.body.copy(compact = true, text = "")

    /**
     * Raw user/assistant messages of today newer than the latest digest and the latest wipe
     * (Config ceiling change: the thread stays on screen, the prompt restarts). Receipts are UI only.
     */
    fun rawSinceDigest(todayMessages: List<ChatMessageEntity>, digests: List<DayDigestEntity>): List<ChatMessageEntity> {
        val wiped = todayMessages.filter { it.role == DayRepository.ROLE_WIPED }.maxOfOrNull { it.id }
        val cut = digests.maxOfOrNull { it.createdAtEpochMs } ?: Long.MIN_VALUE
        return todayMessages
            .filter { it.role in ROLES && it.createdAtEpochMs > cut && (wiped == null || it.id > wiped) }
            .sortedWith(compareBy({ it.createdAtEpochMs }, { it.id }))
    }

    private fun profile(day: DaySnapshot, today: LocalDate) = ChatProfile(
        ceilingKcal = day.metaOn(today),
        pTarget = day.proteinTargetG,
        cTarget = day.carbTargetG,
        gTarget = day.fatTargetG,
        eatBack = when (day.eat) {
            "partial" -> "partial ${day.pct}%"
            "full" -> "full"
            else -> "zero"
        },
        slots = day.slots.sortedBy { it.minutesFromMidnight }
            .map { ChatSlot(it.id.toString(), it.name, SlotSuggestions.format(it.minutesFromMidnight)) },
    )

    private fun snapshot(day: DaySnapshot, today: LocalDate) = ChatDay(
        date = today.toString(),
        eatenKcal = day.logs.sumOf { it.kcal },
        eatenP = day.logs.sumOf { it.p },
        eatenC = day.logs.sumOf { it.carbs },
        eatenG = day.logs.sumOf { it.fat },
        workoutKcal = day.workoutKcal,
        slots = day.slots.sortedBy { it.minutesFromMidnight }.map { slot ->
            val logs = day.logs.filter { it.slotId == slot.id }
            when {
                logs.isNotEmpty() -> ChatDaySlot(
                    id = slot.id.toString(),
                    status = "eaten",
                    text = logs.joinToString("; ") { it.text },
                    kcal = logs.sumOf { it.kcal },
                    p = logs.sumOf { it.p },
                    c = logs.sumOf { it.carbs },
                    g = logs.sumOf { it.fat },
                )
                slot.id in day.skippedSlotIds -> ChatDaySlot(slot.id.toString(), "skipped")
                else -> ChatDaySlot(slot.id.toString(), "empty")
            }
        },
    )
}
