package com.snapreceipt.io.domain.model

/**
 * 分类内存缓存（来自 /api/category/list）。
 *
 * 缓存更新时机：
 * - FetchCategoriesUseCase 成功返回后调用 update。
 * - 分类新增/删除后重新拉取并 update。
 */
object ReceiptCategory {
    /**
     * 分类条目。
     *
     * @property id 分类ID
     * @property label 分类名称
     * @property isCustom 是否用户自定义分类
     */
    data class Item(
        val id: Long,
        val label: String,
        val isCustom: Boolean = false
    )

    @Volatile
    private var categories: List<Item> = emptyList()

    @Volatile
    private var lastUpdatedAt: Long? = null

    fun all(): List<Item> = categories

    fun update(items: List<Item>) {
        categories = items
        lastUpdatedAt = System.currentTimeMillis()
    }

    fun labelForId(id: Long): String {
        return categories.firstOrNull { it.id == id }?.label.orEmpty()
    }

    fun idForLabel(label: String): Long {
        val fromList = categories.firstOrNull { it.label.equals(label, ignoreCase = true) }?.id
        if (fromList != null) return fromList
        return -1L
    }

    fun lastUpdatedAt(): Long? = lastUpdatedAt
}
