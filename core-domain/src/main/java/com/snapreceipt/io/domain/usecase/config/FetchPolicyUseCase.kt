package com.snapreceipt.io.domain.usecase.config

import com.snapreceipt.io.domain.model.PolicyEntity
import com.snapreceipt.io.domain.repository.PolicyRepository
import javax.inject.Inject

class FetchPolicyUseCase @Inject constructor(
    private val repository: PolicyRepository
) {
    suspend operator fun invoke(): Result<PolicyEntity> =
        runCatching { repository.fetchPolicy() }
}
