package com.snapreceipt.io.data.network.service

import com.skybound.space.core.network.BaseResponse
import com.skybound.space.core.network.BaseEmptyResponse
import com.skybound.space.core.network.BasePagedResponse
import com.snapreceipt.io.data.network.model.ReceiptItemDto
import com.snapreceipt.io.data.network.model.ReceiptListRequestDto
import com.snapreceipt.io.data.network.model.ReceiptSaveRequestDto
import com.snapreceipt.io.data.network.model.ReceiptScanResultDto
import com.snapreceipt.io.data.network.model.ReceiptUpdateRequestDto
import com.snapreceipt.io.data.network.model.ScanRequestDto
import retrofit2.http.Body
import retrofit2.http.POST

interface ReceiptApi {
    @POST("api/receipt/scan")
    suspend fun scan(@Body request: ScanRequestDto): BaseResponse<ReceiptScanResultDto>

    @POST("api/receipt/save")
    suspend fun save(@Body request: ReceiptSaveRequestDto): BaseEmptyResponse

    @POST("api/receipt/update")
    suspend fun update(@Body request: ReceiptUpdateRequestDto): BaseEmptyResponse

    @POST("api/receipt/list")
    suspend fun list(@Body request: ReceiptListRequestDto): BasePagedResponse<ReceiptItemDto>
}
