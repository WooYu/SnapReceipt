package com.snapreceipt.io.domain.usecase.category

import com.snapreceipt.io.domain.manager.CategoryCacheManager
import javax.inject.Inject

/**
 * 根据分类标签解析分类 ID
 * 
 * 策略：
 * 1. 先从内存缓存查找
 * 2. 缓存未命中或过期，拉取最新分类并刷新缓存
 * 3. 从刷新后的缓存再次查找
 * 
 * 使用场景：
 * - 用户输入自定义分类名称，需要转换为 ID 传给服务端
 * - 离线场景下尽量利用缓存，减少网络请求
 */
class ResolveCategoryIdUseCase @Inject constructor(
    private val fetchCategoriesUseCase: FetchCategoriesUseCase,
    private val categoryCache: CategoryCacheManager
) {
    suspend operator fun invoke(label: String): Long {
        val normalized = label.trim()
        if (normalized.isBlank()) return -1L

        // 先从缓存查找
        categoryCache.idForLabel(normalized).takeIf { it > 0L }?.let { return it }

        // 缓存未命中，拉取最新分类
        val remoteResult = fetchCategoriesUseCase()
        remoteResult.onSuccess { list ->
            categoryCache.update(list)
        }

        // 从刷新后的缓存再次查找
        return categoryCache.idForLabel(normalized)
    }
}
