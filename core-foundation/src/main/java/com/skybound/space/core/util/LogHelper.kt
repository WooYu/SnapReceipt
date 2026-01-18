package com.skybound.space.core.util

import timber.log.Timber

object LogHelper {
    private const val PRIORITY_DEBUG = 3
    private const val PRIORITY_INFO = 4
    private const val PRIORITY_WARN = 5
    private const val PRIORITY_ERROR = 6

    var isDebug: Boolean = true
        private set
    var messageTransformer: ((String) -> String)? = null
    var reporter: ((level: String, tag: String, message: String, throwable: Throwable?) -> Unit)? = null

    fun init(isDebug: Boolean) {
        this.isDebug = isDebug
        Timber.uprootAll()
        if (isDebug) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReportOnlyTree())
        }
    }

    fun d(tag: String, message: String) = log("D", tag, message, null)
    fun i(tag: String, message: String) = log("I", tag, message, null)
    fun w(tag: String, message: String, throwable: Throwable? = null) = log("W", tag, message, throwable)
    fun e(tag: String, message: String, throwable: Throwable? = null) = log("E", tag, message, throwable)

    private fun log(level: String, tag: String, message: String, throwable: Throwable?) {
        val transformed = messageTransformer?.invoke(message) ?: message
        if (isDebug) {
            when (level) {
                "D" -> Timber.tag(tag).d(throwable, transformed)
                "I" -> Timber.tag(tag).i(throwable, transformed)
                "W" -> Timber.tag(tag).w(throwable, transformed)
                "E" -> Timber.tag(tag).e(throwable, transformed)
                else -> Timber.tag(tag).v(throwable, transformed)
            }
        }
        reporter?.invoke(level, tag, transformed, throwable)
    }

    private class ReportOnlyTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            val level = when (priority) {
                PRIORITY_DEBUG -> "D"
                PRIORITY_INFO -> "I"
                PRIORITY_WARN -> "W"
                PRIORITY_ERROR -> "E"
                else -> "V"
            }
            reporter?.invoke(level, tag ?: "LogHelper", message, t)
        }
    }
}
