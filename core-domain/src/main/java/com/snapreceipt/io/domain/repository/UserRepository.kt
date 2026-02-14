package com.snapreceipt.io.domain.repository

import com.snapreceipt.io.domain.model.UserEntity
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUser(): Flow<UserEntity?>
    suspend fun getUserSync(): UserEntity?
    suspend fun updateUser(user: UserEntity)
    suspend fun insertUser(user: UserEntity)
    suspend fun requestProfileUpdateCode(target: String)
    suspend fun updateNickName(nickName: String)
    suspend fun updateEmail(email: String, code: String)
    suspend fun updatePhone(phoneNumber: String, code: String)
}
