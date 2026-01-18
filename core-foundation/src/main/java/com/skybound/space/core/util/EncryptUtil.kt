package com.skybound.space.core.util

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptUtil {
    fun md5(input: String): String = digest("MD5", input)
    fun sha256(input: String): String = digest("SHA-256", input)

    fun aesEncrypt(plainText: String, key: ByteArray, iv: ByteArray): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    fun aesDecrypt(cipherText: String, key: ByteArray, iv: ByteArray): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        val decoded = Base64.decode(cipherText, Base64.NO_WRAP)
        return String(cipher.doFinal(decoded), Charsets.UTF_8)
    }

    private fun digest(algorithm: String, input: String): String {
        val bytes = MessageDigest.getInstance(algorithm).digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
