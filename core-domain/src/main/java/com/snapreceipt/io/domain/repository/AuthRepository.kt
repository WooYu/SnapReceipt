package com.snapreceipt.io.domain.repository

import com.snapreceipt.io.domain.model.AuthTokensEntity
import com.snapreceipt.io.domain.model.UserEntity

interface AuthRepository {
    suspend fun requestCode(target: String)
    suspend fun login(target: String, code: String, timezone: String): AuthTokensEntity
    suspend fun fetchUserProfile(): UserEntity
}
