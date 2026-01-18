package com.snapreceipt.io.domain.model

data class UserEntity(
    val id: Int = 1,
    val username: String = "",
    val email: String = "",
    val phone: String = "",
    val avatar: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
