package com.snapreceipt.io.data.network.datasource

import com.snapreceipt.io.data.base.BaseRemoteDataSource
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.skybound.space.core.network.NetworkResult
import com.skybound.space.core.network.BasePagedResponse
import com.snapreceipt.io.data.network.model.ReceiptItemDto
import com.snapreceipt.io.data.network.model.ReceiptListRequestDto
import com.snapreceipt.io.data.network.model.ReceiptSaveRequestDto
import com.snapreceipt.io.data.network.model.ReceiptScanResultDto
import com.snapreceipt.io.data.network.model.ReceiptUpdateRequestDto
import com.snapreceipt.io.data.network.model.ReceiptDeleteRequestDto
import com.snapreceipt.io.data.network.model.ScanRequestDto
import com.snapreceipt.io.data.network.service.ReceiptApi

class ReceiptRemoteDataSource(
    private val api: ReceiptApi,
    dispatchers: CoroutineDispatchersProvider
) : BaseRemoteDataSource(dispatchers) {
    suspend fun scan(imageUrl: String): NetworkResult<ReceiptScanResultDto> {
        return request { api.scan(ScanRequestDto(imageUrl)) }
    }

    suspend fun save(request: ReceiptSaveRequestDto): NetworkResult<Unit> {
        return requestUnit { api.save(request) }
    }

    suspend fun update(request: ReceiptUpdateRequestDto): NetworkResult<Unit> {
        return requestUnit { api.update(request) }
    }

    suspend fun list(request: ReceiptListRequestDto): NetworkResult<BasePagedResponse<ReceiptItemDto>> {
        return requestEnvelope({ api.list(request) }) { it }
    }

    suspend fun delete(receiptId: Long): NetworkResult<Unit> {
        return requestUnit { api.delete(ReceiptDeleteRequestDto(receiptId)) }
    }
}
