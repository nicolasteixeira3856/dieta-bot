package com.nutri.android.data

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

@Serializable
data class HealthOut(val ok: Boolean = false, val model: String = "")

@Serializable
data class EstimateIn(
    val local_time: String,
    val janela: String,
    val text: String,
    val image_b64: String? = null,
)

@Serializable
data class ItemOut(val nome: String = "", val g: Double = 0.0, val kcal: Double = 0.0)

@Serializable
data class EstimateOut(
    val kcal: Double = 0.0,
    val p: Double = 0.0,
    val c: Double = 0.0,
    val g: Double = 0.0,
    val confianca: String = "baixa",
    val pergunta: String? = null,
    val itens: List<ItemOut> = emptyList(),
    val model: String = "",
)

@Serializable
data class OrcamentoIn(val kcal: Double, val p: Double)

@Serializable
data class FitIn(
    val mode: String,
    val text: String = "",
    val itens_disponiveis: List<String> = emptyList(),
    val orcamento: OrcamentoIn,
    val image_b64: String? = null,
)

@Serializable
data class PorcaoOut(val nome: String = "", val quantidade: String = "")

@Serializable
data class PratoOut(
    val nome: String = "",
    val porcoes: List<PorcaoOut> = emptyList(),
    val kcal: Double = 0.0,
    val p: Double = 0.0,
    val cabe: Boolean = false,
)

@Serializable
data class FitOut(
    val prato: PratoOut = PratoOut(),
    val cabe: Boolean = false,
    val pergunta: String = "",
    val opcoes: List<PratoOut> = emptyList(),
)

interface NutriApi {
    @GET("health")
    suspend fun health(): HealthOut

    @POST("v1/estimate")
    suspend fun estimate(@Body body: EstimateIn): EstimateOut

    @POST("v1/fit")
    suspend fun fit(@Body body: FitIn): FitOut
}
