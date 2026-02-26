package com.snapreceipt.io.domain.manager

import com.snapreceipt.io.domain.model.CategoryItem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 收据分类内存缓存管理器
 * 
 * 职责：
 * - 提供分类数据的内存缓存（单例，进程内全局共享）
 * - 支持 TTL 过期判断（推荐 5 分钟刷新）
 * - 线程安全（@Volatile + 单线程写）
 * 
 * 设计原则：
 * - 仅内存缓存，不持久化（分类数据小，冷启动拉取一次即可）
 * - 依赖注入单例（便于测试、替换实现）
 * - 简单直接（无复杂状态机）
 * 
 * 使用示例：
 * ```kotlin
 * @Inject lateinit var categoryCache: CategoryCacheManager
 * 
 * // 读取缓存
 * val categories = categoryCache.getCategories()
 * 
 * // 判断是否需要刷新
 * if (categories.isEmpty() || categoryCache.isStale()) {
 *     val remote = fetchCategoriesUseCase()
 *     categoryCache.update(remote)
 * }
 * ```
 */
@Singleton
class CategoryCacheManager @Inject constructor() {
    
    companion object {
        /**
         * 缓存有效期：5 分钟
         * 
         * 考虑：
         * - 分类变化频率低（用户不常新增/删除分类）
         * - 5 分钟内多次打开分类弹窗复用缓存，减少网络请求
         * - 超过 5 分钟后自动刷新，保证数据一致性
         */
        private const val TTL_MILLIS = 5 * 60 * 1000L
    }
    
    // ── 内存缓存 ──────────────────────────────────────────
    
    @Volatile
    private var categories: List<CategoryItem> = emptyList()
    
    @Volatile
    private var lastUpdatedAt: Long = 0L
    
    // ── 公开 API ──────────────────────────────────────────
    
    /**
     * 获取所有分类（从内存）。
     * 
     * @return 分类列表（可能为空）
     */
    fun getCategories(): List<CategoryItem> = categories
    
    /**
     * 更新分类缓存（覆盖写入）。
     * 
     * 线程安全：
     * - 调用方应在主线程或单一后台线程调用（避免并发写）
     * - 读取无锁（@Volatile 保证可见性）
     * 
     * @param items 分类列表（来自远程 API）
     */
    fun update(items: List<CategoryItem>) {
        categories = items
        lastUpdatedAt = System.currentTimeMillis()
    }
    
    /**
     * 判断缓存是否过期。
     * 
     * 过期条件：
     * - 从未更新过（lastUpdatedAt == 0）
     * - 距离上次更新超过 TTL（默认 5 分钟）
     * 
     * @return true = 已过期，建议重新拉取
     */
    fun isStale(): Boolean {
        if (lastUpdatedAt == 0L) return true
        return System.currentTimeMillis() - lastUpdatedAt > TTL_MILLIS
    }
    
    /**
     * 根据分类名称查找分类 ID。
     * 
     * 查找逻辑：
     * - 忽略大小写
     * - 仅在当前内存缓存中查找（不触发远程拉取）
     * 
     * @param label 分类名称
     * @return 分类 ID，未找到返回 -1
     */
    fun idForLabel(label: String): Long {
        val normalized = label.trim()
        if (normalized.isBlank()) return -1L
        return categories.firstOrNull { 
            it.label.equals(normalized, ignoreCase = true) 
        }?.id ?: -1L
    }
    
    /**
     * 获取最后更新时间戳。
     * 
     * @return 毫秒时间戳，从未更新过返回 null
     */
    fun getLastUpdatedAt(): Long? = lastUpdatedAt.takeIf { it > 0L }
    
    /**
     * 清空缓存（用于登出等场景）。
     */
    fun clear() {
        categories = emptyList()
        lastUpdatedAt = 0L
    }
}
