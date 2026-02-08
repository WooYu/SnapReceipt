package com.snapreceipt.io.data.repository

import com.skybound.space.core.network.getOrThrow
import com.snapreceipt.io.data.network.datasource.FeedbackRemoteDataSource
import com.snapreceipt.io.data.network.model.feedback.FeedbackRequestDto
import com.snapreceipt.io.domain.repository.FeedbackRepository
import javax.inject.Inject

class FeedbackRepositoryImpl @Inject constructor(
    private val remoteDataSource: FeedbackRemoteDataSource
) : FeedbackRepository {
    override suspend fun submitFeedback(contact: String, content: String) {
        val request = FeedbackRequestDto(contact = contact, content = content)
        remoteDataSource.submitFeedback(request).getOrThrow()
    }
}
