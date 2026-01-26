package com.snapreceipt.io.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 本地发票表（用于离线缓存）。
 *
 * 与接口字段的对应关系：
 * - merchantName -> merchant
 * - amount -> totalAmount
 * - date -> receiptDate + receiptTime（合并为时间戳）
 * - category -> categoryName（本地 label）
 * - invoiceType -> receiptType（Business/Individual）
 * - imagePath -> receiptUrl
 * - description -> remark
 */
@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true)
    /** 主键ID（本地生成） */
    val id: Int = 0,
    /** 商户名称 */
    val merchantName: String,
    /** 消费总额 */
    val amount: Double,
    /** 货币（接口暂未返回，保留本地字段） */
    val currency: String = "CNY",
    /** 发票时间戳（由日期+时间合成） */
    val date: Long = System.currentTimeMillis(),
    /** 分类名称（本地 label） */
    val category: String = "",
    /** 发票类型（Business/Individual） */
    val invoiceType: String = "Individual",
    /** 发票图片地址/路径 */
    val imagePath: String = "",
    /** 备注 */
    val description: String = "",
    /** 本地创建时间戳 */
    val createdAt: Long = System.currentTimeMillis(),
    /** 本地更新时间戳 */
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 本地用户表（用于缓存用户信息）。
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    /** 用户ID */
    val id: Int = 1,
    /** 昵称（接口字段 nickName） */
    val username: String = "",
    /** 邮箱 */
    val email: String = "",
    /** 电话（接口字段 phonenumber） */
    val phone: String = "",
    /** 头像地址 */
    val avatar: String = "",
    /** 本地创建时间戳 */
    val createdAt: Long = System.currentTimeMillis()
)
