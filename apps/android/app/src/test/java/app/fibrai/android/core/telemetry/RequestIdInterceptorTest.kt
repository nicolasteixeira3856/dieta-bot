package app.fibrai.android.core.telemetry

import com.google.common.truth.Truth.assertThat
import app.fibrai.android.screenName
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertThrows
import org.junit.Test

class RequestIdInterceptorTest {
    private val telemetry = FakeTelemetry()
    private val ids = RequestIds()
    private var clock = 0L
    private val interceptor = RequestIdInterceptor(
        telemetry, ids, appVersion = "1.0-dev", appEnv = "dev",
        newId = { "req-1" },
        nanoTime = { clock.also { clock += 250_000_000 } },
    )
    private val original = Request.Builder().url("https://api.test/v1/chat").build()

    private fun chain(answer: (Request) -> Response): Pair<Interceptor.Chain, () -> Request?> {
        var sent: Request? = null
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns original
        every { chain.proceed(any()) } answers { sent = firstArg(); answer(firstArg()) }
        return chain to { sent }
    }

    private fun response(request: Request, code: Int) = Response.Builder()
        .request(request).protocol(Protocol.HTTP_1_1).code(code).message("x")
        .body("{}".toResponseBody()).build()

    @Test
    fun `sends request id, app version and env, and reports api_call`() {
        val (chain, sent) = chain { response(it, 200) }
        interceptor.intercept(chain)

        assertThat(sent()!!.header("X-Request-Id")).isEqualTo("req-1")
        assertThat(sent()!!.header("X-App-Version")).isEqualTo("1.0-dev")
        assertThat(sent()!!.header("X-App-Env")).isEqualTo("dev")
        assertThat(ids.last).isEqualTo("req-1")
        assertThat(telemetry.keys[TelemetryEvents.KEY_LAST_REQUEST_ID]).isEqualTo("req-1")
        assertThat(telemetry.params(TelemetryEvents.API_CALL))
            .containsExactly(mapOf("route" to "/v1/chat", "status" to "200", "latency_ms" to 250L))
        assertThat(telemetry.nonFatals).isEmpty()
    }

    @Test
    fun `http error becomes an ApiFailure non-fatal with the request id`() {
        val (chain, _) = chain { response(it, 502) }
        interceptor.intercept(chain)

        assertThat(telemetry.params(TelemetryEvents.API_CALL).single()["status"]).isEqualTo("502")
        val failure = telemetry.nonFatals.single()
        assertThat(failure).isInstanceOf(ApiFailure::class.java)
        assertThat(failure.message).isEqualTo("route=/v1/chat detail=http_502 request_id=req-1")
    }

    @Test
    fun `network error is reported and rethrown`() {
        val (chain, _) = chain { throw IOException("down") }
        assertThrows(IOException::class.java) { interceptor.intercept(chain) }

        assertThat(telemetry.params(TelemetryEvents.API_CALL).single()["status"]).isEqualTo("error")
        assertThat(telemetry.nonFatals.single().message).isEqualTo("route=/v1/chat detail=IOException request_id=req-1")
    }

    @Test
    fun `text length goes out as a bucket only`() {
        assertThat(TelemetryEvents.lengthBucket(5)).isEqualTo("<20")
        assertThat(TelemetryEvents.lengthBucket(20)).isEqualTo("20-100")
        assertThat(TelemetryEvents.lengthBucket(101)).isEqualTo(">100")
    }

    @Test
    fun `screen names follow ADR-012 ids`() {
        assertThat(screenName("app.fibrai.android.RouteCeiling")).isEqualTo("o1")
        assertThat(screenName("app.fibrai.android.RouteMacros")).isEqualTo("o4")
        assertThat(screenName("app.fibrai.android.RouteChat")).isEqualTo("chat")
        assertThat(screenName("app.fibrai.android.RouteConfig")).isEqualTo("cfg")
        assertThat(screenName("app.fibrai.android.RouteOnboarding")).isNull()
        assertThat(screenName(null)).isNull()
    }
}
