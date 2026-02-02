package com.skybound.space.core.network

import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider

/**
 * 封装“直接返回 data 或抛 ApiException”的请求方式。
 * 当前 data 层主要使用 BaseRemoteDataSource + getOrThrow；本类供需要“同步风格”调用的场景使用。
 */
class ApiService(
    private val dispatchers: CoroutineDispatchersProvider
) {
    suspend fun <T> request(call: suspend () -> BaseResponse<T>): T =
        safeApiCall(dispatchers, call).getOrThrow()

    suspend fun requestUnit(call: suspend () -> BaseEmptyResponse) {
        safeApiCallBasic(dispatchers, call).getOrThrow()
    }

    suspend fun <T> requestResult(call: suspend () -> BaseResponse<T>): NetworkResult<T> =
        safeApiCall(dispatchers, call)
}
