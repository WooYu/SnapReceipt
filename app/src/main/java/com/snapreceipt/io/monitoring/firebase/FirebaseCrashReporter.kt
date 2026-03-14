package com.snapreceipt.io.monitoring.firebase

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.skybound.space.core.monitoring.CrashReporter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Crashlytics 崩溃上报适配器。
 *
 * 负责把 core 层统一的崩溃监控接口转发到 Firebase Crashlytics，
 * 并在上报前附带关键元数据，方便线上问题按用户行为和上下文检索。
 */
@Singleton
class FirebaseCrashReporter @Inject constructor() : CrashReporter {

    // 开关只控制上报行为，不销毁底层实例，便于运行时快速切换。
    @Volatile
    private var enabled: Boolean = true

    // 仅在首次真正上报时初始化 Crashlytics，避免应用冷启动时做额外工作。
    private val crashlytics: FirebaseCrashlytics? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        runCatching { FirebaseCrashlytics.getInstance() }.getOrNull()
    }

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        runCatching { crashlytics?.setCrashlyticsCollectionEnabled(enabled) }
    }

    override fun recordException(throwable: Throwable, metadata: Map<String, String>) {
        if (!enabled) return
        runCatching {
            val delegate = crashlytics ?: return@runCatching
            // 自定义 key 会跟随异常一起进入后台，便于按模块、链路、接口名过滤问题。
            metadata.forEach { (key, value) ->
                delegate.setCustomKey(key, value)
            }
            delegate.recordException(throwable)
        }
    }

    override fun log(level: String, tag: String, message: String, throwable: Throwable?) {
        if (!enabled) return
        runCatching {
            val delegate = crashlytics ?: return@runCatching
            // 普通日志也会进入 Crashlytics 面包屑，方便还原异常发生前的上下文。
            delegate.log("[$level][$tag] $message")
            throwable?.let { delegate.log(it.stackTraceToString()) }
        }
    }
}
