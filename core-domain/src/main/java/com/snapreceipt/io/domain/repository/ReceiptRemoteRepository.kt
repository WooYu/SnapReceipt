package com.snapreceipt.io.domain.repository

import com.snapreceipt.io.domain.model.ExportRecordEntity
import com.snapreceipt.io.domain.model.CategoryItem
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.model.query.ExportRecordListQueryEntity
import com.snapreceipt.io.domain.model.query.ReceiptListQueryEntity

/**
 * 收据远程仓库。
 *
 * 统一封装收据识别、保存、更新、删除、分页查询、导出以及分类管理等远程能力，
 * 供 use case 层以领域模型的形式调用。
 */
interface ReceiptRemoteRepository {
    /**
     * @param imageUrl 已上传到对象存储的图片公开地址
     * @return OCR 识别后的收据实体；字段可能不完整，需要在详情页进一步补录
     */
    suspend fun scan(imageUrl: String): ReceiptEntity

    /**
     * @param request 待保存的收据内容；通常来自扫描预填或用户手动录入
     */
    suspend fun save(request: ReceiptEntity)

    /**
     * @param request 待更新的收据内容；必须包含有效 receiptId
     */
    suspend fun update(request: ReceiptEntity)

    /**
     * @param receiptId 需要删除的收据 ID
     */
    suspend fun delete(receiptId: Long)

    /**
     * @param query 列表分页与筛选条件
     * @return 当前页收据列表
     */
    suspend fun list(query: ReceiptListQueryEntity): List<ReceiptEntity>

    /**
     * @param receiptIds 需要导出的收据 ID 列表
     * @return 导出结果地址或任务标识，具体由服务端协议决定
     */
    suspend fun export(receiptIds: List<Long>): String

    /**
     * @param query 导出记录分页查询条件
     * @return 导出历史记录列表
     */
    suspend fun listExportRecords(query: ExportRecordListQueryEntity): List<ExportRecordEntity>

    /**
     * @return 推荐分类和自定义分类合并后的分类列表
     */
    suspend fun listCategories(): List<CategoryItem>

    /**
     * @param name 新增分类名称
     */
    suspend fun addCategory(name: String)

    /**
     * @param ids 需要删除的分类 ID 列表
     */
    suspend fun removeCategories(ids: List<Long>)
}
