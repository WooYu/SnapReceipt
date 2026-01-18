package com.skybound.space.core.network

import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider

class ApiService(
    private val dispatchers: CoroutineDispatchersProvider
) {
    suspend fun <T> request(call: suspend () -> BaseResponse<T>): T {
        return when (val result = safeApiCall(dispatchers, call)) {
            is NetworkResult.Success -> result.data
            is NetworkResult.Failure -> throw result.toApiException()
        }
    }

    suspend fun requestUnit(call: suspend () -> BaseEmptyResponse) {
        when (val result = safeApiCallBasic(dispatchers, call)) {
            is NetworkResult.Success -> Unit
            is NetworkResult.Failure -> throw result.toApiException()
        }
    }

    suspend fun <T> requestResult(call: suspend () -> BaseResponse<T>): NetworkResult<T> =
        safeApiCall(dispatchers, call)

    private fun NetworkResult.Failure.toApiException(): ApiException {
        return when (val error = error) {
            is NetworkError.Http -> ApiException(error.code, error.message, error.throwable)
            is NetworkError.Network -> ApiException(-1, error.message, error.throwable)
            is NetworkError.Serialization -> ApiException(-2, error.message, error.throwable)
            is NetworkError.Unexpected -> ApiException(-3, error.message, error.throwable)
        }
    }
}
