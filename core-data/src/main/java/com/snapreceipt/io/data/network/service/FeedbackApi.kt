package com.snapreceipt.io.data.network.service

import com.skybound.space.core.network.BaseEmptyResponse
import com.snapreceipt.io.data.network.model.feedback.FeedbackRequestDto
import retrofit2.http.Body
import retrofit2.http.POST

interface FeedbackApi {
    @POST("api/feedback/submit")
    suspend fun submitFeedback(@Body request: FeedbackRequestDto): BaseEmptyResponse
}
