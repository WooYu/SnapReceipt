package com.snapreceipt.io.data.network.service

import com.skybound.space.core.network.BaseResponse
import com.snapreceipt.io.data.network.model.file.UploadUrlRequestDto
import com.snapreceipt.io.domain.model.UploadUrlEntity
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 文件模块业务接口定义。
 *
 * 注意这里只负责向业务服务申请上传凭证，不直接上传文件内容。
 */
interface FileApi {
    /**
     * @param request 上传地址申请参数，当前主要使用文件名
     * @return 服务端返回的预签名上传地址与公开访问地址
     */
    @POST("api/file/upload")
    suspend fun requestUploadUrl(@Body request: UploadUrlRequestDto): BaseResponse<UploadUrlEntity>
}
