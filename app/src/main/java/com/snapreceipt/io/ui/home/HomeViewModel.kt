package com.snapreceipt.io.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snapreceipt.io.data.db.ReceiptEntity
import com.snapreceipt.io.data.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val receiptRepository: ReceiptRepository
) : ViewModel() {

    private val _receipts = MutableStateFlow<List<ReceiptEntity>>(emptyList())
    val receipts: StateFlow<List<ReceiptEntity>> = _receipts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var hasSeededSampleData = false

    init {
        loadReceipts()
    }

    private fun loadReceipts() {
        viewModelScope.launch {
            _isLoading.value = true
            receiptRepository.getAllReceipts().collect { receiptList ->
                _receipts.value = receiptList
                _isLoading.value = false
            }
        }
    }

    fun deleteReceipt(receipt: ReceiptEntity) {
        viewModelScope.launch {
            receiptRepository.deleteReceipt(receipt)
        }
    }

    fun insertReceipt(receipt: ReceiptEntity) {
        viewModelScope.launch {
            receiptRepository.insertReceipt(receipt)
        }
    }

    fun updateReceipt(receipt: ReceiptEntity) {
        viewModelScope.launch {
            receiptRepository.updateReceipt(receipt)
        }
    }

    fun seedSampleDataIfEmpty(sampleReceipts: List<ReceiptEntity>) {
        if (hasSeededSampleData || _receipts.value.isNotEmpty()) return
        hasSeededSampleData = true
        viewModelScope.launch {
            sampleReceipts.forEach { receiptRepository.insertReceipt(it) }
        }
    }
}
