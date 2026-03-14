package com.skybound.space.core.util

import timber.log.Timber

/**
 * 日志辅助工具
 * 
 * 职责：
 * - 统一管理应用日志输出（基于 Timber）
 * - 支持编译期和运行时切换调试模式
 * - 提供自定义上报钩子（崩溃收集、远程日志）
 * 
 * 日志级别：
 * - d：调试信息（仅 Debug 模式输出）
 * - i：一般信息（Debug 模式输出）
 * - w：警告（Debug 模式输出，Release 仅上报）
 * - e：错误（Debug 模式输出，Release 仅上报）
 * 
 * 初始化：
 * ```kotlin
 * // Application.onCreate()
 * LogHelper.init(isDebug = BuildConfig.DEBUG)
 * 
 * // 运行时覆盖（如测试人员临时开启生产包日志）
 * LogHelper.updateDebugMode(true)
 * ```
 */
object LogHelper {
    private const val PRIORITY_DEBUG = 3
    private const val PRIORITY_INFO = 4
    private const val PRIORITY_WARN = 5
    private const val PRIORITY_ERROR = 6
    private const val ROOT_TAG = "Snap"

    /**
     * 当前调试模式状态。
     * - true：输出日志到 Logcat + 上报到 reporter
     * - false：仅上报到 reporter（不输出 Logcat）
     */
    var isDebug: Boolean = true
        private set
    
    /**
     * 可选的消息转换器（用于脱敏、格式化等）。
     * 在日志输出前对 message 进行处理。
     */
    var messageTransformer: ((String) -> String)? = null
    
    /**
     * 可选的日志上报器（用于崩溃收集、远程日志）。
     * 无论 isDebug 状态，所有日志都会调用此回调。
     */
    var reporter: ((level: String, tag: String, message: String, throwable: Throwable?) -> Unit)? = null

    /**
     * 初始化日志系统（Application.onCreate() 调用）。
     * 
     * @param isDebug 是否为调试模式（通常为 BuildConfig.DEBUG）
     */
    fun init(isDebug: Boolean) {
        this.isDebug = isDebug
        replantTrees()
    }

    /**
     * 运行时更新调试模式。
     * 
     * 使用场景：
     * - 生产包中，测试人员通过隐藏入口临时开启日志
     * - 开发包中，关闭日志减少噪音
     * 
     * @param enabled true = 开启 Logcat 输出，false = 关闭 Logcat 输出
     */
    fun updateDebugMode(enabled: Boolean) {
        if (isDebug == enabled) return
        isDebug = enabled
        replantTrees()
        i("LogHelper", "Debug mode updated: isDebug=$enabled")
    }

    /**
     * 重新种植 Timber 树。
     * - Debug 模式：DebugTree（输出到 Logcat）
     * - Release 模式：ReportOnlyTree（仅调用 reporter，不输出 Logcat）
     */
    private fun replantTrees() {
        Timber.uprootAll()
        if (isDebug) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReportOnlyTree())
        }
    }

    // ── 日志输出方法 ──────────────────────────────────────

    /** 调试日志（Debug 模式可见） */
    fun d(tag: String, message: String) = log("D", tag, message, null)
    
    /** 信息日志（Debug 模式可见） */
    fun i(tag: String, message: String) = log("I", tag, message, null)
    
    /** 警告日志（Debug 模式可见，Release 仅上报） */
    fun w(tag: String, message: String, throwable: Throwable? = null) = log("W", tag, message, throwable)
    
    /** 错误日志（Debug 模式可见，Release 仅上报） */
    fun e(tag: String, message: String, throwable: Throwable? = null) = log("E", tag, message, throwable)

    /**
     * 统一日志处理。
     * 1. 应用 messageTransformer（脱敏、格式化）
     * 2. 添加模块标签前缀
     * 3. Debug 模式：输出到 Logcat
     * 4. 调用 reporter（无论 Debug 与否）
     */
    private fun log(level: String, tag: String, message: String, throwable: Throwable?) {
        val transformed = messageTransformer?.invoke(message) ?: message
        val reportTag = tag.ifBlank { ROOT_TAG }
        val moduleMessage = if (tag.isBlank()) transformed else "[$reportTag] $transformed"
        
        // Debug 模式输出到 Logcat
        if (isDebug) {
            when (level) {
                "D" -> Timber.tag(ROOT_TAG).d(throwable, moduleMessage)
                "I" -> Timber.tag(ROOT_TAG).i(throwable, moduleMessage)
                "W" -> Timber.tag(ROOT_TAG).w(throwable, moduleMessage)
                "E" -> Timber.tag(ROOT_TAG).e(throwable, moduleMessage)
                else -> Timber.tag(ROOT_TAG).v(throwable, moduleMessage)
            }
        }
        
        // 无论 Debug 与否，都上报到 reporter（用于崩溃收集）
        reporter?.invoke(level, reportTag, transformed, throwable)
    }

    /**
     * Release 模式专用的 Timber Tree。
     * 不输出到 Logcat，仅调用 reporter 上报。
     */
    private class ReportOnlyTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            val level = when (priority) {
                PRIORITY_DEBUG -> "D"
                PRIORITY_INFO -> "I"
                PRIORITY_WARN -> "W"
                PRIORITY_ERROR -> "E"
                else -> "V"
            }
            reporter?.invoke(level, tag ?: ROOT_TAG, message, t)
        }
    }
}
