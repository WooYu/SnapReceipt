package com.snapreceipt.io.data.network.datasource

import android.os.SystemClock
import com.snapreceipt.io.data.base.BaseRemoteDataSource
import com.skybound.space.core.monitoring.PerformanceMonitorManager
import com.skybound.space.core.monitoring.MonitoringNames
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.skybound.space.core.network.NetworkError
import com.skybound.space.core.network.NetworkResult
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException

/**
 * 使用预签名上传地址将文件直传到对象存储。
 *
 * 这里不走 Retrofit 业务接口，而是直接发起原始 PUT 请求，
 * 因为上传目标通常是对象存储返回的临时地址，只关心 HTTP 成功与否。
 */
class UploadRemoteDataSource(
    private val okHttpClient: OkHttpClient,
    dispatchers: CoroutineDispatchersProvider
) : BaseRemoteDataSource(dispatchers) {
    suspend fun uploadFile(
        uploadUrl: String,
        file: File,
        contentType: String = "image/jpeg"
    ): NetworkResult<Unit> = withContext(dispatchers.io) {
        // 整段上传耗时都会被记录下来，便于区分“申请上传地址慢”还是“直传对象存储慢”。
        val startedAt = SystemClock.elapsedRealtime()
        if (!file.exists()) {
            return@withContext NetworkResult.Failure(
                NetworkError.Unexpected("File not found: ${file.path}")
            )
        }

        // 预签名上传通常要求请求体 Content-Type 与签名约定一致，否则服务端可能判定签名失效。
        val body = file.asRequestBody(contentType.toMediaTypeOrNull())
        val request = Request.Builder()
            .url(uploadUrl)
            .put(body)
            .build()

        return@withContext try {
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    NetworkResult.Success(Unit)
                } else {
                    // 对象存储直传没有统一业务响应体，这里直接按 HTTP 状态码归类失败原因。
                    NetworkResult.Failure(
                        NetworkError.Http(response.code, response.message)
                    )
                }
            }
        } catch (io: IOException) {
            NetworkResult.Failure(NetworkError.Network(io.message ?: "IO error", io))
        } catch (throwable: Throwable) {
            NetworkResult.Failure(
                NetworkError.Unexpected(throwable.message ?: "Unexpected", throwable)
            )
        } finally {
            // 无论成功还是失败都记录耗时，方便在监控平台观察上传链路稳定性。
            PerformanceMonitorManager.trackApiCall(
                endpoint = MonitoringNames.Traces.uploadPutObjectStorage,
                durationMs = SystemClock.elapsedRealtime() - startedAt
            )
        }
    }
}
