package com.snapreceipt.io.data.network.service

import com.skybound.space.core.network.BaseResponse
import com.skybound.space.core.network.BaseEmptyResponse
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
import com.snapreceipt.io.domain.model.ExportRecordEntity
import com.snapreceipt.io.domain.model.query.ExportRecordListQueryEntity
import com.snapreceipt.io.domain.model.query.ReceiptListQueryEntity
import retrofit2.http.Body
import retrofit2.http.POST

interface ReceiptApi {
    @POST("api/receipt/scan")
    suspend fun scan(@Body request: ScanRequestDto): BaseResponse<ReceiptScanResultDto>

    @POST("api/receipt/save")
    suspend fun save(@Body request: ReceiptSaveRequestDto): BaseEmptyResponse

    @POST("api/receipt/update")
    suspend fun update(@Body request: ReceiptUpdateRequestDto): BaseEmptyResponse

    @POST("api/receipt/list")
    suspend fun list(@Body request: ReceiptListQueryEntity): BasePagedResponse<ReceiptItemDto>

    @POST("api/receipt/delete")
    suspend fun delete(@Body request: ReceiptDeleteRequestDto): BaseEmptyResponse

    @POST("api/receipt/export")
    suspend fun export(@Body request: ReceiptExportRequestDto): BaseResponse<String>

    @POST("api/record/list")
    suspend fun exportRecords(@Body request: ExportRecordListQueryEntity): BasePagedResponse<ExportRecordEntity>

    @POST("api/category/list")
    suspend fun listCategories(@Body request: CategoryListRequestDto): BaseResponse<List<CategoryItemDto>>

    @POST("api/category/remove")
    suspend fun addCategory(@Body request: CategoryCreateRequestDto): BaseEmptyResponse

    @POST("api/category/remove")
    suspend fun deleteCategory(@Body request: CategoryDeleteRequestDto): BaseEmptyResponse
}
