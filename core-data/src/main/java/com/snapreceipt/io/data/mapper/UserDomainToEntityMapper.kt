package com.snapreceipt.io.data.mapper

import com.snapreceipt.io.data.base.BaseMapper
import com.snapreceipt.io.data.db.UserEntity as UserDbEntity
import com.snapreceipt.io.domain.model.UserEntity
import javax.inject.Inject

class UserDomainToEntityMapper @Inject constructor() : BaseMapper<UserEntity, UserDbEntity> {
    override fun map(input: UserEntity): UserDbEntity {
        return UserDbEntity(
            id = input.id,
            username = input.username,
            email = input.email,
            phone = input.phone,
            avatar = input.avatar,
            createdAt = input.createdAt
        )
    }
}
