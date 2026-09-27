package com.nutri.android.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val ceilingMode: String = "same",
    val kcalSame: Int = 2000,
    val kcalWeekday: Int = 2000,
    val kcalWeekend: Int = 2300,
    val kcalDays: List<Int> = listOf(2000, 2000, 2000, 2000, 2000, 2000, 2000),
    val eat: String = "zero",
    val pct: Int = 50,
    val onboardingDone: Int = 0,
    val firstDay: String = "",
)
