package com.skybound.space.core.config

import java.util.concurrent.ConcurrentHashMap

class ConfigManager {
    private val configs = ConcurrentHashMap<String, String>()

    fun update(newConfigs: Map<String, String>) {
        configs.putAll(newConfigs)
    }

    fun getString(key: String, defaultValue: String? = null): String? =
        configs[key] ?: defaultValue

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean =
        configs[key]?.toBooleanStrictOrNull() ?: defaultValue

    fun getInt(key: String, defaultValue: Int = 0): Int =
        configs[key]?.toIntOrNull() ?: defaultValue

    fun getLong(key: String, defaultValue: Long = 0L): Long =
        configs[key]?.toLongOrNull() ?: defaultValue
}
