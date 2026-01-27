package com.snapreceipt.io.data.network.model.config

import com.google.gson.annotations.SerializedName
import com.snapreceipt.io.domain.model.PolicyEntity

/**
 * 配置接口返回结构（包含用户协议与隐私政策链接）。
 *
 * @property userAgreement 用户协议链接
 * @property privacyPolicy 隐私政策链接
 */
data class PolicyConfigDto(
    @SerializedName("user_agreement") val userAgreement: String? = null,
    @SerializedName("privacy_policy") val privacyPolicy: String? = null
)

fun PolicyConfigDto.toEntity(): PolicyEntity = PolicyEntity(
    userAgreement = userAgreement.orEmpty(),
    privacyPolicy = privacyPolicy.orEmpty()
)
