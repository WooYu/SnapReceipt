package com.snapreceipt.io.data.base

import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import kotlinx.coroutines.withContext

abstract class BaseLocalDataSource(
    private val dispatchers: CoroutineDispatchersProvider
) {
    protected suspend fun <T> withIo(block: suspend () -> T): T {
        return withContext(dispatchers.io) { block() }
    }
}
