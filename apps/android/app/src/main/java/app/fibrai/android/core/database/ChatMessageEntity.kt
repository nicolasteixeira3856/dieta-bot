package app.fibrai.android.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** (createdAtEpochMs, id): the newest-first page of the Chat (A32, v7). */
@Entity(tableName = "chat_message", indices = [Index("date"), Index("createdAtEpochMs", "id")])
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String = "",
    /**
     * "user" | "assistant" | receipts: "logged" | "replaced" | "skipped" | "moved" | "restored" (UI only,
     * never sent to the server) | "wiped": marker of wipeToday, never rendered; today's prompt starts after it.
     */
    val role: String = "user",
    val text: String = "",
    val createdAtEpochMs: Long = 0,
    val estimateKcal: Int? = null,
    val estimateP: Int? = null,
    val estimateC: Int? = null,
    val estimateG: Int? = null,
    val estimateConfidence: String? = null,
    val photoPath: String? = null,
    /** Slot suggested by the server (assistant) or the slot a receipt refers to. */
    val estimateSlotId: Long? = null,
    val estimateQuestion: String? = null,
    /** Item names joined by [ITEM_SEPARATOR]. */
    val estimateItems: String? = null,
    /** Server meal_text (A27): the text Gravar records. Null on old rows and old servers. */
    val estimateMealText: String? = null,
    /** "log" | "plan" | "question" (A27). Null = old row or old server, handled as "log". */
    val intent: String? = null,
    /** Routine add/reinforce of this answer (A28), JSON of ChatMemoryUpdate: applied only when it is recorded. */
    val pendingMemory: String? = null,
    /** memory_used resolved at answer time (A28): "permanent", "dynamic" or "permanent,dynamic". */
    val memoryUsedKinds: String? = null,
    /** At least one memory change was applied with this row: the answer, or the receipt of a record (A28). */
    @ColumnInfo(defaultValue = "0")
    val memoryUpdated: Boolean = false,
    /**
     * Assistant rows (A34): "auto" | "ask" | "none", from the server `record`. Null = row from before A34:
     * no record actions at all.
     */
    val recordMode: String? = null,
    /**
     * Assistant rows (A34): "recorded" | "pending_replace" | "not_recorded" | null (nothing decided yet). A47 adds
     * "pending_add" (Adicionar ao {slot}?, chatI) and "pending_revise" (Atualizar {slot}?, chatIC).
     */
    val recordState: String? = null,
    /** Receipt rows (A34): "undone" | "deleted" | "moved" | "edited" | null (active). */
    val receiptState: String? = null,
    /**
     * Receipt rows (A34): [app.fibrai.android.domain.UndoData] JSON, what the receipt changed. Assistant rows with
     * recordState "pending_replace": [app.fibrai.android.domain.SlotChange] JSON, the slot the confirmation asks about.
     */
    val undoData: String? = null,
    /** Receipt rows (A34): "user" | "photo" | "plan" | "routine" (drives Editar). */
    val recordSource: String? = null,
    /**
     * Assistant rows (A47, v10): [app.fibrai.android.domain.MealProposal] JSON, the versioned addition/revision proposal
     * with the slot states it was built from. Null = no proposal (older row or a server without the capability).
     */
    val mealChange: String? = null,
    /**
     * Assistant rows (A59, v11): [app.fibrai.android.domain.SkipOutcomes] JSON, the listed skips of the answer with the
     * state each slot had when applied and its outcome. Null = no skips (older row or a server without the capability).
     */
    val skipOutcomes: String? = null,
) {
    val itemNames: List<String>
        get() = estimateItems?.split(ITEM_SEPARATOR)?.filter { it.isNotBlank() }.orEmpty()
}

const val ITEM_SEPARATOR = "\n"
