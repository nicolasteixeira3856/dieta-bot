package app.fibrai.android.domain

import java.text.Normalizer
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable

/** The four sections of the onboarding progress (D27 `Onboarding/Progress`). */
enum class OnboardingSection(val label: String) { PROFILE("Perfil"), MEALS("Refeições"), ROUTINE("Rotina"), NOTICES("Avisos") }

/** How a step is answered: buttons, a number, free text, meal lines, the prefilled targets or yes/no with a time. */
enum class OnboardingKind { CHOICE, NUMBER, TEXT, SLOTS, PREFILL, YES_NO_TIME }

/**
 * The questions of the conversational onboarding (ADR-057 decision 3), one message each, in this order. [number] is the
 * `{n} de 16` of the progress; [skippable] steps offer `Pular`. The name is the stored key (`onboarding_answer.step`) and,
 * lowercased, the telemetry enum.
 */
enum class OnboardingStep(val number: Int, val section: OnboardingSection, val kind: OnboardingKind, val skippable: Boolean = false) {
    SEX(1, OnboardingSection.PROFILE, OnboardingKind.CHOICE),
    AGE(2, OnboardingSection.PROFILE, OnboardingKind.NUMBER),
    HEIGHT(3, OnboardingSection.PROFILE, OnboardingKind.NUMBER),
    WEIGHT(4, OnboardingSection.PROFILE, OnboardingKind.NUMBER),
    TARGETS(5, OnboardingSection.PROFILE, OnboardingKind.PREFILL),
    MEAL_COUNT(6, OnboardingSection.MEALS, OnboardingKind.CHOICE),
    MEALS(7, OnboardingSection.MEALS, OnboardingKind.SLOTS),
    EAT_BACK(8, OnboardingSection.ROUTINE, OnboardingKind.CHOICE),
    TONE(9, OnboardingSection.ROUTINE, OnboardingKind.CHOICE),
    RESTRICTIONS(10, OnboardingSection.ROUTINE, OnboardingKind.TEXT),
    MEASURING(11, OnboardingSection.ROUTINE, OnboardingKind.TEXT),
    FOODS(12, OnboardingSection.ROUTINE, OnboardingKind.TEXT),
    DISLIKES(13, OnboardingSection.ROUTINE, OnboardingKind.TEXT, skippable = true),
    EQUIPMENT(14, OnboardingSection.ROUTINE, OnboardingKind.TEXT),
    GOAL(15, OnboardingSection.ROUTINE, OnboardingKind.TEXT, skippable = true),
    NOTIFICATIONS(16, OnboardingSection.NOTICES, OnboardingKind.YES_NO_TIME),
    ;

    /** Free text, the prefilled targets and the meal lines show the `{n}/2000` counter (gold `ob2`). */
    val counted: Boolean get() = kind == OnboardingKind.TEXT || kind == OnboardingKind.PREFILL || kind == OnboardingKind.SLOTS

    val telemetry: String get() = name.lowercase(Locale.ROOT)

    companion object {
        val TOTAL = entries.size
    }
}

/** A meal of the onboarding: its name and minutes from midnight. */
@Serializable
data class OnboardingMeal(val name: String, val minutes: Int)

/** The ceiling and the macro targets the user sent (the prefilled text, edited or not). */
@Serializable
data class OnboardingTargets(val kcal: Int, val p: Int, val c: Int, val g: Int)

/** A goal weight and an optional ISO date. */
@Serializable
data class OnboardingGoal(val weightKg: Double, val date: String? = null)

/**
 * One stored answer (`onboarding_answer.value`): [text] is the user bubble; the typed field of its step carries the value.
 * [free] is null on a skipped or "nenhuma" free-text answer.
 */
@Serializable
data class OnboardingAnswer(
    val text: String,
    val sex: String? = null,
    val number: Double? = null,
    val targets: OnboardingTargets? = null,
    val meals: List<OnboardingMeal>? = null,
    val pct: Int? = null,
    val tone: String? = null,
    val free: String? = null,
    val goal: OnboardingGoal? = null,
    val notifications: Boolean? = null,
    val closureTime: String? = null,
)

/** The pieces of the profile once every step is answered. */
data class OnboardingProfile(
    val sex: String,
    val age: Int,
    val heightCm: Int,
    val weightKg: Double,
    val targets: OnboardingTargets,
    val meals: List<OnboardingMeal>,
    val eatBackPct: Int,
    val tone: String,
    val restrictions: String?,
    val measuring: String?,
    val foods: String?,
    val dislikes: String?,
    val equipment: String?,
    val goal: OnboardingGoal?,
    val notifications: Boolean,
    val closureTime: String,
) {
    /** `zero` | `partial` | `full`, as the profile and the server store it. */
    val eatBackMode: String get() = when (eatBackPct) {
        0 -> "zero"
        100 -> "full"
        else -> "partial"
    }
}

/**
 * The scripted onboarding chat (ADR-057 decision 2): the prompt of each step, its quick replies and the pt-BR parser of
 * its answer. No model call. A null parse is the "não entendi" retry.
 */
object OnboardingScript {
    const val TEXT_MAX = ChatText.MAX_CHARS
    const val NOT_UNDERSTOOD = "Desculpa, não entendi. Pode repetir?"
    const val SKIP = "Pular"
    const val DEFAULT_MEALS = "Usar o padrão"
    const val CLOSURE_DEFAULT = "22:00"
    const val GOAL_REFUSED = "Meta de peso não aplicada."

    val AGE = 13..120
    val HEIGHT = 100..250
    val WEIGHT = 25.0..400.0
    val GOAL_WEIGHT = 20.0..400.0
    val KCAL = 1..20000
    val GRAMS = 0..5000

    private val ptBr: Locale = Locale.forLanguageTag("pt-BR")
    private val dayMonthYear = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun first(): OnboardingStep = OnboardingStep.entries.first()

    /** The first step without an answer, null when all are answered. */
    fun nextUnanswered(answers: Map<OnboardingStep, OnboardingAnswer>): OnboardingStep? =
        OnboardingStep.entries.firstOrNull { it !in answers || !stillValid(it, answers) }

    /** The meal lines must match the count answered before them; a changed count reopens them. */
    fun stillValid(step: OnboardingStep, answers: Map<OnboardingStep, OnboardingAnswer>): Boolean = when (step) {
        OnboardingStep.MEALS -> answers[step]?.meals?.size == answers[OnboardingStep.MEAL_COUNT]?.number?.toInt()
        else -> true
    }

    // ------------------------------------------------------------------ prompts

    fun prompt(step: OnboardingStep, answers: Map<OnboardingStep, OnboardingAnswer>): String = when (step) {
        OnboardingStep.SEX -> "Oi, eu sou a Tali. Para calcular o seu gasto em repouso, preciso de quatro números. Qual é o seu sexo?"
        OnboardingStep.AGE -> "Quantos anos você tem?"
        OnboardingStep.HEIGHT -> "Qual é a sua altura, em cm?"
        OnboardingStep.WEIGHT -> "Quanto você pesa hoje, em kg?"
        OnboardingStep.TARGETS -> targetsPrompt(answers)
        OnboardingStep.MEAL_COUNT -> "Quantas refeições você faz por dia? De 2 a 6."
        OnboardingStep.MEALS -> {
            val count = answers[OnboardingStep.MEAL_COUNT]?.number?.toInt() ?: 4
            "Quais são as refeições e os horários? Escreva uma por linha, como “Almoço 12:30”, ou use o padrão: " +
                defaultMeals(count).joinToString(", ") { "${it.name} ${SlotSuggestions.format(it.minutes)}" } + "."
        }
        OnboardingStep.EAT_BACK -> "Treino conta na meta do dia? Escolha quanto do gasto do treino volta para o teto: 0 %, 50 % ou 100 %, ou escreva outro valor."
        OnboardingStep.TONE -> "Como você quer que eu fale com você?\n" +
            "Seco: só os números. Ex.: “Jantar 520 kcal. Sobram 380.”\n" +
            "Duro: cobra o que estourou e o que faltou. Ex.: “Passou 210 kcal. O almoço de amanhã cabe em 600.”"
        OnboardingStep.RESTRICTIONS -> "Tem alguma restrição alimentar? Toque numa opção ou escreva."
        OnboardingStep.MEASURING -> "Como você mede o que come: balança ou medidas caseiras (colher, xícara, unidade)?"
        OnboardingStep.FOODS -> "O que você costuma comer, e em quais refeições? Ex.: “Café: pão com ovo. Almoço: arroz, feijão e frango.”"
        OnboardingStep.DISLIKES -> "Tem algo que você não come de jeito nenhum?"
        OnboardingStep.EQUIPMENT -> "Quais equipamentos você tem na cozinha?"
        OnboardingStep.GOAL -> "Tem uma meta de peso? Escreva o peso e, se quiser, a data. Ex.: “60 kg até 30/04/2027”."
        OnboardingStep.NOTIFICATIONS -> "Quer lembretes no horário de cada refeição e o fechamento do dia? O fechamento sai às 22:00; " +
            "para outro horário, escreva o horário (ex.: 21:30)."
    }

    private fun targetsPrompt(answers: Map<OnboardingStep, OnboardingAnswer>): String {
        val tmb = tmb(answers) ?: return "Qual teto de calorias por dia você quer, com os macros? Ex.: “1.800 kcal · P 135 · C 180 · G 60”."
        val ceiling = (tmb / 10.0).roundToInt() * 10
        val bmi = bmi(answers)
        return "Pelo que você me disse, o seu gasto em repouso (TMB) é ${thousands(tmb)} kcal" +
            (bmi?.let { " e o IMC é ${decimal(it)}" } ?: "") +
            ". Proponho o teto de ${thousands(ceiling)} kcal por dia, com 30 % de proteína, 40 % de carboidrato e 30 % de gordura. " +
            "Edite os números abaixo ou envie como está."
    }

    /** The text put in the composer at the targets step: `{ceiling} kcal · P {p} · C {c} · G {g}` (ADR-057 decision 3). */
    fun prefill(answers: Map<OnboardingStep, OnboardingAnswer>): String {
        answers[OnboardingStep.TARGETS]?.let { return it.text }
        val tmb = tmb(answers) ?: return ""
        val ceiling = (tmb / 10.0).roundToInt() * 10
        val split = MacroSplit.of(ceiling)
        return targetsText(OnboardingTargets(ceiling, split.proteinG, split.carbG, split.fatG))
    }

    /** The quick replies of a step; empty for a number or free-only step. */
    fun quickReplies(step: OnboardingStep): List<String> = when (step) {
        OnboardingStep.SEX -> listOf("Feminino", "Masculino")
        OnboardingStep.MEAL_COUNT -> (SlotSuggestions.MIN_SLOTS..SlotSuggestions.MAX_SLOTS).map { it.toString() }
        OnboardingStep.MEALS -> listOf(DEFAULT_MEALS)
        OnboardingStep.EAT_BACK -> listOf("0 %", "50 %", "100 %")
        OnboardingStep.TONE -> listOf("Seco", "Duro")
        OnboardingStep.RESTRICTIONS -> listOf("Nenhuma", "Sem lactose", "Sem glúten", "Vegetariano", "Vegano", "Diabetes", "Hipertensão")
        OnboardingStep.MEASURING -> listOf("Balança", "Medidas caseiras")
        OnboardingStep.EQUIPMENT -> listOf("Air fryer", "Micro-ondas", "Panela de pressão", "Nenhum")
        OnboardingStep.DISLIKES, OnboardingStep.GOAL -> listOf(SKIP)
        OnboardingStep.NOTIFICATIONS -> listOf("Sim", "Não")
        else -> emptyList()
    }

    /** The quick reply drawn selected: the answer given (an edit) or, for the tone, Seco (preselected, ADR-044). */
    fun selectedReply(step: OnboardingStep, answer: OnboardingAnswer?): String? = when {
        answer != null -> quickReplies(step).firstOrNull { fold(it) == fold(answer.text) }
        step == OnboardingStep.TONE -> "Seco"
        else -> null
    }

    // ------------------------------------------------------------------ parsing

    /** The answer of [input] at [step], or null when it is not understood (the step is asked again). */
    fun parse(step: OnboardingStep, input: String, answers: Map<OnboardingStep, OnboardingAnswer>): OnboardingAnswer? {
        val text = input.trim()
        if (text.isEmpty() || text.codePointCount(0, text.length) > TEXT_MAX) return null
        val f = fold(text)
        return when (step) {
            OnboardingStep.SEX -> when {
                f in setOf("feminino", "feminina", "mulher", "f") -> OnboardingAnswer("Feminino", sex = "female")
                f in setOf("masculino", "homem", "m") -> OnboardingAnswer("Masculino", sex = "male")
                else -> null
            }
            OnboardingStep.AGE -> integer(f)?.takeIf { it in AGE }?.let { OnboardingAnswer("$it anos", number = it.toDouble()) }
            OnboardingStep.HEIGHT -> height(f)?.let { OnboardingAnswer("$it cm", number = it.toDouble()) }
            OnboardingStep.WEIGHT -> decimalNumber(f)?.takeIf { it in WEIGHT }?.let { OnboardingAnswer("${decimal(it)} kg", number = it) }
            OnboardingStep.TARGETS -> targets(f)?.let { OnboardingAnswer(targetsText(it), targets = it) }
            OnboardingStep.MEAL_COUNT -> count(f)?.let { OnboardingAnswer(it.toString(), number = it.toDouble()) }
            OnboardingStep.MEALS -> meals(text, answers[OnboardingStep.MEAL_COUNT]?.number?.toInt() ?: return null)
            OnboardingStep.EAT_BACK -> pct(f)?.let { OnboardingAnswer("$it %", pct = it) }
            OnboardingStep.TONE -> when (f) {
                "seco" -> OnboardingAnswer("Seco", tone = "seco")
                "duro" -> OnboardingAnswer("Duro", tone = "duro")
                else -> null
            }
            OnboardingStep.RESTRICTIONS -> if (f in NONE) OnboardingAnswer("Nenhuma") else OnboardingAnswer(text, free = text)
            OnboardingStep.EQUIPMENT -> if (f in NONE) OnboardingAnswer("Nenhum") else OnboardingAnswer(text, free = text)
            OnboardingStep.MEASURING, OnboardingStep.FOODS -> OnboardingAnswer(text, free = text)
            OnboardingStep.DISLIKES -> if (f == fold(SKIP) || f in NONE) OnboardingAnswer(SKIP) else OnboardingAnswer(text, free = text)
            OnboardingStep.GOAL -> if (f == fold(SKIP) || f in NO_GOAL) OnboardingAnswer(SKIP) else goal(text)
            OnboardingStep.NOTIFICATIONS -> notifications(f)
        }
    }

    /** The profile once every step is answered; null while a step is missing. */
    fun profile(answers: Map<OnboardingStep, OnboardingAnswer>): OnboardingProfile? {
        if (nextUnanswered(answers) != null) return null
        fun a(step: OnboardingStep) = answers.getValue(step)
        return OnboardingProfile(
            sex = a(OnboardingStep.SEX).sex ?: return null,
            age = a(OnboardingStep.AGE).number?.toInt() ?: return null,
            heightCm = a(OnboardingStep.HEIGHT).number?.toInt() ?: return null,
            weightKg = a(OnboardingStep.WEIGHT).number ?: return null,
            targets = a(OnboardingStep.TARGETS).targets ?: return null,
            meals = a(OnboardingStep.MEALS).meals ?: return null,
            eatBackPct = a(OnboardingStep.EAT_BACK).pct ?: return null,
            tone = a(OnboardingStep.TONE).tone ?: "seco",
            restrictions = a(OnboardingStep.RESTRICTIONS).free,
            measuring = a(OnboardingStep.MEASURING).free,
            foods = a(OnboardingStep.FOODS).free,
            dislikes = a(OnboardingStep.DISLIKES).free,
            equipment = a(OnboardingStep.EQUIPMENT).free,
            goal = a(OnboardingStep.GOAL).goal,
            notifications = a(OnboardingStep.NOTIFICATIONS).notifications ?: true,
            closureTime = a(OnboardingStep.NOTIFICATIONS).closureTime ?: CLOSURE_DEFAULT,
        )
    }

    /** The meals of `Usar o padrão` for [count]: the default times with the usual name of each hour. */
    fun defaultMeals(count: Int): List<OnboardingMeal> = SlotSuggestions.defaultTimes(count).map { OnboardingMeal(defaultName(it), it) }

    fun defaultName(minutes: Int): String = when (SlotSuggestions.bandOf(minutes)) {
        SlotBand.BREAKFAST -> "Café da manhã"
        SlotBand.MORNING_SNACK -> "Lanche da manhã"
        SlotBand.LUNCH -> "Almoço"
        SlotBand.AFTERNOON_SNACK -> "Lanche"
        SlotBand.DINNER -> "Jantar"
        SlotBand.NIGHT -> "Ceia"
    }

    fun tmb(answers: Map<OnboardingStep, OnboardingAnswer>): Int? = TmbCalculator.tmb(
        when (answers[OnboardingStep.SEX]?.sex) {
            "male" -> Sex.MALE
            "female" -> Sex.FEMALE
            else -> null
        },
        answers[OnboardingStep.AGE]?.number?.toInt() ?: 0,
        answers[OnboardingStep.HEIGHT]?.number?.toInt() ?: 0,
        answers[OnboardingStep.WEIGHT]?.number ?: 0.0,
    )

    /** IMC = kg / m², one decimal; information only (ADR-057 decision 4). */
    fun bmi(answers: Map<OnboardingStep, OnboardingAnswer>): Double? {
        val kg = answers[OnboardingStep.WEIGHT]?.number ?: return null
        val cm = answers[OnboardingStep.HEIGHT]?.number ?: return null
        if (kg <= 0 || cm <= 0) return null
        val m = cm / 100.0
        return (kg / (m * m) * 10).roundToInt() / 10.0
    }

    fun targetsText(t: OnboardingTargets) = "${thousands(t.kcal)} kcal · P ${t.p} · C ${t.c} · G ${t.g}"

    fun goalText(goal: OnboardingGoal): String =
        "${decimal(goal.weightKg)} kg" + (goal.date?.let { " até ${LocalDate.parse(it).format(dayMonthYear)}" } ?: "")

    fun thousands(n: Int): String = NumberFormat.getIntegerInstance(ptBr).format(n)

    /** `66`, `66,5`: one decimal at most, pt-BR comma. */
    fun decimal(value: Double): String {
        val rounded = (value * 10).roundToInt() / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else String.format(ptBr, "%.1f", rounded)
    }

    /** Lowercase, no accents, single spaces. */
    fun fold(text: String): String = Normalizer.normalize(text.trim().lowercase(ptBr), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("\\s+"), " ")

    // ------------------------------------------------------------------ parsers

    private val NONE = setOf("nenhuma", "nenhum", "nao", "nada", "nao tenho", "sem restricao", "sem restricoes")
    private val NO_GOAL = setOf("nao", "nao tenho", "sem meta", "nenhuma")
    private val WORDS = mapOf("dois" to 2, "duas" to 2, "tres" to 3, "quatro" to 4, "cinco" to 5, "seis" to 6)
    private val INTEGER = Regex("""\d+""")
    private val NUMBER = Regex("""\d+(?:[.,]\d+)?""")
    private val TIME = Regex("""(\d{1,2})(?:\s*(?::|h)\s*(\d{2}))?\s*h?""")
    private val DATE = Regex("""(\d{1,2})/(\d{1,2})/(\d{2,4})""")

    private fun integer(f: String): Int? = INTEGER.findAll(f).singleOrNull()?.value?.toIntOrNull()

    private fun decimalNumber(f: String): Double? = NUMBER.findAll(f).singleOrNull()?.value?.replace(',', '.')?.toDoubleOrNull()

    /** `180`, `180 cm`, `1,80`, `1.80 m`: centimetres within [HEIGHT]. */
    private fun height(f: String): Int? {
        val value = decimalNumber(f) ?: return null
        val cm = if (value < 3.0) (value * 100).roundToInt() else value.roundToInt()
        return cm.takeIf { it in HEIGHT }
    }

    private fun count(f: String): Int? {
        val n = integer(f) ?: WORDS.entries.firstOrNull { (word, _) -> f.split(' ').contains(word) }?.value ?: return null
        return n.takeIf { it in SlotSuggestions.MIN_SLOTS..SlotSuggestions.MAX_SLOTS }
    }

    private fun pct(f: String): Int? {
        if (f in setOf("nao", "nada", "zero", "nenhum")) return 0
        if (f in setOf("tudo", "todo", "total")) return 100
        return integer(f)?.takeIf { it in 0..100 }
    }

    /**
     * `1.370 kcal · P 103 · C 137 · G 46`: the thousands dot is dropped; P, C and G are read by their letter; a lone number
     * is the ceiling with the 30/40/30 split.
     */
    private fun targets(f: String): OnboardingTargets? {
        val plain = f.replace(Regex("""(\d)\.(\d{3})(?!\d)"""), "$1$2")
        fun macro(letter: String) = Regex("""(?:^|[^a-z])$letter\s*[:=]?\s*(\d+)\s*g?(?![a-z])""").find(plain)?.groupValues?.get(1)?.toIntOrNull()
        val p = macro("p")
        val c = macro("c")
        val g = macro("g")
        val kcal = Regex("""(\d+)\s*kcal""").find(plain)?.groupValues?.get(1)?.toIntOrNull()
            ?: INTEGER.findAll(plain).firstOrNull()?.value?.toIntOrNull()
            ?: return null
        if (kcal !in KCAL) return null
        if (p == null && c == null && g == null) {
            if (INTEGER.findAll(plain).count() != 1) return null
            val split = MacroSplit.of(kcal)
            return OnboardingTargets(kcal, split.proteinG, split.carbG, split.fatG)
        }
        if (p == null || c == null || g == null) return null
        if (listOf(p, c, g).any { it !in GRAMS }) return null
        return OnboardingTargets(kcal, p, c, g)
    }

    /** `Usar o padrão`, or [count] lines `nome HH:mm` (newline, `;` or `,` between them). */
    private fun meals(text: String, count: Int): OnboardingAnswer? {
        if (fold(text) == fold(DEFAULT_MEALS) || fold(text) == "padrao") {
            return OnboardingAnswer(DEFAULT_MEALS, meals = defaultMeals(count))
        }
        val parts = text.split('\n', ';', ',').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size != count) return null
        val meals = parts.map { part ->
            val m = Regex("""^(.*?\S)\s+(?:às\s+|as\s+)?${TIME.pattern}$""", RegexOption.IGNORE_CASE).find(part) ?: return null
            val name = m.groupValues[1].trim().take(40)
            val h = m.groupValues[2].toInt()
            val min = m.groupValues[3].ifEmpty { "0" }.toInt()
            if (name.isBlank() || h !in 0..23 || min !in 0..59) return null
            OnboardingMeal(name, h * 60 + min)
        }.sortedBy { it.minutes }
        return OnboardingAnswer(meals.joinToString("\n") { "${it.name} ${SlotSuggestions.format(it.minutes)}" }, meals = meals)
    }

    /** `60 kg até 30/04/2027`, `72,5`, `60 kg`: the date is optional; a past or invalid date is not understood. */
    private fun goal(text: String): OnboardingAnswer? {
        val f = fold(text)
        val dateMatch = DATE.find(f)
        val date = dateMatch?.let {
            val (d, m, y) = it.destructured
            val year = y.toInt().let { yy -> if (yy < 100) 2000 + yy else yy }
            runCatching { LocalDate.of(year, m.toInt(), d.toInt()) }.getOrNull() ?: return null
        }
        val rest = if (dateMatch != null) f.removeRange(dateMatch.range) else f
        val kg = decimalNumber(rest)?.takeIf { it in GOAL_WEIGHT } ?: return null
        val goal = OnboardingGoal(kg, date?.toString())
        return OnboardingAnswer(goalText(goal), goal = goal)
    }

    /** `sim`, `não`, a time (`21:30`, `21h`) or `sim, 21:30`. */
    private fun notifications(f: String): OnboardingAnswer? {
        val time = TIME.findAll(f).lastOrNull { it.value.any(Char::isDigit) }?.let {
            val h = it.groupValues[1].toInt()
            val m = it.groupValues[2].ifEmpty { "0" }.toInt()
            if (h !in 0..23 || m !in 0..59) return null
            SlotSuggestions.format(h * 60 + m)
        }
        val yes = f.startsWith("sim") || f == "s" || f.startsWith("quero") || f.startsWith("pode")
        val no = f.startsWith("nao") || f == "n"
        return when {
            no && time == null -> OnboardingAnswer("Não", notifications = false)
            time != null && !no -> OnboardingAnswer("Sim, às $time", notifications = true, closureTime = time)
            yes -> OnboardingAnswer("Sim", notifications = true, closureTime = CLOSURE_DEFAULT)
            else -> null
        }
    }
}
