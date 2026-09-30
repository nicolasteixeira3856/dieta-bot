package com.nutri.android.core.push

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nutri.android.core.database.slotsOfDay
import com.nutri.android.core.database.DayRepository
import com.nutri.android.core.database.InstantClock
import com.nutri.android.domain.PushPlan
import com.nutri.android.domain.PushSlot
import com.nutri.android.domain.SlotAlarm
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * One AlarmManager alarm per slot still due today + the 00:05 SP resync. Exact when the app may
 * (Android 12+: SCHEDULE_EXACT_ALARM, not pre-granted on 14+); otherwise inexact
 * setAndAllowWhileIdle — the reminder may come some minutes late, never early. No Firebase.
 */
@Singleton
class SlotAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DayRepository,
    private val clock: InstantClock,
) {
    private val alarms: AlarmManager = context.getSystemService(AlarmManager::class.java)
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val mutex = Mutex()

    /** Cancels what was scheduled and schedules today's due slots from a fresh snapshot. */
    suspend fun resync(): List<SlotAlarm> = mutex.withLock {
        val day = repository.observeToday().first()
        val now = clock.now()
        val due = PushPlan.alarms(
            slots = day.slotsOfDay.map { PushSlot(it.id, it.name, it.minutesFromMidnight) },
            loggedSlotIds = day.logs.mapNotNull { it.slotId }.toSet(),
            skippedSlotIds = day.skippedSlotIds,
            dayDate = runCatching { LocalDate.parse(day.date) }.getOrNull(),
            now = now,
        )
        scheduledIds().forEach { cancel(it) }
        if (day.onboardingDone) due.forEach { set(it.at, slotIntent(it.slotId)) }
        set(PushPlan.nextResync(now), resyncIntent())
        prefs.edit().putStringSet(KEY_IDS, due.map { it.slotId.toString() }.toSet()).apply()
        due
    }

    fun cancel(slotId: Long) {
        alarms.cancel(slotIntent(slotId))
    }

    fun exactAllowed(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

    private fun set(at: Instant, intent: PendingIntent) {
        val ms = at.toEpochMilli()
        if (exactAllowed()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, intent)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, intent)
        }
    }

    private fun scheduledIds(): List<Long> = prefs.getStringSet(KEY_IDS, emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }

    private fun slotIntent(slotId: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        slotId.toInt(),
        Intent(context, PushReceiver::class.java).setAction(PushReceiver.ACTION_SLOT).putExtra(PushReceiver.EXTRA_SLOT, slotId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun resyncIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        RESYNC_CODE,
        Intent(context, PushReceiver::class.java).setAction(PushReceiver.ACTION_RESYNC),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val PREFS = "nutri_push"
        const val KEY_IDS = "scheduled_slot_ids"
        const val RESYNC_CODE = -1
    }
}
