package com.skybound.space.core.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureStringPreferences(
    context: Context,
    prefsName: String
) {

    private val currentPrefs: SharedPreferences =
        context.getSharedPreferences("${prefsName}_v2", Context.MODE_PRIVATE)
    private val encryptor = KeyStoreStringEncryptor(KEY_ALIAS_PREFIX + prefsName)

    fun getString(key: String, defaultValue: String? = null): String? {
        readCurrent(key)?.let { return it }
        return defaultValue
    }

    fun putString(key: String, value: String?) {
        if (value == null) {
            remove(key)
            return
        }

        currentPrefs.edit()
            .putString(key, encryptor.encrypt(value))
            .apply()
    }

    fun remove(key: String) {
        currentPrefs.edit().remove(key).apply()
    }

    fun clear() {
        currentPrefs.edit().clear().apply()
    }

    private fun readCurrent(key: String): String? {
        val encryptedValue = currentPrefs.getString(key, null) ?: return null
        val decrypted = runCatching { encryptor.decrypt(encryptedValue) }.getOrNull()
        if (decrypted == null) {
            currentPrefs.edit().remove(key).apply()
        }
        return decrypted
    }

    private companion object {
        private const val KEY_ALIAS_PREFIX = "snapreceipt.secureprefs."
    }
}

private class KeyStoreStringEncryptor(
    private val keyAlias: String
) {

    fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

        val encryptedBytes = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val payload = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        return "$iv:$payload"
    }

    fun decrypt(value: String): String {
        val parts = value.split(SEPARATOR, limit = 2)
        require(parts.size == 2) { "Malformed encrypted value" }

        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val payload = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(TAG_LENGTH_BITS, iv)
        )
        return String(cipher.doFinal(payload), Charsets.UTF_8)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val TAG_LENGTH_BITS = 128
        private const val SEPARATOR = ":"
    }
}
