package com.snapreceipt.io.domain.model.query

/**
 * 通用分页查询参数。
 *
 * @property pageNum 页码
 * @property pageSize 每页数量
 */
data class PageQueryEntity(
    val pageNum: Int? = 1,
    val pageSize: Int? = 20
)
