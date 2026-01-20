package com.snapreceipt.io.ui.home

import android.os.Bundle
import androidx.lifecycle.viewModelScope
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.model.ReceiptCategory
import com.snapreceipt.io.domain.model.ReceiptUpdateEntity
import com.snapreceipt.io.domain.usecase.receipt.DeleteReceiptRemoteUseCase
import com.snapreceipt.io.domain.usecase.receipt.FetchReceiptsUseCase
import com.snapreceipt.io.domain.usecase.receipt.UpdateReceiptRemoteUseCase
import com.snapreceipt.io.domain.usecase.receipt.UploadAndScanReceiptUseCase
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
    private val fetchReceiptsUseCase: FetchReceiptsUseCase,
    private val updateReceiptRemoteUseCase: UpdateReceiptRemoteUseCase,
    private val deleteReceiptRemoteUseCase: DeleteReceiptRemoteUseCase,
    private val uploadAndScanReceiptUseCase: UploadAndScanReceiptUseCase,
    private val dispatchers: CoroutineDispatchersProvider
) : BaseViewModel(dispatchers) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var receiptsJob: Job? = null

    init {
        loadReceipts()
    }

    fun loadReceipts() {
        receiptsJob?.cancel()
        _uiState.update { it.copy(loading = true, error = null) }
        receiptsJob = viewModelScope.launch(dispatchers.io) {
            fetchReceiptsUseCase()
                .onSuccess { receipts ->
                    _uiState.update { current ->
                        current.copy(
                            receipts = receipts,
                            loading = false,
                            error = null,
                            empty = receipts.isEmpty()
                        )
                    }
                }
                .onFailure { updateError(it) }
        }
    }

    fun deleteReceipt(receipt: ReceiptEntity) {
        viewModelScope.launch(dispatchers.io) {
            deleteReceiptRemoteUseCase(receipt.id.toLong())
                .onSuccess { loadReceipts() }
                .onFailure { updateError(it) }
        }
    }

    fun insertReceipt(receipt: ReceiptEntity) {
        // No-op: remote save happens in InvoiceDetails.
    }

    fun updateReceipt(receipt: ReceiptEntity) {
        viewModelScope.launch(dispatchers.io) {
            updateReceiptRemoteUseCase(receipt.toUpdateEntity())
                .onSuccess { loadReceipts() }
                .onFailure { updateError(it) }
        }
    }

    fun processCroppedImage(imagePath: String) {
        viewModelScope.launch(dispatchers.io) {
            emitEvent(UiEvent.Toast(message = "正在解析收据，请稍候…", long = true))
            _uiState.update { it.copy(loading = true, error = null) }
            uploadAndScanReceiptUseCase(imagePath)
                .onSuccess { scan ->
                    _uiState.update { it.copy(loading = false) }
                    emitEvent(
                        UiEvent.Custom(
                            HomeEventKeys.PREFILL_READY,
                            Bundle().apply {
                                putString(HomeEventKeys.EXTRA_IMAGE_PATH, imagePath)
                                putString(HomeEventKeys.EXTRA_IMAGE_URL, scan.receiptUrl.orEmpty())
                                putString(HomeEventKeys.EXTRA_MERCHANT, scan.merchant.orEmpty())
                                putString(HomeEventKeys.EXTRA_AMOUNT, scan.totalAmount?.toString().orEmpty())
                                putString(HomeEventKeys.EXTRA_DATE, scan.receiptDate.orEmpty())
                                putString(HomeEventKeys.EXTRA_TIME, scan.receiptTime.orEmpty())
                                putString(HomeEventKeys.EXTRA_TIP_AMOUNT, scan.tipAmount?.toString().orEmpty())
                                putString(HomeEventKeys.EXTRA_CARD, scan.paymentCardNo.orEmpty())
                                putString(HomeEventKeys.EXTRA_CONSUMER, scan.consumer.orEmpty())
                                putString(HomeEventKeys.EXTRA_REMARK, scan.remark.orEmpty())
                            }
                        )
                    )
                }
                .onFailure { throwable ->
                    _uiState.update { state ->
                        state.copy(loading = false, error = throwable.message)
                    }
                    emitEvent(UiEvent.Custom(HomeEventKeys.SCAN_FAILED))
                }
        }
    }

    private fun updateError(throwable: Throwable) {
        _uiState.update { it.copy(loading = false, error = throwable.message ?: "Unexpected error") }
        handleError(throwable)
    }

    private fun ReceiptEntity.toUpdateEntity(): ReceiptUpdateEntity {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return ReceiptUpdateEntity(
            receiptId = id.toLong(),
            merchant = merchantName,
            receiptDate = dateFormat.format(java.util.Date(date)),
            receiptTime = timeFormat.format(java.util.Date(date)),
            totalAmount = amount,
            tipAmount = 0.0,
            paymentCardNo = "",
            consumer = "",
            remark = description,
            receiptUrl = imagePath,
            categoryId = ReceiptCategory.idForLabel(category)
        )
    }
}
