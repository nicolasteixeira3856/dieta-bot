package com.nutri.android.data

data class MealLog(
    val window: String,
    val text: String,
    val kcal: Int,
    val p: Int,
    val stable: Boolean,
)

data class DaySnapshot(
    val date: String = "",
    val ceilingMode: String = "same",
    val kcalSame: Int = 2000,
    val kcalWeekday: Int = 2000,
    val kcalWeekend: Int = 2300,
    val kcalDays: List<Int> = List(7) { 2000 },
    val eat: String = "zero",
    val pct: Int = 50,
    val onboardingDone: Boolean = false,
    val firstDay: String = "",
    val workoutKcal: Int? = null,
    val removedWindows: List<String> = emptyList(),
    val askedWindows: List<String> = emptyList(),
    val logs: List<MealLog> = emptyList(),
)
