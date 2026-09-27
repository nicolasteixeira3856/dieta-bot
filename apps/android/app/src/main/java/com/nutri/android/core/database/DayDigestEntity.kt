package com.nutri.android.core.database

import androidx.room.Entity

@Entity(tableName = "day_digest", primaryKeys = ["date", "seq"])
data class DayDigestEntity(
    val date: String,
    /** 1 or 2. */
    val seq: Int,
    val text: String,
    val createdAtEpochMs: Long,
)
