package com.snapreceipt.io.ui.home

import android.os.Bundle
import androidx.lifecycle.viewModelScope
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.usecase.ocr.BuildReceiptPrefillUseCase
import com.snapreceipt.io.domain.usecase.receipt.DeleteReceiptUseCase
import com.snapreceipt.io.domain.usecase.receipt.GetReceiptsUseCase
import com.snapreceipt.io.domain.usecase.receipt.InsertReceiptUseCase
import com.snapreceipt.io.domain.usecase.receipt.UpdateReceiptUseCase
import com.skybound.space.base.presentation.UiEvent
import com.skybound.space.base.presentation.viewmodel.BaseViewModel
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getReceiptsUseCase: GetReceiptsUseCase,
    private val insertReceiptUseCase: InsertReceiptUseCase,
    private val updateReceiptUseCase: UpdateReceiptUseCase,
    private val deleteReceiptUseCase: DeleteReceiptUseCase,
    private val buildReceiptPrefillUseCase: BuildReceiptPrefillUseCase,
    private val dispatchers: CoroutineDispatchersProvider
) : BaseViewModel(dispatchers) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var receiptsJob: Job? = null
    private var hasSeededSampleData = false

    init {
        loadReceipts()
    }

    fun loadReceipts() {
        receiptsJob?.cancel()
        _uiState.update { it.copy(loading = true, error = null) }
        receiptsJob = viewModelScope.launch(dispatchers.io) {
            getReceiptsUseCase()
                .collect { result ->
                    result.onSuccess { receipts ->
                        _uiState.update { current ->
                            current.copy(
                                receipts = receipts,
                                loading = false,
                                error = null,
                                empty = receipts.isEmpty()
                            )
                        }
                        if (receipts.isEmpty()) {
                            seedSampleData(buildSampleReceipts())
                        }
                    }.onFailure { updateError(it) }
                }
        }
    }

    fun deleteReceipt(receipt: ReceiptEntity) {
        runOperation { deleteReceiptUseCase(receipt) }
    }

    fun insertReceipt(receipt: ReceiptEntity) {
        runOperation { insertReceiptUseCase(receipt) }
    }

    fun updateReceipt(receipt: ReceiptEntity) {
        runOperation { updateReceiptUseCase(receipt) }
    }

    fun processCroppedImage(imagePath: String) {
        viewModelScope.launch(dispatchers.io) {
            buildReceiptPrefillUseCase(imagePath)
                .onSuccess { prefill ->
                    emitEvent(
                        UiEvent.Custom(
                            HomeEventKeys.PREFILL_READY,
                            Bundle().apply {
                                putString(HomeEventKeys.EXTRA_IMAGE_PATH, prefill.imagePath)
                                putString(HomeEventKeys.EXTRA_MERCHANT, prefill.merchant)
                                putString(HomeEventKeys.EXTRA_AMOUNT, prefill.amount)
                            }
                        )
                    )
                }
                .onFailure { updateError(it) }
        }
    }

    private fun runOperation(block: suspend () -> Result<*>) {
        viewModelScope.launch(dispatchers.io) {
            block().onFailure { updateError(it) }
        }
    }

    private fun seedSampleData(receipts: List<ReceiptEntity>) {
        if (hasSeededSampleData || receipts.isEmpty()) return
        hasSeededSampleData = true
        viewModelScope.launch(dispatchers.io) {
            receipts.forEach { receipt ->
                insertReceiptUseCase(receipt).onFailure { updateError(it) }
            }
        }
    }

    private fun buildSampleReceipts(): List<ReceiptEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            ReceiptEntity(
                merchantName = "Starbucks Coffee",
                amount = 45.50,
                date = now,
                invoiceType = "normal",
                category = "Food & Beverage"
            ),
            ReceiptEntity(
                merchantName = "Apple Store",
                amount = 899.99,
                date = now - 86400000,
                invoiceType = "normal",
                category = "Electronics"
            ),
            ReceiptEntity(
                merchantName = "Gas Station",
                amount = 67.80,
                date = now - 172800000,
                invoiceType = "normal",
                category = "Transportation"
            )
        )
    }

    private fun updateError(throwable: Throwable) {
        _uiState.update { it.copy(loading = false, error = throwable.message ?: "Unexpected error") }
        handleError(throwable)
    }
}
