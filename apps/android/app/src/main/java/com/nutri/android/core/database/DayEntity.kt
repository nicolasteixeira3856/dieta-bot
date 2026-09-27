package com.nutri.android.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day")
data class DayEntity(
    @PrimaryKey
    val date: String = "",
    var workoutKcal: Int? = null,
    var removedWindows: List<String> = emptyList(),
    var askedWindows: List<String> = emptyList(),
)
