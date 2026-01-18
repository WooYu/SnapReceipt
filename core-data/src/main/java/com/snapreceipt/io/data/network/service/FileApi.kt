package com.snapreceipt.io.data.network.service

import com.skybound.space.core.network.BaseResponse
import com.snapreceipt.io.data.network.model.UploadUrlRequestDto
import com.snapreceipt.io.data.network.model.UploadUrlResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface FileApi {
    @POST("api/file/upload")
    suspend fun requestUploadUrl(@Body request: UploadUrlRequestDto): BaseResponse<UploadUrlResponseDto>
}
