package com.snapreceipt.io.data.network.model

import com.google.gson.annotations.SerializedName

data class AuthCodeRequest(
    @SerializedName("to") val to: String
)

data class LoginRequest(
    @SerializedName("to") val to: String,
    @SerializedName("code") val code: String,
    @SerializedName("timezone") val timezone: String
)

data class RefreshRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

data class AuthTokens(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String
)

data class UserProfile(
    @SerializedName("createBy") val createBy: String? = null,
    @SerializedName("createTime") val createTime: String? = null,
    @SerializedName("updateBy") val updateBy: String? = null,
    @SerializedName("updateTime") val updateTime: String? = null,
    @SerializedName("remark") val remark: String? = null,
    @SerializedName("userId") val userId: Long,
    @SerializedName("nickName") val nickName: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("phonenumber") val phoneNumber: String? = null,
    @SerializedName("avatar") val avatar: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("delFlag") val delFlag: String? = null,
    @SerializedName("timezone") val timezone: String? = null,
    @SerializedName("lastLoginDate") val lastLoginDate: String? = null
)
