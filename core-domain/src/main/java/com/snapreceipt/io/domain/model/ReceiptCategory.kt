package com.snapreceipt.io.domain.model

object ReceiptCategory {
    data class Item(val id: Int, val label: String)

    private val categories = listOf(
        Item(1, "Food"),
        Item(2, "Travel"),
        Item(3, "Office"),
        Item(4, "Hotel"),
        Item(5, "Other")
    )

    fun all(): List<Item> = categories

    fun labelForId(id: Int): String =
        categories.firstOrNull { it.id == id }?.label ?: "Other"

    fun idForLabel(label: String): Int =
        categories.firstOrNull { it.label.equals(label, ignoreCase = true) }?.id ?: 0
}
