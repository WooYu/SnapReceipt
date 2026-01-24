package com.snapreceipt.io.domain.model

object ReceiptCategory {
    data class Item(
        val id: Int,
        val label: String,
        val isCustom: Boolean = false
    )

    private val defaultCategories = listOf(
        Item(1, "Food"),
        Item(2, "Travel"),
        Item(3, "Office"),
        Item(4, "Hotel"),
        Item(5, "Other")
    )

    @Volatile
    private var categories: List<Item> = defaultCategories

    fun all(): List<Item> = categories

    fun update(items: List<Item>) {
        categories = if (items.isEmpty()) defaultCategories else items
    }

    fun labelForId(id: Int): String {
        val fromList = categories.firstOrNull { it.id == id }?.label
        if (!fromList.isNullOrBlank()) return fromList
        val fallback = defaultCategories.firstOrNull { it.id == id }?.label
        return fallback ?: "Other"
    }

    fun idForLabel(label: String): Int {
        if (label.isBlank() || label.equals("All", ignoreCase = true)) return 0
        val fromList = categories.firstOrNull { it.label.equals(label, ignoreCase = true) }?.id
        if (fromList != null) return fromList
        return defaultCategories.firstOrNull { it.label.equals(label, ignoreCase = true) }?.id ?: 0
    }
}
