package com.snapreceipt.io.data.network.datasource

import com.skybound.space.core.dispatcher.DispatchersProvider
import com.skybound.space.core.network.NetworkResult
import com.skybound.space.core.network.safeApiCall
import com.snapreceipt.io.data.network.model.UploadUrlRequest
import com.snapreceipt.io.data.network.model.UploadUrlResponse
import com.snapreceipt.io.data.network.service.FileApi

class FileRemoteDataSource(
    private val api: FileApi,
    private val dispatchers: DispatchersProvider
) {
    suspend fun requestUploadUrl(fileName: String): NetworkResult<UploadUrlResponse> {
        return safeApiCall(dispatchers) { api.requestUploadUrl(UploadUrlRequest(fileName)) }
    }
}
