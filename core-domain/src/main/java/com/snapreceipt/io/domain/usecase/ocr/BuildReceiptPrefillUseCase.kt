package com.snapreceipt.io.domain.usecase.ocr

import com.snapreceipt.io.domain.model.ReceiptPrefillEntity
import com.snapreceipt.io.domain.repository.OcrRepository
import java.util.Locale
import javax.inject.Inject

class BuildReceiptPrefillUseCase @Inject constructor(
    private val repository: OcrRepository
) {
    suspend operator fun invoke(imagePath: String): Result<ReceiptPrefillEntity> =
        runCatching {
            val result = repository.recognizeImage(imagePath)
            val merchant = result.merchant?.takeIf { it.isNotBlank() } ?: parseMerchant(result.text)
            val amount = result.amount?.let { formatAmount(it) } ?: parseAmount(result.text)
            ReceiptPrefillEntity(
                imagePath = imagePath,
                merchant = merchant,
                amount = amount
            )
        }

    private fun parseMerchant(text: String): String {
        return text.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
            .orEmpty()
    }

    private fun parseAmount(text: String): String {
        val amountRegex = Regex("(\\d+[\\.,]\\d{2})")
        val amountMatch = amountRegex.find(text) ?: return ""
        return amountMatch.value.replace(",", ".")
    }

    private fun formatAmount(amount: Double): String {
        return String.format(Locale.getDefault(), "%.2f", amount)
    }
}
