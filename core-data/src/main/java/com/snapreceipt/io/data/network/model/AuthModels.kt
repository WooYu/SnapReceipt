package com.snapreceipt.io.data.network.model

import com.google.gson.annotations.SerializedName
import com.snapreceipt.io.domain.model.AuthTokensEntity
import com.snapreceipt.io.domain.model.UserEntity

data class AuthCodeRequestDto(
    val to: String
)

data class LoginRequestDto(
    val to: String,
    val code: String,
    val timezone: String
)

data class RefreshRequestDto(
    @SerializedName("refresh_token") val refreshToken: String
)

data class AuthTokensDto(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String
)

data class UserProfileDto(
    val createBy: String? = null,
    val createTime: String? = null,
    val updateBy: String? = null,
    val updateTime: String? = null,
    val remark: String? = null,
    val userId: Long,
    val nickName: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val avatar: String? = null,
    val status: String? = null,
    val delFlag: String? = null,
    val timezone: String? = null,
    val lastLoginDate: String? = null
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
