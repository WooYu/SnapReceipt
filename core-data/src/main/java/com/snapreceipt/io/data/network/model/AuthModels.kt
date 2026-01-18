package com.snapreceipt.io.data.network.model

import com.google.gson.annotations.SerializedName
import com.snapreceipt.io.domain.model.AuthTokensEntity
import com.snapreceipt.io.domain.model.UserEntity

data class AuthCodeRequestDto(
    @SerializedName("to") val to: String
)

data class LoginRequestDto(
    @SerializedName("to") val to: String,
    @SerializedName("code") val code: String,
    @SerializedName("timezone") val timezone: String
)

data class RefreshRequestDto(
    @SerializedName("refresh_token") val refreshToken: String
)

data class AuthTokensDto(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String
)

data class UserProfileDto(
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

fun AuthTokensDto.toEntity(): AuthTokensEntity = AuthTokensEntity(
    accessToken = accessToken,
    refreshToken = refreshToken
)

fun AuthTokensEntity.toDto(): AuthTokensDto = AuthTokensDto(
    accessToken = accessToken,
    refreshToken = refreshToken
)

fun UserProfileDto.toEntity(): UserEntity = UserEntity(
    id = userId.toInt(),
    username = nickName.orEmpty(),
    email = email.orEmpty(),
    phone = phoneNumber.orEmpty(),
    avatar = avatar.orEmpty()
)

fun UserEntity.toDto(): UserProfileDto = UserProfileDto(
    userId = id.toLong(),
    nickName = username,
    email = email,
    phoneNumber = phone,
    avatar = avatar
)
