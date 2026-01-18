package com.skybound.space.core.network

class ApiException(
    val code: Int,
    override val message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
