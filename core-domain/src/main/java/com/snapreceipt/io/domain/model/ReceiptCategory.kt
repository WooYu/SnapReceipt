package com.snapreceipt.io.domain.model

/**
 * 收据分类领域模型
 * 
 * 职责：
 * - 定义分类的数据结构
 * - 不包含缓存逻辑（缓存由 CategoryCacheManager 管理）
 * 
 * 架构说明：
 * - data 层的 CategoryItemDto 映射为此模型
 * - domain 层的 CategoryCacheManager 缓存此模型
 * - UI 层展示此模型
 */
object ReceiptCategory {
    /**
     * 分类条目
     *
     * @property id 分类 ID（服务端唯一标识）
     * @property label 分类名称（如"餐饮"、"交通"、"办公"等）
     * @property isCustom 是否用户自定义分类（true = 用户创建，false = 系统预设）
     */
    data class Item(
        val id: Long,
        val label: String,
        val isCustom: Boolean = false
    )
}
