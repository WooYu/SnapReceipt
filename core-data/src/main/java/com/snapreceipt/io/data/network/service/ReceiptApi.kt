package com.snapreceipt.io.data.network.service

import com.skybound.space.core.network.BaseEmptyResponse
import com.skybound.space.core.network.BasePagedResponse
import com.skybound.space.core.network.BaseResponse
import com.snapreceipt.io.data.network.model.category.CategoryCreateRequestDto
import com.snapreceipt.io.data.network.model.category.CategoryDeleteRequestDto
import com.snapreceipt.io.data.network.model.category.CategoryItemDto
import com.snapreceipt.io.data.network.model.category.CategoryListResponseDto
import com.snapreceipt.io.data.network.model.category.CategoryListRequestDto
import com.snapreceipt.io.data.network.model.receipt.ReceiptDeleteRequestDto
import com.snapreceipt.io.data.network.model.receipt.ReceiptExportRequestDto
import com.snapreceipt.io.data.network.model.receipt.ScanRequestDto
import com.snapreceipt.io.domain.model.ExportRecordEntity
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.model.query.ExportRecordListQueryEntity
import com.snapreceipt.io.domain.model.query.ReceiptListQueryEntity
import kotlin.jvm.JvmSuppressWildcards
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 收据模块 Retrofit 接口定义。
 *
 * 涵盖 OCR 识别、收据 CRUD、导出记录以及分类管理等接口。
 */
interface ReceiptApi {
    /**
     * @param request 扫描识别请求，包含已上传图片地址
     * @return OCR 识别后的收据内容
     */
    @POST("api/receipt/scan")
    suspend fun scan(@Body request: ScanRequestDto): BaseResponse<ReceiptEntity>

    /**
     * @param request 新建收据请求体
     */
    @POST("api/receipt/save")
    suspend fun save(@Body request: Map<String, @JvmSuppressWildcards Any>): BaseEmptyResponse

    /**
     * @param request 更新收据请求体，需包含 receiptId
     */
    @POST("api/receipt/update")
    suspend fun update(@Body request: Map<String, @JvmSuppressWildcards Any>): BaseEmptyResponse

    /**
     * @param request 分页和筛选条件
     * @return 带分页信息的收据列表
     */
    @POST("api/receipt/list")
    suspend fun list(@Body request: ReceiptListQueryEntity): BasePagedResponse<ReceiptEntity>

    /**
     * @param request 删除请求，包含 receiptId
     */
    @POST("api/receipt/delete")
    suspend fun delete(@Body request: ReceiptDeleteRequestDto): BaseEmptyResponse

    /**
     * @param request 导出请求，包含待导出的收据 ID 列表
     * @return 导出结果字符串
     */
    @POST("api/receipt/export")
    suspend fun export(@Body request: ReceiptExportRequestDto): BaseResponse<String>

    /**
     * @param request 导出记录列表查询条件
     */
    @POST("api/record/list")
    suspend fun exportRecords(@Body request: ExportRecordListQueryEntity): BasePagedResponse<ExportRecordEntity>

    /**
     * @param request 分类列表请求；当前为空对象，占位统一 POST 协议
     */
    @POST("api/category/list")
    suspend fun listCategories(@Body request: CategoryListRequestDto): BaseResponse<CategoryListResponseDto>

    /**
     * @param request 新增分类请求
     */
    @POST("api/category/add")
    suspend fun addCategory(@Body request: CategoryCreateRequestDto): BaseEmptyResponse

    /**
     * @param request 批量删除分类请求
     */
    @POST("api/category/remove")
    suspend fun deleteCategory(@Body request: CategoryDeleteRequestDto): BaseEmptyResponse
}
