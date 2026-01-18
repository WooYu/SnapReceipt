package com.skybound.space.core.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.io.Closeable
import java.util.concurrent.Executors

/**
 * 统一管理协程调度器，便于在测试中替换。
 */
interface CoroutineDispatchersProvider {
    val io: CoroutineDispatcher
    val main: CoroutineDispatcher
    val default: CoroutineDispatcher

    object Default : CoroutineDispatchersProvider {
        override val io: CoroutineDispatcher = Dispatchers.IO
        override val main: CoroutineDispatcher = Dispatchers.Main
        override val default: CoroutineDispatcher = Dispatchers.Default
    }

    class SingleThread(
        threadName: String = "single-thread-dispatcher"
    ) : CoroutineDispatchersProvider, Closeable {
        private val dispatcher = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, threadName).apply { isDaemon = true }
        }.asCoroutineDispatcher()

        override val io: CoroutineDispatcher = dispatcher
        override val main: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher

        override fun close() {
            dispatcher.close()
        }
    }
}
