package com.snapreceipt.io.data.network.datasource

import com.snapreceipt.io.data.base.BaseRemoteDataSource
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.skybound.space.core.network.NetworkResult
import com.snapreceipt.io.data.network.model.UploadUrlRequestDto
import com.snapreceipt.io.domain.model.UploadUrlEntity
import com.snapreceipt.io.data.network.service.FileApi

class FileRemoteDataSource(
    private val api: FileApi,
    dispatchers: CoroutineDispatchersProvider
) : BaseRemoteDataSource(dispatchers) {
    suspend fun requestUploadUrl(fileName: String): NetworkResult<UploadUrlEntity> {
        return request { api.requestUploadUrl(UploadUrlRequestDto(fileName)) }
    }
}
