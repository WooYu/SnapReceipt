package com.snapreceipt.io.data.base

import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.snapreceipt.io.data.manager.CacheManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

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
