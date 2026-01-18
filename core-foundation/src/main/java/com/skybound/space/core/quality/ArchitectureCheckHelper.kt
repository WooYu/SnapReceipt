package com.skybound.space.core.quality

object ArchitectureCheckHelper {
    private val checks = mutableListOf<() -> Unit>()

    fun register(check: () -> Unit) {
        checks += check
    }

    fun check() {
        checks.forEach { it.invoke() }
    }
}
