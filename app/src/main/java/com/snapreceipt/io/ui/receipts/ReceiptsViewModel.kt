package com.snapreceipt.io.ui.receipts

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
class ReceiptsViewModel @Inject constructor(
    private val receiptRepository: ReceiptRepository
) : ViewModel() {

    private val _receipts = MutableStateFlow<List<ReceiptEntity>>(emptyList())
    val receipts: StateFlow<List<ReceiptEntity>> = _receipts.asStateFlow()

    private val _selectedReceipts = MutableStateFlow<Set<Int>>(emptySet())
    val selectedReceipts: StateFlow<Set<Int>> = _selectedReceipts.asStateFlow()

    init {
        loadReceipts()
    }

    private fun loadReceipts() {
        viewModelScope.launch {
            receiptRepository.getAllReceipts().collect { receiptList ->
                _receipts.value = receiptList
            }
        }
    }

    fun toggleSelection(id: Int) {
        val current = _selectedReceipts.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedReceipts.value = current
    }

    fun selectAll() {
        _selectedReceipts.value = _receipts.value.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedReceipts.value = emptySet()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            receiptRepository.deleteMultiple(_selectedReceipts.value.toList())
            _selectedReceipts.value = emptySet()
        }
    }

    fun deleteReceipt(receipt: ReceiptEntity) {
        viewModelScope.launch {
            receiptRepository.deleteReceipt(receipt)
        }
    }

    fun updateReceipt(receipt: ReceiptEntity) {
        viewModelScope.launch {
            receiptRepository.updateReceipt(receipt)
        }
    }

    fun getReceiptsByDateRange(startDate: Long, endDate: Long) {
        viewModelScope.launch {
            receiptRepository.getReceiptsByDateRange(startDate, endDate).collect { receipts ->
                _receipts.value = receipts
            }
        }
    }

    fun getReceiptsByType(type: String) {
        viewModelScope.launch {
            receiptRepository.getReceiptsByType(type).collect { receipts ->
                _receipts.value = receipts
            }
        }
    }
}
