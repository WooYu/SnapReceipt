package com.snapreceipt.io.data.network.datasource

import com.snapreceipt.io.data.base.BaseRemoteDataSource
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.skybound.space.core.network.NetworkResult
import com.skybound.space.core.network.BasePagedResponse
import com.snapreceipt.io.data.network.model.category.CategoryCreateRequestDto
import com.snapreceipt.io.data.network.model.category.CategoryDeleteRequestDto
import com.snapreceipt.io.data.network.model.category.CategoryItemDto
import com.snapreceipt.io.data.network.model.category.CategoryListRequestDto
import com.snapreceipt.io.data.network.model.receipt.ReceiptDeleteRequestDto
import com.snapreceipt.io.data.network.model.receipt.ReceiptExportRequestDto
import com.snapreceipt.io.data.network.model.receipt.ReceiptItemDto
import com.snapreceipt.io.data.network.model.receipt.ReceiptSaveRequestDto
import com.snapreceipt.io.data.network.model.receipt.ReceiptScanResultDto
import com.snapreceipt.io.data.network.model.receipt.ReceiptUpdateRequestDto
import com.snapreceipt.io.data.network.model.receipt.ScanRequestDto
import com.snapreceipt.io.data.network.service.ReceiptApi
import com.snapreceipt.io.domain.model.ExportRecordEntity
import com.snapreceipt.io.domain.model.query.ExportRecordListQueryEntity
import com.snapreceipt.io.domain.model.query.ReceiptListQueryEntity

class ReceiptRemoteDataSource(
    private val api: ReceiptApi,
    dispatchers: CoroutineDispatchersProvider
) : BaseRemoteDataSource(dispatchers) {
    suspend fun scan(imageUrl: String): NetworkResult<ReceiptScanResultDto> {
        return request { api.scan(ScanRequestDto(imageUrl)) }
    }

    suspend fun save(request: ReceiptSaveRequestDto): NetworkResult<Unit> {
        return requestUnit { api.save(request) }
    }

    suspend fun update(request: ReceiptUpdateRequestDto): NetworkResult<Unit> {
        return requestUnit { api.update(request) }
    }

    suspend fun list(request: ReceiptListQueryEntity): NetworkResult<BasePagedResponse<ReceiptItemDto>> {
        return requestEnvelope({ api.list(request) }) { it }
    }

    suspend fun delete(receiptId: Long): NetworkResult<Unit> {
        return requestUnit { api.delete(ReceiptDeleteRequestDto(receiptId)) }
    }

    suspend fun export(receiptIds: List<Long>): NetworkResult<String> {
        return request { api.export(ReceiptExportRequestDto(receiptIds)) }
    }

    suspend fun exportRecords(request: ExportRecordListQueryEntity): NetworkResult<BasePagedResponse<ExportRecordEntity>> {
        return requestEnvelope({ api.exportRecords(request) }) { it }
    }

    suspend fun listCategories(): NetworkResult<List<CategoryItemDto>> {
        return request { api.listCategories(CategoryListRequestDto()) }
    }

    suspend fun addCategory(name: String): NetworkResult<Unit> {
        return requestUnit { api.addCategory(CategoryCreateRequestDto(name)) }
    }

    suspend fun deleteCategories(ids: List<Long>): NetworkResult<Unit> {
        return requestUnit { api.deleteCategory(CategoryDeleteRequestDto(ids)) }
    }
}
