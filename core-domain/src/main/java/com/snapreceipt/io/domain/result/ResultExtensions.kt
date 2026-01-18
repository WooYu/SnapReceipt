package com.snapreceipt.io.domain.result

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

fun <T> Flow<T>.asResult(): Flow<Result<T>> =
    map { Result.success(it) }
        .catch { emit(Result.failure(it)) }
