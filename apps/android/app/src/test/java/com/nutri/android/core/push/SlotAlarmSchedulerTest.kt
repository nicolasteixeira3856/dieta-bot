package com.nutri.android.core.push

import android.app.AlarmManager
import android.app.Application
import android.content.Intent
import com.google.common.truth.Truth.assertThat
import com.nutri.android.core.database.MealSlot
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class SlotAlarmSchedulerTest : PushTestBase() {
    private val alarmManager get() = context.getSystemService(AlarmManager::class.java)

    private fun scheduler() = SlotAlarmScheduler(context, repo, clock)

    /** slotId -> trigger ms of the scheduled slot alarms (the resync alarm apart). */
    private fun slotAlarms(): Map<Long, Long> = shadowOf(alarmManager).scheduledAlarms
        .mapNotNull { alarm ->
            val intent: Intent = shadowOf(alarm.operation).savedIntent
            if (intent.action == PushReceiver.ACTION_SLOT) intent.getLongExtra(PushReceiver.EXTRA_SLOT, -1) to alarm.triggerAtTime else null
        }.toMap()

    private fun resyncAlarms() = shadowOf(alarmManager).scheduledAlarms
        .filter { shadowOf(it.operation).savedIntent.action == PushReceiver.ACTION_RESYNC }

    @After
    fun exactBack() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    @Test
    fun fourSlots_fourAlarms_plusMidnightResync() = runBlocking<Unit> {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        scheduler().resync()
        assertThat(slotAlarms().keys).containsExactlyElementsIn(slotIds)
        assertThat(slotAlarms()[slotIds[3]]).isEqualTo(Instant.parse("2026-09-25T20:00:00-03:00").toEpochMilli())
        assertThat(resyncAlarms().single().triggerAtTime).isEqualTo(Instant.parse("2026-09-26T00:05:00-03:00").toEpochMilli())
    }

    @Test
    fun existingSkipOrLog_isNotScheduled_andResyncCancelsIt() = runBlocking<Unit> {
        val s = scheduler()
        s.resync()
        repo.addSkip(slotIds[2])
        repo.addLog("", "PF", 780, 40, true, slotId = slotIds[1])
        s.resync()
        assertThat(slotAlarms().keys).containsExactly(slotIds[0], slotIds[3])
    }

    @Test
    fun configSavesNewSlots_reschedules_removedSlotCancelled() = runBlocking<Unit> {
        val s = scheduler()
        s.resync()
        repo.saveSlots(
            listOf(
                MealSlot(id = slotIds[1], name = "Almoço", minutesFromMidnight = 13 * 60),
                MealSlot(id = slotIds[3], name = "Jantar", minutesFromMidnight = 20 * 60),
                MealSlot(name = "Ceia", minutesFromMidnight = 22 * 60 + 30),
            ),
        )
        s.resync()
        val alarms = slotAlarms()
        assertThat(alarms).hasSize(3)
        assertThat(alarms.keys).containsNoneOf(slotIds[0], slotIds[2])
        assertThat(alarms[slotIds[1]]).isEqualTo(Instant.parse("2026-09-25T13:00:00-03:00").toEpochMilli())
    }

    @Test
    fun exactDenied_stillSchedulesAll_inexact() = runBlocking<Unit> {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        val s = scheduler()
        assertThat(s.exactAllowed()).isFalse()
        s.resync()
        assertThat(slotAlarms()).hasSize(4)
    }

    @Test
    fun beforeOnboarding_onlyTheResync() = runBlocking<Unit> {
        repo.saveProfile("same", 2000, 2000, 2300, List(7) { 2000 }, "zero", 50, false, "2026-09-25")
        scheduler().resync()
        assertThat(slotAlarms()).isEmpty()
        assertThat(resyncAlarms()).hasSize(1)
    }

    @Test
    fun lateInTheDay_onlyFutureSlots() = runBlocking<Unit> {
        now = Instant.parse("2026-09-25T17:00:00-03:00")
        scheduler().resync()
        assertThat(slotAlarms().keys).containsExactly(slotIds[3])
    }

    @Test fun rolloverToSaturdayCancelsWeekdayAlarmsAndSchedulesWeekendOnly() = runBlocking<Unit> {
        repo.saveSlots(
            listOf(MealSlot(name = "Útil", minutesFromMidnight = 450, days = 31),
                MealSlot(name = "Fim de semana", minutesFromMidnight = 570, days = 96)), "split",
        )
        val slots = repo.observeToday().first().slots
        val weekday = slots.single { it.days == 31 }.id
        val weekend = slots.single { it.days == 96 }.id
        val s = scheduler()
        s.resync()
        assertThat(slotAlarms().keys).containsExactly(weekday)
        now = Instant.parse("2026-09-26T06:00:00-03:00")
        s.resync()
        assertThat(slotAlarms().keys).containsExactly(weekend)
        assertThat(slotAlarms()[weekend]).isEqualTo(Instant.parse("2026-09-26T09:30:00-03:00").toEpochMilli())
    }

}
