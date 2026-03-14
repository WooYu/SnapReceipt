package com.snapreceipt.io.data.network.datasource

import com.snapreceipt.io.data.base.BaseRemoteDataSource
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.skybound.space.core.network.NetworkResult
import com.skybound.space.core.network.BasePagedResponse
import com.snapreceipt.io.data.network.model.category.CategoryCreateRequestDto
import com.snapreceipt.io.data.network.model.category.CategoryDeleteRequestDto
import com.snapreceipt.io.data.network.model.category.CategoryItemDto
import com.snapreceipt.io.data.network.model.category.CategoryListResponseDto
import com.snapreceipt.io.data.network.model.category.CategoryListRequestDto
import com.snapreceipt.io.data.network.model.receipt.ReceiptDeleteRequestDto
import com.snapreceipt.io.data.network.model.receipt.ReceiptExportRequestDto
import com.snapreceipt.io.data.network.model.receipt.ScanRequestDto
import com.snapreceipt.io.data.network.service.ReceiptApi
import com.snapreceipt.io.domain.model.ExportRecordEntity
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.model.query.ExportRecordListQueryEntity
import com.snapreceipt.io.domain.model.query.ReceiptListQueryEntity

/**
 * 收据相关远程数据源。
 *
 * 负责把收据模块的 Retrofit 接口调用统一包装成 [NetworkResult]，
 * 便于 repository 层在一个地方处理成功/失败分支。
 */
class ReceiptRemoteDataSource(
    private val api: ReceiptApi,
    dispatchers: CoroutineDispatchersProvider
) : BaseRemoteDataSource(dispatchers) {
    /**
     * @param imageUrl 已上传的图片地址
     * @return OCR 识别结果
     */
    suspend fun scan(imageUrl: String): NetworkResult<ReceiptEntity> {
        return request { api.scan(ScanRequestDto(imageUrl)) }
    }

    /**
     * @param request 保存收据的请求体；字段来自 [ReceiptEntity] 映射后的字典
     */
    suspend fun save(request: Map<String, Any>): NetworkResult<Unit> {
        return requestUnit { api.save(request) }
    }

    /**
     * @param request 更新收据的请求体；需包含 receiptId
     */
    suspend fun update(request: Map<String, Any>): NetworkResult<Unit> {
        return requestUnit { api.update(request) }
    }

    /**
     * @param request 列表查询条件
     * @return 带分页信息的收据列表响应
     */
    suspend fun list(request: ReceiptListQueryEntity): NetworkResult<BasePagedResponse<ReceiptEntity>> {
        return requestEnvelope({ api.list(request) }) { it }
    }

    /**
     * @param receiptId 待删除收据 ID
     */
    suspend fun delete(receiptId: Long): NetworkResult<Unit> {
        return requestUnit { api.delete(ReceiptDeleteRequestDto(receiptId)) }
    }

    /**
     * @param receiptIds 待导出的收据 ID 列表
     * @return 导出结果字符串，通常为下载链接或任务结果
     */
    suspend fun export(receiptIds: List<Long>): NetworkResult<String> {
        return request { api.export(ReceiptExportRequestDto(receiptIds)) }
    }

    /**
     * @param request 导出记录分页查询条件
     */
    suspend fun exportRecords(request: ExportRecordListQueryEntity): NetworkResult<BasePagedResponse<ExportRecordEntity>> {
        return requestEnvelope({ api.exportRecords(request) }) { it }
    }

    /**
     * @return 推荐分类与自定义分类分组结果
     */
    suspend fun listCategories(): NetworkResult<CategoryListResponseDto> {
        return request { api.listCategories(CategoryListRequestDto()) }
    }

    /**
     * @param name 新增分类名称
     */
    suspend fun addCategory(name: String): NetworkResult<Unit> {
        return requestUnit { api.addCategory(CategoryCreateRequestDto(name)) }
    }

    /**
     * @param ids 待删除分类 ID 列表
     */
    suspend fun deleteCategories(ids: List<Long>): NetworkResult<Unit> {
        return requestUnit { api.deleteCategory(CategoryDeleteRequestDto(ids)) }
    }
}
