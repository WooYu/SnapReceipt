package com.snapreceipt.io.data.network.datasource

import com.snapreceipt.io.data.base.BaseRemoteDataSource
import com.snapreceipt.io.data.network.model.config.PolicyConfigDto
import com.snapreceipt.io.data.network.service.ConfigApi
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.skybound.space.core.network.NetworkResult

class ConfigRemoteDataSource(
    private val api: ConfigApi,
    dispatchers: CoroutineDispatchersProvider
) : BaseRemoteDataSource(dispatchers) {
    suspend fun fetchPolicy(): NetworkResult<PolicyConfigDto> {
        return request { api.fetchPolicy() }
    }
}
