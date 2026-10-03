package com.nutri.android.feature.chat

import com.nutri.android.core.database.ChatMessageEntity
import com.nutri.android.core.database.DayDigestEntity
import com.nutri.android.core.database.DayRepository
import com.nutri.android.core.database.MealLogEntity
import com.nutri.android.core.database.MealSlot
import com.nutri.android.core.database.slotsOn
import com.nutri.android.core.database.DaySnapshot
import com.nutri.android.core.database.metaOn
import com.nutri.android.core.network.ChatDay
import com.nutri.android.core.network.ChatDaySlot
import com.nutri.android.core.network.ChatFact
import com.nutri.android.core.network.ChatIn
import com.nutri.android.core.network.ChatProfile
import com.nutri.android.core.network.ChatRecentMeal
import com.nutri.android.core.network.ChatSlot
import com.nutri.android.core.network.ChatTurn
import com.nutri.android.domain.ChatText
import com.nutri.android.domain.Fact
import com.nutri.android.domain.SaoPaulo
import com.nutri.android.domain.SlotSuggestions
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Turn prompt (spec chat rule 8): profile + memory facts + day snapshot + meals of the last 7 days +
 * <= 2 digests + <= 12 raw messages of today since the last digest. The snapshot never compacts.
 */
object PromptBuilder {
    const val MAX_RAW = 12
    /** A38: the newest raw messages a compaction leaves out of its block. */
    const val KEEP_RAW = 4
    /** A38: compactions in one send at most. */
    const val MAX_BLOCKS = 2
    const val MAX_DIGESTS = 2
    const val MAX_RECENT = 42
    const val MAX_RECENT_TEXT = 240
    const val OTHERS = "Outros"
    private val ROLES = setOf("user", "assistant")

    /**
     * Server compact is live since S3 (A5b). 12 raw since the last digest: the next send asks
     * for compact=true first. False turns compaction into a no-op (oldest raw just leave).
     */
    const val COMPACT_ENABLED = true

    /** A38: one compact request, the oldest raw [messages], and the id of the newest one it summarises. */
    data class CompactBlock(val messages: List<ChatTurn>, val coversUntilId: Long)

    /** [blocks]: the compactions this send asks for, oldest first (A38); [kept] raw stay out of them. */
    data class Turn(val body: ChatIn, val blocks: List<CompactBlock> = emptyList(), val kept: Int = 0) {
        val needsCompact: Boolean get() = blocks.isNotEmpty()
    }

    /** `[refeição sugerida: …]`: the slot an assistant row suggested, as the model reads it in HISTORY (A38). */
    fun slotMarker(name: String) = "[refeição sugerida: $name]"

    fun build(
        day: DaySnapshot,
        todayMessages: List<ChatMessageEntity>,
        digests: List<DayDigestEntity>,
        text: String,
        now: Instant,
        compactEnabled: Boolean = COMPACT_ENABLED,
        /** FactMemory after expiration (A28). Always sent, even empty: the server treats the app as v2. */
        facts: List<Fact> = emptyList(),
        /** meal_log rows of the days before today (A27); today and older than 7 days are dropped. */
        recentLogs: List<MealLogEntity> = emptyList(),
        /** Forçar estimativa (A30): the server releases the estimate now. */
        forceEstimate: Boolean = false,
    ): Turn {
        val today = SaoPaulo.date(now)
        val raw = rawSinceDigest(todayMessages, digests)
        val slotNames = day.slotsOn(today).associate { it.id to it.name }
        val blocks = if (compactEnabled) compactBlocks(raw, todayMessages) else emptyList()
        return Turn(
            body = ChatIn(
                localTime = now.atZone(SaoPaulo.zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                profile = profile(day, today),
                memory = "",
                day = snapshot(day, today),
                digests = digests.sortedWith(compareBy({ it.createdAtEpochMs }, { it.coversUntilId ?: Long.MIN_VALUE }))
                    .takeLast(MAX_DIGESTS).map { it.text },
                messages = raw.takeLast(MAX_RAW).map { chatTurn(it, slotNames) },
                text = ChatText.clip(text),
                compact = false,
                recent = recent(recentLogs, day.slots, today),
                facts = chatFacts(facts, day.slotsOn(today).map { it.id.toString() }.toSet()),
                clarifyRounds = clarifyRounds(todayMessages),
                forceEstimate = forceEstimate,
                autoRecord = true,
                tempFacts = true,
            ),
            blocks = blocks.map { block -> CompactBlock(block.map { chatTurn(it, slotNames) }, block.last().id) },
            kept = raw.size - blocks.sumOf { it.size },
        )
    }

    /**
     * A38 (ADR-029 decision 2): the blocks to summarise, computed on every raw message (never on the newest 12).
     * Nothing below [MAX_RAW] raw. The newest [KEEP_RAW] stay raw, back to the user message before the first
     * question of an open clarify sequence when that is older. Each block is the oldest ≤ [MAX_RAW] raw before
     * them; a second block only while more than [MAX_RAW] raw would be left. A kept tail of [MAX_RAW] or more:
     * nothing is summarised.
     */
    fun compactBlocks(raw: List<ChatMessageEntity>, todayMessages: List<ChatMessageEntity>): List<List<ChatMessageEntity>> {
        if (raw.size < MAX_RAW) return emptyList()
        var keepFrom = raw.size - KEEP_RAW
        if (clarifyRounds(todayMessages) > 0) {
            val anchor = openClarifyStart(todayMessages)
            val i = raw.indexOfFirst { it.id == anchor?.id }
            if (i in 0 until keepFrom) keepFrom = i
        }
        if (raw.size - keepFrom >= MAX_RAW) return emptyList()
        val blocks = mutableListOf<List<ChatMessageEntity>>()
        var from = 0
        while (blocks.size < MAX_BLOCKS && from < keepFrom && (blocks.isEmpty() || raw.size - from > MAX_RAW)) {
            val block = raw.subList(from, minOf(keepFrom, from + MAX_RAW)).toList()
            blocks += block
            from += block.size
        }
        return blocks
    }

    /**
     * The user message right before the first question of the open clarify sequence (the meal that was asked
     * about), or that question when no user message precedes it. Same walk as [clarifyRounds].
     */
    private fun openClarifyStart(todayMessages: List<ChatMessageEntity>): ChatMessageEntity? {
        val sorted = todayMessages.sortedWith(compareBy<ChatMessageEntity>({ it.createdAtEpochMs }, { it.id }))
        var first = -1
        for (i in sorted.indices.reversed()) {
            val m = sorted[i]
            when {
                m.role == "user" -> continue
                isQuestionOnly(m) -> first = i
                else -> break
            }
        }
        if (first < 0) return null
        return sorted.getOrNull(first - 1)?.takeIf { it.role == "user" } ?: sorted[first]
    }

    /**
     * One HISTORY turn. An assistant row with a suggested slot of today ends with [slotMarker] (A38): the text
     * is clipped first, leaving room for the line, so the marker always survives and the turn stays in the limit.
     */
    private fun chatTurn(m: ChatMessageEntity, slotNames: Map<Long, String>): ChatTurn {
        val text = turnText(m)
        val name = m.estimateSlotId?.takeIf { m.role == "assistant" }?.let(slotNames::get)
            ?: return ChatTurn(m.role, ChatText.clip(text))
        val marker = "\n" + slotMarker(name)
        return ChatTurn(m.role, ChatText.clip(text, ChatText.MAX_CHARS - marker.codePointCount(0, marker.length)) + marker)
    }

    /**
     * A photo message keeps a marker in the history; the image itself is sent only once (A6).
     * An estimate stored with its question (before A30) carries the question too, so the model sees
     * what was asked. A question-only row already holds draft + question in [ChatMessageEntity.text].
     */
    private fun turnText(m: ChatMessageEntity): String {
        val text = when {
            m.photoPath == null -> m.text
            m.text.isBlank() -> "[foto]"
            else -> "[foto] ${m.text}"
        }
        val question = m.estimateQuestion?.takeIf { m.role == "assistant" && m.estimateKcal != null && it.isNotBlank() }
        return if (question == null) text else "$text\n$question"
    }

    const val MAX_CLARIFY_ROUNDS = 3

    /** Question-only assistant turn (A30): the server asked before estimating. */
    fun isQuestionOnly(m: ChatMessageEntity): Boolean =
        m.role == "assistant" && m.estimateKcal == null && !m.estimateQuestion.isNullOrBlank()

    /**
     * Question rounds already shown for the pending meal (A30): the question-only assistant messages at
     * the end of [todayMessages], walking back over the user answers between them. An estimate, a
     * receipt, a wipe or any other assistant message ends the walk; so does the start of the day.
     * Capped at [MAX_CLARIFY_ROUNDS].
     */
    fun clarifyRounds(todayMessages: List<ChatMessageEntity>): Int {
        var rounds = 0
        for (m in todayMessages.sortedWith(compareByDescending<ChatMessageEntity> { it.createdAtEpochMs }.thenByDescending { it.id })) {
            when {
                m.role == "user" -> continue
                isQuestionOnly(m) -> rounds++
                else -> break
            }
        }
        return rounds.coerceAtMost(MAX_CLARIFY_ROUNDS)
    }

    /**
     * days_seen / last_seen from the stored days. A routine slot that is not a slot of today goes as
     * null: the server accepts only profile slots. A temp fact goes as seen once, on its creation (A38).
     */
    fun chatFacts(facts: List<Fact>, todaySlots: Set<String>): List<ChatFact> = facts.map {
        ChatFact(
            id = it.id,
            kind = it.kind,
            category = it.category,
            key = it.key,
            text = it.text,
            slot = it.slot?.takeIf { slot -> slot in todaySlots },
            daysSeen = if (it.temp) 1 else it.days.size,
            lastSeen = if (it.temp) it.created else it.lastSeen,
        )
    }

    /** compact=true request: only [block], the raw messages to summarise (spec rule 9, A38). */
    fun compact(turn: Turn, block: CompactBlock): ChatIn = turn.body.copy(compact = true, text = "", messages = block.messages)

    /**
     * Raw user/assistant messages of today after the newest digest and the latest wipe (Config ceiling change:
     * the thread stays on screen, the prompt restarts). Receipts are UI only. The digest cut is the newest
     * message it summarised (A38, v9); a digest from before v9 cuts by its creation time. All of them: the
     * prompt takes the newest [MAX_RAW].
     */
    fun rawSinceDigest(todayMessages: List<ChatMessageEntity>, digests: List<DayDigestEntity>): List<ChatMessageEntity> {
        val wiped = todayMessages.filter { it.role == DayRepository.ROLE_WIPED }.maxOfOrNull { it.id }
        val newest = digests.maxWithOrNull(compareBy({ it.createdAtEpochMs }, { it.coversUntilId ?: Long.MIN_VALUE }))
        val until = newest?.coversUntilId
        val cut = newest?.createdAtEpochMs ?: Long.MIN_VALUE
        return todayMessages
            .filter { it.role in ROLES && (wiped == null || it.id > wiped) }
            .filter { if (until != null) it.id > until else it.createdAtEpochMs > cut }
            .sortedWith(compareBy({ it.createdAtEpochMs }, { it.id }))
    }

    /**
     * The 7 days before [today], oldest first; inside a day by slot time, "Outros" last.
     * A null or deleted slot goes as slot_id null, "Outros". At most [MAX_RECENT]: the newest stay.
     */
    fun recent(logs: List<MealLogEntity>, slots: List<MealSlot>, today: LocalDate): List<ChatRecentMeal> {
        val from = today.minusDays(DayRepository.RECENT_DAYS).toString()
        val to = today.toString()
        val slotById = slots.associateBy { it.id }
        return logs
            .filter { it.date >= from && it.date < to }
            .sortedWith(
                compareBy<MealLogEntity>({ it.date }, { it.slotId?.let(slotById::get)?.minutesFromMidnight ?: Int.MAX_VALUE }, { it.id }),
            )
            .takeLast(MAX_RECENT)
            .map { log ->
                val slot = log.slotId?.let(slotById::get)
                ChatRecentMeal(
                    date = log.date,
                    slotId = slot?.id?.toString(),
                    slotName = slot?.name ?: OTHERS,
                    text = clip(log.text, MAX_RECENT_TEXT),
                    kcal = log.kcal,
                    p = log.p,
                    c = log.carbs,
                    g = log.fat,
                )
            }
    }

    private fun clip(text: String, max: Int): String {
        if (text.codePointCount(0, text.length) <= max) return text
        return text.substring(0, text.offsetByCodePoints(0, max))
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
        slots = day.slotsOn(today).sortedBy { it.minutesFromMidnight }
            .map { ChatSlot(it.id.toString(), it.name, SlotSuggestions.format(it.minutesFromMidnight)) },
    )

    private fun snapshot(day: DaySnapshot, today: LocalDate) = ChatDay(
        date = today.toString(),
        eatenKcal = day.logs.sumOf { it.kcal },
        remainingKcal = day.metaOn(today) - day.logs.sumOf { it.kcal },
        eatenP = day.logs.sumOf { it.p },
        eatenC = day.logs.sumOf { it.carbs },
        eatenG = day.logs.sumOf { it.fat },
        workoutKcal = day.workoutKcal,
        slots = day.slotsOn(today).sortedBy { it.minutesFromMidnight }.map { slot ->
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
