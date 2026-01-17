package com.snapreceipt.io.data.network.model

import com.google.gson.annotations.SerializedName

data class UploadUrlRequest(
    @SerializedName("fileName") val fileName: String
)

data class UploadUrlResponse(
    @SerializedName("fileName") val fileName: String,
    @SerializedName("uploadUrl") val uploadUrl: String,
    @SerializedName("publicUrl") val publicUrl: String
)
