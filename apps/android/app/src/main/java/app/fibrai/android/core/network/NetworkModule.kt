package app.fibrai.android.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import app.fibrai.android.BuildConfig
import app.fibrai.android.core.telemetry.RequestIdInterceptor
import app.fibrai.android.core.telemetry.RequestIds
import app.fibrai.android.core.telemetry.Telemetry
import app.fibrai.android.feature.chat.ChatService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun client(telemetry: Telemetry, ids: RequestIds, installation: InstallationId): OkHttpClient = OkHttpClient.Builder()
        // Server timeout is 60 s (S1); the client waits as long, then shows "não deu".
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(65, TimeUnit.SECONDS)
        .addInterceptor(InviteInterceptor(BuildConfig.INVITE_CODE))
        // A11: X-Request-Id on every call, matched by the dev server log (ADR-015).
        .addInterceptor(RequestIdInterceptor(telemetry, ids, BuildConfig.VERSION_NAME, BuildConfig.ENV))
        // CP4: per hop, so a cross-origin redirect never carries the installation id.
        .addNetworkInterceptor(InstallationIdInterceptor(apiBase().toHttpUrl(), installation))
        .build()

    @Provides
    @Singleton
    fun api(client: OkHttpClient): FibraiApi {
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl(apiBase())
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FibraiApi::class.java)
    }

    private fun apiBase(): String = BuildConfig.API_PUBLIC_URL.let { if (it.endsWith("/")) it else "$it/" }

    @Provides
    @Singleton
    fun chat(api: FibraiApi): ChatService = ChatService { api.chat(it) }

    @Provides
    @Singleton
    fun close(api: FibraiApi): app.fibrai.android.core.closure.CloseService = app.fibrai.android.core.closure.CloseService { api.close(it) }
}
