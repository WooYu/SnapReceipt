package com.snapreceipt.io.data.network.service

import com.snapreceipt.io.data.network.model.PolicyConfigDto
import com.skybound.space.core.network.BaseResponse
import retrofit2.http.GET

interface ConfigApi {
    @GET("api/config/policy")
    suspend fun fetchPolicy(): BaseResponse<PolicyConfigDto>
}
