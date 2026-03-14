package com.skybound.space.core.monitoring

interface CrashReporter {
    fun setEnabled(enabled: Boolean)
    fun recordException(throwable: Throwable, metadata: Map<String, String> = emptyMap())
    fun log(level: String, tag: String, message: String, throwable: Throwable? = null)
}

private object NoOpCrashReporter : CrashReporter {
    override fun setEnabled(enabled: Boolean) = Unit

    override fun recordException(throwable: Throwable, metadata: Map<String, String>) = Unit

    override fun log(level: String, tag: String, message: String, throwable: Throwable?) = Unit
}

object ExceptionMonitorManager {
    @Volatile
    private var delegate: CrashReporter = NoOpCrashReporter

    fun init(delegate: CrashReporter?) {
        this.delegate = delegate ?: NoOpCrashReporter
    }

    fun setEnabled(enabled: Boolean) {
        runCatching { delegate.setEnabled(enabled) }
    }

    fun report(throwable: Throwable, metadata: Map<String, String> = emptyMap()) {
        runCatching { delegate.recordException(throwable, metadata) }
    }

    fun log(level: String, tag: String, message: String, throwable: Throwable? = null) {
        runCatching { delegate.log(level, tag, message, throwable) }
    }
}
