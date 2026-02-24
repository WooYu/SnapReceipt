package com.snapreceipt.io.util

import android.util.Patterns

object ContactInputValidator {
    private const val PHONE_MIN_LENGTH = 5
    private const val PHONE_MAX_LENGTH = 13

    fun isEmailValid(email: String): Boolean {
        val normalized = email.trim()
        return normalized.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(normalized).matches()
    }

    fun isPhoneLengthValid(phone: String): Boolean {
        val normalized = phone.trim()
        return normalized.length in PHONE_MIN_LENGTH..PHONE_MAX_LENGTH
    }
}
