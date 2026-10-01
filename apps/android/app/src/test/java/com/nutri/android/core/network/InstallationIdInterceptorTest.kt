package com.nutri.android.core.network

import com.google.common.truth.Truth.assertThat
import com.nutri.android.core.telemetry.FakeTelemetry
import com.nutri.android.core.telemetry.RequestIdInterceptor
import com.nutri.android.core.telemetry.RequestIds
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.util.Collections
import kotlin.concurrent.thread
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.After
import org.junit.Test

class InstallationIdInterceptorTest {
    private val id = "0f8e2c4a-1b3d-4e5f-9a7b-6c5d4e3f2a1b"
    private val installation = object : InstallationId {
        override fun get() = id
    }

    /** Minimal HTTP/1.1 server: records request lines and headers, answers by path. */
    private class Server(private val answer: (path: String) -> String) : AutoCloseable {
        private val socket = ServerSocket(0)
        val port get() = socket.localPort
        val seen: MutableList<Pair<String, Map<String, String>>> = Collections.synchronizedList(mutableListOf())

        init {
            thread(isDaemon = true) {
                while (!socket.isClosed) {
                    val client = runCatching { socket.accept() }.getOrNull() ?: break
                    client.use { c ->
                        val reader = BufferedReader(InputStreamReader(c.getInputStream()))
                        val path = reader.readLine()?.split(" ")?.getOrNull(1) ?: return@use
                        val headers = mutableMapOf<String, String>()
                        var length = 0
                        while (true) {
                            val line = reader.readLine() ?: break
                            if (line.isEmpty()) break
                            val (k, v) = line.split(":", limit = 2).map { it.trim() }
                            headers[k.lowercase()] = v
                            if (k.equals("Content-Length", true)) length = v.toInt()
                        }
                        repeat(length) { reader.read() }
                        seen += path to headers
                        c.getOutputStream().write(answer(path).toByteArray())
                        c.getOutputStream().flush()
                    }
                }
            }
        }

        override fun close() = socket.close()
    }

    private fun ok() = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 2\r\nConnection: close\r\n\r\n{}"
    private fun redirect(to: String) = "HTTP/1.1 307 Temporary Redirect\r\nLocation: $to\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"

    private val other = Server { ok() }
    private val api = Server { path ->
        when (path) {
            "/v1/away" -> redirect("http://127.0.0.1:${other.port}/v1/chat")
            "/v1/moved" -> redirect("/v1/chat")
            else -> ok()
        }
    }
    private val telemetry = FakeTelemetry()

    // Same wiring as NetworkModule.
    private val client = OkHttpClient.Builder()
        .addInterceptor(InviteInterceptor("convite"))
        .addInterceptor(RequestIdInterceptor(telemetry, RequestIds(), "0.0.0-test", "dev"))
        .addNetworkInterceptor(InstallationIdInterceptor("http://127.0.0.1:${api.port}/".toHttpUrl(), installation))
        .build()

    @After
    fun close() {
        api.close()
        other.close()
    }

    private fun post(url: String) = client.newCall(
        Request.Builder().url(url).post("{}".toRequestBody("application/json".toMediaType())).build(),
    ).execute().use { it.code }

    private fun get(url: String) = client.newCall(Request.Builder().url(url).build()).execute().use { it.code }

    private fun Server.headersFor(path: String) = seen.single { it.first == path }.second

    @Test
    fun `estimate, fit and chat (compaction included) carry the id with invite and request id`() {
        listOf("/v1/estimate", "/v1/fit", "/v1/chat").forEach { assertThat(post("http://127.0.0.1:${api.port}$it")).isEqualTo(200) }

        listOf("/v1/estimate", "/v1/fit", "/v1/chat").forEach {
            val headers = api.headersFor(it)
            assertThat(headers["x-client-instance-id"]).isEqualTo(id)
            assertThat(headers["x-invite"]).isEqualTo("convite")
            assertThat(headers["x-request-id"]).isNotEmpty()
        }
    }

    @Test
    fun `health and other origins never get the id`() {
        get("http://127.0.0.1:${api.port}/health")
        post("http://127.0.0.1:${other.port}/v1/chat")
        post("http://localhost:${api.port}/v1/chat")

        assertThat(api.headersFor("/health")).doesNotContainKey("x-client-instance-id")
        other.seen.forEach { assertThat(it.second).doesNotContainKey("x-client-instance-id") }
        api.seen.filter { it.first == "/v1/chat" }.forEach { assertThat(it.second).doesNotContainKey("x-client-instance-id") }
    }

    @Test
    fun `cross-origin redirect drops the id, same-origin redirect keeps it`() {
        assertThat(post("http://127.0.0.1:${api.port}/v1/away")).isEqualTo(200)
        assertThat(post("http://127.0.0.1:${api.port}/v1/moved")).isEqualTo(200)

        assertThat(api.headersFor("/v1/away")["x-client-instance-id"]).isEqualTo(id)
        assertThat(other.headersFor("/v1/chat")).doesNotContainKey("x-client-instance-id")
        assertThat(api.headersFor("/v1/moved")["x-client-instance-id"]).isEqualTo(id)
        assertThat(api.headersFor("/v1/chat")["x-client-instance-id"]).isEqualTo(id)
    }

    @Test
    fun `a caller-set header is replaced, never forwarded`() {
        client.newCall(
            Request.Builder().url("http://127.0.0.1:${other.port}/x").header(InstallationIdInterceptor.HEADER, id).build(),
        ).execute().close()

        assertThat(other.headersFor("/x")).doesNotContainKey("x-client-instance-id")
    }

    @Test
    fun `storage failure or non-canonical id sends the call without the header`() {
        listOf(
            object : InstallationId { override fun get(): String = error("disk") },
            object : InstallationId { override fun get() = "device-serial-123" },
        ).forEach { broken ->
            val chain = io.mockk.mockk<okhttp3.Interceptor.Chain>()
            var sent: Request? = null
            io.mockk.every { chain.request() } returns Request.Builder().url("http://127.0.0.1:${api.port}/v1/chat").build()
            io.mockk.every { chain.proceed(any()) } answers { sent = firstArg(); io.mockk.mockk(relaxed = true) }

            InstallationIdInterceptor("http://127.0.0.1:${api.port}/".toHttpUrl(), broken).intercept(chain)

            assertThat(sent!!.header(InstallationIdInterceptor.HEADER)).isNull()
        }
    }

    @Test
    fun `API base with a path scopes to its own v1 routes`() {
        val chain = io.mockk.mockk<okhttp3.Interceptor.Chain>()
        val sent = mutableListOf<Request>()
        io.mockk.every { chain.proceed(any()) } answers { sent += firstArg<Request>(); io.mockk.mockk(relaxed = true) }
        val interceptor = InstallationIdInterceptor("https://api.test/nutri/".toHttpUrl(), installation)
        listOf("https://api.test/nutri/v1/fit", "https://api.test/v1/fit", "http://api.test/nutri/v1/fit").forEach {
            io.mockk.every { chain.request() } returns Request.Builder().url(it).build()
            interceptor.intercept(chain)
        }

        assertThat(sent.map { it.header(InstallationIdInterceptor.HEADER) }).containsExactly(id, null, null).inOrder()
    }

    @Test
    fun `the id never reaches telemetry`() {
        post("http://127.0.0.1:${api.port}/v1/chat")
        post("http://127.0.0.1:${api.port}/v1/away")

        val everything = telemetry.events.toString() + telemetry.breadcrumbs + telemetry.keys + telemetry.nonFatals.map { it.message }
        assertThat(everything).doesNotContain(id)
        assertThat(telemetry.events).isNotEmpty()
    }

    @Test
    fun `legacy server that ignores the header still answers`() {
        // The server stub knows nothing about the header: the call succeeds unchanged.
        assertThat(post("http://127.0.0.1:${api.port}/v1/estimate")).isEqualTo(200)
    }
}
