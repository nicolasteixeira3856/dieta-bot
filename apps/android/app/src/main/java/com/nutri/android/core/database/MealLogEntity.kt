package com.nutri.android.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_log",
    foreignKeys = [
        ForeignKey(
            entity = MealSlotEntity::class,
            parentColumns = ["id"],
            childColumns = ["slotId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("slotId")],
)
data class MealLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String = "",
    val window: String = "",
    val text: String = "",
    val kcal: Int = 0,
    val p: Int = 0,
    val stable: Int = 0,
    /** Null = orphan, shown as "Outros". */
    val slotId: Long? = null,
    @ColumnInfo(defaultValue = "0")
    val carbs: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val fat: Int = 0,
    @ColumnInfo(defaultValue = "'user'")
    val source: String = "user",
)
