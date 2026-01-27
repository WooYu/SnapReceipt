package com.snapreceipt.io.data.repository

import com.snapreceipt.io.data.network.datasource.ReceiptRemoteDataSource
import com.snapreceipt.io.data.network.model.category.toItem
import com.snapreceipt.io.data.network.model.receipt.toEntity
import com.snapreceipt.io.data.network.model.receipt.toSaveRequestDto
import com.snapreceipt.io.data.network.model.receipt.toUpdateRequestDto
import com.snapreceipt.io.domain.model.ExportRecordEntity
import com.snapreceipt.io.domain.model.query.ExportRecordListQueryEntity
import com.snapreceipt.io.domain.model.ReceiptCategory
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.model.query.ReceiptListQueryEntity
import com.snapreceipt.io.domain.repository.ReceiptRemoteRepository
import com.skybound.space.core.network.ApiException
import com.skybound.space.core.network.NetworkResult
import javax.inject.Inject

class ReceiptRemoteRepositoryImpl @Inject constructor(
    private val remoteDataSource: ReceiptRemoteDataSource
) : ReceiptRemoteRepository {

    override suspend fun scan(imageUrl: String): ReceiptEntity {
        return when (val result = remoteDataSource.scan(imageUrl)) {
            is NetworkResult.Success -> result.data.toEntity()
            is NetworkResult.Failure -> throw result.toApiException()
        }
    }

    override suspend fun save(request: ReceiptEntity) {
        when (val result = remoteDataSource.save(request.toSaveRequestDto())) {
            is NetworkResult.Success -> Unit
            is NetworkResult.Failure -> throw result.toApiException()
        }
    }

    override suspend fun update(request: ReceiptEntity) {
        when (val result = remoteDataSource.update(request.toUpdateRequestDto())) {
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
        return when (val result = remoteDataSource.list(query)) {
            is NetworkResult.Success -> result.data.rows.map { it.toEntity() }
            is NetworkResult.Failure -> throw result.toApiException()
        }
    }

    override suspend fun export(receiptIds: List<Long>): String {
        return when (val result = remoteDataSource.export(receiptIds)) {
            is NetworkResult.Success -> result.data
            is NetworkResult.Failure -> throw result.toApiException()
        }
    }

    override suspend fun listExportRecords(query: ExportRecordListQueryEntity): List<ExportRecordEntity> {
        return when (val result = remoteDataSource.exportRecords(query)) {
            is NetworkResult.Success -> result.data.rows
            is NetworkResult.Failure -> throw result.toApiException()
        }
    }

    override suspend fun listCategories(): List<ReceiptCategory.Item> {
        return when (val result = remoteDataSource.listCategories()) {
            is NetworkResult.Success -> result.data
                .sortedWith(compareBy({ it.orderNum ?: Int.MAX_VALUE }, { it.categoryId }))
                .map { it.toItem() }
            is NetworkResult.Failure -> throw result.toApiException()
        }
    }

    override suspend fun addCategory(name: String) {
        when (val result = remoteDataSource.addCategory(name)) {
            is NetworkResult.Success -> Unit
            is NetworkResult.Failure -> throw result.toApiException()
        }
    }

    override suspend fun removeCategories(ids: List<Long>) {
        when (val result = remoteDataSource.deleteCategories(ids)) {
            is NetworkResult.Success -> Unit
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
