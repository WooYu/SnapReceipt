package com.snapreceipt.io.data.mapper

import com.snapreceipt.io.data.base.BaseMapper
import com.snapreceipt.io.data.db.ReceiptEntity as ReceiptDbEntity
import com.snapreceipt.io.domain.model.ReceiptEntity
import javax.inject.Inject

class ReceiptEntityToDomainMapper @Inject constructor() : BaseMapper<ReceiptDbEntity, ReceiptEntity> {
    override fun map(input: ReceiptDbEntity): ReceiptEntity {
        return ReceiptEntity(
            id = input.id,
            merchantName = input.merchantName,
            amount = input.amount,
            currency = input.currency,
            date = input.date,
            category = input.category,
            invoiceType = input.invoiceType,
            imagePath = input.imagePath,
            description = input.description,
            createdAt = input.createdAt,
            updatedAt = input.updatedAt
        )
    }
}
