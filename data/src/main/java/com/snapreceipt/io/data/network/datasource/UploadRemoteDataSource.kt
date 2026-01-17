package com.snapreceipt.io.data.network.datasource

import com.skybound.space.core.dispatcher.DispatchersProvider
import com.skybound.space.core.network.NetworkError
import com.skybound.space.core.network.NetworkResult
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException

class UploadRemoteDataSource(
    private val okHttpClient: OkHttpClient,
    private val dispatchers: DispatchersProvider
) {
    suspend fun uploadFile(
        uploadUrl: String,
        file: File,
        contentType: String = "image/jpeg"
    ): NetworkResult<Unit> = withContext(dispatchers.io) {
        if (!file.exists()) {
            return@withContext NetworkResult.Failure(
                NetworkError.Unexpected("File not found: ${file.path}")
            )
        }

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
        }
    }
}
