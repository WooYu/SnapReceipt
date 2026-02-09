package com.snapreceipt.io.ui.invoice

import com.snapreceipt.io.R
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.usecase.category.ResolveCategoryIdUseCase
import com.snapreceipt.io.domain.usecase.receipt.DeleteReceiptRemoteUseCase
import com.snapreceipt.io.domain.usecase.receipt.SaveReceiptRemoteUseCase
import com.snapreceipt.io.domain.usecase.receipt.UpdateReceiptRemoteUseCase
import com.skybound.space.base.presentation.UiEvent
import com.skybound.space.base.presentation.viewmodel.BaseViewModel
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class InvoiceDetailsViewModel @Inject constructor(
    private val saveReceiptRemoteUseCase: SaveReceiptRemoteUseCase,
    private val updateReceiptRemoteUseCase: UpdateReceiptRemoteUseCase,
    private val deleteReceiptRemoteUseCase: DeleteReceiptRemoteUseCase,
    private val resolveCategoryIdUseCase: ResolveCategoryIdUseCase,
    private val dispatchers: CoroutineDispatchersProvider
) : BaseViewModel(dispatchers, R.string.unexpected_error) {

    private val _uiState = MutableStateFlow(InvoiceDetailsUiState())
    val uiState: StateFlow<InvoiceDetailsUiState> = _uiState.asStateFlow()

    fun saveReceipt(receipt: ReceiptEntity) {
        launchWithLoading(
            updateLoading = { loading -> _uiState.update { it.copy(loading = loading, error = if (loading) null else it.error) } },
            block = { submitReceipt(receipt, saveReceiptRemoteUseCase::invoke) },
            onSuccess = {
                _uiState.update { it.copy(loading = false) }
                emitEvent(UiEvent.Custom(InvoiceDetailsEventKeys.SHOW_SUCCESS))
                emitEvent(UiEvent.Custom(InvoiceDetailsEventKeys.NAVIGATE_TO_MAIN))
            },
            onFailure = ::handleSubmitFailure
        )
    }

    fun updateReceipt(receipt: ReceiptEntity) {
        launchWithLoading(
            updateLoading = { loading -> _uiState.update { it.copy(loading = loading, error = if (loading) null else it.error) } },
            block = { submitReceipt(receipt, updateReceiptRemoteUseCase::invoke) },
            onSuccess = {
                _uiState.update { it.copy(loading = false) }
                emitEvent(UiEvent.Custom(InvoiceDetailsEventKeys.SHOW_SUCCESS))
                emitEvent(UiEvent.Custom(InvoiceDetailsEventKeys.NAVIGATE_TO_MAIN))
            },
            onFailure = ::handleSubmitFailure
        )
    }

    fun deleteReceipt(receiptId: Long) {
        launchWithLoading(
            updateLoading = { loading -> _uiState.update { it.copy(loading = loading, error = if (loading) null else it.error) } },
            block = { deleteReceiptRemoteUseCase(receiptId) },
            onSuccess = {
                _uiState.update { it.copy(loading = false) }
                emitEvent(UiEvent.Custom(InvoiceDetailsEventKeys.SHOW_SUCCESS))
                emitEvent(UiEvent.Custom(InvoiceDetailsEventKeys.NAVIGATE_TO_MAIN))
            },
            onFailure = { updateError(it) }
        )
    }

    private fun updateError(throwable: Throwable) {
        _uiState.update { it.copy(loading = false, error = throwable.message) }
        handleError(throwable)
    }

    private suspend fun submitReceipt(
        receipt: ReceiptEntity,
        remoteSubmit: suspend (ReceiptEntity) -> Result<Unit>
    ): Result<Unit> {
        val normalizedLabel = receipt.categoryName.orEmpty().trim()
        if (normalizedLabel.isBlank()) {
            return Result.failure(CategoryRequiredException())
        }

        val categoryId = receipt.categoryId?.takeIf { it > 0L }
            ?: resolveCategoryIdUseCase(normalizedLabel).takeIf { it > 0L }
            ?: return Result.failure(CategoryRequiredException())

        val request = receipt.copy(
            categoryId = categoryId,
            categoryName = normalizedLabel
        )
        return remoteSubmit(request)
    }

    private fun handleSubmitFailure(throwable: Throwable) {
        if (throwable is CategoryRequiredException) {
            _uiState.update { it.copy(loading = false, error = null) }
            emitEvent(UiEvent.Toast(message = "", resId = R.string.select_invoice_category))
            return
        }
        updateError(throwable)
    }

    private class CategoryRequiredException : IllegalStateException("category_required")
}
