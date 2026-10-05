package app.fibrai.android.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** One reminder: the slot's time today, America/Sao_Paulo. */
data class SlotAlarm(val slotId: Long, val name: String, val at: Instant)

data class PushSlot(val id: Long, val name: String, val minutesFromMidnight: Int)

/**
 * Push rules (spec memoria-push, push): one alarm per profile slot at its time, only while the
 * slot has no log and no skip. Weekends use the same slots the user registered. Pure: no Android.
 */
object PushPlan {
    /** Daily resync right after midnight SP: the new day's slots get their alarms. */
    val RESYNC_AT: LocalTime = LocalTime.of(0, 5)

    /**
     * Alarms still due today. [loggedSlotIds]/[skippedSlotIds] belong to [dayDate]; a snapshot of
     * another day (stale after midnight) counts as an empty day.
     */
    fun alarms(
        slots: List<PushSlot>,
        loggedSlotIds: Set<Long>,
        skippedSlotIds: Set<Long>,
        dayDate: LocalDate?,
        now: Instant,
    ): List<SlotAlarm> {
        val today = SaoPaulo.date(now)
        val current = dayDate == today
        return slots
            .filter { !current || (it.id !in loggedSlotIds && it.id !in skippedSlotIds) }
            .map { SlotAlarm(it.id, it.name, at(today, it.minutesFromMidnight)) }
            .filter { it.at.isAfter(now) }
            .sortedBy { it.at }
    }

    /** Fire-time check (spec rule 2): a slot with a log or a skip never notifies. */
    fun shouldNotify(slotId: Long, loggedSlotIds: Set<Long>, skippedSlotIds: Set<Long>): Boolean =
        slotId !in loggedSlotIds && slotId !in skippedSlotIds

    fun nextResync(now: Instant): Instant {
        val today = SaoPaulo.date(now)
        val todayAt = today.atTime(RESYNC_AT).atZone(SaoPaulo.zone).toInstant()
        return if (todayAt.isAfter(now)) todayAt else today.plusDays(1).atTime(RESYNC_AT).atZone(SaoPaulo.zone).toInstant()
    }

    /** "{nome}. Ainda não registrou." (spec rule 3). */
    fun copy(name: String): String = "$name. Ainda não registrou."

    private fun at(date: LocalDate, minutes: Int): Instant =
        date.atStartOfDay(SaoPaulo.zone).plusMinutes(minutes.toLong()).toInstant()
}
