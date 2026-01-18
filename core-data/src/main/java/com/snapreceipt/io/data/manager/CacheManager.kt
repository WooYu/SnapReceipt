package com.snapreceipt.io.data.manager

import android.util.LruCache
import com.skybound.space.core.util.EncryptUtil
import java.io.File

class CacheManager(
    private val cacheDir: File,
    maxMemoryEntries: Int = DEFAULT_MAX_ENTRIES,
    private val clock: () -> Long = System::currentTimeMillis
) {
    interface CacheSerializer<T> {
        fun serialize(value: T): ByteArray
        fun deserialize(bytes: ByteArray): T?
    }

    data class CacheEntry<T>(val value: T, val timestamp: Long)

    private val memoryCache = object : LruCache<String, CacheEntry<Any>>(maxMemoryEntries) {}

    fun <T : Any> put(key: String, value: T, serializer: CacheSerializer<T>) {
        val timestamp = clock()
        memoryCache.put(key, CacheEntry(value, timestamp) as CacheEntry<Any>)
        writeToDisk(key, serializer.serialize(value), timestamp)
    }

    fun <T : Any> get(key: String, ttlMs: Long, serializer: CacheSerializer<T>): T? {
        val cached = memoryCache.get(key)
        if (cached != null && !isExpired(cached.timestamp, ttlMs)) {
            @Suppress("UNCHECKED_CAST")
            return cached.value as T
        }

        val diskValue = readFromDisk(key, ttlMs)
        if (diskValue != null) {
            val decoded = serializer.deserialize(diskValue.bytes)
            if (decoded != null) {
                memoryCache.put(key, CacheEntry(decoded, diskValue.timestamp) as CacheEntry<Any>)
                return decoded
            }
        }
        return null
    }

    suspend fun <T : Any> getOrFetch(
        key: String,
        ttlMs: Long,
        serializer: CacheSerializer<T>,
        fetcher: suspend () -> T
    ): T {
        val cached = get(key, ttlMs, serializer)
        if (cached != null) return cached
        val fresh = fetcher()
        put(key, fresh, serializer)
        return fresh
    }

    private fun cacheFile(key: String): File {
        val fileName = EncryptUtil.md5(key)
        return File(cacheDir, fileName)
    }

    private fun readFromDisk(key: String, ttlMs: Long): DiskEntry? {
        val file = cacheFile(key)
        if (!file.exists()) return null
        val timestamp = file.lastModified()
        if (isExpired(timestamp, ttlMs)) {
            file.delete()
            return null
        }
        return DiskEntry(file.readBytes(), timestamp)
    }

    private fun writeToDisk(key: String, bytes: ByteArray, timestamp: Long) {
        val file = cacheFile(key)
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
        file.setLastModified(timestamp)
    }

    private fun isExpired(timestamp: Long, ttlMs: Long): Boolean {
        return ttlMs > 0 && clock() - timestamp > ttlMs
    }

    private data class DiskEntry(val bytes: ByteArray, val timestamp: Long)

    companion object {
        const val DEFAULT_MAX_ENTRIES = 128
    }
}
