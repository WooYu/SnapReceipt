package com.snapreceipt.io.data.repository

import com.snapreceipt.io.data.network.datasource.FileRemoteDataSource
import com.snapreceipt.io.data.network.datasource.UploadRemoteDataSource
import com.snapreceipt.io.domain.model.UploadUrlEntity
import com.snapreceipt.io.domain.repository.FileRepository
import com.skybound.space.core.network.ApiException
import com.skybound.space.core.network.NetworkResult
import javax.inject.Inject

class FileRepositoryImpl @Inject constructor(
    private val fileRemoteDataSource: FileRemoteDataSource,
    private val uploadRemoteDataSource: UploadRemoteDataSource
) : FileRepository {
    override suspend fun requestUploadUrl(fileName: String): UploadUrlEntity {
        return when (val result = fileRemoteDataSource.requestUploadUrl(fileName)) {
            is NetworkResult.Success -> result.data
            is NetworkResult.Failure -> throw result.toApiException()
        }
    }

    override suspend fun uploadFile(uploadUrl: String, filePath: String, contentType: String) {
        when (val result = uploadRemoteDataSource.uploadFile(uploadUrl, java.io.File(filePath), contentType)) {
            is NetworkResult.Success -> Unit
            is NetworkResult.Failure -> throw result.toApiException()
        }
    }

    private fun NetworkResult.Failure.toApiException(): ApiException {
        return when (val failure = error) {
            is com.skybound.space.core.network.NetworkError.Http ->
                ApiException(failure.code, failure.message, failure.throwable)
            is com.skybound.space.core.network.NetworkError.Network ->
                ApiException(-1, failure.message, failure.throwable)
            is com.skybound.space.core.network.NetworkError.Serialization ->
                ApiException(-2, failure.message, failure.throwable)
            is com.skybound.space.core.network.NetworkError.Unexpected ->
                ApiException(-3, failure.message, failure.throwable)
        }
    }
}
