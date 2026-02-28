package com.skybound.space.core.network.serializer

/**
 * Abstraction for JSON serialization/deserialization.
 *
 * Allows consuming projects to swap the underlying library (Gson, Moshi, kotlinx.serialization)
 * without modifying core-foundation.
 */
interface JsonSerializer {
    fun <T> fromJson(json: String, clazz: Class<T>): T?
    fun toJson(obj: Any): String
}

inline fun <reified T> JsonSerializer.fromJson(json: String): T? = fromJson(json, T::class.java)