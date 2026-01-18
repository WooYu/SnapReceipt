package com.snapreceipt.io.domain.model

data class AuthTokensEntity(
    val accessToken: String,
    val refreshToken: String
)
