package com.skybound.space.core.monitoring

interface ExceptionReporter {
    fun report(throwable: Throwable, metadata: Map<String, String> = emptyMap())
}

object ExceptionMonitorManager {
    var reporter: ExceptionReporter? = null

    fun report(throwable: Throwable, metadata: Map<String, String> = emptyMap()) {
        reporter?.report(throwable, metadata)
    }
}