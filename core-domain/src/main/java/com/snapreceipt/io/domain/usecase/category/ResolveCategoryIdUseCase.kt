package com.snapreceipt.io.domain.usecase.category

import com.snapreceipt.io.domain.manager.CategoryCache
import javax.inject.Inject

class ResolveCategoryIdUseCase @Inject constructor(
    private val fetchCategoriesUseCase: FetchCategoriesUseCase,
    private val categoryCache: CategoryCache
) {
    suspend operator fun invoke(label: String): Long {
        val normalized = label.trim()
        if (normalized.isBlank()) return -1L

        categoryCache.idForLabel(normalized).takeIf { it > 0L }?.let { return it }

        val remoteResult = fetchCategoriesUseCase()
        remoteResult.onSuccess { list ->
            categoryCache.update(list)
        }

        return categoryCache.idForLabel(normalized)
    }
}