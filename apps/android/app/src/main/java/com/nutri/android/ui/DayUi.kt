package com.nutri.android.ui

import com.nutri.android.data.DishOut
import com.nutri.android.data.EstimateOut
import com.nutri.android.data.FitOut
import com.nutri.android.domain.Chip

enum class Stage { SPLASH, O1, O2, HOME, T2 }
enum class SheetKind { T1, T3 }

data class DayUi(
    val ready: Boolean = true,
    val stage: Stage = Stage.SPLASH,
    val sheet: SheetKind? = null,
    val capture: Boolean = false,
    val ceilingMode: String = "same",
    val sameField: String = "2000",
    val weekdayField: String = "2000",
    val weekendField: String = "2300",
    val dayFields: List<String> = List(7) { "2000" },
    val eat: String = "zero",
    val pct: String = "50",
    val text: String = "",
    val photoB64: String? = null,
    val workout: String = "",
    val eaten: Int = 0,
    val protein: Int = 0,
    val effectiveCeiling: Int = 2000,
    val windowBudget: Int = 2000,
    val window: String = "dinner",
    val shortDate: String = "",
    val chips: List<Chip> = emptyList(),
    val appDay: Int = 1,
    val estimate: EstimateOut? = null,
    val fitText: String = "",
    val fit: FitOut? = null,
    val fitMode: String = "want",
    val loading: Boolean = false,
    val remaining: Int = 2000,
    val remainingLabel: String = "",
    val nextTitle: String = "",
    val nextDetail: String = "",
    val bar: Float = 0f,
    val chipLabel: String? = null,
    val chipNote: String? = null,
    val t2Title: String = "confirmação",
    val t2Name: String = "",
    val t2Range: String? = null,
    val t2Question: String? = null,
    val t2Answer: Int = 0,
    val t3Headline: String? = null,
    val t3Line: String? = null,
    val t3Sub: String? = null,
    val t3Cta: String = "Encaixar",
    val weekend: Boolean = false,
)

private fun homeDinner(chip: Boolean = false): DayUi {
    val base = DayUi(
        stage = Stage.HOME,
        sheet = null,
        appDay = if (chip) 2 else 1,
        chips = emptyList(),
        shortDate = "quinta, 24 set",
        remaining = 1840,
        remainingLabel = "cabe na janta",
        nextTitle = "Janta · reserva 520",
        nextDetail = "almoço já 640 · P 48 g",
        bar = 0.38f,
        eaten = 640,
        protein = 48,
        effectiveCeiling = 2000,
        windowBudget = 1840,
        window = "dinner",
        capture = true,
        ready = true,
    )
    return if (chip) {
        base.copy(
            chipLabel = "iogurte manhã",
            chipNote = "desnatado? perguntado 1x",
        )
    } else {
        base
    }
}

fun captureState(id: String): DayUi = when (id) {
    "splash" -> DayUi(stage = Stage.SPLASH, capture = true)
    "o1" -> DayUi(stage = Stage.O1, capture = true, ceilingMode = "same", sameField = "2000")
    "o2" -> DayUi(stage = Stage.O2, capture = true, eat = "zero")
    "t0" -> homeDinner(false)
    "t0d2" -> homeDinner(true)
    "t0fds" -> DayUi(
        stage = Stage.HOME,
        capture = true,
        appDay = 1,
        shortDate = "sábado, 26 set",
        remaining = 2000,
        remainingLabel = "fds · almoço + janta + fecha",
        nextTitle = "Almoço",
        nextDetail = "café não cobrado",
        bar = 0f,
        window = "lunch",
        weekend = true,
        windowBudget = 2000,
        effectiveCeiling = 2000,
    )
    "t1" -> homeDinner().copy(
        sheet = SheetKind.T1,
        text = "2 pães, ovo, café com leite",
        loading = false,
    )
    "t1load" -> homeDinner().copy(
        sheet = SheetKind.T1,
        text = "2 pães, ovo, café com leite",
        loading = true,
    )
    "t2" -> DayUi(
        stage = Stage.T2,
        capture = true,
        t2Title = "confirmação",
        t2Name = "2 pães + ovo + café",
        estimate = EstimateOut(kcal = 420.0, p = 22.0, confidence = "high"),
        windowBudget = 1840,
        effectiveCeiling = 2000,
        window = "dinner",
    )
    "t2q" -> DayUi(
        stage = Stage.T2,
        capture = true,
        t2Title = "confiança baixa · 1 pergunta",
        t2Name = "2 pães + ovo + café",
        t2Range = "~380–480 kcal",
        t2Question = "Pão era francês?",
        t2Answer = 0,
        estimate = EstimateOut(kcal = 0.0, p = 0.0, confidence = "low", question = "Pão era francês?"),
        windowBudget = 1840,
    )
    "t3quero" -> homeDinner().copy(
        sheet = SheetKind.T3,
        fitMode = "want",
        fitText = "lasanha",
        t3Headline = "Inteira não cabe.",
        t3Line = "1/2 lasanha + salada · 480 kcal · 28 g P",
        t3Sub = "sobra 40 pra um café",
        t3Cta = "Encaixar 480",
        fit = FitOut(
            fits = false,
            dish = DishOut(name = "1/2 lasanha + salada", kcal = 480.0, p = 28.0, fits = false),
        ),
    )
    "t3tenho" -> homeDinner().copy(
        sheet = SheetKind.T3,
        fitMode = "have",
        fitText = "ovo, arroz, alface",
        t3Headline = "Cabe.",
        t3Line = "omelete 3 ovos + arroz 180 g + folha · 520 kcal · 32 g P",
        t3Cta = "Encaixar 520",
        fit = FitOut(
            fits = true,
            dish = DishOut(name = "omelete 3 ovos + arroz 180 g + folha", kcal = 520.0, p = 32.0, fits = true),
        ),
    )
    "t3ideia" -> homeDinner().copy(
        sheet = SheetKind.T3,
        fitMode = "idea",
        t3Cta = "Já comi",
        fit = FitOut(
            fits = true,
            options = listOf(
                DishOut(name = "omelete 3 ovos", kcal = 420.0, p = 30.0, fits = true),
                DishOut(name = "sopa + pão", kcal = 380.0, p = 18.0, fits = true),
            ),
        ),
    )
    else -> DayUi(stage = Stage.O1, capture = true)
}
