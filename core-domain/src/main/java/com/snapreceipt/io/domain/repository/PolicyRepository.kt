package com.snapreceipt.io.domain.repository

import com.snapreceipt.io.domain.model.PolicyEntity

interface PolicyRepository {
    suspend fun fetchPolicy(): PolicyEntity
}
