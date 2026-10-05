package app.fibrai.android.core.database

import androidx.room.Entity

@Entity(tableName = "slot_skip", primaryKeys = ["date", "slotId"])
data class SlotSkipEntity(
    val date: String,
    val slotId: Long,
)
