package com.skybound.space.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

class ExportTimeoutInterceptor(
    private val exportPath: String = "/api/receipt/export",
    private val timeoutSec: Long
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        if (!path.endsWith(exportPath) || timeoutSec <= 0L) {
            return chain.proceed(request)
        }
        val adjusted = chain
            .withReadTimeout(timeoutSec.toInt(), TimeUnit.SECONDS)
            .withWriteTimeout(timeoutSec.toInt(), TimeUnit.SECONDS)
        return adjusted.proceed(request)
    }
}
