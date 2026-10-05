package app.fibrai.android.domain

/** Meal-time bands for O3 name suggestions. Minutes from midnight, America/Sao_Paulo. */
enum class SlotBand { BREAKFAST, MORNING_SNACK, LUNCH, AFTERNOON_SNACK, DINNER, NIGHT }

object SlotSuggestions {
    const val MIN_SLOTS = 2
    const val MAX_SLOTS = 6

    fun bandOf(minutes: Int): SlotBand {
        val hour = (minutes.mod(1440)) / 60
        return when (hour) {
            in 5..9 -> SlotBand.BREAKFAST
            10 -> SlotBand.MORNING_SNACK
            in 11..14 -> SlotBand.LUNCH
            in 15..17 -> SlotBand.AFTERNOON_SNACK
            in 18..21 -> SlotBand.DINNER
            else -> SlotBand.NIGHT
        }
    }

    /** Chip labels. They fill the field only when tapped; a name is never assumed. */
    fun namesFor(minutes: Int): List<String> = when (bandOf(minutes)) {
        SlotBand.BREAKFAST -> listOf("Café", "Desjejum")
        SlotBand.MORNING_SNACK -> listOf("Lanche da manhã", "Lanche")
        SlotBand.LUNCH -> listOf("Almoço", "Prato feito")
        SlotBand.AFTERNOON_SNACK -> listOf("Lanche", "Café da tarde")
        SlotBand.DINNER -> listOf("Jantar", "Ceia")
        SlotBand.NIGHT -> listOf("Ceia", "Lanche da noite")
    }

    /** Default times for a slot count. Names start empty. */
    fun defaultTimes(count: Int): List<Int> = when (count.coerceIn(MIN_SLOTS, MAX_SLOTS)) {
        2 -> listOf(hm(12, 30), hm(20, 0))
        3 -> listOf(hm(7, 30), hm(12, 30), hm(20, 0))
        4 -> listOf(hm(7, 30), hm(12, 30), hm(16, 0), hm(20, 0))
        5 -> listOf(hm(7, 30), hm(10, 0), hm(12, 30), hm(16, 0), hm(20, 0))
        else -> listOf(hm(7, 30), hm(10, 0), hm(12, 30), hm(16, 0), hm(20, 0), hm(22, 30))
    }

    fun format(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)

    private fun hm(h: Int, m: Int) = h * 60 + m
}
