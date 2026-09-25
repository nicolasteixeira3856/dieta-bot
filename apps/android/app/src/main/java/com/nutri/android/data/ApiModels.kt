package com.nutri.android.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

@Serializable
data class HealthOut(val ok: Boolean = false, val model: String = "")

@Serializable
data class EstimateIn(
    val local_time: String,
    val window: String,
    val text: String,
    val image_b64: String? = null,
)

@Serializable
data class ItemOut(val name: String = "", val g: Double = 0.0, val kcal: Double = 0.0)

@Serializable
data class EstimateOut(
    val kcal: Double = 0.0,
    val p: Double = 0.0,
    val c: Double = 0.0,
    val g: Double = 0.0,
    val confidence: String = "low",
    val question: String? = null,
    val items: List<ItemOut> = emptyList(),
    val model: String = "",
)

@Serializable
data class BudgetIn(val kcal: Double, val p: Double)

@Serializable
data class FitIn(
    val mode: String,
    val text: String = "",
    @SerialName("available_items") val availableItems: List<String> = emptyList(),
    val budget: BudgetIn,
    val image_b64: String? = null,
)

@Serializable
data class PortionOut(val name: String = "", val quantity: String = "")

@Serializable
data class DishOut(
    val name: String = "",
    val portions: List<PortionOut> = emptyList(),
    val kcal: Double = 0.0,
    val p: Double = 0.0,
    val fits: Boolean = false,
)

@Serializable
data class FitOut(
    val dish: DishOut = DishOut(),
    val fits: Boolean = false,
    val question: String = "",
    val options: List<DishOut> = emptyList(),
)

interface NutriApi {
    @GET("health")
    suspend fun health(): HealthOut

    @POST("v1/estimate")
    suspend fun estimate(@Body body: EstimateIn): EstimateOut

    @POST("v1/fit")
    suspend fun fit(@Body body: FitIn): FitOut
}
