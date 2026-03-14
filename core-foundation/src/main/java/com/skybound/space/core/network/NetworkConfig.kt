package com.skybound.space.core.network

/**
 * 网络层基础配置。
 *
 * 这些值会在创建 Retrofit、OkHttpClient 以及导出等特殊请求时统一复用，
 * 因此适合作为“环境配置入口”，而不是散落在各个模块里单独维护。
 */
data class NetworkConfig(
    // 业务接口根地址；内部会统一去掉结尾 `/`，避免拼接路径时出现 `//`。
    val baseUrl: String,
    // 建连超时，控制 DNS、TCP/TLS 建立阶段最长等待时间。
    val connectTimeoutSec: Long = 10,
    // 读超时，限制服务端响应体读取阶段的最长等待时间。
    val readTimeoutSec: Long = 20,
    // 写超时，主要影响大请求体上传时的发送等待时间。
    val writeTimeoutSec: Long = 20,
    // 是否输出调试日志；通常跟随运行时 Debug 开关动态生效。
    val enableLogging: Boolean = false,
    // 所有请求默认附带的公共请求头，例如 Accept、灰度标识等。
    val defaultHeaders: Map<String, String> = emptyMap(),
    // 导出等长耗时接口的单独超时配置，避免沿用普通接口的默认限制。
    val exportTimeoutSec: Long = 60
)
