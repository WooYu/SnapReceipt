package com.snapreceipt.io.domain.manager

import com.snapreceipt.io.domain.model.CategoryItem

/**
 * Contract for category cache operations. Implementation lives in the data layer.
 */
interface CategoryCache {
    fun getCategories(): List<CategoryItem>
    fun update(items: List<CategoryItem>)
    fun isStale(): Boolean
    fun idForLabel(label: String): Long
    fun getLastUpdatedAt(): Long?
    fun clear()
}