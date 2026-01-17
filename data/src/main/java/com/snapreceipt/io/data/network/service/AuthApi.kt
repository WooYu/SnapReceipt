package com.snapreceipt.io.data.network.service

import com.skybound.space.core.network.ApiResponse
import com.skybound.space.core.network.BasicResponse
import com.snapreceipt.io.data.network.model.AuthCodeRequest
import com.snapreceipt.io.data.network.model.AuthTokens
import com.snapreceipt.io.data.network.model.LoginRequest
import com.snapreceipt.io.data.network.model.RefreshRequest
import com.snapreceipt.io.data.network.model.UserProfile
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AuthApi {
    @Headers("No-Auth: true")
    @POST("api/auth/code")
    suspend fun requestCode(@Body request: AuthCodeRequest): BasicResponse

    @Headers("No-Auth: true")
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthTokens>

    @POST("api/auth/user")
    suspend fun fetchUser(): ApiResponse<UserProfile>

    @Headers("No-Auth: true")
    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): ApiResponse<AuthTokens>
}
