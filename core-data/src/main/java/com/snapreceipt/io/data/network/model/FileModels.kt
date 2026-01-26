package com.snapreceipt.io.data.network.model

/**
 * 获取上传地址请求（/api/file/upload）。
 *
 * @property fileName 文件名
 */
data class UploadUrlRequestDto(
    val fileName: String
)
