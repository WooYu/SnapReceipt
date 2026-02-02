package com.snapreceipt.io.data.base

import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.snapreceipt.io.data.manager.CacheManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * 数据层 Repository 基类，提供统一的 IO 调度、Flow 映射和缓存优先能力。
 *
 * **继承约定：**
 * - **本地或混合仓库**（如 [UserRepositoryImpl]）：继承本类，复用 [withIo]、[mapFlow]、[cacheFirst]。
 * - **纯远程仓库**（如 [ReceiptRemoteRepositoryImpl]）：数据源已在 DataSource 内使用 withContext(io)，
 *   可不继承本类，避免重复调度；若需统一调度或缓存可评估后继承。
 */
abstract class BaseRepository(
    private val dispatchers: CoroutineDispatchersProvider,
    private val cacheManager: CacheManager? = null
) {
    protected suspend fun <T> withIo(block: suspend () -> T): T {
        return withContext(dispatchers.io) { block() }
    }

    protected fun <I, O> mapFlow(
        source: Flow<List<I>>,
        mapper: BaseMapper<I, O>
    ): Flow<List<O>> = source.map { mapper.mapList(it) }

    protected suspend fun <T : Any> cacheFirst(
        key: String,
        ttlMs: Long,
        serializer: CacheManager.CacheSerializer<T>,
        fetcher: suspend () -> T
    ): T {
        val manager = cacheManager ?: return fetcher()
        return manager.getOrFetch(key, ttlMs, serializer, fetcher)
    }
}
