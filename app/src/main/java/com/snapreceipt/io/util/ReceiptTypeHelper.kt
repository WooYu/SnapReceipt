package com.snapreceipt.io.util

import android.content.Context
import com.snapreceipt.io.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 收据类型辅助工具
 * 
 * 职责：
 * - 处理收据类型（公司/个人）在服务端 payload 和本地化显示之间的转换
 * - 封装本地化字符串，避免调用方重复传参
 * 
 * 服务端约定：
 * - "Business" = 公司抬头
 * - "Individual" = 个人抬头
 * 
 * 客户端本地化：
 * - 中文："企业" / "个人"
 * - 英文："Company" / "Individual"
 * 
 * 使用示例：
 * ```kotlin
 * @Inject lateinit var receiptTypeHelper: ReceiptTypeHelper
 * 
 * // 服务端 → UI 显示
 * val display = receiptTypeHelper.toDisplayLabel("Business")  // → "企业"
 * 
 * // UI 显示 → 服务端
 * val payload = receiptTypeHelper.toPayloadValue("企业")  // → "Business"
 * ```
 */
@Singleton
class ReceiptTypeHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** 服务端 payload 常量：公司抬头 */
    val RECEIPT_TYPE_BUSINESS = "Business"
    
    /** 服务端 payload 常量：个人抬头 */
    val RECEIPT_TYPE_INDIVIDUAL = "Individual"
    
    // 本地化标签（懒加载，避免过早访问 Resources）
    private val companyLabel by lazy { context.getString(R.string.type_company) }
    private val individualLabel by lazy { context.getString(R.string.type_individual) }
    
    /**
     * 将服务端 payload 或用户输入转换为本地化显示文本。
     * 
     * 转换规则：
     * - "Business" / "企业" / "Company" → "企业"（本地化）
     * - "Individual" / "个人" → "个人"（本地化）
     * - 其他值：原样返回（支持自定义类型）
     * 
     * @param raw 原始值（来自服务端或用户输入）
     * @return 本地化显示文本
     */
    fun toDisplayLabel(raw: String): String {
        val normalized = raw.trim()
        if (normalized.isBlank()) return ""
        
        return when {
            // 匹配公司抬头（服务端值 或 本地化值）
            normalized.equals(RECEIPT_TYPE_BUSINESS, ignoreCase = true) ||
                normalized.equals(companyLabel, ignoreCase = true) -> companyLabel

            // 匹配个人抬头（服务端值 或 本地化值）
            normalized.equals(RECEIPT_TYPE_INDIVIDUAL, ignoreCase = true) ||
                normalized.equals(individualLabel, ignoreCase = true) -> individualLabel

            // 其他值原样返回（自定义类型）
            else -> normalized
        }
    }
    
    /**
     * 将本地化显示文本或用户输入转换为服务端 payload。
     * 
     * 转换规则：
     * - "企业" / "Company" / "Business" → "Business"
     * - "个人" / "Individual" → "Individual"
     * - 其他值：原样返回（自定义类型）
     * 
     * @param raw 原始值（来自 UI 或用户输入）
     * @return 服务端 payload
     */
    fun toPayloadValue(raw: String): String {
        val normalized = raw.trim()
        if (normalized.isBlank()) return ""
        
        return when {
            // 匹配公司抬头（本地化值 或 服务端值）
            normalized.equals(companyLabel, ignoreCase = true) ||
                normalized.equals(RECEIPT_TYPE_BUSINESS, ignoreCase = true) -> RECEIPT_TYPE_BUSINESS

            // 匹配个人抬头（本地化值 或 服务端值）
            normalized.equals(individualLabel, ignoreCase = true) ||
                normalized.equals(RECEIPT_TYPE_INDIVIDUAL, ignoreCase = true) -> RECEIPT_TYPE_INDIVIDUAL

            // 其他值原样返回（自定义类型）
            else -> normalized
        }
    }
}
