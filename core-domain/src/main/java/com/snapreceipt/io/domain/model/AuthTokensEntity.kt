package com.snapreceipt.io.domain.model

/**
 * 登录/刷新得到的令牌。
 *
 * @property accessToken access_token（请求头 Authorization: Bearer xxx）
 * @property refreshToken refresh_token（用于刷新 access_token）
 */
data class AuthTokensEntity(
    val accessToken: String,
    val refreshToken: String
)
