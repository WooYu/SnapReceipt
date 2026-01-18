package com.skybound.space.core.util

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

object LifecycleHelper {
    fun addObserver(
        owner: LifecycleOwner,
        onStart: (() -> Unit)? = null,
        onStop: (() -> Unit)? = null
    ): DefaultLifecycleObserver {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                onStart?.invoke()
            }

            override fun onStop(owner: LifecycleOwner) {
                onStop?.invoke()
            }
        }
        owner.lifecycle.addObserver(observer)
        return observer
    }
}
