package com.snapreceipt.io.domain.model

/**
 * 导出记录列表查询条件（对应 /api/record/list）。
 *
 * @property pageNum 页码
 * @property pageSize 每页数量
 */
data class ExportRecordListQueryEntity(
    val pageNum: Int? = 1,
    val pageSize: Int? = 20
)
