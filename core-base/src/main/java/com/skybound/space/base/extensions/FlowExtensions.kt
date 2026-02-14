package com.skybound.space.base.extensions

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import com.skybound.space.base.presentation.observeState as observeStateCompat
import kotlinx.coroutines.flow.Flow

/**
 * New extension entry-point for lifecycle-safe state collection.
 * Delegates to existing presentation-level implementation for compatibility.
 */
fun <T> Fragment.observeState(flow: Flow<T>, collector: (T) -> Unit) {
    observeStateCompat(flow, collector)
}

fun <T> ComponentActivity.observeState(flow: Flow<T>, collector: (T) -> Unit) {
    observeStateCompat(flow, collector)
}
