package com.snapreceipt.io.data.network.service

import com.skybound.space.core.network.ApiResponse
import com.snapreceipt.io.data.network.model.UploadUrlRequest
import com.snapreceipt.io.data.network.model.UploadUrlResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface FileApi {
    @POST("api/file/upload")
    suspend fun requestUploadUrl(@Body request: UploadUrlRequest): ApiResponse<UploadUrlResponse>
}
