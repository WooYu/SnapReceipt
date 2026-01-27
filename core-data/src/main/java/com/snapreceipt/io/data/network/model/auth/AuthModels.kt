package com.snapreceipt.io.data.network.model.auth

import com.google.gson.annotations.SerializedName
import com.snapreceipt.io.domain.model.AuthTokensEntity
import com.snapreceipt.io.domain.model.UserEntity

/**
 * 获取验证码请求（/api/auth/code）。
 *
 * @property to 手机号或邮箱
 */
data class AuthCodeRequestDto(
    val to: String
)

/**
 * 登录请求（/api/auth/login）。
 *
 * @property to 手机号或邮箱
 * @property code 验证码
 * @property timezone IANA 时区（例如 Asia/Shanghai）
 */
data class LoginRequestDto(
    val to: String,
    val code: String,
    val timezone: String
)

/**
 * 刷新 token 请求（/api/auth/refresh）。
 *
 * @property refreshToken refresh_token
 */
data class RefreshRequestDto(
    @SerializedName("refresh_token") val refreshToken: String
)

/**
 * 登录/刷新返回的 token 信息。
 *
 * @property accessToken access_token
 * @property refreshToken refresh_token
 */
data class AuthTokensDto(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String
)

/**
 * 用户信息响应体（/api/auth/user 的 data）。
 *
 * @property createBy 创建人
 * @property createTime 创建时间
 * @property updateBy 更新人
 * @property updateTime 更新时间
 * @property remark 备注
 * @property userId 用户ID
 * @property nickName 昵称
 * @property email 邮箱
 * @property phoneNumber 电话（脱敏，字段名 phonenumber）
 * @property avatar 头像
 * @property status 状态
 * @property delFlag 删除标记
 * @property timezone 时区
 * @property lastLoginDate 最近登录日期
 */
data class UserProfileDto(
    val createBy: String? = null,
    val createTime: String? = null,
    val updateBy: String? = null,
    val updateTime: String? = null,
    val remark: String? = null,
    val userId: Long,
    val nickName: String? = null,
    val email: String? = null,
    @SerializedName("phonenumber") val phoneNumber: String? = null,
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
    id = userId,
    username = nickName.orEmpty(),
    email = email.orEmpty(),
    phone = phoneNumber.orEmpty(),
    avatar = avatar.orEmpty()
)

fun UserEntity.toDto(): UserProfileDto = UserProfileDto(
    userId = id ?: 0L,
    nickName = username,
    email = email,
    phoneNumber = phone,
    avatar = avatar
)
