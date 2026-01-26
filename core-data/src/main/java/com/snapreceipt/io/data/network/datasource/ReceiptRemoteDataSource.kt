package com.snapreceipt.io.data.network.datasource

import com.snapreceipt.io.data.base.BaseRemoteDataSource
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.skybound.space.core.network.NetworkResult
import com.skybound.space.core.network.BasePagedResponse
import com.snapreceipt.io.data.network.model.ReceiptExportRequestDto
import com.snapreceipt.io.data.network.model.ReceiptItemDto
import com.snapreceipt.io.data.network.model.ReceiptDeleteRequestDto
import com.snapreceipt.io.data.network.model.ScanRequestDto
import com.snapreceipt.io.data.network.model.CategoryCreateRequestDto
import com.snapreceipt.io.data.network.model.CategoryDeleteRequestDto
import com.snapreceipt.io.data.network.model.CategoryItemDto
import com.snapreceipt.io.data.network.model.CategoryListRequestDto
import com.snapreceipt.io.data.network.service.ReceiptApi
import com.snapreceipt.io.domain.model.ExportRecordEntity
import com.snapreceipt.io.domain.model.ExportRecordListQueryEntity
import com.snapreceipt.io.domain.model.ReceiptListQueryEntity
import com.snapreceipt.io.domain.model.ReceiptSaveEntity
import com.snapreceipt.io.domain.model.ReceiptScanResultEntity
import com.snapreceipt.io.domain.model.ReceiptUpdateEntity

class ReceiptRemoteDataSource(
    private val api: ReceiptApi,
    dispatchers: CoroutineDispatchersProvider
) : BaseRemoteDataSource(dispatchers) {
    suspend fun scan(imageUrl: String): NetworkResult<ReceiptScanResultEntity> {
        return request { api.scan(ScanRequestDto(imageUrl)) }
    }

    suspend fun save(request: ReceiptSaveEntity): NetworkResult<Unit> {
        return requestUnit { api.save(request) }
    }

    suspend fun update(request: ReceiptUpdateEntity): NetworkResult<Unit> {
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
