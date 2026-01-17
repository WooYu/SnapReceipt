package com.snapreceipt.io.ocr

import android.graphics.BitmapFactory
import com.snapreceipt.io.ocr.model.OCRResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.io.File

/**
 * Local ML Kit OCR implementation using on-device Text Recognition.
 */
class MLKitOCRService : OCRService {
    override suspend fun recognizeImage(imagePath: String): OCRResult? {
        try {
            val file = File(imagePath)
            if (!file.exists()) return OCRResult(code = -2, msg = "file not found", data = null)
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return OCRResult(code = -3, msg = "unable to decode image", data = null)
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val resultText = suspendCancellableCoroutine<String> { cont ->
                val task = recognizer.process(image)
                task.addOnSuccessListener { visionText ->
                    cont.resume(visionText.text)
                }
                task.addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
            }
            return OCRResult(code = 0, msg = "OK", data = mapOf("text" to resultText))
        } catch (e: Exception) {
            return OCRResult(code = -1, msg = e.message, data = null)
        }
    }
}
