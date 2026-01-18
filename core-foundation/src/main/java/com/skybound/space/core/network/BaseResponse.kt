package com.skybound.space.core.network

import com.google.gson.annotations.SerializedName

/**
 * 通用响应信封，用于统一处理 code/msg。
 */
interface BaseResponseEnvelope {
    val code: Int
    val message: String
    fun isSuccess(): Boolean
}

/**
 * 标准 API 响应包装。
 */
data class BaseResponse<T>(
    @SerializedName("code")
    override val code: Int,
    @SerializedName(value = "msg", alternate = ["message"])
    override val message: String,
    @SerializedName("data")
    val data: T? = null
) : BaseResponseEnvelope {
    override fun isSuccess(): Boolean = code == SUCCESS_CODE

    companion object {
        const val SUCCESS_CODE = 200
    }
}

/**
 * 无 data 的响应。
 */
data class BaseEmptyResponse(
    @SerializedName("code")
    override val code: Int,
    @SerializedName(value = "msg", alternate = ["message"])
    override val message: String
) : BaseResponseEnvelope {
    override fun isSuccess(): Boolean = code == BaseResponse.SUCCESS_CODE
}

/**
 * 列表型响应（rows + total）。
 */
data class BasePagedResponse<T>(
    @SerializedName("code")
    override val code: Int,
    @SerializedName(value = "msg", alternate = ["message"])
    override val message: String,
    @SerializedName("total")
    val total: Int = 0,
    @SerializedName("rows")
    val rows: List<T> = emptyList()
) : BaseResponseEnvelope {
    override fun isSuccess(): Boolean = code == BaseResponse.SUCCESS_CODE
}
