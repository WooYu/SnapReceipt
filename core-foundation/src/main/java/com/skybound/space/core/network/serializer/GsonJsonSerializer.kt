package com.skybound.space.core.network.serializer

import com.google.gson.Gson

/**
 * Default [JsonSerializer] backed by Gson. Projects that prefer Moshi or
 * kotlinx.serialization can provide their own implementation.
 */
class GsonJsonSerializer(private val gson: Gson = Gson()) : JsonSerializer {
    override fun <T> fromJson(json: String, clazz: Class<T>): T? =
        gson.fromJson(json, clazz)

    override fun toJson(obj: Any): String =
        gson.toJson(obj)
}