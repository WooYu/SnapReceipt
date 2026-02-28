package com.skybound.space.core.testing

import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/**
 * [CoroutineDispatchersProvider] backed by test dispatchers.
 *
 * Uses [UnconfinedTestDispatcher] by default so coroutine launches
 * execute eagerly, simplifying most unit tests.
 *
 * Pass a [StandardTestDispatcher] when you need explicit control
 * over execution order via `advanceUntilIdle()`.
 */
class TestDispatchers(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : CoroutineDispatchersProvider {
    override val io: CoroutineDispatcher get() = dispatcher
    override val main: CoroutineDispatcher get() = dispatcher
    override val default: CoroutineDispatcher get() = dispatcher

    val testDispatcher: TestDispatcher get() = dispatcher
}