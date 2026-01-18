package com.skybound.space.core.monitoring

object ExceptionMonitorManager {
    fun report(throwable: Throwable, metadata: Map<String, String> = emptyMap()) {
        // Hook for crash/ANR reporting SDKs
    }
}
