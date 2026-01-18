package com.snapreceipt.io.data.network.service

import com.skybound.space.core.network.BaseResponse
import com.skybound.space.core.network.BaseEmptyResponse
import com.snapreceipt.io.data.network.model.AuthCodeRequestDto
import com.snapreceipt.io.data.network.model.AuthTokensDto
import com.snapreceipt.io.data.network.model.LoginRequestDto
import com.snapreceipt.io.data.network.model.RefreshRequestDto
import com.snapreceipt.io.data.network.model.UserProfileDto
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AuthApi {
    @Headers("No-Auth: true")
    @POST("api/auth/code")
    suspend fun requestCode(@Body request: AuthCodeRequestDto): BaseEmptyResponse

    @Headers("No-Auth: true")
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestDto): BaseResponse<AuthTokensDto>

    @POST("api/auth/user")
    suspend fun fetchUser(): BaseResponse<UserProfileDto>

    @Headers("No-Auth: true")
    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequestDto): BaseResponse<AuthTokensDto>
}
