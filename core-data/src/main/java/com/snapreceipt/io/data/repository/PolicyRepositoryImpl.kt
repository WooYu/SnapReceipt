package com.snapreceipt.io.data.repository

import com.snapreceipt.io.data.local.datasource.PolicyLocalDataSource
import com.snapreceipt.io.data.network.datasource.ConfigRemoteDataSource
import com.snapreceipt.io.data.network.model.config.toEntity
import com.snapreceipt.io.domain.model.PolicyEntity
import com.snapreceipt.io.domain.repository.PolicyRepository
import com.skybound.space.core.network.NetworkResult
import com.skybound.space.core.network.toApiException
import javax.inject.Inject

class PolicyRepositoryImpl @Inject constructor(
    private val remoteDataSource: ConfigRemoteDataSource,
    private val localDataSource: PolicyLocalDataSource
) : PolicyRepository {
    override suspend fun fetchPolicy(): PolicyEntity {
        val cached = localDataSource.getPolicySync()
        return when (val result = remoteDataSource.fetchPolicy()) {
            is NetworkResult.Success -> {
                val entity = result.data.toEntity()
                localDataSource.updatePolicy(entity)
                entity
            }
            is NetworkResult.Failure -> cached ?: throw result.error.toApiException()
        }
    }
}
