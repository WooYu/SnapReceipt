package com.skybound.space.core.network

import com.skybound.space.core.dispatcher.DispatchersProvider
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * 网络调用统一结果模型，避免在页面层直接处理异常。
 */
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Failure(val error: NetworkError) : NetworkResult<Nothing>()

    inline fun onSuccess(block: (T) -> Unit): NetworkResult<T> = apply {
        if (this is Success) block(data)
    }

    inline fun onFailure(block: (NetworkError) -> Unit): NetworkResult<T> = apply {
        if (this is Failure) block(error)
    }
}

/**
 * 统一错误描述，便于上层根据类型做兜底处理。
 */
sealed class NetworkError(open val throwable: Throwable? = null) {
    data class Http(val code: Int, val message: String, override val throwable: Throwable? = null) :
        NetworkError(throwable)

    data class Network(val message: String = "Network unavailable", override val throwable: Throwable? = null) :
        NetworkError(throwable)

    data class Serialization(val message: String = "Parse error", override val throwable: Throwable? = null) :
        NetworkError(throwable)

    data class Unexpected(val message: String = "Unexpected error", override val throwable: Throwable? = null) :
        NetworkError(throwable)
}

/**
 * 处理带有 code/msg 的响应，统一返回 NetworkResult。
 */
suspend fun <E : ApiEnvelope, T> safeApiCallEnvelope(
    dispatchers: DispatchersProvider,
    apiCall: suspend () -> E,
    extractor: (E) -> T?
): NetworkResult<T> = withContext(dispatchers.io) {
    return@withContext try {
        val response = apiCall()
        if (response.isSuccess()) {
            val data = extractor(response)
            if (data != null) {
                NetworkResult.Success(data)
            } else {
                NetworkResult.Failure(NetworkError.Serialization("Empty body"))
            }
        } else {
            NetworkResult.Failure(NetworkError.Http(response.code, response.message))
        }
    } catch (http: HttpException) {
        NetworkResult.Failure(NetworkError.Http(http.code(), http.message(), http))
    } catch (io: IOException) {
        NetworkResult.Failure(NetworkError.Network(io.message ?: "IO error", io))
    } catch (throwable: Throwable) {
        NetworkResult.Failure(NetworkError.Unexpected(throwable.message ?: "Unexpected", throwable))
    }
}

/**
 * 处理标准 ApiResponse。
 */
suspend fun <T> safeApiCall(
    dispatchers: DispatchersProvider,
    apiCall: suspend () -> ApiResponse<T>
): NetworkResult<T> = safeApiCallEnvelope(dispatchers, apiCall) { it.data }

/**
 * 处理无 data 的响应。
 */
suspend fun safeApiCallBasic(
    dispatchers: DispatchersProvider,
    apiCall: suspend () -> BasicResponse
): NetworkResult<Unit> = safeApiCallEnvelope(dispatchers, apiCall) { Unit }

/**
 * 将 Retrofit Response 转换为 NetworkResult，集中处理 HTTP/IO/解析错误。
 */
suspend fun <T> safeResponseCall(
    dispatchers: DispatchersProvider,
    apiCall: suspend () -> Response<T>
): NetworkResult<T> = withContext(dispatchers.io) {
    return@withContext try {
        val response = apiCall()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                NetworkResult.Success(body)
            } else {
                NetworkResult.Failure(NetworkError.Serialization("Empty body"))
            }
        } else {
            NetworkResult.Failure(
                NetworkError.Http(
                    code = response.code(),
                    message = response.message()
                )
            )
        }
    } catch (http: HttpException) {
        NetworkResult.Failure(NetworkError.Http(http.code(), http.message(), http))
    } catch (io: IOException) {
        NetworkResult.Failure(NetworkError.Network(io.message ?: "IO error", io))
    } catch (throwable: Throwable) {
        NetworkResult.Failure(NetworkError.Unexpected(throwable.message ?: "Unexpected", throwable))
    }
}
