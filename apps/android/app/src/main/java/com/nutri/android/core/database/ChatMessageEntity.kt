package com.nutri.android.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "chat_message", indices = [Index("date")])
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String = "",
    /**
     * "user" | "assistant" | receipts: "logged" | "replaced" | "skipped" (UI only, never sent to the server) |
     * "wiped": marker of wipeToday, never rendered; today's prompt starts after it.
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
) {
    val itemNames: List<String>
        get() = estimateItems?.split(ITEM_SEPARATOR)?.filter { it.isNotBlank() }.orEmpty()
}

const val ITEM_SEPARATOR = "\n"
