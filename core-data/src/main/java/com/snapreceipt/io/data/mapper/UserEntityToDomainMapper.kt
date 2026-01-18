package com.snapreceipt.io.data.mapper

import com.snapreceipt.io.data.base.BaseMapper
import com.snapreceipt.io.data.db.UserEntity as UserDbEntity
import com.snapreceipt.io.domain.model.UserEntity
import javax.inject.Inject

class UserEntityToDomainMapper @Inject constructor() : BaseMapper<UserDbEntity, UserEntity> {
    override fun map(input: UserDbEntity): UserEntity {
        return UserEntity(
            id = input.id,
            username = input.username,
            email = input.email,
            phone = input.phone,
            avatar = input.avatar,
            createdAt = input.createdAt
        )
    }
}
