package com.snapreceipt.io.domain.usecase.file

import com.snapreceipt.io.domain.repository.FileRepository
import javax.inject.Inject

/**
 * 执行文件直传的用例。
 *
 * 它接收上一步申请到的预签名地址，并把本地文件上传到对象存储。
 * 这里不关心业务字段，只关注“文件是否成功送达存储侧”。
 */
class UploadFileUseCase @Inject constructor(
    private val repository: FileRepository
) {
    /**
     * @param uploadUrl 服务端下发的预签名上传地址
     * @param filePath 本地文件绝对路径
     * @param contentType 上传时使用的 MIME 类型，需尽量与真实文件类型一致
     * @return 成功时返回 [Result.success]；失败时将异常封装进 [Result.failure]
     */
    suspend operator fun invoke(
        uploadUrl: String,
        filePath: String,
        contentType: String = "image/jpeg"
    ): Result<Unit> = runCatching {
        repository.uploadFile(uploadUrl, filePath, contentType)
    }
}
