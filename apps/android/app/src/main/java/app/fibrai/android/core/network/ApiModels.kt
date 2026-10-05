package app.fibrai.android.core.network

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

@Serializable
data class HealthOut(val ok: Boolean = false, val model: String = "")

@Serializable
data class ItemOut(val name: String = "", val g: Double = 0.0, val kcal: Double = 0.0)

interface FibraiApi {
    @GET("health")
    suspend fun health(): HealthOut

    @POST("v1/chat")
    suspend fun chat(@Body body: ChatIn): ChatOut
}
