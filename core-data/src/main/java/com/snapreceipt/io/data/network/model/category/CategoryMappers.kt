package com.snapreceipt.io.data.network.model.category

fun CategoryItemDto.toItem(): com.snapreceipt.io.domain.model.ReceiptCategory.Item =
    com.snapreceipt.io.domain.model.ReceiptCategory.Item(
        id = categoryId,
        label = categoryName,
        isCustom = (userId ?: 0L) != 0L
    )
