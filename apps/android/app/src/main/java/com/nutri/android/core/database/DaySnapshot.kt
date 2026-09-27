package com.nutri.android.core.database

data class MealLog(
    val window: String,
    val text: String,
    val kcal: Int,
    val p: Int,
    val stable: Boolean,
    val slotId: Long? = null,
    val carbs: Int = 0,
    val fat: Int = 0,
    val source: String = "user",
)

data class MealSlot(
    val id: Long = 0,
    val name: String,
    val minutesFromMidnight: Int,
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
    val sex: String = "",
    val ageYears: Int = 0,
    val heightCm: Int = 0,
    val weightKg: Double = 0.0,
    val proteinTargetG: Int = 150,
    val carbTargetG: Int = 200,
    val fatTargetG: Int = 67,
    val workoutKcal: Int? = null,
    val removedWindows: List<String> = emptyList(),
    val askedWindows: List<String> = emptyList(),
    val logs: List<MealLog> = emptyList(),
    val slots: List<MealSlot> = emptyList(),
    val skippedSlotIds: Set<Long> = emptySet(),
)
