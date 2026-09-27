package com.nutri.android.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_slot")
data class MealSlotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    /** 0..1439, America/Sao_Paulo. */
    val minutesFromMidnight: Int = 0,
    val sortOrder: Int = 0,
)
