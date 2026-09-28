package com.nutri.android.core.telemetry

import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

/** Id of the last API call, for the Chat's non-fatal (one send at a time, spec). */
@Singleton
class RequestIds @Inject constructor() {
    @Volatile
    var last: String? = null
        internal set
}

/**
 * Every API call carries X-Request-Id (+ app version/env), matched by the dev server log (ADR-015).
 * Measures latency and reports api_call; network/HTTP errors become ApiFailure non-fatals.
 */
class RequestIdInterceptor(
    private val telemetry: Telemetry,
    private val ids: RequestIds,
    private val appVersion: String,
    private val appEnv: String,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val nanoTime: () -> Long = System::nanoTime,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val id = newId()
        ids.last = id
        telemetry.setKey(TelemetryEvents.KEY_LAST_REQUEST_ID, id)
        val original = chain.request()
        val route = original.url.encodedPath
        val request = original.newBuilder()
            .header(HEADER_REQUEST_ID, id)
            .header(HEADER_APP_VERSION, appVersion)
            .header(HEADER_APP_ENV, appEnv)
            .build()
        val started = nanoTime()
        val response = try {
            chain.proceed(request)
        } catch (e: IOException) {
            report(route, "error", started)
            telemetry.nonFatal(ApiFailure(route, e.javaClass.simpleName, id))
            throw e
        }
        report(route, response.code.toString(), started)
        if (!response.isSuccessful) telemetry.nonFatal(ApiFailure(route, "http_${response.code}", id))
        return response
    }

    private fun report(route: String, status: String, started: Long) {
        val latencyMs = (nanoTime() - started) / 1_000_000
        telemetry.breadcrumb("api $route $status ${latencyMs}ms")
        telemetry.event(
            TelemetryEvents.API_CALL,
            mapOf("route" to route, "status" to status, "latency_ms" to latencyMs),
        )
    }

    companion object {
        const val HEADER_REQUEST_ID = "X-Request-Id"
        const val HEADER_APP_VERSION = "X-App-Version"
        const val HEADER_APP_ENV = "X-App-Env"
    }
}
