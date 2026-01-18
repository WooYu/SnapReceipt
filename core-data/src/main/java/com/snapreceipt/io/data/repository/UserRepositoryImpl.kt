package com.snapreceipt.io.data.repository

import com.snapreceipt.io.data.base.BaseRepository
import com.snapreceipt.io.data.local.datasource.UserLocalDataSource
import com.snapreceipt.io.data.mapper.UserDomainToEntityMapper
import com.snapreceipt.io.data.mapper.UserEntityToDomainMapper
import com.snapreceipt.io.domain.model.UserEntity
import com.snapreceipt.io.domain.repository.UserRepository
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val localDataSource: UserLocalDataSource,
    private val entityToDomainMapper: UserEntityToDomainMapper,
    private val domainToEntityMapper: UserDomainToEntityMapper,
    dispatchers: CoroutineDispatchersProvider
) : BaseRepository(dispatchers), UserRepository {
    override fun getUser(): Flow<UserEntity?> {
        return localDataSource.getUser().map { entity -> entity?.let { entityToDomainMapper.map(it) } }
    }

    override suspend fun getUserSync(): UserEntity? {
        return localDataSource.getUserSync()?.let { entityToDomainMapper.map(it) }
    }

    override suspend fun updateUser(user: UserEntity) {
        localDataSource.updateUser(domainToEntityMapper.map(user))
    }

    override suspend fun insertUser(user: UserEntity) {
        localDataSource.insertUser(domainToEntityMapper.map(user))
    }
}
