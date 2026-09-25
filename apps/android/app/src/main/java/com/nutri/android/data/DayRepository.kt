package com.nutri.android.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.withTransaction
import com.nutri.android.domain.SaoPaulo
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@Singleton
class DayRepository @Inject constructor(
    private val db: NutriDatabase,
    private val clock: InstantClock,
    private val legacyStore: DataStore<Preferences>,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val importMutex = Mutex()
    private var imported = false

    fun observeToday(): Flow<DaySnapshot> = flow {
        importOnce()
        val today = todayIso()
        emitAll(
            combine(
                db.profileDao().observe(),
                db.dayDao().observe(today),
                db.mealLogDao().observeByDate(today),
            ) { profile, day, logs ->
                snapshotOf(today, profile, day, logs)
            },
        )
    }

    suspend fun saveProfile(
        ceilingMode: String,
        kcalSame: Int,
        kcalWeekday: Int,
        kcalWeekend: Int,
        kcalDays: List<Int>,
        eat: String,
        pct: Int,
        onboardingDone: Boolean,
        firstDay: String,
    ) {
        importOnce()
        val days = (kcalDays + List(7) { 2000 }).take(7)
        withContext(Dispatchers.IO) {
            db.profileDao().upsert(
                ProfileEntity(
                    1,
                    ceilingMode,
                    kcalSame,
                    kcalWeekday,
                    kcalWeekend,
                    days,
                    eat,
                    pct,
                    if (onboardingDone) 1 else 0,
                    firstDay,
                ),
            )
        }
    }

    suspend fun addLog(window: String, text: String, kcal: Int, p: Int, stable: Boolean) {
        importOnce()
        withContext(Dispatchers.IO) {
            db.mealLogDao().insert(
                MealLogEntity(
                    todayIso(),
                    window,
                    text,
                    kcal,
                    p,
                    if (stable) 1 else 0,
                ),
            )
        }
    }

    suspend fun setWorkout(kcal: Int?) {
        importOnce()
        withContext(Dispatchers.IO) {
            mutateToday { it.workoutKcal = kcal }
        }
    }

    suspend fun removeChip(window: String) {
        importOnce()
        withContext(Dispatchers.IO) {
            mutateToday { row ->
                row.removedWindows = (row.removedWindows + window).distinct()
            }
        }
    }

    suspend fun markAsked(window: String) {
        importOnce()
        withContext(Dispatchers.IO) {
            mutateToday { row ->
                row.askedWindows = (row.askedWindows + window).distinct()
            }
        }
    }

    private fun mutateToday(transform: (DayEntity) -> Unit) {
        val date = todayIso()
        val current = db.dayDao().get(date) ?: DayEntity(date, null, emptyList(), emptyList())
        transform(current)
        db.dayDao().upsert(current)
    }

    private suspend fun importOnce() {
        importMutex.withLock {
            if (imported) return
            imported = true
            val key = stringPreferencesKey(DAY_PREF_KEY)
            val raw = legacyStore.data.first()[key] ?: return
            val saved = runCatching { json.decodeFromString<SavedDay>(raw) }.getOrNull()
            if (saved != null) {
                val date = saved.firstDay.ifBlank { todayIso() }
                val days = saved.kcalDays.let { if (it.size == 7) it else List(7) { 2000 } }
                withContext(Dispatchers.IO) {
                    db.withTransaction {
                        db.profileDao().upsert(
                            ProfileEntity(
                                1,
                                saved.ceilingMode,
                                saved.kcalSame,
                                saved.kcalWeekday,
                                saved.kcalWeekend,
                                days,
                                saved.eat,
                                saved.pct,
                                if (saved.onboardingDone) 1 else 0,
                                saved.firstDay,
                            ),
                        )
                        db.dayDao().upsert(
                            DayEntity(
                                date,
                                saved.workoutKcal,
                                saved.removed,
                                saved.asked,
                            ),
                        )
                        db.mealLogDao().deleteByDate(date)
                        saved.logs.forEach { log ->
                            db.mealLogDao().insert(
                                MealLogEntity(
                                    date,
                                    log.window,
                                    log.text,
                                    log.kcal,
                                    log.p,
                                    if (log.stable) 1 else 0,
                                ),
                            )
                        }
                    }
                }
            }
            legacyStore.edit { it.remove(key) }
        }
    }

    private fun todayIso(): String = SaoPaulo.date(clock.now()).toString()

    private fun snapshotOf(
        date: String,
        profile: ProfileEntity?,
        day: DayEntity?,
        logs: List<MealLogEntity>,
    ): DaySnapshot {
        val kcalDays = profile?.kcalDays?.map { it.toInt() }.let { list ->
            if (list != null && list.size == 7) list else List(7) { 2000 }
        }
        return DaySnapshot(
            date = date,
            ceilingMode = profile?.ceilingMode ?: "same",
            kcalSame = profile?.kcalSame ?: 2000,
            kcalWeekday = profile?.kcalWeekday ?: 2000,
            kcalWeekend = profile?.kcalWeekend ?: 2300,
            kcalDays = kcalDays,
            eat = profile?.eat ?: "zero",
            pct = profile?.pct ?: 50,
            onboardingDone = (profile?.onboardingDone ?: 0) != 0,
            firstDay = profile?.firstDay ?: "",
            workoutKcal = day?.workoutKcal,
            removedWindows = day?.removedWindows ?: emptyList(),
            askedWindows = day?.askedWindows ?: emptyList(),
            logs = logs.map { row ->
                MealLog(
                    window = row.window,
                    text = row.text,
                    kcal = row.kcal,
                    p = row.p,
                    stable = row.stable != 0,
                )
            },
        )
    }
}
