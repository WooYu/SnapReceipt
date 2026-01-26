package com.snapreceipt.io.data.db

/**
 * 本地用户缓存结构（用于加密存储）。
 */
data class UserEntity(
    /** 用户ID */
    val id: Long = 1L,
    /** 昵称（接口字段 nickName） */
    val username: String = "",
    /** 邮箱 */
    val email: String = "",
    /** 电话（接口字段 phonenumber） */
    val phone: String = "",
    /** 头像地址 */
    val avatar: String = "",
    /** 本地创建时间戳 */
    val createdAt: Long = System.currentTimeMillis()
)
