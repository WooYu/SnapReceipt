package com.snapreceipt.io.util

import android.util.Patterns

object ContactInputValidator {

    fun isEmailValid(email: String): Boolean {
        val normalized = email.trim()
        return normalized.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(normalized).matches()
    }

    /**
     * 宽松校验手机号：去除非数字字符和自动填充可能携带的国际区号后，
     * 只检查位数是否在 7-15 位的合理范围内，兼容全球主流号码格式。
     */
    fun isPhoneLengthValid(phone: String): Boolean {
        val digits = phone.trim().replace(Regex("[^\\d]"), "")
        val stripped = if (digits.length > 11 && digits.startsWith("86")) digits.substring(2) else digits
        return stripped.length in 7..15
    }
}