package app.fibrai.android.core.database

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.withTransaction
import app.fibrai.android.domain.PlannedSlot
import app.fibrai.android.domain.ReceiptRules
import app.fibrai.android.domain.SaoPaulo
import app.fibrai.android.domain.SlotCheck
import app.fibrai.android.domain.SlotChange
import app.fibrai.android.domain.SlotRecord
import app.fibrai.android.domain.SkipOutcomes
import app.fibrai.android.domain.SlotState
import app.fibrai.android.domain.WorkoutChange
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

/**
 * A47: what a proposal's record also requires inside the transaction, never written. [date] is the request day (today
 * when it commits), [wipeId] its latest wipe marker, [checks] the slots it was built from that it does not change (the
 * source of a rerouted addition) and [open] the answer, still undecided.
 */
data class RecordGuard(val date: String, val wipeId: Long?, val checks: List<SlotCheck> = emptyList(), val open: Long? = null)

/**
 * A59: the skip of [slotId] in the answer [answerId] becomes [to] (with [state] when given) while it is [from], inside the
 * same transaction as the slot write: a skip is never applied twice.
 */
data class SkipMove(val answerId: Long, val slotId: Long, val from: String?, val to: String, val state: SlotState? = null)

/** A60 part D: the outcome of [DayRepository.reserve]. */
sealed interface ReserveResult {
    data class Reserved(val replaced: PlannedSlot?) : ReserveResult

    /** The day, the wipe or the slot moved: nothing written. */
    data object Stale : ReserveResult
}

/** A60 part B: a day as a closure reads it. [logs]: each record with its slot (null = Outros). */
data class DayRecord(
    val date: String,
    val logs: List<Pair<SlotRecord, Long?>>,
    val skipped: Set<Long>,
    val planned: Map<Long, PlannedSlot>,
    val workoutKcal: Int?,
)

@Singleton
class DayRepository @Inject constructor(
    private val db: FibraiDatabase,
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
                    combine(
                        db.profileDao().observe(),
                        db.dayDao().observe(today),
                        db.mealLogDao().observeByDate(today),
                        db.mealSlotDao().observeAll(),
                        db.slotSkipDao().observeByDate(today),
                    ) { profile, day, logs, slots, skips ->
                        snapshotOf(today, profile, day, logs, slots, skips)
                    },
                    db.plannedMealDao().observeByDate(today),
                ) { snapshot, planned -> snapshot.copy(planned = planned.associate { it.slotId to it.toPlanned() }) },
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
        tone: String? = null,
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
                        tone = tone?.takeIf { it == "seco" || it == "duro" } ?: current.tone,
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
                if (slotId != null) {
                    db.slotSkipDao().delete(date, slotId)
                    db.plannedMealDao().delete(date, slotId)
                }
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
                db.plannedMealDao().delete(date, slotId)
            }
        }
    }

    /** Explicit user action only. Removes today's logs and the reservation of that slot. */
    suspend fun addSkip(slotId: Long) {
        importOnce()
        val date = todayIso()
        withContext(Dispatchers.IO) {
            db.withTransaction {
                db.slotSkipDao().insert(SlotSkipEntity(date = date, slotId = slotId))
                db.mealLogDao().deleteBySlot(date, slotId)
                db.plannedMealDao().delete(date, slotId)
            }
        }
    }

    /** What [slotId] holds on [date] (ISO): logs in id order and the skip (A34). */
    suspend fun slotState(date: String, slotId: Long): SlotState {
        importOnce()
        return withContext(Dispatchers.IO) { readSlot(date, slotId) }
    }

    /** Every slot of [date] that has a log, a skip or a reservation; a missing slot is empty. */
    suspend fun slotStates(date: String): Map<Long, SlotState> = withContext(Dispatchers.IO) {
        val logs = db.mealLogDao().getByDate(date).filter { it.slotId != null }.groupBy { it.slotId!! }
        val skips = db.slotSkipDao().getByDate(date).map { it.slotId }.toSet()
        val planned = db.plannedMealDao().getByDate(date).associate { it.slotId to it.toPlanned() }
        (logs.keys + skips + planned.keys).associateWith { id -> SlotState(logs[id].orEmpty().map { it.toRecord() }, id in skips, planned[id]) }
    }

    /**
     * Reservar para o {slot} (A60 part D, ADR-046): [plan] becomes the reservation of [slotId] today, in one transaction that
     * rechecks the request day [date], the latest wipe [wipeId] and that the slot has nothing eaten and is not skipped. A
     * reservation already there is replaced. Returns the reservation it replaced (null = none), or [ReserveResult.Stale].
     */
    suspend fun reserve(date: String, wipeId: Long?, slotId: Long, plan: PlannedSlot): ReserveResult {
        importOnce()
        return withContext(Dispatchers.IO) {
            db.withTransaction {
                val today = todayIso()
                if (date != today || db.chatMessageDao().latestIdOf(today, ROLE_WIPED) != wipeId) return@withTransaction ReserveResult.Stale
                val state = readSlot(today, slotId)
                if (!state.open) return@withTransaction ReserveResult.Stale
                writeSlot(today, slotId, state.copy(planned = plan))
                ReserveResult.Reserved(replaced = state.planned)
            }
        }
    }

    /** A60 part B: the tone of the Tali, `seco` or `duro`. The next turn uses it; nothing else changes. */
    suspend fun saveTone(tone: String) {
        require(tone in setOf("seco", "duro"))
        importOnce()
        withContext(Dispatchers.IO) { db.withTransaction { updateProfile { it.copy(tone = tone) } } }
    }

    // ------------------------------------------------------------------ closures (A60 part B, ADR-044)

    suspend fun closure(key: String): ClosureEntity? = withContext(Dispatchers.IO) { db.closureDao().get(key) }

    /** False when a closure with that key already exists: a closure is produced once. */
    suspend fun insertClosure(row: ClosureEntity): Boolean = withContext(Dispatchers.IO) { db.closureDao().insert(row) != -1L }

    suspend fun setClosureText(key: String, text: String?, status: String, retried: Boolean) =
        withContext(Dispatchers.IO) { db.closureDao().setText(key, text, status, retried) }

    /** Closures left without the text for lack of network, whose one retry was not spent. */
    suspend fun offlineClosures(): List<ClosureEntity> = withContext(Dispatchers.IO) { db.closureDao().getOffline() }

    fun observeClosures(from: String): Flow<List<ClosureEntity>> = db.closureDao().observeSince(from)

    /** What a past (or the current) day holds for a closure: its logs, skips, reservations and workout. */
    suspend fun dayRecord(date: String): DayRecord = withContext(Dispatchers.IO) {
        DayRecord(
            date = date,
            logs = db.mealLogDao().getByDate(date).map { it.toRecord() to it.slotId },
            skipped = db.slotSkipDao().getByDate(date).map { it.slotId }.toSet(),
            planned = db.plannedMealDao().getByDate(date).associate { it.slotId to it.toPlanned() },
            workoutKcal = db.dayDao().get(date)?.workoutKcal,
        )
    }

    /**
     * A34: one transaction for every record action of the Chat. Each change must find its slot exactly at
     * `before`; otherwise nothing is written and the result is null. Then each slot becomes `after`, the
     * [receiptMarks] and [recordStates] are set, and [receipts] are inserted in order (date and time = now).
     * Returns the new receipt ids.
     */
    suspend fun commitRecord(
        changes: List<SlotChange>,
        receipts: List<ChatMessageEntity> = emptyList(),
        receiptMarks: Map<Long, String> = emptyMap(),
        recordStates: Map<Long, String> = emptyMap(),
        guard: RecordGuard? = null,
        skip: SkipMove? = null,
        /** A65: the day's workout number must be [WorkoutChange.before]; it becomes [WorkoutChange.after]. */
        workout: WorkoutChange? = null,
    ): List<Long>? {
        importOnce()
        val now = clock.now()
        return withContext(Dispatchers.IO) {
            db.withTransaction {
                if (changes.any { readSlot(it.date, it.slotId) != it.before }) return@withTransaction null
                if (guard != null && !holds(guard, SaoPaulo.date(now).toString())) return@withTransaction null
                if (skip != null && !moveSkipRow(skip)) return@withTransaction null
                if (workout != null && db.dayDao().get(workout.date)?.workoutKcal != workout.before) return@withTransaction null
                changes.forEach { writeSlot(it.date, it.slotId, it.after) }
                workout?.let { w ->
                    val row = db.dayDao().get(w.date) ?: DayEntity(w.date, null, emptyList(), emptyList())
                    db.dayDao().upsert(row.copy(workoutKcal = w.after))
                }
                receiptMarks.forEach { (id, state) -> db.chatMessageDao().setReceiptState(id, state) }
                recordStates.forEach { (id, state) -> db.chatMessageDao().setRecordState(id, state, null) }
                receipts.map {
                    db.chatMessageDao().insert(
                        it.copy(id = 0, date = SaoPaulo.date(now).toString(), createdAtEpochMs = now.toEpochMilli()),
                    )
                }
            }
        }
    }

    /** A47: inside the transaction, the day, the wipe boundary, every read-only slot and the open answer still hold. */
    private suspend fun holds(guard: RecordGuard, today: String): Boolean {
        if (guard.date != today) return false
        if (db.chatMessageDao().latestIdOf(today, ROLE_WIPED) != guard.wipeId) return false
        if (guard.checks.any { readSlot(it.date, it.slotId) != it.state }) return false
        val open = guard.open ?: return true
        return db.chatMessageDao().recordStateOf(open) in OPEN_RECORD_STATES
    }

    /** A59: the skip moves alone (already, pending_delete, kept, expired, failed); false when it was not [SkipMove.from]. */
    suspend fun moveSkip(move: SkipMove): Boolean = withContext(Dispatchers.IO) { db.withTransaction { moveSkipRow(move) } }

    private suspend fun moveSkipRow(move: SkipMove): Boolean {
        val dao = db.chatMessageDao()
        val moved = SkipOutcomes.decode(dao.skipOutcomesOf(move.answerId))?.move(move.slotId, move.from, move.to, move.state) ?: return false
        dao.setSkipOutcomes(move.answerId, moved.encode())
        return true
    }

    /** A59: answers whose delete proposal is still open, any day. */
    suspend fun openSkips(): List<ChatMessageEntity> = withContext(Dispatchers.IO) { db.chatMessageDao().getOpenSkips() }

    /** A60 part A: Pode passar is stored on the plan's row, so it survives recreation. */
    suspend fun setPlanBudget(id: Long, planBudget: String) = withContext(Dispatchers.IO) { db.chatMessageDao().setPlanBudget(id, planBudget) }

    /** A47: [id] becomes [state] only while nothing was decided on it (a tap that lost a race changes nothing). */
    suspend fun closeOpenRecord(id: Long, state: String): Boolean =
        withContext(Dispatchers.IO) { db.chatMessageDao().closeOpenRecord(id, state) > 0 }

    /** A47: the proposal of an open answer and its record state (pending_add for a picked destination). */
    suspend fun setMealChange(id: Long, state: String?, mealChange: String): Boolean =
        withContext(Dispatchers.IO) { db.chatMessageDao().setMealChange(id, state, mealChange) > 0 }

    /** Assistant [id]: its record state; [undoData] = the pending slot of a pending_replace, else null. */
    suspend fun setRecordState(id: Long, state: String?, undoData: String? = null) =
        withContext(Dispatchers.IO) { db.chatMessageDao().setRecordState(id, state, undoData) }

    suspend fun setReceiptState(id: Long, state: String) = withContext(Dispatchers.IO) { db.chatMessageDao().setReceiptState(id, state) }

    /** A receipt's undo data once its memory change is known (the record is committed first). */
    suspend fun setReceiptUndo(id: Long, undoData: String, memoryUpdated: Boolean) =
        withContext(Dispatchers.IO) { db.chatMessageDao().setReceiptUndo(id, undoData, memoryUpdated) }

    /** Assistant rows still waiting for Registrar or Substituir, any day. */
    suspend fun openRecords(): List<ChatMessageEntity> = withContext(Dispatchers.IO) { db.chatMessageDao().getOpenRecords() }

    /** Receipt rows of the Chat's 60 days, oldest first. */
    suspend fun receipts(): List<ChatMessageEntity> =
        withContext(Dispatchers.IO) { db.chatMessageDao().getReceiptsSince(messagesFrom(), ReceiptRules.ROLES.toList()) }

    private suspend fun readSlot(date: String, slotId: Long) = SlotState(
        db.mealLogDao().getBySlot(date, slotId).map { it.toRecord() },
        db.slotSkipDao().getByDate(date).any { it.slotId == slotId },
        db.plannedMealDao().get(date, slotId)?.toPlanned(),
    )

    private suspend fun writeSlot(date: String, slotId: Long, state: SlotState) {
        db.mealLogDao().deleteBySlot(date, slotId)
        db.slotSkipDao().delete(date, slotId)
        db.plannedMealDao().delete(date, slotId)
        state.planned?.let { db.plannedMealDao().upsert(PlannedMealEntity(date, slotId, it.text, it.kcal, it.p, it.c, it.g, it.sourceMessageId)) }
        state.records.forEach { r ->
            db.mealLogDao().insert(
                MealLogEntity(
                    date = date,
                    window = r.window,
                    text = r.text,
                    kcal = r.kcal,
                    p = r.p,
                    stable = if (r.stable) 1 else 0,
                    slotId = slotId,
                    carbs = r.c,
                    fat = r.g,
                    source = r.source,
                    recipeVersionId = r.recipeVersionId,
                ),
            )
        }
        if (state.skipped) db.slotSkipDao().insert(SlotSkipEntity(date = date, slotId = slotId))
    }

    private fun MealLogEntity.toRecord() = SlotRecord(text, kcal, p, carbs, fat, source, window, stable != 0, recipeVersionId)

    private fun PlannedMealEntity.toPlanned() = PlannedSlot(text, kcal, p, c, g, sourceMessageId)

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

    /** App reset, first step (ADR-040): an interrupted reset lands on onboarding. No profile, nothing to mark. */
    suspend fun markOnboardingPending() {
        withContext(Dispatchers.IO) {
            db.withTransaction {
                db.profileDao().get()?.let { db.profileDao().upsert(it.copy(onboardingDone = 0)) }
            }
        }
    }

    /** App reset, last step (ADR-040): every table emptied, schema unchanged. */
    suspend fun clearAll() {
        withContext(Dispatchers.IO) { db.clearAllTables() }
    }

    /** True when any table still has a row: an interrupted reset left something behind. */
    suspend fun hasRows(): Boolean = withContext(Dispatchers.IO) {
        db.query("SELECT 1 FROM profile UNION ALL SELECT 1 FROM day UNION ALL SELECT 1 FROM meal_log UNION ALL SELECT 1 FROM meal_slot UNION ALL SELECT 1 FROM slot_skip UNION ALL SELECT 1 FROM chat_message UNION ALL SELECT 1 FROM day_digest UNION ALL SELECT 1 FROM closure UNION ALL SELECT 1 FROM planned_meal LIMIT 1", null)
            .use { it.moveToFirst() }
    }

    // ------------------------------------------------------------- A71 onboarding (ADR-057)

    fun observeOnboardingAnswers(): Flow<List<OnboardingAnswerEntity>> = db.onboardingAnswerDao().observeAll()

    suspend fun onboardingAnswers(): List<OnboardingAnswerEntity> = withContext(Dispatchers.IO) { db.onboardingAnswerDao().getAll() }

    /** An onboarding in progress: its answers are kept, so a cold start never takes them for a reset cut short. */
    suspend fun hasOnboardingAnswers(): Boolean = onboardingAnswers().isNotEmpty()

    /** One answer as it is given, with the phase it leads to, in one transaction (ADR-057 decision 7). */
    suspend fun saveOnboardingAnswer(row: OnboardingAnswerEntity, phase: String) = withContext(Dispatchers.IO) {
        db.withTransaction {
            db.onboardingAnswerDao().upsert(row)
            updateProfile { it.copy(onboardingPhase = phase) }
        }
    }

    /** Back: the last answer is taken back; the chat asks that step again. */
    suspend fun deleteOnboardingAnswer(step: String) = withContext(Dispatchers.IO) {
        db.withTransaction {
            db.onboardingAnswerDao().delete(step)
            updateProfile { it.copy(onboardingPhase = PHASE_CHAT) }
        }
    }

    suspend fun setOnboardingPhase(phase: String) = withContext(Dispatchers.IO) {
        db.withTransaction { updateProfile { it.copy(onboardingPhase = phase) } }
    }

    /**
     * The profile build succeeded, first step: today's meals replace any stored ones and the profile takes every number,
     * the tone, the goal and the notification choice; the onboarding is not done yet (the facts come next). Returns the
     * new slot ids in [meals] order.
     */
    suspend fun storeOnboardingProfile(
        meals: List<MealSlot>,
        transform: (ProfileEntity) -> ProfileEntity,
    ): List<Long> = withContext(Dispatchers.IO) {
        db.withTransaction {
            db.mealSlotDao().deleteAllExcept(emptyList())
            val ids = meals.mapIndexed { index, slot ->
                db.mealSlotDao().upsert(
                    MealSlotEntity(name = slot.name, minutesFromMidnight = slot.minutesFromMidnight, sortOrder = index, days = slot.days),
                )
            }
            updateProfile { transform(it).copy(onboardingDone = 0, onboardingPhase = PHASE_BUILDING) }
            ids
        }
    }

    /** Last step: the onboarding is done and the raw answers go (ADR-057 decision 5). */
    suspend fun finishOnboarding() = withContext(Dispatchers.IO) {
        db.withTransaction {
            db.onboardingAnswerDao().clear()
            updateProfile { it.copy(onboardingDone = 1, onboardingPhase = PHASE_CHAT) }
        }
    }

    /** A71 Config: the notification choice (reminders and closure notifications). */
    suspend fun saveNotifications(enabled: Boolean) = withContext(Dispatchers.IO) {
        db.withTransaction { updateProfile { it.copy(notificationsEnabled = if (enabled) 1 else 0) } }
    }

    /** A71 Config: the closure time `HH:mm`. */
    suspend fun saveClosureTime(time: String) = withContext(Dispatchers.IO) {
        db.withTransaction { updateProfile { it.copy(closureTime = time) } }
    }

    /** A71 Config: the goal weight and date; null = no goal. */
    suspend fun saveGoal(weightKg: Double?, date: String?) = withContext(Dispatchers.IO) {
        db.withTransaction { updateProfile { it.copy(goalWeightKg = weightKg, goalDate = date.takeIf { weightKg != null }) } }
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
        db.plannedMealDao().deleteByDate(date)
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
        recordMode: String? = null,
        recordSource: String? = null,
        undoData: String? = null,
        recordState: String? = null,
        mealChange: String? = null,
        skipOutcomes: String? = null,
        planBudget: String? = null,
        noted: String? = null,
        actions: String? = null,
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
                    recordMode = recordMode,
                    recordSource = recordSource,
                    undoData = undoData,
                    recordState = recordState,
                    mealChange = mealChange,
                    skipOutcomes = skipOutcomes,
                    planBudget = planBudget,
                    noted = noted,
                    actions = actions,
                ),
            )
        }
    }

    // ------------------------------------------------------------------ saved recipes (A68, ADR-052)

    /** Saves a recipe and its version 1 in one transaction; returns the recipe id. */
    suspend fun saveRecipe(recipe: RecipeEntity, version: RecipeVersionEntity): Long = withContext(Dispatchers.IO) {
        db.withTransaction {
            val id = db.recipeDao().insertRecipe(recipe.copy(createdAtEpochMs = clock.now().toEpochMilli()))
            db.recipeDao().insertVersion(version.copy(recipeId = id, version = 1, createdAtEpochMs = clock.now().toEpochMilli()))
            id
        }
    }

    /** Every saved recipe with its current version, most recent first. */
    fun observeRecipes(): Flow<List<SavedRecipe>> = combine(db.recipeDao().observeRecipes(), db.recipeDao().observeVersions()) { recipes, versions ->
        val current = versions.groupBy { it.recipeId }.mapValues { (_, v) -> v.maxBy { it.version } }
        recipes.mapNotNull { r -> current[r.id]?.let { SavedRecipe(r, it) } }
    }

    suspend fun savedRecipes(): List<SavedRecipe> = observeRecipes().first()

    suspend fun recipeByOrigin(messageId: Long): RecipeEntity? = withContext(Dispatchers.IO) { db.recipeDao().byOrigin(messageId) }

    suspend fun currentRecipeVersion(recipeId: Long): RecipeVersionEntity? = withContext(Dispatchers.IO) { db.recipeDao().currentVersion(recipeId) }

    /** Deletes the recipe and its versions; records that used it keep their numbers. */
    suspend fun deleteRecipe(id: Long) = withContext(Dispatchers.IO) { db.recipeDao().delete(id) }

    /** A67: the chosen option of a plan row becomes its estimate. */
    suspend fun setEstimate(id: Long, kcal: Int, p: Int, c: Int, g: Int, mealText: String?) =
        withContext(Dispatchers.IO) { db.chatMessageDao().setEstimate(id, kcal, p, c, g, mealText) }

    /** A67: assistant rows ever stored. */
    suspend fun assistantCount(): Int = withContext(Dispatchers.IO) { db.chatMessageDao().assistantCount() }

    /** meal_log of the [RECENT_DAYS] days before today (America/Sao_Paulo), for the Chat `recent` (A27). */
    suspend fun recentLogs(): List<MealLogEntity> {
        importOnce()
        val today = SaoPaulo.date(clock.now())
        return withContext(Dispatchers.IO) {
            db.mealLogDao().getBetween(today.minusDays(RECENT_DAYS).toString(), today.minusDays(1).toString())
        }
    }

    /** Today and the previous 59 days, oldest first. */
    fun observeMessages(): Flow<List<ChatMessageEntity>> = db.chatMessageDao().observeSince(messagesFrom())

    /** Chat window (A32): the newest [limit] rows of the same 60 days, newest first. */
    fun observeLatestMessages(limit: Int): Flow<List<ChatMessageEntity>> =
        db.chatMessageDao().observeLatest(messagesFrom(), limit)

    /** The whole conversation of [date] (ISO), oldest first: logic that must not depend on the window. */
    suspend fun messagesOf(date: String): List<ChatMessageEntity> =
        withContext(Dispatchers.IO) { db.chatMessageDao().getByDate(date) }

    suspend fun message(id: Long): ChatMessageEntity? = withContext(Dispatchers.IO) { db.chatMessageDao().getById(id) }

    private fun messagesFrom() = SaoPaulo.date(clock.now()).minusDays(MESSAGE_DAYS - 1L).toString()

    /** Id of today's latest wipe marker, null when today has none (A38: captured before a compaction). */
    suspend fun latestWipeToday(): Long? {
        importOnce()
        return withContext(Dispatchers.IO) { db.chatMessageDao().latestIdOf(todayIso(), ROLE_WIPED) }
    }

    /**
     * At most 2 digests per day. A 3rd overwrites the oldest (seq 1, then seq 2, ...). [coversUntilId]: the newest
     * message it summarised (A38). [sentOn] and [wipeId]: the day and latest wipe captured before the compact
     * call; when today or its latest wipe differ now, nothing is written and the result is false.
     * Null [sentOn] = no check.
     */
    suspend fun upsertDigest(text: String, coversUntilId: Long? = null, sentOn: String? = null, wipeId: Long? = null): Boolean {
        importOnce()
        val now = clock.now()
        val date = SaoPaulo.date(now).toString()
        return withContext(Dispatchers.IO) {
            db.withTransaction {
                if (sentOn != null && (sentOn != date || db.chatMessageDao().latestIdOf(date, ROLE_WIPED) != wipeId)) {
                    return@withTransaction false
                }
                val existing = db.dayDigestDao().getByDate(date)
                val seq = when {
                    existing.none { it.seq == 1 } -> 1
                    existing.none { it.seq == 2 } -> 2
                    else -> existing.minWith(compareBy({ it.createdAtEpochMs }, { it.coversUntilId ?: Long.MIN_VALUE }, { it.seq })).seq
                }
                db.dayDigestDao().upsert(
                    DayDigestEntity(date = date, seq = seq, text = text, createdAtEpochMs = now.toEpochMilli(), coversUntilId = coversUntilId),
                )
                true
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
            tone = profile?.tone ?: "seco",
            goalWeightKg = profile?.goalWeightKg,
            goalDate = profile?.goalDate,
            closureTime = profile?.closureTime ?: "22:00",
            notificationsEnabled = (profile?.notificationsEnabled ?: 1) != 0,
            onboardingPhase = profile?.onboardingPhase ?: "chat",
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

        /** A71: where an unfinished onboarding resumes (`profile.onboardingPhase`). */
        const val PHASE_CHAT = "chat"
        const val PHASE_SUMMARY = "summary"
        const val PHASE_BUILDING = "building"
        const val PHASE_ERROR = "error"

        /** chat_message role written by [wipeToday]. Never rendered, never sent. */
        const val ROLE_WIPED = "wiped"

        /** recordState of an answer nothing was decided on yet (A47 guard): null or a pending confirmation. */
        private val OPEN_RECORD_STATES = setOf(null, "pending_replace", "pending_add", "pending_revise")
    }
}
