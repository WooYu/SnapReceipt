package com.snapreceipt.io.ui.invoice.bottomsheet

/**
 * UI model for a category chip.
 * `label` comparison is case-insensitive because backend/user input may vary in casing.
 */
internal data class CategoryOption(
    val id: Long,
    val label: String,
    val isCustom: Boolean
) {
    fun matches(value: String): Boolean = label.equals(value, ignoreCase = true)
}

/**
 * Flat list item model for the RecyclerView.
 * The "Add" action is represented as a normal row so layout and spacing stay consistent.
 */
internal sealed class CategoryListItem {
    data class Category(val option: CategoryOption) : CategoryListItem()
    object AddCategory : CategoryListItem()
}
