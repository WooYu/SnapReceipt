package com.snapreceipt.io.data.repository

import com.snapreceipt.io.data.base.BaseRepository
import com.snapreceipt.io.data.local.datasource.UserLocalDataSource
import com.snapreceipt.io.data.network.datasource.AuthRemoteDataSource
import com.snapreceipt.io.data.network.datasource.UserRemoteDataSource
import com.snapreceipt.io.data.network.model.auth.toEntity
import com.snapreceipt.io.domain.model.UserEntity
import com.snapreceipt.io.domain.repository.UserRepository
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.skybound.space.core.network.getOrThrow
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val localDataSource: UserLocalDataSource,
    private val remoteDataSource: UserRemoteDataSource,
    private val authRemoteDataSource: AuthRemoteDataSource,
    dispatchers: CoroutineDispatchersProvider
) : BaseRepository(dispatchers), UserRepository {
    override fun getUser(): Flow<UserEntity?> {
        return localDataSource.getUser()
    }

    override suspend fun getUserSync(): UserEntity? {
        return localDataSource.getUserSync()
    }

    override suspend fun updateUser(user: UserEntity) {
        localDataSource.updateUser(user)
    }

    override suspend fun insertUser(user: UserEntity) {
        localDataSource.insertUser(user)
    }

    override suspend fun requestProfileUpdateCode(target: String) {
        remoteDataSource.requestProfileUpdateCode(target).getOrThrow()
    }

    override suspend fun updateNickName(nickName: String) {
        remoteDataSource.updateNickName(nickName).getOrThrow()
        localDataSource.getUserSync()?.let { localDataSource.updateUser(it.copy(username = nickName)) }
        refreshUserProfileSilently()
    }

    override suspend fun updateEmail(email: String, code: String) {
        remoteDataSource.updateEmail(email, code).getOrThrow()
        localDataSource.getUserSync()?.let { localDataSource.updateUser(it.copy(email = email)) }
        refreshUserProfileSilently()
    }

    override suspend fun updatePhone(phoneNumber: String, code: String) {
        remoteDataSource.updatePhone(phoneNumber, code).getOrThrow()
        localDataSource.getUserSync()?.let { localDataSource.updateUser(it.copy(phone = phoneNumber)) }
        refreshUserProfileSilently()
    }

    private suspend fun refreshUserProfileSilently() {
        runCatching {
            authRemoteDataSource.fetchUser().getOrThrow().toEntity()
        }.onSuccess { latest ->
            localDataSource.updateUser(latest)
        }
    }
}
