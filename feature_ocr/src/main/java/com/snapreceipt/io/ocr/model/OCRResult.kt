package com.snapreceipt.io.ocr.model

/**
 * OCR 结果封装（兼容本地识别与后端识别）。
 *
 * @property code 状态码（0/200 表示成功）
 * @property msg 提示信息
 * @property data 结果数据（可能包含 text/merchant/totalAmount 等）
 */
data class OCRResult(
    val code: Int,
    val msg: String?,
    val data: Map<String, Any?>?
)
