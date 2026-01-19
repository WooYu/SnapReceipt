package com.snapreceipt.io.domain.usecase.file

import com.snapreceipt.io.domain.model.UploadUrlEntity
import com.snapreceipt.io.domain.repository.FileRepository
import javax.inject.Inject

class RequestUploadUrlUseCase @Inject constructor(
    private val repository: FileRepository
) {
    suspend operator fun invoke(fileName: String): Result<UploadUrlEntity> =
        runCatching { repository.requestUploadUrl(fileName) }
}
