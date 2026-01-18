package com.skybound.space.base.presentation

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.StateFlow

class StateRestoreHelper(
    private val handle: SavedStateHandle
) {
    fun <T> get(key: String, defaultValue: T): T {
        return handle.get<T>(key) ?: defaultValue
    }

    fun <T> set(key: String, value: T) {
        handle[key] = value
    }

    fun <T> getStateFlow(key: String, initialValue: T): StateFlow<T> {
        return handle.getStateFlow(key, initialValue)
    }
}
