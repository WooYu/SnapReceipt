package com.snapreceipt.io.data.repository

import com.snapreceipt.io.data.base.BaseRepository
import com.snapreceipt.io.data.local.datasource.UserLocalDataSource
import com.snapreceipt.io.domain.model.UserEntity
import com.snapreceipt.io.domain.repository.UserRepository
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val localDataSource: UserLocalDataSource,
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
}
