package com.snapreceipt.io.data.repository

import com.snapreceipt.io.data.network.datasource.FileRemoteDataSource
import com.snapreceipt.io.data.network.datasource.UploadRemoteDataSource
import com.snapreceipt.io.domain.model.UploadUrlEntity
import com.snapreceipt.io.domain.repository.FileRepository
import com.skybound.space.core.network.getOrThrow
import java.io.File
import javax.inject.Inject

class FileRepositoryImpl @Inject constructor(
    private val fileRemoteDataSource: FileRemoteDataSource,
    private val uploadRemoteDataSource: UploadRemoteDataSource
) : FileRepository {
    override suspend fun requestUploadUrl(fileName: String): UploadUrlEntity =
        fileRemoteDataSource.requestUploadUrl(fileName).getOrThrow()

    override suspend fun uploadFile(uploadUrl: String, filePath: String, contentType: String) {
        uploadRemoteDataSource.uploadFile(uploadUrl, File(filePath), contentType).getOrThrow()
    }
}
