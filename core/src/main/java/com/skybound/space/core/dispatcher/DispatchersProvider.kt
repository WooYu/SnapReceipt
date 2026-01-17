package com.skybound.space.core.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * 统一管理协程调度器，便于在测试中替换。
 */
interface DispatchersProvider {
    val io: CoroutineDispatcher
    val computation: CoroutineDispatcher
    val main: CoroutineDispatcher

    object Default : DispatchersProvider {
        override val io: CoroutineDispatcher = Dispatchers.IO
        override val computation: CoroutineDispatcher = Dispatchers.Default
        override val main: CoroutineDispatcher = Dispatchers.Main
    }
}
