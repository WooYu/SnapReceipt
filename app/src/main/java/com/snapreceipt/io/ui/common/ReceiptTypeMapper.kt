package com.snapreceipt.io.ui.common

/**
 * Maps receipt type between server payload values and localized display labels.
 */
object ReceiptTypeMapper {
    const val RECEIPT_TYPE_BUSINESS = "Business"
    const val RECEIPT_TYPE_INDIVIDUAL = "Individual"

    fun toDisplayLabel(raw: String, companyLabel: String, individualLabel: String): String {
        val normalized = raw.trim()
        if (normalized.isBlank()) return ""
        return when {
            normalized.equals(RECEIPT_TYPE_BUSINESS, ignoreCase = true) ||
                normalized.equals(companyLabel, ignoreCase = true) -> companyLabel

            normalized.equals(RECEIPT_TYPE_INDIVIDUAL, ignoreCase = true) ||
                normalized.equals(individualLabel, ignoreCase = true) -> individualLabel

            else -> normalized
        }
    }

    fun toPayloadValue(raw: String, companyLabel: String, individualLabel: String): String {
        val normalized = raw.trim()
        if (normalized.isBlank()) return ""
        return when {
            normalized.equals(companyLabel, ignoreCase = true) ||
                normalized.equals(RECEIPT_TYPE_BUSINESS, ignoreCase = true) -> RECEIPT_TYPE_BUSINESS

            normalized.equals(individualLabel, ignoreCase = true) ||
                normalized.equals(RECEIPT_TYPE_INDIVIDUAL, ignoreCase = true) -> RECEIPT_TYPE_INDIVIDUAL

            else -> normalized
        }
    }
}

