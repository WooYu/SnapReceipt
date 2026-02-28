package com.snapreceipt.io.data.manager

import com.snapreceipt.io.domain.manager.CategoryCache
import com.snapreceipt.io.domain.model.CategoryItem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory category cache with TTL-based staleness detection.
 *
 * Thread safety: uses @Volatile fields; writes are expected from a single thread.
 */
@Singleton
class CategoryCacheManager @Inject constructor() : CategoryCache {

    companion object {
        private const val TTL_MILLIS = 5 * 60 * 1000L
    }

    @Volatile
    private var categories: List<CategoryItem> = emptyList()

    @Volatile
    private var lastUpdatedAt: Long = 0L

    override fun getCategories(): List<CategoryItem> = categories

    override fun update(items: List<CategoryItem>) {
        categories = items
        lastUpdatedAt = System.currentTimeMillis()
    }

    override fun isStale(): Boolean {
        if (lastUpdatedAt == 0L) return true
        return System.currentTimeMillis() - lastUpdatedAt > TTL_MILLIS
    }

    override fun idForLabel(label: String): Long {
        val normalized = label.trim()
        if (normalized.isBlank()) return -1L
        return categories.firstOrNull {
            it.label.equals(normalized, ignoreCase = true)
        }?.id ?: -1L
    }

    override fun getLastUpdatedAt(): Long? = lastUpdatedAt.takeIf { it > 0L }

    override fun clear() {
        categories = emptyList()
        lastUpdatedAt = 0L
    }
}