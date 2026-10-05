package app.fibrai.android.core.database

import androidx.room.Entity

@Entity(tableName = "day_digest", primaryKeys = ["date", "seq"])
data class DayDigestEntity(
    val date: String,
    /** 1 or 2. */
    val seq: Int,
    val text: String,
    val createdAtEpochMs: Long,
    /** A38 (v9): id of the newest chat_message it summarised. Null = digest from before v9, cut by [createdAtEpochMs]. */
    val coversUntilId: Long? = null,
)
