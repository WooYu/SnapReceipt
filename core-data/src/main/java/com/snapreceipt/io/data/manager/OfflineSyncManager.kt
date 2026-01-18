package com.snapreceipt.io.data.manager

import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

class OfflineSyncManager(
    private val taskStore: OfflineTaskStore,
    private val networkMonitor: NetworkMonitor,
    private val dispatchers: CoroutineDispatchersProvider
) {
    private val idGenerator = AtomicLong(0)

    data class OfflineTask(
        val id: Long,
        val type: String,
        val payload: String,
        val createdAt: Long = System.currentTimeMillis(),
        val attemptCount: Int = 0,
        val lastError: String? = null
    )

    interface OfflineTaskStore {
        suspend fun save(task: OfflineTask)
        suspend fun update(task: OfflineTask)
        suspend fun remove(id: Long)
        suspend fun getAll(): List<OfflineTask>
    }

    interface NetworkMonitor {
        fun isOnline(): Boolean
        fun setListener(listener: ((Boolean) -> Unit)?)
    }

    suspend fun enqueue(type: String, payload: String) {
        val task = OfflineTask(idGenerator.incrementAndGet(), type, payload)
        taskStore.save(task)
    }

    fun startSync(
        scope: CoroutineScope,
        handler: suspend (OfflineTask) -> Boolean
    ): Job {
        networkMonitor.setListener { online ->
            if (online) {
                scope.launch(dispatchers.io) {
                    syncPending(handler)
                }
            }
        }
        return scope.launch(dispatchers.io) {
            if (networkMonitor.isOnline()) {
                syncPending(handler)
            }
        }
    }

    suspend fun syncPending(handler: suspend (OfflineTask) -> Boolean) {
        if (!networkMonitor.isOnline()) return
        val tasks = taskStore.getAll()
        for (task in tasks) {
            val success = runCatching { handler(task) }.getOrDefault(false)
            if (success) {
                taskStore.remove(task.id)
            } else {
                val updated = task.copy(
                    attemptCount = task.attemptCount + 1,
                    lastError = "sync_failed"
                )
                taskStore.update(updated)
            }
        }
    }

    companion object {
        fun inMemory(
            dispatchers: CoroutineDispatchersProvider,
            networkMonitor: NetworkMonitor = AlwaysOnlineMonitor()
        ): OfflineSyncManager {
            return OfflineSyncManager(InMemoryTaskStore(), networkMonitor, dispatchers)
        }
    }

    class InMemoryTaskStore : OfflineTaskStore {
        private val tasks = mutableMapOf<Long, OfflineTask>()

        override suspend fun save(task: OfflineTask) {
            tasks[task.id] = task
        }

        override suspend fun update(task: OfflineTask) {
            tasks[task.id] = task
        }

        override suspend fun remove(id: Long) {
            tasks.remove(id)
        }

        override suspend fun getAll(): List<OfflineTask> {
            return tasks.values.sortedBy { it.createdAt }
        }
    }

    class AlwaysOnlineMonitor : NetworkMonitor {
        private var listener: ((Boolean) -> Unit)? = null

        override fun isOnline(): Boolean = true

        override fun setListener(listener: ((Boolean) -> Unit)?) {
            this.listener = listener
            listener?.invoke(true)
        }
    }
}
