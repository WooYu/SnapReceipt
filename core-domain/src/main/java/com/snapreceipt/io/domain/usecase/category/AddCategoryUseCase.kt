package com.snapreceipt.io.domain.usecase.category

import com.snapreceipt.io.domain.repository.ReceiptRemoteRepository
import javax.inject.Inject

/**
 * 新增分类用例：对外暴露一个简单入口，封装远端新增分类调用。
 */
class AddCategoryUseCase @Inject constructor(
    private val repository: ReceiptRemoteRepository
) {
    /**
     * 提交分类名称并返回结果。
     *
     * @param name 分类名称
     */
    suspend operator fun invoke(name: String): Result<Unit> =
        runCatching { repository.addCategory(name) }
}
