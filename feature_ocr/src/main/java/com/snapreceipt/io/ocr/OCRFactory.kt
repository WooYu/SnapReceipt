package com.snapreceipt.io.ocr

import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.skybound.space.core.network.NetworkManager
import com.skybound.space.core.network.NetworkConfig
import com.skybound.space.core.network.auth.InMemoryAuthTokenStore
import com.snapreceipt.io.data.network.datasource.FileRemoteDataSource
import com.snapreceipt.io.data.network.datasource.ReceiptRemoteDataSource
import com.snapreceipt.io.data.network.datasource.UploadRemoteDataSource
import com.skybound.space.core.network.interceptor.AuthInterceptor
import com.snapreceipt.io.data.network.service.FileApi
import com.snapreceipt.io.data.network.service.ReceiptApi
import com.skybound.space.core.config.AppConfig
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

enum class OCRMode { MLKIT, BACKEND }

object OCRFactory {
    fun create(
        mode: OCRMode,
        backendBaseUrl: String = AppConfig.baseUrl,
        accessToken: String? = null
    ): OCRService {
        return when (mode) {
            OCRMode.MLKIT -> MLKitOCRService()
            OCRMode.BACKEND -> createBackendService(backendBaseUrl, accessToken)
        }
    }

    private fun createBackendService(baseUrl: String, accessToken: String?): OCRService {
        val config = NetworkConfig(
            baseUrl = baseUrl,
            enableLogging = AppConfig.isDebug,
            defaultHeaders = mapOf("Accept" to "application/json")
        )

        val tokenStore = InMemoryAuthTokenStore().apply {
            update(accessToken, null)
        }
        val networkManager = NetworkManager(
            config,
            extraInterceptors = listOf(AuthInterceptor(tokenStore)),
            authenticator = null
        )
        val retrofit = networkManager.retrofit
        val dispatchers = CoroutineDispatchersProvider.Default

        val fileApi = retrofit.create(FileApi::class.java)
        val receiptApi = retrofit.create(ReceiptApi::class.java)

        val uploadClient = OkHttpClient.Builder()
            .connectTimeout(config.connectTimeoutSec, TimeUnit.SECONDS)
            .readTimeout(config.readTimeoutSec, TimeUnit.SECONDS)
            .writeTimeout(config.writeTimeoutSec, TimeUnit.SECONDS)
            .build()

        return BackendOCRService(
            FileRemoteDataSource(fileApi, dispatchers),
            UploadRemoteDataSource(uploadClient, dispatchers),
            ReceiptRemoteDataSource(receiptApi, dispatchers)
        )
    }
}

