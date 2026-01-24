package com.snapreceipt.io.data.base

import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.skybound.space.core.network.BaseEmptyResponse
import com.skybound.space.core.network.BaseResponse
import com.skybound.space.core.network.NetworkResult
import com.skybound.space.core.network.safeApiCall
import com.skybound.space.core.network.safeApiCallBasic
import com.skybound.space.core.network.safeApiCallEnvelope
import com.skybound.space.core.network.safeApiCallNoEnvelope
import com.skybound.space.core.network.BaseResponseEnvelope

abstract class BaseRemoteDataSource(
    protected val dispatchers: CoroutineDispatchersProvider
) {
    protected suspend fun <T> request(call: suspend () -> BaseResponse<T>): NetworkResult<T> {
        return safeApiCall(dispatchers, call)
    }

    protected suspend fun requestUnit(call: suspend () -> BaseEmptyResponse): NetworkResult<Unit> {
        return safeApiCallBasic(dispatchers, call)
    }

    protected suspend fun requestNoEnvelope(call: suspend () -> Unit): NetworkResult<Unit> {
        return safeApiCallNoEnvelope(dispatchers, call)
    }

    protected suspend fun <E : BaseResponseEnvelope, T> requestEnvelope(
        call: suspend () -> E,
        extractor: (E) -> T?
    ): NetworkResult<T> {
        return safeApiCallEnvelope(dispatchers, call, extractor)
    }
}
