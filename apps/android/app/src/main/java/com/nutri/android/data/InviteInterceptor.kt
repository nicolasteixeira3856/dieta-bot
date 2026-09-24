package com.nutri.android.data

import okhttp3.Interceptor
import okhttp3.Response

class InviteInterceptor(private val code: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("X-Invite", code)
            .build()
        return chain.proceed(request)
    }
}
