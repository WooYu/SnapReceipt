package com.snapreceipt.io.domain.usecase.category

import com.snapreceipt.io.domain.model.ReceiptCategory
import com.snapreceipt.io.domain.repository.ReceiptRemoteRepository
import javax.inject.Inject

class FetchCategoriesUseCase @Inject constructor(
    private val repository: ReceiptRemoteRepository
) {
    suspend operator fun invoke(): Result<List<ReceiptCategory.Item>> =
        runCatching { repository.listCategories() }
}
