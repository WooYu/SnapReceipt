package com.snapreceipt.io.data.repository

import com.snapreceipt.io.data.network.datasource.FileRemoteDataSource
import com.snapreceipt.io.data.network.datasource.UploadRemoteDataSource
import com.snapreceipt.io.domain.model.UploadUrlEntity
import com.snapreceipt.io.domain.repository.FileRepository
import com.skybound.space.core.network.getOrThrow
import java.io.File
import javax.inject.Inject

/**
 * [FileRepository] 的 data 层实现。
 *
 * 流程被拆成两段：
 * 1. 通过业务接口申请预签名上传地址；
 * 2. 通过原始 PUT 请求把文件直传到对象存储。
 */
class FileRepositoryImpl @Inject constructor(
    private val fileRemoteDataSource: FileRemoteDataSource,
    private val uploadRemoteDataSource: UploadRemoteDataSource
) : FileRepository {
    /**
     * @param fileName 待上传文件名
     * @return 服务端返回的上传信息，失败时由 [getOrThrow] 抛出网络异常
     */
    override suspend fun requestUploadUrl(fileName: String): UploadUrlEntity =
        fileRemoteDataSource.requestUploadUrl(fileName).getOrThrow()

    /**
     * @param uploadUrl 预签名上传地址
     * @param filePath 本地文件路径
     * @param contentType 文件 MIME 类型
     */
    override suspend fun uploadFile(uploadUrl: String, filePath: String, contentType: String) {
        uploadRemoteDataSource.uploadFile(uploadUrl, File(filePath), contentType).getOrThrow()
    }
}
