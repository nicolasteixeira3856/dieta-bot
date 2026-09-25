package com.nutri.android.domain

data class DishOffer(val name: String, val kcal: Double)

/** Never offer a dish that blows the window ceiling. */
fun dishesThatFit(dishes: List<DishOffer>, budgetKcal: Double): List<DishOffer> {
    return dishes.filter { it.name.isNotBlank() && it.kcal <= budgetKcal }
}
