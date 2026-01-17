package com.snapreceipt.io.data.network.datasource

import com.skybound.space.core.dispatcher.DispatchersProvider
import com.skybound.space.core.network.NetworkResult
import com.skybound.space.core.network.PagedResponse
import com.skybound.space.core.network.safeApiCall
import com.skybound.space.core.network.safeApiCallBasic
import com.skybound.space.core.network.safeApiCallEnvelope
import com.snapreceipt.io.data.network.model.ReceiptItem
import com.snapreceipt.io.data.network.model.ReceiptListRequest
import com.snapreceipt.io.data.network.model.ReceiptSaveRequest
import com.snapreceipt.io.data.network.model.ReceiptScanResult
import com.snapreceipt.io.data.network.model.ReceiptUpdateRequest
import com.snapreceipt.io.data.network.model.ScanRequest
import com.snapreceipt.io.data.network.service.ReceiptApi

class ReceiptRemoteDataSource(
    private val api: ReceiptApi,
    private val dispatchers: DispatchersProvider
) {
    suspend fun scan(imageUrl: String): NetworkResult<ReceiptScanResult> {
        return safeApiCall(dispatchers) { api.scan(ScanRequest(imageUrl)) }
    }

    suspend fun save(request: ReceiptSaveRequest): NetworkResult<Unit> {
        return safeApiCallBasic(dispatchers) { api.save(request) }
    }

    suspend fun update(request: ReceiptUpdateRequest): NetworkResult<Unit> {
        return safeApiCallBasic(dispatchers) { api.update(request) }
    }

    suspend fun list(request: ReceiptListRequest): NetworkResult<PagedResponse<ReceiptItem>> {
        return safeApiCallEnvelope(dispatchers, { api.list(request) }) { it }
    }
}
