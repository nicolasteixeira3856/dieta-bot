package app.fibrai.android.domain

import java.time.Instant

object SlotClock {
    fun minutesFromMidnight(instant: Instant): Int {
        val local = instant.atZone(SaoPaulo.zone).toLocalTime()
        return local.hour * 60 + local.minute
    }

    /**
     * Current slot = last slot with minutes <= now.
     * Before the first slot of the day, the first slot is current.
     */
    fun <T> current(slots: List<T>, nowMinutes: Int, minutesOf: (T) -> Int): T? {
        if (slots.isEmpty()) return null
        val sorted = slots.sortedBy(minutesOf)
        return sorted.lastOrNull { minutesOf(it) <= nowMinutes } ?: sorted.first()
    }
}
