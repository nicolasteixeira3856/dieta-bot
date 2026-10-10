package app.fibrai.android.feature.onboarding

import app.fibrai.android.domain.OnboardingAnswer
import app.fibrai.android.domain.OnboardingScript
import app.fibrai.android.domain.OnboardingStep
import app.fibrai.android.domain.OnboardingSummary

/** The D27 sample person (synthetic, ADR-033): female, 32 years, 165 cm, 66 kg; the states of the golds ob0–ob6. */
object OnboardingFixtures {
    private fun parse(step: OnboardingStep, text: String, answers: Map<OnboardingStep, OnboardingAnswer>): OnboardingAnswer =
        OnboardingScript.parse(step, text, answers) ?: error("fixture answer not understood: $step '$text'")

    /** Every answer of the gold ob3. */
    val answers: Map<OnboardingStep, OnboardingAnswer> by lazy {
        val out = linkedMapOf<OnboardingStep, OnboardingAnswer>()
        listOf(
            OnboardingStep.SEX to "Feminino",
            OnboardingStep.AGE to "32",
            OnboardingStep.HEIGHT to "165",
            OnboardingStep.WEIGHT to "66",
            OnboardingStep.TARGETS to "1.370 kcal · P 103 · C 137 · G 46",
            OnboardingStep.MEAL_COUNT to "4",
            OnboardingStep.MEALS to OnboardingScript.DEFAULT_MEALS,
            OnboardingStep.EAT_BACK to "50 %",
            OnboardingStep.TONE to "Seco",
            OnboardingStep.RESTRICTIONS to "Sem lactose",
            OnboardingStep.MEASURING to "Medidas caseiras",
            OnboardingStep.FOODS to "Café: pão com ovo · Almoço: arroz, feijão e frango",
            OnboardingStep.DISLIKES to "fígado",
            OnboardingStep.EQUIPMENT to "Air fryer · micro-ondas",
            OnboardingStep.GOAL to "60 kg até 30/04/2027",
            OnboardingStep.NOTIFICATIONS to "Sim",
        ).forEach { (step, text) -> out[step] = parse(step, text, out) }
        out
    }

    private fun before(step: OnboardingStep) = answers.filterKeys { it.ordinal < step.ordinal }

    private fun q(step: OnboardingStep, time: String) =
        OnboardingMessage("q-${step.name}", OnboardingScript.prompt(step, before(step)), fromUser = false, time = time)

    val ob0 = OnboardingUiState(screen = OnboardingScreen.WELCOME)

    /** Step 9 (tone): the workout question answered `50 %`, the tone question with Seco preselected. */
    val ob1 = OnboardingUiState(
        screen = OnboardingScreen.CHAT,
        step = OnboardingStep.TONE,
        messages = listOf(
            q(OnboardingStep.EAT_BACK, "09:04"),
            OnboardingMessage("a-EAT_BACK", "50 %", fromUser = true, time = "09:04"),
            q(OnboardingStep.TONE, "09:05"),
        ),
        quickReplies = OnboardingScript.quickReplies(OnboardingStep.TONE),
        selectedReply = "Seco",
    )

    /** The tone answered with something the script does not understand: "não entendi" and the buttons again. */
    val ob1e = ob1.copy(
        messages = listOf(
            q(OnboardingStep.TONE, "09:05"),
            OnboardingMessage("r-0", "pode ser um meio termo", fromUser = true, time = "09:06"),
            OnboardingMessage("n-0", OnboardingScript.NOT_UNDERSTOOD, fromUser = false, time = "09:06"),
        ),
    )

    /** Step 5: the weight answered, the TMB and IMC proposal and the targets prefilled in the composer. */
    val ob2 = OnboardingUiState(
        screen = OnboardingScreen.CHAT,
        step = OnboardingStep.TARGETS,
        messages = listOf(
            q(OnboardingStep.WEIGHT, "09:02"),
            OnboardingMessage("a-WEIGHT", "66 kg", fromUser = true, time = "09:02"),
            q(OnboardingStep.TARGETS, "09:03"),
        ),
        composer = OnboardingScript.prefill(before(OnboardingStep.TARGETS)),
    )

    val ob3 by lazy { OnboardingUiState(screen = OnboardingScreen.SUMMARY, summary = OnboardingSummary.blocks(answers)) }

    val ob4 = OnboardingUiState(screen = OnboardingScreen.BUILDING)

    val ob5 = OnboardingUiState(screen = OnboardingScreen.SUCCESS)

    val ob6 = OnboardingUiState(screen = OnboardingScreen.ERROR)
}
