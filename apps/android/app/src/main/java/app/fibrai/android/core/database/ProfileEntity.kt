package app.fibrai.android.core.database

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
    /** A60 part B (ADR-044, v12): "seco" | "duro", the tone of the Tali. */
    @ColumnInfo(defaultValue = "'seco'")
    val tone: String = "seco",
    /** A71 (ADR-057 decision 9, v16): the goal weight the profile build accepted; null = no goal. */
    val goalWeightKg: Double? = null,
    /** A71 (v16): the goal date, ISO; null = a goal without a date or no goal. */
    val goalDate: String? = null,
    /** A71 (ADR-057 decision 10, v16): the day and week closure time, `HH:mm` America/Sao_Paulo. */
    @ColumnInfo(defaultValue = "'22:00'")
    val closureTime: String = "22:00",
    /** A71 (v16): 0 turns off the meal reminders and the closure notifications. */
    @ColumnInfo(defaultValue = "1")
    val notificationsEnabled: Int = 1,
    /** A71 (v16): where an unfinished onboarding resumes: `chat` | `summary` | `building` | `error`. */
    @ColumnInfo(defaultValue = "'chat'")
    val onboardingPhase: String = "chat",
)
