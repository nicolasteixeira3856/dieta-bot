package app.fibrai.android.domain

/** One block of the summary (`ob3`): its title, its lines and the steps its pencil reopens, in order. */
data class SummaryBlock(val id: String, val title: String, val lines: List<String>, val steps: List<OnboardingStep>)

/**
 * The summary before the profile call (ADR-057 decision 4): one block per question group, built from the answers. The IMC
 * and the TMB are information, never a judgment.
 */
object OnboardingSummary {
    fun blocks(answers: Map<OnboardingStep, OnboardingAnswer>): List<SummaryBlock> {
        fun text(step: OnboardingStep) = answers[step]?.text.orEmpty()
        val s = OnboardingScript
        val body = listOf(text(OnboardingStep.SEX), text(OnboardingStep.AGE), text(OnboardingStep.HEIGHT), text(OnboardingStep.WEIGHT))
            .filter { it.isNotEmpty() }.joinToString(" · ")
        val info = listOfNotNull(
            s.tmb(answers)?.let { "TMB ${s.thousands(it)} kcal" },
            s.bmi(answers)?.let { "IMC ${s.decimal(it)}" },
        ).joinToString(" · ")
        val targets = answers[OnboardingStep.TARGETS]?.targets
        val meals = answers[OnboardingStep.MEALS]?.meals.orEmpty()
            .map { "${it.name} ${SlotSuggestions.format(it.minutes)}" }
            .chunked(2) { it.joinToString(" · ") }
        val pct = answers[OnboardingStep.EAT_BACK]?.pct ?: 0
        val tone = if (answers[OnboardingStep.TONE]?.tone == "duro") "Duro · cobra o que estourou e o que faltou" else "Seco · só os números"
        val dislikes = answers[OnboardingStep.DISLIKES]?.free
        val notifications = answers[OnboardingStep.NOTIFICATIONS]
        return listOf(
            SummaryBlock("body", "Corpo", listOf(body, info).filter { it.isNotEmpty() }, listOf(OnboardingStep.SEX, OnboardingStep.AGE, OnboardingStep.HEIGHT, OnboardingStep.WEIGHT)),
            SummaryBlock(
                "targets",
                "Teto e macros",
                targets?.let { listOf("${s.thousands(it.kcal)} kcal por dia", "P ${it.p} g · C ${it.c} g · G ${it.g} g") }.orEmpty(),
                listOf(OnboardingStep.TARGETS),
            ),
            SummaryBlock("meals", "Refeições", meals, listOf(OnboardingStep.MEAL_COUNT, OnboardingStep.MEALS)),
            SummaryBlock("eat_back", "Treino", listOf("$pct % do gasto do treino volta para o teto"), listOf(OnboardingStep.EAT_BACK)),
            SummaryBlock("tone", "Tom", listOf(tone), listOf(OnboardingStep.TONE)),
            SummaryBlock("restrictions", "Restrições", listOf(text(OnboardingStep.RESTRICTIONS)), listOf(OnboardingStep.RESTRICTIONS)),
            SummaryBlock("measuring", "Medidas", listOf(text(OnboardingStep.MEASURING)), listOf(OnboardingStep.MEASURING)),
            SummaryBlock(
                "foods",
                "Comidas",
                listOfNotNull(text(OnboardingStep.FOODS), dislikes?.let { "Não gosta: $it" }),
                listOf(OnboardingStep.FOODS, OnboardingStep.DISLIKES),
            ),
            SummaryBlock("equipment", "Equipamentos", listOf(text(OnboardingStep.EQUIPMENT)), listOf(OnboardingStep.EQUIPMENT)),
            SummaryBlock(
                "goal",
                "Meta de peso",
                listOf(answers[OnboardingStep.GOAL]?.goal?.let(s::goalText) ?: "Sem meta"),
                listOf(OnboardingStep.GOAL),
            ),
            SummaryBlock(
                "notifications",
                "Avisos",
                listOf(
                    if (notifications?.notifications == false) "Desligados"
                    else "Ligados · fechamento às ${notifications?.closureTime ?: OnboardingScript.CLOSURE_DEFAULT}",
                ),
                listOf(OnboardingStep.NOTIFICATIONS),
            ),
        )
    }
}
