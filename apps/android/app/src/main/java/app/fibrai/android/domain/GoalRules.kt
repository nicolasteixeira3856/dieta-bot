package app.fibrai.android.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The goal safety limits of the content policy (§ Onboarding profile, S41), mirrored on the device for the Config edit
 * (A71): the onboarding goal is checked by `POST /v1/profile`; a goal typed in Config never reaches that route, so the app
 * applies the same three limits. Pure.
 */
object GoalRules {
    const val MIN_BMI = 18.5

    /** Above 1 % of the current weight per week, loss or gain, is refused. */
    const val MAX_WEEKLY_PCT = 1.0

    /** True when the goal is inside the limits: goal BMI ≥ 18.5, a date after [today], a pace ≤ 1 % a week. */
    fun accepted(goal: OnboardingGoal, heightCm: Int, weightKg: Double, today: LocalDate): Boolean {
        if (goal.weightKg !in OnboardingScript.GOAL_WEIGHT) return false
        if (heightCm > 0) {
            val m = heightCm / 100.0
            if (goal.weightKg / (m * m) < MIN_BMI) return false
        }
        val date = goal.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() ?: return false } ?: return true
        if (!date.isAfter(today)) return false
        if (weightKg <= 0) return true
        val weeks = ChronoUnit.DAYS.between(today, date) / 7.0
        val pace = kotlin.math.abs(weightKg - goal.weightKg) / weeks
        return pace <= weightKg * MAX_WEEKLY_PCT / 100.0
    }

    /** `30042027`, `30/04/2027` → ISO; null when it is not a whole valid date. */
    fun parseDate(text: String): String? {
        val digits = text.filter(Char::isDigit)
        if (digits.length != 8) return null
        return runCatching { LocalDate.of(digits.substring(4).toInt(), digits.substring(2, 4).toInt(), digits.substring(0, 2).toInt()).toString() }.getOrNull()
    }

    /** The date field as typed: digits with the slashes put in (`30/04/2027`). */
    fun formatDateField(text: String): String {
        val d = text.filter(Char::isDigit).take(8)
        return buildString {
            d.forEachIndexed { i, ch ->
                if (i == 2 || i == 4) append('/')
                append(ch)
            }
        }
    }
}
