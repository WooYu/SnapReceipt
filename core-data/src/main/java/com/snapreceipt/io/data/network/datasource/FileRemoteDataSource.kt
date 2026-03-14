package com.snapreceipt.io.data.network.datasource

import com.snapreceipt.io.data.base.BaseRemoteDataSource
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.skybound.space.core.network.NetworkResult
import com.snapreceipt.io.data.network.model.file.UploadUrlRequestDto
import com.snapreceipt.io.domain.model.UploadUrlEntity
import com.snapreceipt.io.data.network.service.FileApi

/**
 * 文件业务接口数据源。
 *
 * 这里只负责和业务后端通信申请上传地址，不直接执行文件上传。
 * 真正的文件字节上传由 [UploadRemoteDataSource] 完成。
 */
class FileRemoteDataSource(
    private val api: FileApi,
    dispatchers: CoroutineDispatchersProvider
) : BaseRemoteDataSource(dispatchers) {
    /**
     * @param fileName 待上传文件名
     * @return 上传地址信息；包装成 [NetworkResult] 供上层统一处理网络异常
     */
    suspend fun requestUploadUrl(fileName: String): NetworkResult<UploadUrlEntity> {
        return request { api.requestUploadUrl(UploadUrlRequestDto(fileName)) }
    }
}
