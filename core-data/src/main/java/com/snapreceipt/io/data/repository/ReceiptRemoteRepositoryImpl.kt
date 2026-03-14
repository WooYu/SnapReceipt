package com.snapreceipt.io.data.repository

import com.skybound.space.core.network.getOrThrow
import com.snapreceipt.io.data.network.datasource.ReceiptRemoteDataSource
import com.snapreceipt.io.data.network.model.category.toItem
import com.snapreceipt.io.data.network.model.receipt.toRequestPayload
import com.snapreceipt.io.domain.model.ExportRecordEntity
import com.snapreceipt.io.domain.model.CategoryItem
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.model.query.ExportRecordListQueryEntity
import com.snapreceipt.io.domain.model.query.ReceiptListQueryEntity
import com.snapreceipt.io.domain.repository.ReceiptRemoteRepository
import javax.inject.Inject

/**
 * [ReceiptRemoteRepository] 的远程实现。
 *
 * 负责把 domain 层的实体和查询条件转成 data 层请求格式，
 * 再把远程返回值整理成上层可直接使用的领域模型。
 */
class ReceiptRemoteRepositoryImpl @Inject constructor(
    private val remoteDataSource: ReceiptRemoteDataSource
) : ReceiptRemoteRepository {

    /**
     * @param imageUrl 已上传图片的可访问地址
     * @return OCR 识别结果
     */
    override suspend fun scan(imageUrl: String): ReceiptEntity =
        remoteDataSource.scan(imageUrl).getOrThrow()

    /**
     * 保存新收据时不需要带 receiptId，因此这里使用默认映射规则。
     */
    override suspend fun save(request: ReceiptEntity) {
        remoteDataSource.save(request.toRequestPayload()).getOrThrow()
    }

    /**
     * 更新收据时必须把 receiptId 一起带给服务端。
     */
    override suspend fun update(request: ReceiptEntity) {
        remoteDataSource.update(request.toRequestPayload(includeReceiptId = true)).getOrThrow()
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

    /**
     * 服务端把推荐分类和自定义分类分开返回，这里统一排序后合并，
     * 让 UI 层只处理一个扁平列表。
     */
    override suspend fun listCategories(): List<CategoryItem> {
        val response = remoteDataSource.listCategories().getOrThrow()
        val recommend = response.recommend
            .sortedWith(compareBy({ it.orderNum ?: Int.MAX_VALUE }, { it.categoryId }))
            .map { it.toItem() }
        val customize = response.customize
            .sortedWith(compareBy({ it.orderNum ?: Int.MAX_VALUE }, { it.categoryId }))
            .map { it.toItem() }
        return recommend + customize
    }

    override suspend fun addCategory(name: String) {
        remoteDataSource.addCategory(name).getOrThrow()
    }

    override suspend fun removeCategories(ids: List<Long>) {
        remoteDataSource.deleteCategories(ids).getOrThrow()
    }
}
