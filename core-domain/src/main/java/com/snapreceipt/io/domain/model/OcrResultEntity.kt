package com.snapreceipt.io.domain.model

/**
 * OCR 识别摘要结果（用于 UI 展示/进一步处理）。
 *
 * @property text OCR 原始文本
 * @property merchant 商户名称（可选）
 * @property amount 识别到的金额（可选）
 */
data class OcrResultEntity(
    val text: String,
    val merchant: String? = null,
    val amount: Double? = null
)
