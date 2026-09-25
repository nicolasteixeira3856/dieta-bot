package com.nutri.android.data

import kotlinx.serialization.Serializable

const val DAY_STORE_NAME = "nutri_day"
const val DAY_PREF_KEY = "day"

@Serializable
data class SavedLog(
    val window: String,
    val text: String,
    val kcal: Int,
    val p: Int,
    val stable: Boolean = true,
)

@Serializable
data class SavedDay(
    val ceilingMode: String = "same",
    val kcalSame: Int = 2000,
    val kcalWeekday: Int = 2000,
    val kcalWeekend: Int = 2300,
    val kcalDays: List<Int> = List(7) { 2000 },
    val eat: String = "zero",
    val pct: Int = 50,
    val workoutKcal: Int? = null,
    val logs: List<SavedLog> = emptyList(),
    val removed: List<String> = emptyList(),
    val asked: List<String> = emptyList(),
    val firstDay: String = "",
    val onboardingDone: Boolean = false,
)
