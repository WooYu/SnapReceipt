package com.snapreceipt.io.domain.usecase.file

import com.snapreceipt.io.domain.repository.FileRepository
import javax.inject.Inject

class UploadFileUseCase @Inject constructor(
    private val repository: FileRepository
) {
    suspend operator fun invoke(
        uploadUrl: String,
        filePath: String,
        contentType: String = "image/jpeg"
    ): Result<Unit> = runCatching {
        repository.uploadFile(uploadUrl, filePath, contentType)
    }
}
