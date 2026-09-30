package com.nutri.android.core.database

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.withTransaction
import com.nutri.android.domain.SaoPaulo
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
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
    private val db: DietaBotDatabase,
    private val clock: InstantClock,
    private val legacyStore: DataStore<Preferences>,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val importMutex = Mutex()
    private var imported = false

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeToday(): Flow<DaySnapshot> = flow {
        while (currentCoroutineContext().isActive) {
            emit(todayIso())
            delay(30_000)
        }
    }.distinctUntilChanged().flatMapLatest { today ->
        flow {
            importOnce()
            emitAll(
                combine(
                    db.profileDao().observe(),
                    db.dayDao().observe(today),
                    db.mealLogDao().observeByDate(today),
                    db.mealSlotDao().observeAll(),
                    db.slotSkipDao().observeByDate(today),
                ) { profile, day, logs, slots, skips ->
                    snapshotOf(today, profile, day, logs, slots, skips)
                },
            )
        }
    }

    /** Body and macro fields left null keep the stored value. */
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
        sex: String? = null,
        ageYears: Int? = null,
        heightCm: Int? = null,
        weightKg: Double? = null,
        proteinTargetG: Int? = null,
        carbTargetG: Int? = null,
        fatTargetG: Int? = null,
    ) {
        importOnce()
        val days = (kcalDays + List(7) { 2000 }).take(7)
        withContext(Dispatchers.IO) {
            db.withTransaction {
                val current = db.profileDao().get() ?: ProfileEntity()
                db.profileDao().upsert(
                    current.copy(
                        id = 1,
                        ceilingMode = ceilingMode,
                        kcalSame = kcalSame,
                        kcalWeekday = kcalWeekday,
                        kcalWeekend = kcalWeekend,
                        kcalDays = days,
                        eat = eat,
                        pct = pct,
                        onboardingDone = if (onboardingDone) 1 else 0,
                        firstDay = firstDay,
                        sex = sex ?: current.sex,
                        ageYears = ageYears ?: current.ageYears,
                        heightCm = heightCm ?: current.heightCm,
                        weightKg = weightKg ?: current.weightKg,
                        proteinTargetG = proteinTargetG ?: current.proteinTargetG,
                        carbTargetG = carbTargetG ?: current.carbTargetG,
                        fatTargetG = fatTargetG ?: current.fatTargetG,
                    ),
                )
            }
        }
    }

    /**
     * INSERT, never merge: a second log on the same slot adds up. The Chat never does that: a taken
     * slot goes through [replaceSlotLog] (ADR-017). Clears that slot's skip.
     */
    suspend fun addLog(
        window: String,
        text: String,
        kcal: Int,
        p: Int,
        stable: Boolean,
        slotId: Long? = null,
        carbs: Int = 0,
        fat: Int = 0,
        source: String = "user",
    ) {
        importOnce()
        val date = todayIso()
        withContext(Dispatchers.IO) {
            db.withTransaction {
                db.mealLogDao().insert(
                    MealLogEntity(
                        date = date,
                        window = window,
                        text = text,
                        kcal = kcal,
                        p = p,
                        stable = if (stable) 1 else 0,
                        slotId = slotId,
                        carbs = carbs,
                        fat = fat,
                        source = source,
                    ),
                )
                if (slotId != null) db.slotSkipDao().delete(date, slotId)
            }
        }
    }

    /**
     * One meal, one log (ADR-017): today's logs of [slotId] go and the new one takes their place,
     * in one transaction. Clears that slot's skip.
     */
    suspend fun replaceSlotLog(
        slotId: Long,
        text: String,
        kcal: Int,
        p: Int,
        carbs: Int,
        fat: Int,
        source: String = "user",
    ) {
        importOnce()
        val date = todayIso()
        withContext(Dispatchers.IO) {
            db.withTransaction {
                db.mealLogDao().deleteBySlot(date, slotId)
                db.mealLogDao().insert(
                    MealLogEntity(
                        date = date,
                        window = "",
                        text = text,
                        kcal = kcal,
                        p = p,
                        stable = 1,
                        slotId = slotId,
                        carbs = carbs,
                        fat = fat,
                        source = source,
                    ),
                )
                db.slotSkipDao().delete(date, slotId)
            }
        }
    }

    /** Explicit user action only. Removes today's logs of that slot. */
    suspend fun addSkip(slotId: Long) {
        importOnce()
        val date = todayIso()
        withContext(Dispatchers.IO) {
            db.withTransaction {
                db.slotSkipDao().insert(SlotSkipEntity(date = date, slotId = slotId))
                db.mealLogDao().deleteBySlot(date, slotId)
            }
        }
    }

    /**
     * Keeps chat_message, profile, slots and workoutKcal. Adds a [ROLE_WIPED] marker: the thread
     * stays on screen, today's prompt restarts after it.
     */
    suspend fun wipeToday() {
        importOnce()
        withContext(Dispatchers.IO) {
            db.withTransaction { wipeTodayRows() }
        }
    }

    /** Config: a new ceiling restarts today (spec memoria-push, Config rule 5). One transaction. */
    suspend fun changeCeiling(
        ceilingMode: String,
        kcalSame: Int,
        kcalWeekday: Int,
        kcalWeekend: Int,
        kcalDays: List<Int>,
    ) {
        importOnce()
        val days = (kcalDays + List(7) { 2000 }).take(7)
        withContext(Dispatchers.IO) {
            db.withTransaction {
                updateProfile {
                    it.copy(
                        ceilingMode = ceilingMode,
                        kcalSame = kcalSame,
                        kcalWeekday = kcalWeekday,
                        kcalWeekend = kcalWeekend,
                        kcalDays = days,
                    )
                }
                wipeTodayRows()
            }
        }
    }

    suspend fun saveEatBack(eat: String, pct: Int) {
        importOnce()
        withContext(Dispatchers.IO) {
            db.withTransaction { updateProfile { it.copy(eat = eat, pct = pct) } }
        }
    }

    suspend fun saveMacroTargets(proteinG: Int, carbG: Int, fatG: Int) {
        importOnce()
        withContext(Dispatchers.IO) {
            db.withTransaction {
                updateProfile { it.copy(proteinTargetG = proteinG, carbTargetG = carbG, fatTargetG = fatG) }
            }
        }
    }

    private suspend fun updateProfile(transform: (ProfileEntity) -> ProfileEntity) {
        val current = db.profileDao().get() ?: ProfileEntity()
        db.profileDao().upsert(transform(current).copy(id = 1))
    }

    private suspend fun wipeTodayRows() {
        val now = clock.now()
        val date = SaoPaulo.date(now).toString()
        db.mealLogDao().deleteByDate(date)
        db.slotSkipDao().deleteByDate(date)
        db.dayDigestDao().deleteByDate(date)
        db.chatMessageDao().insert(
            ChatMessageEntity(date = date, role = ROLE_WIPED, createdAtEpochMs = now.toEpochMilli()),
        )
    }

    /**
     * Replace all: the stored set becomes [slots], ordered as given.
     * Slots with id != 0 are updated in place so their logs keep slotId.
     */
    suspend fun saveSlots(slots: List<MealSlot>, slotMode: String? = null) {
        require(slots.all { it.days in 1..127 }) { "days out of 1..127" }
        require(slotMode == null || slotMode in setOf("same", "split", "each"))
        require(slots.all { it.minutesFromMidnight in 0..1439 }) { "minutesFromMidnight out of 0..1439" }
        importOnce()
        withContext(Dispatchers.IO) {
            db.withTransaction {
                val keep = slots.mapIndexed { index, slot ->
                    val rowId = db.mealSlotDao().upsert(
                        MealSlotEntity(
                            id = slot.id,
                            name = slot.name,
                            minutesFromMidnight = slot.minutesFromMidnight,
                            sortOrder = index,
                            days = slot.days,
                        ),
                    )
                    if (slot.id != 0L) slot.id else rowId
                }
                db.mealSlotDao().deleteAllExcept(keep)
                if (slotMode != null) updateProfile { it.copy(slotMode = slotMode) }
            }
        }
    }

    suspend fun insertMessage(
        role: String,
        text: String,
        estimateKcal: Int? = null,
        estimateP: Int? = null,
        estimateC: Int? = null,
        estimateG: Int? = null,
        estimateConfidence: String? = null,
        photoPath: String? = null,
        estimateSlotId: Long? = null,
        estimateQuestion: String? = null,
        estimateItems: List<String> = emptyList(),
        estimateMealText: String? = null,
        intent: String? = null,
        pendingMemory: String? = null,
        memoryUsedKinds: String? = null,
        memoryUpdated: Boolean = false,
    ): Long {
        importOnce()
        val now = clock.now()
        return withContext(Dispatchers.IO) {
            db.chatMessageDao().insert(
                ChatMessageEntity(
                    date = SaoPaulo.date(now).toString(),
                    role = role,
                    text = text,
                    createdAtEpochMs = now.toEpochMilli(),
                    estimateKcal = estimateKcal,
                    estimateP = estimateP,
                    estimateC = estimateC,
                    estimateG = estimateG,
                    estimateConfidence = estimateConfidence,
                    photoPath = photoPath,
                    estimateSlotId = estimateSlotId,
                    estimateQuestion = estimateQuestion,
                    estimateItems = estimateItems.takeIf { it.isNotEmpty() }?.joinToString(ITEM_SEPARATOR),
                    estimateMealText = estimateMealText,
                    intent = intent,
                    pendingMemory = pendingMemory,
                    memoryUsedKinds = memoryUsedKinds,
                    memoryUpdated = memoryUpdated,
                ),
            )
        }
    }

    /** meal_log of the [RECENT_DAYS] days before today (America/Sao_Paulo), for the Chat `recent` (A27). */
    suspend fun recentLogs(): List<MealLogEntity> {
        importOnce()
        val today = SaoPaulo.date(clock.now())
        return withContext(Dispatchers.IO) {
            db.mealLogDao().getBetween(today.minusDays(RECENT_DAYS).toString(), today.minusDays(1).toString())
        }
    }

    /** Thread for the UI: today and the previous 59 days. */
    fun observeMessages(): Flow<List<ChatMessageEntity>> {
        val from = SaoPaulo.date(clock.now()).minusDays(MESSAGE_DAYS - 1L).toString()
        return db.chatMessageDao().observeSince(from)
    }

    /** At most 2 digests per day. A 3rd overwrites the oldest (seq 1, then seq 2, ...). */
    suspend fun upsertDigest(text: String) {
        importOnce()
        val now = clock.now()
        val date = SaoPaulo.date(now).toString()
        withContext(Dispatchers.IO) {
            db.withTransaction {
                val existing = db.dayDigestDao().getByDate(date)
                val seq = when {
                    existing.none { it.seq == 1 } -> 1
                    existing.none { it.seq == 2 } -> 2
                    else -> existing.minWith(compareBy({ it.createdAtEpochMs }, { it.seq })).seq
                }
                db.dayDigestDao().upsert(
                    DayDigestEntity(date = date, seq = seq, text = text, createdAtEpochMs = now.toEpochMilli()),
                )
            }
        }
    }

    suspend fun digestsToday(): List<DayDigestEntity> {
        importOnce()
        return withContext(Dispatchers.IO) { db.dayDigestDao().getByDate(todayIso()) }
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

    private suspend fun mutateToday(transform: (DayEntity) -> Unit) {
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
                                id = 1,
                                ceilingMode = saved.ceilingMode,
                                kcalSame = saved.kcalSame,
                                kcalWeekday = saved.kcalWeekday,
                                kcalWeekend = saved.kcalWeekend,
                                kcalDays = days,
                                eat = saved.eat,
                                pct = saved.pct,
                                onboardingDone = if (saved.onboardingDone) 1 else 0,
                                firstDay = saved.firstDay,
                            ),
                        )
                        db.dayDao().upsert(
                            DayEntity(
                                date = date,
                                workoutKcal = saved.workoutKcal,
                                removedWindows = saved.removed,
                                askedWindows = saved.asked,
                            ),
                        )
                        db.mealLogDao().deleteByDate(date)
                        saved.logs.forEach { log ->
                            db.mealLogDao().insert(
                                MealLogEntity(
                                    date = date,
                                    window = log.window,
                                    text = log.text,
                                    kcal = log.kcal,
                                    p = log.p,
                                    stable = if (log.stable) 1 else 0,
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
        slots: List<MealSlotEntity>,
        skips: List<SlotSkipEntity>,
    ): DaySnapshot {
        val kcalDays = profile?.kcalDays?.map { it }?.let { list ->
            if (list.size == 7) list else List(7) { 2000 }
        } ?: List(7) { 2000 }
        return DaySnapshot(
            date = date,
            slotMode = profile?.slotMode ?: "same",
            ceilingMode = profile?.ceilingMode ?: "same",
            kcalSame = profile?.kcalSame ?: 2000,
            kcalWeekday = profile?.kcalWeekday ?: 2000,
            kcalWeekend = profile?.kcalWeekend ?: 2300,
            kcalDays = kcalDays,
            eat = profile?.eat ?: "zero",
            pct = profile?.pct ?: 50,
            onboardingDone = (profile?.onboardingDone ?: 0) != 0,
            firstDay = profile?.firstDay ?: "",
            sex = profile?.sex ?: "",
            ageYears = profile?.ageYears ?: 0,
            heightCm = profile?.heightCm ?: 0,
            weightKg = profile?.weightKg ?: 0.0,
            proteinTargetG = profile?.proteinTargetG ?: 150,
            carbTargetG = profile?.carbTargetG ?: 200,
            fatTargetG = profile?.fatTargetG ?: 67,
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
                    slotId = row.slotId,
                    carbs = row.carbs,
                    fat = row.fat,
                    source = row.source,
                )
            },
            slots = slots.map { MealSlot(id = it.id, name = it.name, minutesFromMidnight = it.minutesFromMidnight, days = it.days) },
            skippedSlotIds = skips.mapTo(mutableSetOf()) { it.slotId },
        )
    }

    companion object {
        private const val MESSAGE_DAYS = 60
        const val RECENT_DAYS = 7L

        /** chat_message role written by [wipeToday]. Never rendered, never sent. */
        const val ROLE_WIPED = "wiped"
    }
}
