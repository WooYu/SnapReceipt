package com.snapreceipt.io.domain.usecase.category

import com.snapreceipt.io.domain.model.ReceiptCategory
import javax.inject.Inject

/**
 * Resolves category id by label with cache-first strategy.
 *
 * Flow:
 * 1) Try in-memory cache.
 * 2) If missing, fetch latest categories once and refresh cache.
 * 3) Resolve again from refreshed cache.
 */
class ResolveCategoryIdUseCase @Inject constructor(
    private val fetchCategoriesUseCase: FetchCategoriesUseCase
) {
    suspend operator fun invoke(label: String): Long {
        val normalized = label.trim()
        if (normalized.isBlank()) return -1L

        ReceiptCategory.idForLabel(normalized).takeIf { it > 0L }?.let { return it }

        val remoteResult = fetchCategoriesUseCase()
        remoteResult.onSuccess { list ->
            ReceiptCategory.update(list)
        }

        return ReceiptCategory.idForLabel(normalized)
    }
}

