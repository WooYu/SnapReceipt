package com.snapreceipt.io.domain.usecase.file

import com.snapreceipt.io.domain.model.UploadUrlEntity
import com.snapreceipt.io.domain.repository.FileRepository
import javax.inject.Inject

/**
 * 申请文件直传地址的用例。
 *
 * 职责是向服务端申请一次性上传凭证，返回对象存储上传地址和最终公开访问地址。
 * 该用例通常是“拍照/选图上传识别”流程的第一步。
 */
class RequestUploadUrlUseCase @Inject constructor(
    private val repository: FileRepository
) {
    /**
     * @param fileName 本地待上传文件名；服务端可能据此生成对象存储中的目标文件名
     * @return 成功时返回上传地址信息，失败时抛出异常并由 [Result] 包装
     */
    suspend operator fun invoke(fileName: String): Result<UploadUrlEntity> =
        runCatching { repository.requestUploadUrl(fileName) }
}
