package app.fibrai.android.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * One slot of an answer's `skip_slots` (A59, ADR-047). [state] is what the slot held when the skip was applied (null
 * before that); a delete proposal is confirmed only while the slot still holds it. [outcome] is null until applied.
 */
@Serializable
data class SkipEntry(val slotId: Long, val state: SlotState? = null, val outcome: String? = null) {
    val pending: Boolean get() = outcome == null
}

/**
 * The skips of one assistant answer, stored with it before any write (A59): the request day, its latest wipe and the
 * listed slots in order. Recreation, retry and process death read this and never apply a skip twice.
 */
@Serializable
data class SkipOutcomes(
    val version: Int = VERSION,
    val date: String,
    val wipeId: Long? = null,
    /** The answer's intent (log | plan | question | skip): telemetry `with`. */
    val with: String,
    val slots: List<SkipEntry>,
) {
    fun entry(slotId: Long): SkipEntry? = slots.firstOrNull { it.slotId == slotId }

    /** [slotId] becomes [outcome] (and [state] when given) while it is [from]; null when it is not. */
    fun move(slotId: Long, from: String?, outcome: String, state: SlotState? = null): SkipOutcomes? {
        val current = entry(slotId) ?: return null
        if (current.outcome != from) return null
        val next = current.copy(outcome = outcome, state = state ?: current.state)
        return copy(slots = slots.map { if (it.slotId == slotId) next else it })
    }

    /** Day and wipe of the request still hold: what every pending skip of the answer depends on. */
    fun holds(date: String, wipeId: Long?): Boolean = this.date == date && this.wipeId == wipeId

    fun encode(): String = json.encodeToString(serializer(), this)

    companion object {
        const val VERSION = 1

        /** The slot was empty: skipped now, receipt `Pulado {slot}` with Desfazer (chatSK). */
        const val SKIPPED = "skipped"

        /** The slot was skipped already: nothing written, no receipt. */
        const val ALREADY = "already"

        /** The slot has a record: `Pular {slot}?` below the answer (chatSD). */
        const val PENDING_DELETE = "pending_delete"

        /** Excluir e pular: the record left, the slot is skipped. */
        const val DELETED = "deleted"

        /** Manter registro: nothing written (`Registro mantido`). */
        const val KEPT = "kept"

        /** The proposal or the skip died: next send, day change, wipe, slot changed by another path. */
        const val EXPIRED = "expired"

        /** The write failed: `Não registrado` for that skip only. */
        const val FAILED = "failed"

        private val json = Json { ignoreUnknownKeys = true }

        /** Null for a row without skips, an unreadable payload or a version this app does not know. */
        fun decode(text: String?): SkipOutcomes? = text
            ?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() }
            ?.takeIf { it.version == VERSION }
    }
}
