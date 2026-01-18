package com.snapreceipt.io.data.manager

import com.skybound.space.core.util.EncryptUtil

class PrivacyEncryptManager(
    private val keyProvider: () -> ByteArray,
    private val ivProvider: () -> ByteArray
) {
    enum class SensitiveType {
        PHONE,
        ID_CARD,
        BANK_CARD,
        UNKNOWN
    }

    fun encrypt(value: String): String {
        return EncryptUtil.aesEncrypt(value, keyProvider(), ivProvider())
    }

    fun decrypt(value: String): String {
        return EncryptUtil.aesDecrypt(value, keyProvider(), ivProvider())
    }

    fun mask(value: String): String {
        return mask(value, detectType(value))
    }

    fun mask(value: String, type: SensitiveType): String {
        return when (type) {
            SensitiveType.PHONE -> maskMiddle(value, 3, 4)
            SensitiveType.ID_CARD -> maskMiddle(value, 4, 4)
            SensitiveType.BANK_CARD -> maskMiddle(value, 4, 4)
            SensitiveType.UNKNOWN -> value
        }
    }

    fun detectType(value: String): SensitiveType {
        val trimmed = value.trim()
        return when {
            PHONE_REGEX.matches(trimmed) -> SensitiveType.PHONE
            ID_CARD_REGEX.matches(trimmed) -> SensitiveType.ID_CARD
            BANK_CARD_REGEX.matches(trimmed) -> SensitiveType.BANK_CARD
            else -> SensitiveType.UNKNOWN
        }
    }

    private fun maskMiddle(value: String, keepStart: Int, keepEnd: Int): String {
        if (value.length <= keepStart + keepEnd) return value
        val start = value.substring(0, keepStart)
        val end = value.substring(value.length - keepEnd)
        val maskLength = value.length - keepStart - keepEnd
        return start + "*".repeat(maskLength) + end
    }

    companion object {
        private val PHONE_REGEX = Regex("^1\\d{10}$")
        private val ID_CARD_REGEX = Regex("^\\d{15}(\\d{2}[0-9Xx])?$")
        private val BANK_CARD_REGEX = Regex("^\\d{12,19}$")
    }
}
