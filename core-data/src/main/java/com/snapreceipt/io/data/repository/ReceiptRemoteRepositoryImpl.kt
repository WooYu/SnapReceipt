package com.snapreceipt.io.data.repository

import com.snapreceipt.io.data.network.datasource.ReceiptRemoteDataSource
import com.snapreceipt.io.data.network.model.toDto
import com.snapreceipt.io.data.network.model.toEntity
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.model.ReceiptListQueryEntity
import com.snapreceipt.io.domain.model.ReceiptSaveEntity
import com.snapreceipt.io.domain.model.ReceiptScanResultEntity
import com.snapreceipt.io.domain.model.ReceiptUpdateEntity
import com.snapreceipt.io.domain.repository.ReceiptRemoteRepository
import com.skybound.space.core.network.ApiException
import com.skybound.space.core.network.NetworkResult
import javax.inject.Inject

class ReceiptRemoteRepositoryImpl @Inject constructor(
    private val remoteDataSource: ReceiptRemoteDataSource
) : ReceiptRemoteRepository {

    override suspend fun scan(imageUrl: String): ReceiptScanResultEntity {
        return when (val result = remoteDataSource.scan(imageUrl)) {
            is NetworkResult.Success -> result.data.toEntity()
            is NetworkResult.Failure -> throw result.toApiException()
        }
    }

    override suspend fun save(request: ReceiptSaveEntity) {
        when (val result = remoteDataSource.save(request.toDto())) {
            is NetworkResult.Success -> Unit
            is NetworkResult.Failure -> throw result.toApiException()
        }
    }

    override suspend fun update(request: ReceiptUpdateEntity) {
        when (val result = remoteDataSource.update(request.toDto())) {
            is NetworkResult.Success -> Unit
            is NetworkResult.Failure -> throw result.toApiException()
        }
    }

    override suspend fun delete(receiptId: Long) {
        when (val result = remoteDataSource.delete(receiptId)) {
            is NetworkResult.Success -> Unit
            is NetworkResult.Failure -> throw result.toApiException()
        }
    }

    override suspend fun list(query: ReceiptListQueryEntity): List<ReceiptEntity> {
        return when (val result = remoteDataSource.list(query.toDto())) {
            is NetworkResult.Success -> result.data.rows.map { it.toEntity() }
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
