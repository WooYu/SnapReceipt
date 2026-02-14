package com.snapreceipt.io.data.network.service

import com.skybound.space.core.network.BaseEmptyResponse
import com.snapreceipt.io.data.network.model.auth.UpdateEmailRequestDto
import com.snapreceipt.io.data.network.model.auth.UpdateNickNameRequestDto
import com.snapreceipt.io.data.network.model.auth.UpdatePhoneRequestDto
import com.snapreceipt.io.data.network.model.auth.UserUpdateCodeRequestDto
import retrofit2.http.Body
import retrofit2.http.POST

interface UserApi {
    @POST("api/user/code")
    suspend fun requestProfileUpdateCode(@Body request: UserUpdateCodeRequestDto): BaseEmptyResponse

    @POST("api/user/updateNickName")
    suspend fun updateNickName(@Body request: UpdateNickNameRequestDto): BaseEmptyResponse

    @POST("api/user/updateEmail")
    suspend fun updateEmail(@Body request: UpdateEmailRequestDto): BaseEmptyResponse

    @POST("api/user/updatePhone")
    suspend fun updatePhone(@Body request: UpdatePhoneRequestDto): BaseEmptyResponse
}
