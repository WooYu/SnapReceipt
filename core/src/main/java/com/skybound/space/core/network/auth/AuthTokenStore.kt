package com.skybound.space.core.network.auth

/**
 * 认证信息存储接口，默认提供内存实现，应用层可替换为 DataStore 等。
 */
interface AuthTokenStore {
    fun accessToken(): String?
    fun refreshToken(): String?
    fun update(accessToken: String?, refreshToken: String?)
    fun clear()
}
