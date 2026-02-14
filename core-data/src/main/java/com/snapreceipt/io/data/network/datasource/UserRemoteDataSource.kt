package com.snapreceipt.io.data.network.datasource

import com.snapreceipt.io.data.base.BaseRemoteDataSource
import com.snapreceipt.io.data.network.model.auth.UpdateEmailRequestDto
import com.snapreceipt.io.data.network.model.auth.UpdateNickNameRequestDto
import com.snapreceipt.io.data.network.model.auth.UpdatePhoneRequestDto
import com.snapreceipt.io.data.network.model.auth.UserUpdateCodeRequestDto
import com.snapreceipt.io.data.network.service.UserApi
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.skybound.space.core.network.NetworkResult

class UserRemoteDataSource(
    private val api: UserApi,
    dispatchers: CoroutineDispatchersProvider
) : BaseRemoteDataSource(dispatchers) {
    suspend fun requestProfileUpdateCode(target: String): NetworkResult<Unit> {
        return requestUnit { api.requestProfileUpdateCode(UserUpdateCodeRequestDto(target)) }
    }

    suspend fun updateNickName(nickName: String): NetworkResult<Unit> {
        return requestUnit { api.updateNickName(UpdateNickNameRequestDto(nickName)) }
    }

    suspend fun updateEmail(email: String, code: String): NetworkResult<Unit> {
        return requestUnit { api.updateEmail(UpdateEmailRequestDto(email = email, code = code)) }
    }

    suspend fun updatePhone(phoneNumber: String, code: String): NetworkResult<Unit> {
        return requestUnit {
            api.updatePhone(UpdatePhoneRequestDto(phoneNumber = phoneNumber, code = code))
        }
    }
}
