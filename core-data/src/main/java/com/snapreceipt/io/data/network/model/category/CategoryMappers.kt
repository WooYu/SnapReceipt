package com.snapreceipt.io.data.network.model.category

import com.snapreceipt.io.domain.model.ReceiptCategory

/**
 * CategoryItemDto → ReceiptCategory.Item 映射器
 * 
 * 职责：
 * - 从网络 DTO（12 个字段）提取领域模型需要的 3 个核心字段
 * - 判断分类类型（系统预设 vs 用户自定义）
 * 
 * 字段映射：
 * - categoryId → id
 * - categoryName → label
 * - userId → isCustom（userId 为 null 表示系统预设分类）
 * 
 * 使用位置：
 * - ReceiptRemoteRepositoryImpl.listCategories()
 */
fun CategoryItemDto.toItem(): ReceiptCategory.Item =
    ReceiptCategory.Item(
        id = categoryId,
        label = categoryName,
        isCustom = (userId ?: 0L) != 0L
    )
