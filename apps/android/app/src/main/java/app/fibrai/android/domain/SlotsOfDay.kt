package app.fibrai.android.domain

import java.time.Instant
import java.time.LocalDate

/** Monday is bit 0, Sunday bit 6. The calendar is always Sao Paulo's. */
object SlotsOfDay {
    fun mask(date: LocalDate): Int = 1 shl (date.dayOfWeek.value - 1)
    fun <T> select(slots: List<T>, date: LocalDate, daysOf: (T) -> Int): List<T> =
        slots.filter { daysOf(it) and mask(date) != 0 }
    fun <T> select(slots: List<T>, instant: Instant, daysOf: (T) -> Int): List<T> =
        select(slots, SaoPaulo.date(instant), daysOf)
}

data class SlotDayGroup(val days: Int, val label: String)

object SlotModes {
    val labels = listOf("same" to "Todos os dias", "split" to "Seg–Sex · Sáb–Dom", "each" to "Cada dia")
    fun groups(mode: String): List<SlotDayGroup> = when (mode) {
        "split" -> listOf(SlotDayGroup(31, "Seg a Sex"), SlotDayGroup(96, "Sáb e Dom"))
        "each" -> listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom").mapIndexed { i, label -> SlotDayGroup(1 shl i, label) }
        else -> listOf(SlotDayGroup(127, "Todos os dias"))
    }
}
