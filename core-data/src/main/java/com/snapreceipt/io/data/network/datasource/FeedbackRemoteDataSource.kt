package com.snapreceipt.io.data.network.datasource

import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.skybound.space.core.network.BaseDataSource
import com.skybound.space.core.network.BaseEmptyResponse
import com.snapreceipt.io.data.network.model.feedback.FeedbackRequestDto
import com.snapreceipt.io.data.network.service.FeedbackApi
import javax.inject.Inject

class FeedbackRemoteDataSource @Inject constructor(
    private val api: FeedbackApi,
    dispatchers: CoroutineDispatchersProvider
) : BaseDataSource(dispatchers) {

    suspend fun submitFeedback(request: FeedbackRequestDto): BaseEmptyResponse = request {
        api.submitFeedback(request)
    }
}
