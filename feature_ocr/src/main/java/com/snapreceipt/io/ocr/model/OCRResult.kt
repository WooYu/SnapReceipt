package com.snapreceipt.io.ocr.model

data class OCRResult(
    val code: Int,
    val msg: String?,
    val data: Map<String, Any?>?
)
