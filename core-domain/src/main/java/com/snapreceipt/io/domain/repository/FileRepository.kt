package com.snapreceipt.io.domain.repository

import com.snapreceipt.io.domain.model.UploadUrlEntity

/**
 * 文件上传相关领域仓库。
 *
 * 该接口对 domain 层暴露“申请上传地址”和“执行直传”两个能力，
 * 屏蔽 data 层到底是走业务接口还是原始 HTTP PUT 请求的实现细节。
 */
interface FileRepository {
    /**
     * @param fileName 待上传文件名
     * @return 包含上传地址、存储文件名和公开访问地址的上传信息
     */
    suspend fun requestUploadUrl(fileName: String): UploadUrlEntity

    /**
     * @param uploadUrl 预签名上传地址
     * @param filePath 本地文件绝对路径
     * @param contentType 上传使用的 MIME 类型
     */
    suspend fun uploadFile(uploadUrl: String, filePath: String, contentType: String = "image/jpeg")
}
