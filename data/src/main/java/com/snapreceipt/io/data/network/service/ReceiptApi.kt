package com.snapreceipt.io.data.network.service

import com.skybound.space.core.network.ApiResponse
import com.skybound.space.core.network.BasicResponse
import com.skybound.space.core.network.PagedResponse
import com.snapreceipt.io.data.network.model.ReceiptItem
import com.snapreceipt.io.data.network.model.ReceiptListRequest
import com.snapreceipt.io.data.network.model.ReceiptSaveRequest
import com.snapreceipt.io.data.network.model.ReceiptScanResult
import com.snapreceipt.io.data.network.model.ReceiptUpdateRequest
import com.snapreceipt.io.data.network.model.ScanRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface ReceiptApi {
    @POST("api/receipt/scan")
    suspend fun scan(@Body request: ScanRequest): ApiResponse<ReceiptScanResult>

    @POST("api/receipt/save")
    suspend fun save(@Body request: ReceiptSaveRequest): BasicResponse

    @POST("api/receipt/update")
    suspend fun update(@Body request: ReceiptUpdateRequest): BasicResponse

    @POST("api/receipt/list")
    suspend fun list(@Body request: ReceiptListRequest): PagedResponse<ReceiptItem>
}
