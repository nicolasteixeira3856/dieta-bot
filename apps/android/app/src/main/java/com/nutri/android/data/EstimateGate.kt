package com.nutri.android.data

import com.nutri.android.domain.DishOffer
import com.nutri.android.domain.dishesThatFit

const val FALLBACK_QUESTION = "descreve em 1 linha"

class EstimateGate(private val api: NutriApi) {
    suspend fun estimate(body: EstimateIn): EstimateOut {
        return try {
            val out = api.estimate(body)
            if (out.confidence == "high") out.copy(question = null) else {
                out.copy(question = out.question?.takeIf { it.isNotBlank() } ?: FALLBACK_QUESTION)
            }
        } catch (_: Exception) {
            EstimateOut(confidence = "low", question = FALLBACK_QUESTION)
        }
    }

    suspend fun fit(body: FitIn): FitOut {
        return try {
            val out = api.fit(body)
            val namesThatFit = dishesThatFit(
                dishes = (out.options + out.dish).map { DishOffer(it.name, it.kcal) },
                budgetKcal = body.budget.kcal,
            ).map { it.name }.toSet()
            val options = out.options.filter { it.name in namesThatFit }
            val dish = if (out.dish.name in namesThatFit) out.dish else DishOut()
            out.copy(
                dish = dish,
                options = options,
                fits = dish.name.isNotBlank() || options.isNotEmpty(),
                question = out.question.ifBlank { FALLBACK_QUESTION },
            )
        } catch (_: Exception) {
            FitOut(fits = false, question = FALLBACK_QUESTION)
        }
    }
}
