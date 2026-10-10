package app.fibrai.android.domain

import app.fibrai.android.feature.onboarding.OnboardingFixtures
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Test

/** A71 (ADR-057 decisions 2–4, 8, 10): the pt-BR parsers of the scripted onboarding, TMB and IMC, the summary, the goal limits, the memory cap and the closure time. */
class OnboardingScriptTest {
    private fun parse(step: OnboardingStep, text: String, answers: Map<OnboardingStep, OnboardingAnswer> = emptyMap()) =
        OnboardingScript.parse(step, text, answers)

    @Test
    fun sixteenSteps_inTheOrderOfTheAdr() {
        assertThat(OnboardingStep.TOTAL).isEqualTo(16)
        assertThat(OnboardingStep.TONE.number).isEqualTo(9)
        assertThat(OnboardingStep.TARGETS.number).isEqualTo(5)
        assertThat(OnboardingStep.entries.map { it.section }.distinct()).containsExactly(
            OnboardingSection.PROFILE, OnboardingSection.MEALS, OnboardingSection.ROUTINE, OnboardingSection.NOTICES,
        ).inOrder()
    }

    @Test
    fun sex_buttonsAndVariants() {
        assertThat(parse(OnboardingStep.SEX, "Feminino")?.sex).isEqualTo("female")
        assertThat(parse(OnboardingStep.SEX, "  mulher ")?.sex).isEqualTo("female")
        assertThat(parse(OnboardingStep.SEX, "HOMEM")?.sex).isEqualTo("male")
        assertThat(parse(OnboardingStep.SEX, "talvez")).isNull()
    }

    @Test
    fun body_rangesAndUnits() {
        assertThat(parse(OnboardingStep.AGE, "32 anos")?.number).isEqualTo(32.0)
        assertThat(parse(OnboardingStep.AGE, "12")).isNull()
        assertThat(parse(OnboardingStep.AGE, "121")).isNull()
        assertThat(parse(OnboardingStep.HEIGHT, "1,80")?.number).isEqualTo(180.0)
        assertThat(parse(OnboardingStep.HEIGHT, "165 cm")?.text).isEqualTo("165 cm")
        assertThat(parse(OnboardingStep.HEIGHT, "99")).isNull()
        assertThat(parse(OnboardingStep.WEIGHT, "66,5 kg")?.text).isEqualTo("66,5 kg")
        assertThat(parse(OnboardingStep.WEIGHT, "24")).isNull()
        assertThat(parse(OnboardingStep.WEIGHT, "70 ou 72")).isNull()
    }

    @Test
    fun tmbAndImc_andTheTargetsPrefill() {
        val answers = OnboardingFixtures.answers.filterKeys { it.ordinal < OnboardingStep.TARGETS.ordinal }
        assertThat(OnboardingScript.tmb(answers)).isEqualTo(1370)
        assertThat(OnboardingScript.bmi(answers)).isEqualTo(24.2)
        assertThat(OnboardingScript.prefill(answers)).isEqualTo("1.370 kcal · P 103 · C 137 · G 46")
        assertThat(OnboardingScript.prompt(OnboardingStep.TARGETS, answers)).contains("(TMB) é 1.370 kcal e o IMC é 24,2")
    }

    @Test
    fun targets_editedText_loneNumber_andMissingMacro() {
        assertThat(parse(OnboardingStep.TARGETS, "1.500 kcal · P 120 · C 150 · G 50")?.targets).isEqualTo(OnboardingTargets(1500, 120, 150, 50))
        assertThat(parse(OnboardingStep.TARGETS, "1800")?.targets).isEqualTo(OnboardingTargets(1800, 135, 180, 60))
        assertThat(parse(OnboardingStep.TARGETS, "1500 kcal P 120 C 150")).isNull()
        assertThat(parse(OnboardingStep.TARGETS, "abc")).isNull()
    }

    @Test
    fun meals_defaultAndTypedLines() {
        val four = mapOf(OnboardingStep.MEAL_COUNT to parse(OnboardingStep.MEAL_COUNT, "quatro")!!)
        val default = parse(OnboardingStep.MEALS, "Usar o padrão", four)!!.meals!!
        assertThat(default.map { it.name }).containsExactly("Café da manhã", "Almoço", "Lanche", "Jantar").inOrder()
        assertThat(default.map { it.minutes }).containsExactly(450, 750, 960, 1200).inOrder()
        val three = mapOf(OnboardingStep.MEAL_COUNT to parse(OnboardingStep.MEAL_COUNT, "3")!!)
        val typed = parse(OnboardingStep.MEALS, "Jantar 19h30\nCafé 7:00; Almoço às 12:15", three)!!.meals!!
        assertThat(typed).containsExactly(OnboardingMeal("Café", 420), OnboardingMeal("Almoço", 735), OnboardingMeal("Jantar", 1170)).inOrder()
        assertThat(parse(OnboardingStep.MEALS, "Café 7:00\nAlmoço 12:00", three)).isNull()
        assertThat(parse(OnboardingStep.MEALS, "Café 25:00\nAlmoço 12:00\nJanta 19:00", three)).isNull()
        assertThat(parse(OnboardingStep.MEAL_COUNT, "7")).isNull()
    }

    @Test
    fun eatBack_toneAndFreeText() {
        assertThat(parse(OnboardingStep.EAT_BACK, "50 %")?.pct).isEqualTo(50)
        assertThat(parse(OnboardingStep.EAT_BACK, "tudo")?.pct).isEqualTo(100)
        assertThat(parse(OnboardingStep.EAT_BACK, "120")).isNull()
        assertThat(parse(OnboardingStep.TONE, "duro")?.tone).isEqualTo("duro")
        assertThat(parse(OnboardingStep.TONE, "pode ser um meio termo")).isNull()
        assertThat(parse(OnboardingStep.RESTRICTIONS, "Nenhuma")?.free).isNull()
        assertThat(parse(OnboardingStep.RESTRICTIONS, "sem glúten e sem lactose")?.free).isEqualTo("sem glúten e sem lactose")
        assertThat(parse(OnboardingStep.DISLIKES, "Pular")?.free).isNull()
        assertThat(parse(OnboardingStep.FOODS, "x".repeat(2001))).isNull()
        assertThat(parse(OnboardingStep.FOODS, "😀".repeat(2000))).isNotNull()
    }

    @Test
    fun goal_weightAndOptionalDate() {
        assertThat(parse(OnboardingStep.GOAL, "60 kg até 30/04/2027")?.goal).isEqualTo(OnboardingGoal(60.0, "2027-04-30"))
        assertThat(parse(OnboardingStep.GOAL, "72,5")?.goal).isEqualTo(OnboardingGoal(72.5, null))
        assertThat(parse(OnboardingStep.GOAL, "Pular")?.goal).isNull()
        assertThat(parse(OnboardingStep.GOAL, "30/02/2027 60 kg")).isNull()
        assertThat(parse(OnboardingStep.GOAL, "emagrecer")).isNull()
    }

    @Test
    fun notifications_yesNoAndTime() {
        assertThat(parse(OnboardingStep.NOTIFICATIONS, "Sim")).isEqualTo(OnboardingAnswer("Sim", notifications = true, closureTime = "22:00"))
        assertThat(parse(OnboardingStep.NOTIFICATIONS, "não")?.notifications).isFalse()
        assertThat(parse(OnboardingStep.NOTIFICATIONS, "sim, 21:30")?.closureTime).isEqualTo("21:30")
        assertThat(parse(OnboardingStep.NOTIFICATIONS, "21h")?.closureTime).isEqualTo("21:00")
        assertThat(parse(OnboardingStep.NOTIFICATIONS, "talvez")).isNull()
    }

    @Test
    fun summary_blocksOfTheGold() {
        val blocks = OnboardingSummary.blocks(OnboardingFixtures.answers).associateBy { it.id }
        assertThat(blocks.getValue("body").lines).containsExactly("Feminino · 32 anos · 165 cm · 66 kg", "TMB 1.370 kcal · IMC 24,2").inOrder()
        assertThat(blocks.getValue("targets").lines).containsExactly("1.370 kcal por dia", "P 103 g · C 137 g · G 46 g").inOrder()
        assertThat(blocks.getValue("meals").lines).containsExactly("Café da manhã 07:30 · Almoço 12:30", "Lanche 16:00 · Jantar 20:00").inOrder()
        assertThat(blocks.getValue("foods").lines.last()).isEqualTo("Não gosta: fígado")
        assertThat(blocks.getValue("goal").lines).containsExactly("60 kg até 30/04/2027")
        assertThat(blocks.getValue("notifications").lines).containsExactly("Ligados · fechamento às 22:00")
        assertThat(OnboardingScript.profile(OnboardingFixtures.answers)!!.eatBackMode).isEqualTo("partial")
    }

    @Test
    fun goalRules_mirrorTheContentPolicy() {
        val today = LocalDate.parse("2026-10-09")
        assertThat(GoalRules.accepted(OnboardingGoal(60.0, "2027-04-30"), 165, 66.0, today)).isTrue()
        assertThat(GoalRules.accepted(OnboardingGoal(48.0, null), 165, 66.0, today)).isFalse()
        assertThat(GoalRules.accepted(OnboardingGoal(60.0, "2026-10-09"), 165, 66.0, today)).isFalse()
        assertThat(GoalRules.accepted(OnboardingGoal(60.0, "2026-11-01"), 165, 66.0, today)).isFalse()
        assertThat(GoalRules.parseDate("30/04/2027")).isEqualTo("2027-04-30")
        assertThat(GoalRules.formatDateField("30042027")).isEqualTo("30/04/2027")
    }

    @Test
    fun declare_permanentCap50_atMost30FromTheOnboarding_noDuplicates() {
        val today = LocalDate.parse("2026-10-09")
        val updates = List(35) { MemoryUpdate(MemoryRules.ADD, null, MemoryRules.PERMANENT, "preference", "k$it", "texto $it", declared = true) }
        val memory = MemoryRules.declare(Memory(), updates, today)
        assertThat(memory.facts).hasSize(MemoryRules.ONBOARDING_MAX)
        assertThat(memory.facts.all { it.source == MemoryRules.DECLARED && it.days.isEmpty() }).isTrue()
        val again = MemoryRules.declare(memory, updates.take(3).map { it.copy(text = "novo") }, today)
        assertThat(again.facts).hasSize(MemoryRules.ONBOARDING_MAX)
        assertThat(again.facts.first().text).isEqualTo("novo")
        // 30 declared + 20 explicit = the cap of 50; the 51st is refused.
        val explicit = (1..21).map { MemoryUpdate(MemoryRules.ADD, null, MemoryRules.PERMANENT, "preference", "e$it", "e $it") }
        val full = MemoryRules.apply(memory, explicit, today).memory
        assertThat(full.facts.count { it.permanent }).isEqualTo(50)
    }

    @Test
    fun closures_atTheProfileTime() {
        val at = LocalTime.of(21, 30)
        val before = Instant.parse("2026-10-09T21:00:00-03:00")
        assertThat(Closures.nextAlarm(before, dayDone = false, closureAt = at)).isEqualTo(Instant.parse("2026-10-09T21:30:00-03:00"))
        assertThat(Closures.duePeriods(Instant.parse("2026-10-09T21:31:00-03:00"), at)).containsExactly(Closures.DAY)
        assertThat(Closures.duePeriods(Instant.parse("2026-10-09T21:29:00-03:00"), at)).isEmpty()
        assertThat(Closures.time("bad")).isEqualTo(Closures.AT)
    }
}
