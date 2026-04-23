package com.skybound.space.core.network

class ApiException(
    val code: Int,
    override val message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) {
    companion object {
        const val CODE_UNAUTHORIZED = 401
        const val CODE_FORBIDDEN = 403
    }
}
