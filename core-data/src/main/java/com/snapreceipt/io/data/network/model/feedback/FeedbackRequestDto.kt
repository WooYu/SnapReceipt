package com.snapreceipt.io.data.network.model.feedback

import com.google.gson.annotations.SerializedName

data class FeedbackRequestDto(
    @SerializedName("contact") val contact: String,
    @SerializedName("content") val content: String
)
