package com.snapreceipt.io.data.repository

import com.snapreceipt.io.data.network.datasource.ReceiptRemoteDataSource
import com.snapreceipt.io.data.network.model.category.toItem
import com.snapreceipt.io.data.network.model.receipt.toEntity
import com.snapreceipt.io.data.network.model.receipt.toSaveRequestPayload
import com.snapreceipt.io.data.network.model.receipt.toUpdateRequestPayload
import com.snapreceipt.io.domain.model.ExportRecordEntity
import com.snapreceipt.io.domain.model.query.ExportRecordListQueryEntity
import com.snapreceipt.io.domain.model.ReceiptCategory
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.model.query.ReceiptListQueryEntity
import com.snapreceipt.io.domain.repository.ReceiptRemoteRepository
import com.skybound.space.core.network.getOrThrow
import javax.inject.Inject

class ReceiptRemoteRepositoryImpl @Inject constructor(
    private val remoteDataSource: ReceiptRemoteDataSource
) : ReceiptRemoteRepository {

    override suspend fun scan(imageUrl: String): ReceiptEntity =
        remoteDataSource.scan(imageUrl).getOrThrow().toEntity()

    override suspend fun save(request: ReceiptEntity) {
        remoteDataSource.save(request.toSaveRequestPayload()).getOrThrow()
    }

    override suspend fun update(request: ReceiptEntity) {
        remoteDataSource.update(request.toUpdateRequestPayload()).getOrThrow()
    }

    override suspend fun delete(receiptId: Long) {
        remoteDataSource.delete(receiptId).getOrThrow()
    }

    override suspend fun list(query: ReceiptListQueryEntity): List<ReceiptEntity> =
        remoteDataSource.list(query).getOrThrow().rows

    override suspend fun export(receiptIds: List<Long>): String =
        remoteDataSource.export(receiptIds).getOrThrow()

    override suspend fun listExportRecords(query: ExportRecordListQueryEntity): List<ExportRecordEntity> =
        remoteDataSource.exportRecords(query).getOrThrow().rows

    override suspend fun listCategories(): List<ReceiptCategory.Item> =
        remoteDataSource.listCategories().getOrThrow()
            .sortedWith(compareBy({ it.orderNum ?: Int.MAX_VALUE }, { it.categoryId }))
            .map { it.toItem() }

    override suspend fun addCategory(name: String) {
        remoteDataSource.addCategory(name).getOrThrow()
    }

    override suspend fun removeCategories(ids: List<Long>) {
        remoteDataSource.deleteCategories(ids).getOrThrow()
    }
}
