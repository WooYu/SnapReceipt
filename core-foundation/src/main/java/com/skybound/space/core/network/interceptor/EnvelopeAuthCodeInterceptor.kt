package com.skybound.space.core.network.interceptor

import com.google.gson.JsonParser
import com.skybound.space.core.network.ApiException
import com.skybound.space.core.util.LogHelper
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.nio.charset.StandardCharsets

/**
 * 把业务信封中的鉴权 code 抬升为 HTTP 状态码。
 *
 * 服务端在 token 失效时使用 HTTP 200 + `code=401/403` 的约定，
 * 如果不抬升状态码，[okhttp3.Authenticator] 永远不会触发，
 * 业务层会把"access token expired"当成普通错误 toast 出来。
 *
 * 必须作为 network interceptor 注册，才能早于 RetryAndFollowUpInterceptor
 * 让 Authenticator 看到重写后的 401 并启动静默刷新。
 *
 * 本拦截器只做"code 抬升",不直接驱动会话失效:
 * - 401 交给 [okhttp3.Authenticator]([com.snapreceipt.io.data.network.auth.TokenRefreshAuthenticator]) 做静默刷新;
 * - 403 由 application 层的 [AuthFailureInterceptor] 统一通知 SessionManager,避免重复触发。
 */
class EnvelopeAuthCodeInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code != HTTP_OK) return response
        val body = response.body
        val contentType = body.contentType()
        if (contentType == null || !contentType.isJsonLike()) return response

        val source = body.source()
        source.request(Long.MAX_VALUE)
        val bufferCopy = source.buffer.clone()
        if (bufferCopy.size == 0L) return response
        val charset = contentType.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
        val rawJson = bufferCopy.readString(charset)

        val (envelopeCode, envelopeMsg) = peekAuthInfo(rawJson) ?: return response
        if (envelopeCode != ApiException.CODE_UNAUTHORIZED && envelopeCode != ApiException.CODE_FORBIDDEN) return response

        LogHelper.i(TAG, "Lifting envelope code=$envelopeCode to HTTP status")

        return response.newBuilder()
            .code(envelopeCode)
            .message(envelopeMsg.ifBlank { response.message })
            .body(rawJson.toResponseBody(contentType))
            .build()
    }

    private fun peekAuthInfo(raw: String): Pair<Int, String>? = runCatching {
        val obj = JsonParser.parseString(raw).takeIf { it.isJsonObject }?.asJsonObject
            ?: return@runCatching null
        val code = obj.get(FIELD_CODE)?.asInt ?: return@runCatching null
        val msg = obj.get(FIELD_MSG)?.asString?.takeIf { it.isNotBlank() }
            ?: obj.get(FIELD_MESSAGE)?.asString ?: ""
        Pair(code, msg)
    }.getOrNull()

    private fun MediaType.isJsonLike(): Boolean {
        if (subtype.equals("json", ignoreCase = true)) return true
        return subtype.endsWith("+json", ignoreCase = true)
    }

    private companion object {
        const val TAG = "Auth"
        const val HTTP_OK = 200
        const val FIELD_CODE = "code"
        const val FIELD_MSG = "msg"
        const val FIELD_MESSAGE = "message"
    }
}
