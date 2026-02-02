package com.skybound.space.base.presentation

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * 在 Fragment 中安全收集 Flow，仅在 STARTED 及以上生命周期执行 collector。
 * 避免重复的 lifecycleScope + repeatOnLifecycle(STARTED) 样板代码。
 */
fun <T> Fragment.observeState(flow: Flow<T>, collector: (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { collector(it) }
        }
    }
}

/**
 * 在 Activity 中安全收集 Flow，仅在 STARTED 及以上生命周期执行 collector。
 */
fun <T> ComponentActivity.observeState(flow: Flow<T>, collector: (T) -> Unit) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { collector(it) }
        }
    }
}
