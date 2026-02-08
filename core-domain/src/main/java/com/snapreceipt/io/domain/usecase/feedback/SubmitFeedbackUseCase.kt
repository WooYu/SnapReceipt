package com.snapreceipt.io.domain.usecase.feedback

import com.snapreceipt.io.domain.repository.FeedbackRepository
import javax.inject.Inject

class SubmitFeedbackUseCase @Inject constructor(
    private val repository: FeedbackRepository
) {
    suspend operator fun invoke(contact: String, content: String) =
        runCatching { repository.submitFeedback(contact, content) }
}
