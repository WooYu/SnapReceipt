package com.skybound.space.core.config

/**
 * 应用全局配置
 * 
 * 职责：
 * - 提供全局可访问的配置项（baseUrl、isDebug 等）
 * - 支持编译期初始化 + 运行时动态更新
 * 
 * 初始化：
 * ```kotlin
 * // Application.onCreate()
 * AppConfig.init(isDebug = BuildConfig.DEBUG)
 * 
 * // 运行时切换环境（如测试人员切换到测试服）
 * AppConfig.updateBaseUrl("https://test-api.snapreceipt.io/")
 * 
 * // 运行时开启调试（如生产包临时排查问题）
 * AppConfig.updateDebugMode(true)
 * ```
 * 
 * 线程安全：
 * - 使用 @Volatile 确保多线程可见性
 * - 写操作通常在主线程（Application.onCreate() / 设置页）
 */
object AppConfig {
    /** 默认 API base URL（生产环境） */
    const val DEFAULT_BASE_URL = "https://api.snapreceipt.io/"

    /**
     * 当前 API base URL。
     * 可通过 [updateBaseUrl] 动态切换（如切换到测试环境）。
     */
    @Volatile
    var baseUrl: String = DEFAULT_BASE_URL
        private set

    /**
     * 当前调试模式状态。
     * - true：开发环境（输出详细日志、显示调试工具、关闭混淆）
     * - false：生产环境（关闭日志、启用混淆、优化性能）
     * 
     * 可通过 [updateDebugMode] 运行时覆盖。
     */
    @Volatile
    var isDebug: Boolean = false
        private set

    /**
     * 初始化全局配置（Application.onCreate() 调用）。
     * 
     * @param isDebug 是否为调试模式（通常为 BuildConfig.DEBUG 或 DiEnvironment.DEV）
     * @param baseUrl API base URL（默认为生产环境）
     */
    fun init(isDebug: Boolean, baseUrl: String = DEFAULT_BASE_URL) {
        this.isDebug = isDebug
        this.baseUrl = baseUrl
    }

    /**
     * 运行时更新调试模式。
     * 
     * 使用场景：
     * - 生产包中，测试人员通过隐藏设置页临时开启调试
     * - 动态响应 AppSettings.enableDebugLogging 变化
     * 
     * 注意：仅影响 AppConfig.isDebug 标志位，不影响：
     * - LogHelper（需单独调用 LogHelper.updateDebugMode）
     * - NetworkConfig.enableLogging（由 NetworkManager 初始化时决定，后续不可改）
     */
    fun updateDebugMode(enabled: Boolean) {
        isDebug = enabled
    }

    /**
     * 运行时更新 API base URL。
     * 
     * 使用场景：
     * - 开发人员切换到测试环境
     * - 动态配置（通过远程配置切换 API 服务器）
     * 
     * 注意：已创建的 Retrofit/OkHttpClient 实例不受影响，需重启 App 生效。
     */
    fun updateBaseUrl(url: String) {
        baseUrl = url.trimEnd('/')
    }
}
