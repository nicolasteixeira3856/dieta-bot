package com.nutri.android.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_log")
data class MealLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String = "",
    val window: String = "",
    val text: String = "",
    val kcal: Int = 0,
    val p: Int = 0,
    val stable: Int = 0,
) {
    constructor(
        date: String,
        window: String,
        text: String,
        kcal: Int,
        p: Int,
        stable: Int,
    ) : this(
        id = 0,
        date = date,
        window = window,
        text = text,
        kcal = kcal,
        p = p,
        stable = stable,
    )
}
