package com.snapreceipt.io.data.network.model

import com.google.gson.annotations.SerializedName
import com.snapreceipt.io.domain.model.PolicyEntity

data class PolicyConfigDto(
    @SerializedName("user_agreement") val userAgreement: String? = null,
    @SerializedName("privacy_policy") val privacyPolicy: String? = null
)

fun PolicyConfigDto.toEntity(): PolicyEntity = PolicyEntity(
    userAgreement = userAgreement.orEmpty(),
    privacyPolicy = privacyPolicy.orEmpty()
)
