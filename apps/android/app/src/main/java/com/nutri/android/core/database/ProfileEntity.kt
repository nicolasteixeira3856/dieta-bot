package com.nutri.android.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    @ColumnInfo(defaultValue = "'same'")
    val slotMode: String = "same",
    val ceilingMode: String = "same",
    val kcalSame: Int = 2000,
    val kcalWeekday: Int = 2000,
    val kcalWeekend: Int = 2300,
    val kcalDays: List<Int> = listOf(2000, 2000, 2000, 2000, 2000, 2000, 2000),
    val eat: String = "zero",
    val pct: Int = 50,
    val onboardingDone: Int = 0,
    val firstDay: String = "",
    /** "" | "male" | "female" */
    @ColumnInfo(defaultValue = "''")
    val sex: String = "",
    @ColumnInfo(defaultValue = "0")
    val ageYears: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val heightCm: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val weightKg: Double = 0.0,
    @ColumnInfo(defaultValue = "150")
    val proteinTargetG: Int = 150,
    @ColumnInfo(defaultValue = "200")
    val carbTargetG: Int = 200,
    @ColumnInfo(defaultValue = "67")
    val fatTargetG: Int = 67,
)
