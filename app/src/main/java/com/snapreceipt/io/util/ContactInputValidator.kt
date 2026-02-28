package com.snapreceipt.io.util

object ContactInputValidator {

    fun isEmailValid(email: String): Boolean {
        if (email.isBlank()) return false
        val pattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        return email.matches(Regex(pattern))
    }

    /**
     * 校验手机号格式：支持 5-15 位数字
     */
    fun isPhoneValid(phone: String): Boolean {
        if (phone.isBlank()) return false
        val pattern = "^\\d{5,15}$"
        return phone.matches(Regex(pattern))
    }

    /**
     * 校验验证码格式：6位数字
     */
    fun isVerificationCodeValid(code: String): Boolean {
        if (code.isBlank()) return false
        val pattern = "^\\d{6}$"
        return code.matches(Regex(pattern))
    }
}