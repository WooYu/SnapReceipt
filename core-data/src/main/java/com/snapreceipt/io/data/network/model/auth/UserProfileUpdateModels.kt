package com.snapreceipt.io.data.network.model.auth

import com.google.gson.annotations.SerializedName

data class UserUpdateCodeRequestDto(
    val to: String
)

data class UpdateNickNameRequestDto(
    val nickName: String
)

data class UpdateEmailRequestDto(
    val email: String,
    val code: String
)

data class UpdatePhoneRequestDto(
    @SerializedName("phonenumber") val phoneNumber: String,
    val code: String
)
