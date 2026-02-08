package com.snapreceipt.io.domain.repository

interface FeedbackRepository {
    suspend fun submitFeedback(contact: String, content: String)
}
