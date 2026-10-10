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

    /** A60 part B (ADR-044): the day or week closure text. */
    @POST("v1/close")
    suspend fun close(@Body body: CloseIn): CloseOut

    /** A71 (S41, ADR-057): the profile build of the conversational onboarding, once, on the summary's confirm. */
    @POST("v1/profile")
    suspend fun profile(@Body body: ProfileIn): ProfileOut
}
