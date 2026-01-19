package com.snapreceipt.io.domain.repository

import com.snapreceipt.io.domain.model.UploadUrlEntity

interface FileRepository {
    suspend fun requestUploadUrl(fileName: String): UploadUrlEntity
    suspend fun uploadFile(uploadUrl: String, filePath: String, contentType: String = "image/jpeg")
}
