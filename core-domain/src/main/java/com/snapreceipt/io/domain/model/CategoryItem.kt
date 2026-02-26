package com.snapreceipt.io.domain.model

/**
 * @property id 分类 ID
 * @property label 分类名称
 * @property isCustom 是否用户自定义分类
 */
data class CategoryItem(
    val id: Long,
    val label: String,
    val isCustom: Boolean = false
)
