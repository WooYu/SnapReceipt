package com.snapreceipt.io.domain.model

/**
 * 图片上传地址信息（对应 /api/file/upload 返回的 data）。
 *
 * @property fileName 上传后的文件名称
 * @property uploadUrl 上传地址（预签名 URL）
 * @property publicUrl 公网访问地址前缀
 */
data class UploadUrlEntity(
    val fileName: String,
    val uploadUrl: String,
    val publicUrl: String
)
