package com.snapreceipt.io.domain.model

/**
 * 用户信息（本地缓存 + UI 展示用）。
 *
 * @property id 用户ID
 * @property username 昵称（接口字段 nickName）
 * @property email 邮箱
 * @property phone 电话（接口字段 phonenumber）
 * @property avatar 头像地址
 * @property createdAt 本地创建时间戳
 */
data class UserEntity(
    val id: Long = 1L,
    val username: String = "",
    val email: String = "",
    val phone: String = "",
    val avatar: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
