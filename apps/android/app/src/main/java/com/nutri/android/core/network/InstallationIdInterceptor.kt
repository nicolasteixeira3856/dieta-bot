package com.nutri.android.core.network

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Network interceptor: X-Client-Instance-Id only on the configured API origin, under its v1/ routes
 * (estimate, fit, chat, compaction). It runs on every hop, so a redirect to another origin goes
 * without it (OkHttp rebuilds redirects from the request before network interceptors). Any other
 * host, or an id that is not canonical, gets no header. Old servers ignore the additive header.
 */
class InstallationIdInterceptor(
    private val api: HttpUrl,
    private val installation: InstallationId,
) : Interceptor {
    private val scope = api.encodedPath.let { if (it.endsWith("/")) it else "$it/" } + "v1/"

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        val inScope = url.scheme == api.scheme && url.host == api.host && url.port == api.port &&
            url.encodedPath.startsWith(scope)
        val id = if (inScope) runCatching { installation.get() }.getOrNull()?.takeIf(FileInstallationId::isCanonical) else null
        val builder = request.newBuilder().removeHeader(HEADER)
        if (id != null) builder.header(HEADER, id)
        return chain.proceed(builder.build())
    }

    companion object {
        const val HEADER = "X-Client-Instance-Id"
    }
}
