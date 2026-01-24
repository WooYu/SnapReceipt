package com.snapreceipt.io.data.network.service

import com.snapreceipt.io.data.network.model.PolicyConfigDto
import com.skybound.space.core.network.BaseResponse
import retrofit2.http.POST

interface ConfigApi {
    @POST("api/config/policy")
    suspend fun fetchPolicy(): BaseResponse<PolicyConfigDto>
}
