package com.snapreceipt.io.data.repository

import com.snapreceipt.io.data.local.datasource.PolicyLocalDataSource
import com.snapreceipt.io.data.network.datasource.ConfigRemoteDataSource
import com.snapreceipt.io.data.network.model.config.toEntity
import com.snapreceipt.io.domain.model.PolicyEntity
import com.snapreceipt.io.domain.repository.PolicyRepository
import com.skybound.space.core.network.getOrThrow
import javax.inject.Inject

class PolicyRepositoryImpl @Inject constructor(
    private val remoteDataSource: ConfigRemoteDataSource,
    private val localDataSource: PolicyLocalDataSource
) : PolicyRepository {
    override suspend fun fetchPolicy(): PolicyEntity {
        val cached = localDataSource.getPolicySync()
        val remoteEntity = runCatching {
            remoteDataSource.fetchPolicy().getOrThrow().toEntity()
        }.getOrElse { throwable ->
            cached ?: throw throwable
        }
        localDataSource.updatePolicy(remoteEntity)
        return remoteEntity
    }
}
