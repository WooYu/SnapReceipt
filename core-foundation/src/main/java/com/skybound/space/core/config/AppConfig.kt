package com.skybound.space.core.config

/**
 * 进程级应用配置。
 *
 * 这里保存的是“运行时最终生效”的基础环境参数，
 * 例如 baseUrl 和 debug 开关。它们会在应用启动和设置变更时被更新，
 * 供网络层、日志层等多个模块统一读取。
 */
object AppConfig {
    // 默认线上地址，未显式切换环境时全部回落到该值。
    const val DEFAULT_BASE_URL = "https://api.snapreceipt.io"

    // 当前业务接口根地址，允许在调试场景被运行时覆盖。
    @Volatile
    var baseUrl: String = DEFAULT_BASE_URL
        private set

    // 当前是否输出调试日志；会受编译环境和运行时设置共同影响。
    @Volatile
    var isDebug: Boolean = false
        private set

    /**
     * 应用启动时的统一初始化入口。
     *
     * 这里会把传入的地址做一次标准化，避免后续 URL 拼接出现结尾斜杠差异。
     */
    fun init(isDebug: Boolean, baseUrl: String = DEFAULT_BASE_URL) {
        this.isDebug = isDebug
        this.baseUrl = normalizeBaseUrl(baseUrl)
    }

    // 运行时切换日志调试开关时使用，不会影响其他配置。
    fun updateDebugMode(enabled: Boolean) {
        isDebug = enabled
    }

    // 运行时切换服务端地址时使用，主要服务于联调或环境切换。
    fun updateBaseUrl(url: String) {
        baseUrl = normalizeBaseUrl(url)
    }

    // 统一去掉末尾 `/`，让上层拼接路径时不必重复处理。
    private fun normalizeBaseUrl(url: String): String = url.trimEnd('/')
}
