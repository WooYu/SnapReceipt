package com.snapreceipt.io.domain.model

/**
 * 分类内存缓存（来自 /api/category/list）。
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

    private val defaultCategories = listOf(
        Item(id = 100L, label = "Dining"),
        Item(id = 101L, label = "Grocery"),
        Item(id = 102L, label = "Gas/Fuel"),
        Item(id = 103L, label = "Travel")
    )

    @Volatile
    private var categories: List<Item> = defaultCategories

    fun all(): List<Item> = categories

    fun update(items: List<Item>) {
        categories = items.ifEmpty { defaultCategories }
    }

    fun labelForId(id: Long): String {
        val fromList = categories.firstOrNull { it.id == id }?.label
        if (!fromList.isNullOrBlank()) return fromList
        val fallback = defaultCategories.firstOrNull { it.id == id }?.label
        return fallback ?: "Other"
    }

    fun idForLabel(label: String): Long {
        val fromList = categories.firstOrNull { it.label.equals(label, ignoreCase = true) }?.id
        if (fromList != null) return fromList
        val fallback = defaultCategories.firstOrNull { it.label.equals(label, ignoreCase = true) }?.id
        return fallback ?: -1L
    }
}
