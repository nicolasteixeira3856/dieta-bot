package app.fibrai.android.feature.chat

import app.fibrai.android.core.closure.DayTotals
import app.fibrai.android.core.database.ChatMessageEntity
import app.fibrai.android.core.database.DayDigestEntity
import app.fibrai.android.core.database.DayRepository
import app.fibrai.android.core.database.MealLogEntity
import app.fibrai.android.core.database.MealSlot
import app.fibrai.android.core.database.slotsOn
import app.fibrai.android.core.database.DaySnapshot
import app.fibrai.android.core.database.metaOn
import app.fibrai.android.core.network.ChatAddition
import app.fibrai.android.core.network.ChatAdditionItem
import app.fibrai.android.core.network.ChatDay
import app.fibrai.android.core.network.ChatDaySlot
import app.fibrai.android.core.network.ChatFact
import app.fibrai.android.core.network.ChatIn
import app.fibrai.android.core.network.ChatPendingAddition
import app.fibrai.android.core.network.ChatProfile
import app.fibrai.android.core.network.ChatRecentDay
import app.fibrai.android.core.network.ChatRecentMeal
import app.fibrai.android.core.network.ChatSlot
import app.fibrai.android.core.network.ChatTurn
import app.fibrai.android.domain.ChatText
import app.fibrai.android.domain.Fact
import app.fibrai.android.domain.MealProposal
import app.fibrai.android.domain.ReplyMarkup
import app.fibrai.android.domain.SaoPaulo
import app.fibrai.android.domain.SlotSuggestions
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

    /** A60 part B (ADR-044): the tones of the Tali; anything else goes as seco. */
    const val TONE_SECO = "seco"
    const val TONE_DURO = "duro"
    val TONES = setOf(TONE_SECO, TONE_DURO)
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
        /** A47: the unrecorded addition this message continues, already checked against today's DAY; else null. */
        pendingAddition: ChatPendingAddition? = null,
        /** A60 part A: Ajustar para caber, the target of the adjusted plan; null otherwise. */
        fitKcal: Int? = null,
        /** A64: the days before today as the closures read them ([recentDays]); empty = not sent. */
        pastDays: List<DayTotals> = emptyList(),
    ): Turn {
        val today = SaoPaulo.date(now)
        val raw = rawSinceDigest(todayMessages, digests)
        val slotNames = day.slotsOn(today).associate { it.id to it.name }
        val eaten = day.logs.mapNotNull { it.slotId }.toSet()
        val blocks = if (compactEnabled) compactBlocks(raw, todayMessages) else emptyList()
        return Turn(
            body = ChatIn(
                localTime = now.atZone(SaoPaulo.zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                profile = profile(day, today),
                memory = "",
                day = snapshot(day, today),
                digests = digests.sortedWith(compareBy({ it.createdAtEpochMs }, { it.coversUntilId ?: Long.MIN_VALUE }))
                    .takeLast(MAX_DIGESTS).map { it.text },
                messages = raw.takeLast(MAX_RAW).map { chatTurn(it, slotNames, eaten) },
                text = ChatText.clip(text),
                compact = false,
                recent = recent(recentLogs, day.slots, today),
                recentDays = recentDays(pastDays, day.slotsOn(today).map { it.id.toString() }.toSet(), today),
                facts = chatFacts(facts, day.slotsOn(today).map { it.id.toString() }.toSet()),
                clarifyRounds = clarifyRounds(todayMessages),
                forceEstimate = forceEstimate,
                autoRecord = true,
                tempFacts = true,
                mealChanges = true,
                skipSlots = true,
                planBudget = true,
                fitKcal = fitKcal,
                pendingAddition = pendingAddition,
            ),
            blocks = blocks.map { block -> CompactBlock(block.map { chatTurn(it, slotNames, eaten) }, block.last().id) },
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
     * A54: an addition answer whose destination is not eaten now (not recorded, or its record removed) keeps only
     * its first line, the added food and its `+kcal`: the server's meal total would contradict DAY.
     */
    private fun chatTurn(m: ChatMessageEntity, slotNames: Map<Long, String>, eaten: Set<Long>): ChatTurn {
        // A60 part C: the history line of an answer carries no markers (ADR-045).
        val text = turnText(m).let { if (m.role == "assistant") ReplyMarkup.plain(it) else it }
            .let { if (totalContradictsDay(m, eaten)) it.lineSequence().first() else it }
        val name = m.estimateSlotId?.takeIf { m.role == "assistant" }?.let(slotNames::get)
            ?: return ChatTurn(m.role, ChatText.clip(text))
        val marker = "\n" + slotMarker(name)
        return ChatTurn(m.role, ChatText.clip(text, ChatText.MAX_CHARS - marker.codePointCount(0, marker.length)) + marker)
    }

    private fun totalContradictsDay(m: ChatMessageEntity, eaten: Set<Long>): Boolean {
        if (m.role != "assistant") return false
        val destination = MealProposal.decode(m.mealChange)?.takeIf { it.isAddition }?.destinationSlotId ?: return false
        return destination !in eaten
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
        ).withMacros(it)
    }

    /** A64 (S33): a routine or liked dish goes with its numbers when it has all four; otherwise none of them. */
    private fun ChatFact.withMacros(fact: Fact): ChatFact {
        if (fact.category !in MACRO_CATEGORIES) return this
        val kcal = fact.kcal?.takeIf { it in 0..FACT_KCAL_MAX } ?: return this
        val p = fact.p?.takeIf { it in 0..FACT_GRAMS_MAX } ?: return this
        val c = fact.c?.takeIf { it in 0..FACT_GRAMS_MAX } ?: return this
        val g = fact.g?.takeIf { it in 0..FACT_GRAMS_MAX } ?: return this
        return copy(kcal = kcal, p = p, c = c, g = g)
    }

    private val MACRO_CATEGORIES = setOf("routine", "liked")
    private const val FACT_KCAL_MAX = 5000
    private const val FACT_GRAMS_MAX = 1000

    /**
     * A64 (S33 `recent_days`): the [days] before [today], newest first, at most 7, none before the first day of the app
     * ([DayTotals] of days the user was not using it would read as days without record). A slot id outside [todaySlots]
     * (a slot of another weekday or deleted) is left out: the server accepts only profile slots.
     */
    fun recentDays(days: List<DayTotals>, todaySlots: Set<String>, today: LocalDate): List<ChatRecentDay> = days
        .filter { it.date.isBefore(today) && !it.date.isBefore(today.minusDays(DayRepository.RECENT_DAYS)) }
        .sortedByDescending { it.date }
        .take(DayRepository.RECENT_DAYS.toInt())
        .map { day ->
            val share = if (day.meals.isEmpty()) 0 else day.ceilingKcal / day.meals.size
            val over = day.meals.filter { it.eaten && it.kcal > share }.maxByOrNull { it.kcal - share }
            ChatRecentDay(
                date = day.date.toString(),
                recorded = day.totals.kcal > 0 || day.meals.any { it.eaten },
                kcal = day.totals.kcal.coerceAtLeast(0),
                p = day.totals.p.coerceAtLeast(0),
                c = day.totals.c.coerceAtLeast(0),
                g = day.totals.g.coerceAtLeast(0),
                ceilingKcal = day.ceilingKcal.coerceAtLeast(1),
                overSlot = over?.slotId?.toString()?.takeIf { it in todaySlots },
                missingSlots = day.meals.filter { !it.eaten && !it.skipped }.map { it.slotId.toString() }.filter { it in todaySlots },
            )
        }

    /** compact=true request: only [block], the raw messages to summarise (spec rule 9, A38). */
    fun compact(turn: Turn, block: CompactBlock): ChatIn =
        turn.body.copy(compact = true, text = "", messages = block.messages, recentDays = emptyList(), pendingAddition = null, skipSlots = false, planBudget = false, fitKcal = null)

    /** A47: an addition proposal as the server's `pending_addition` (S18), the same shape it answered. */
    fun pendingAddition(proposal: MealProposal): ChatPendingAddition? {
        val addition = proposal.addition?.takeIf { proposal.isAddition } ?: return null
        return ChatPendingAddition(
            baseSlot = proposal.sourceSlotId?.toString(),
            addition = ChatAddition(
                mealText = addition.mealText,
                kcal = addition.kcal,
                p = addition.p,
                c = addition.c,
                g = addition.g,
                items = addition.items.map { ChatAdditionItem(it.name, it.g, it.kcal) },
            ),
        )
    }

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
        tone = day.tone.takeIf { it in TONES } ?: TONE_SECO,
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
                // A60 part D: a reserved plan goes with its numbers; nothing of it is eaten.
                day.planned[slot.id] != null -> day.planned.getValue(slot.id).let { plan ->
                    ChatDaySlot(slot.id.toString(), "planned", text = plan.text, kcal = plan.kcal, p = plan.p, c = plan.c, g = plan.g)
                }
                else -> ChatDaySlot(slot.id.toString(), "empty")
            }
        },
    )
}
