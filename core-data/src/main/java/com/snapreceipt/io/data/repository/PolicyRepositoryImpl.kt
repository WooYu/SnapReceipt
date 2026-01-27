package com.snapreceipt.io.data.repository

import com.snapreceipt.io.data.network.datasource.ConfigRemoteDataSource
import com.snapreceipt.io.data.network.model.config.toEntity
import com.snapreceipt.io.domain.model.PolicyEntity
import com.snapreceipt.io.domain.repository.PolicyRepository
import com.skybound.space.core.network.ApiException
import com.skybound.space.core.network.NetworkResult
import javax.inject.Inject

class PolicyRepositoryImpl @Inject constructor(
    private val remoteDataSource: ConfigRemoteDataSource
) : PolicyRepository {
    override suspend fun fetchPolicy(): PolicyEntity {
        return when (val result = remoteDataSource.fetchPolicy()) {
            is NetworkResult.Success -> result.data.toEntity()
            is NetworkResult.Failure -> throw result.toApiException()
        }
    }

    private fun NetworkResult.Failure.toApiException(): ApiException {
        return when (val failure = error) {
            is com.skybound.space.core.network.NetworkError.Http ->
                ApiException(failure.code, failure.message, failure.throwable)
            is com.skybound.space.core.network.NetworkError.Network ->
                ApiException(-1, failure.message, failure.throwable)
            is com.skybound.space.core.network.NetworkError.Serialization ->
                ApiException(-2, failure.message, failure.throwable)
            is com.skybound.space.core.network.NetworkError.Unexpected ->
                ApiException(-3, failure.message, failure.throwable)
        }
    }
}
