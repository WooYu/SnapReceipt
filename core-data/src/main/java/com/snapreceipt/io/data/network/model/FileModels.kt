package com.snapreceipt.io.data.network.model

import com.google.gson.annotations.SerializedName
import com.snapreceipt.io.domain.model.UploadUrlEntity

data class UploadUrlRequestDto(
    @SerializedName("fileName") val fileName: String
)

data class UploadUrlResponseDto(
    @SerializedName("fileName") val fileName: String,
    @SerializedName("uploadUrl") val uploadUrl: String,
    @SerializedName("publicUrl") val publicUrl: String
)

fun UploadUrlResponseDto.toEntity(): UploadUrlEntity = UploadUrlEntity(
    fileName = fileName,
    uploadUrl = uploadUrl,
    publicUrl = publicUrl
)

fun UploadUrlEntity.toDto(): UploadUrlResponseDto = UploadUrlResponseDto(
    fileName = fileName,
    uploadUrl = uploadUrl,
    publicUrl = publicUrl
)
