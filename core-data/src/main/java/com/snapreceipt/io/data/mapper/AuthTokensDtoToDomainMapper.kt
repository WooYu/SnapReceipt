package com.snapreceipt.io.data.mapper

import com.snapreceipt.io.data.base.BaseMapper
import com.snapreceipt.io.domain.model.AuthTokensEntity
import com.snapreceipt.io.data.network.model.AuthTokensDto
import com.snapreceipt.io.data.network.model.toEntity
import javax.inject.Inject

class AuthTokensDtoToDomainMapper @Inject constructor() : BaseMapper<AuthTokensDto, AuthTokensEntity> {
    override fun map(input: AuthTokensDto): AuthTokensEntity = input.toEntity()
}
