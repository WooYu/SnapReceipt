package com.skybound.space.core.network

import com.google.gson.annotations.SerializedName

/**
 * 通用响应信封，用于统一处理 code/msg。
 */
interface ApiEnvelope {
    val code: Int
    val message: String
    fun isSuccess(): Boolean
}

/**
 * 标准 API 响应包装。
 */
data class ApiResponse<T>(
    @SerializedName("code")
    override val code: Int,
    @SerializedName(value = "msg", alternate = ["message"])
    override val message: String,
    @SerializedName("data")
    val data: T? = null
) : ApiEnvelope {
    override fun isSuccess(): Boolean = code == SUCCESS_CODE

    companion object {
        const val SUCCESS_CODE = 200
    }
}

/**
 * 无 data 的响应。
 */
data class BasicResponse(
    @SerializedName("code")
    override val code: Int,
    @SerializedName(value = "msg", alternate = ["message"])
    override val message: String
) : ApiEnvelope {
    override fun isSuccess(): Boolean = code == ApiResponse.SUCCESS_CODE
}

/**
 * 列表型响应（rows + total）。
 */
data class PagedResponse<T>(
    @SerializedName("code")
    override val code: Int,
    @SerializedName(value = "msg", alternate = ["message"])
    override val message: String,
    @SerializedName("total")
    val total: Int = 0,
    @SerializedName("rows")
    val rows: List<T> = emptyList()
) : ApiEnvelope {
    override fun isSuccess(): Boolean = code == ApiResponse.SUCCESS_CODE
}
