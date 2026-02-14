package com.snapreceipt.io.ui.main

import androidx.lifecycle.ViewModel
import com.snapreceipt.io.domain.model.ReceiptEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

/**
 * 列表刷新事件管理
 * 支持增量更新（添加、更新、删除）和全量刷新，避免过度网络请求
 */

sealed class HomeRefreshEvent {
    data class ItemAdded(val receipt: ReceiptEntity) : HomeRefreshEvent()
    data class ItemUpdated(val receipt: ReceiptEntity) : HomeRefreshEvent()
    data class ItemDeleted(val receiptId: Long) : HomeRefreshEvent()
    object FullRefresh : HomeRefreshEvent()
}

sealed class ReceiptsRefreshEvent {
    data class ItemAdded(val receipt: ReceiptEntity) : ReceiptsRefreshEvent()
    data class ItemUpdated(val receipt: ReceiptEntity) : ReceiptsRefreshEvent()
    data class ItemDeleted(val receiptId: Long) : ReceiptsRefreshEvent()
    object FullRefresh : ReceiptsRefreshEvent()
}

@HiltViewModel
class ListRefreshViewModel @Inject constructor() : ViewModel() {

    private val _refreshHomeEvent = MutableSharedFlow<HomeRefreshEvent>(replay = 0)
    val refreshHomeEvent = _refreshHomeEvent.asSharedFlow()

    private val _refreshReceiptsEvent = MutableSharedFlow<ReceiptsRefreshEvent>(replay = 0)
    val refreshReceiptsEvent = _refreshReceiptsEvent.asSharedFlow()

    suspend fun requestHomeItemAdded(receipt: ReceiptEntity) {
        _refreshHomeEvent.emit(HomeRefreshEvent.ItemAdded(receipt))
    }

    suspend fun requestHomeItemUpdated(receipt: ReceiptEntity) {
        _refreshHomeEvent.emit(HomeRefreshEvent.ItemUpdated(receipt))
    }

    suspend fun requestHomeItemDeleted(receiptId: Long) {
        _refreshHomeEvent.emit(HomeRefreshEvent.ItemDeleted(receiptId))
    }

    suspend fun requestHomeFullRefresh() {
        _refreshHomeEvent.emit(HomeRefreshEvent.FullRefresh)
    }

    suspend fun requestReceiptsItemAdded(receipt: ReceiptEntity) {
        _refreshReceiptsEvent.emit(ReceiptsRefreshEvent.ItemAdded(receipt))
    }

    suspend fun requestReceiptsItemUpdated(receipt: ReceiptEntity) {
        _refreshReceiptsEvent.emit(ReceiptsRefreshEvent.ItemUpdated(receipt))
    }

    suspend fun requestReceiptsItemDeleted(receiptId: Long) {
        _refreshReceiptsEvent.emit(ReceiptsRefreshEvent.ItemDeleted(receiptId))
    }

    suspend fun requestReceiptsFullRefresh() {
        _refreshReceiptsEvent.emit(ReceiptsRefreshEvent.FullRefresh)
    }
}
