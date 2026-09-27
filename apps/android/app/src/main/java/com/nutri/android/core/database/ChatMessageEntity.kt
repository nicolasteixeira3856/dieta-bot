package com.nutri.android.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "chat_message", indices = [Index("date")])
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String = "",
    /** "user" | "assistant" */
    val role: String = "user",
    val text: String = "",
    val createdAtEpochMs: Long = 0,
    val estimateKcal: Int? = null,
    val estimateP: Int? = null,
    val estimateC: Int? = null,
    val estimateG: Int? = null,
    val estimateConfidence: String? = null,
    val photoPath: String? = null,
)
