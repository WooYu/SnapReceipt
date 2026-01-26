package com.snapreceipt.io.domain.model

/**
 * 协议配置（来自配置接口）。
 *
 * @property userAgreement 用户协议链接
 * @property privacyPolicy 隐私政策链接
 */
data class PolicyEntity(
    val userAgreement: String = "",
    val privacyPolicy: String = ""
)
