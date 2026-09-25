package com.nutri.android.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val DAY_STORE_NAME = "nutri_day"
const val DAY_PREF_KEY = "day"

private val Context.dayStore by preferencesDataStore(DAY_STORE_NAME)

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

@Singleton
class DayStore @Inject constructor(@ApplicationContext context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val data = context.dayStore
    private val key = stringPreferencesKey(DAY_PREF_KEY)

    val flow: Flow<SavedDay> = data.data.map { prefs ->
        prefs[key]?.let { runCatching { json.decodeFromString<SavedDay>(it) }.getOrNull() } ?: SavedDay()
    }

    suspend fun save(day: SavedDay) {
        data.edit { it[key] = json.encodeToString(day) }
    }
}
