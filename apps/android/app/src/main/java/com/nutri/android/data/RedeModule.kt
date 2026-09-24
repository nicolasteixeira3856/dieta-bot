package com.nutri.android.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.nutri.android.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object RedeModule {
    @Provides
    @Singleton
    fun client(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(InviteInterceptor(BuildConfig.INVITE_CODE))
        .build()

    @Provides
    @Singleton
    fun api(client: OkHttpClient): NutriApi {
        val json = Json { ignoreUnknownKeys = true }
        val base = BuildConfig.API_PUBLIC_URL.let { if (it.endsWith("/")) it else "$it/" }
        return Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(NutriApi::class.java)
    }

    @Provides
    @Singleton
    fun gate(api: NutriApi): EstimateGate = EstimateGate(api)
}
