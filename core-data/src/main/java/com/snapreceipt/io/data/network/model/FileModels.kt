package com.snapreceipt.io.data.network.model

import com.snapreceipt.io.domain.model.UploadUrlEntity

data class UploadUrlRequestDto(
    val fileName: String
)

data class UploadUrlResponseDto(
    val fileName: String,
    val uploadUrl: String,
    val publicUrl: String
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
