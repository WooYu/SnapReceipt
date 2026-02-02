package com.snapreceipt.io.data.local.datasource

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.snapreceipt.io.data.base.BaseLocalDataSource
import com.snapreceipt.io.domain.model.PolicyEntity
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PolicyLocalDataSource @Inject constructor(
    @ApplicationContext context: Context,
    private val gson: Gson,
    dispatchers: CoroutineDispatchersProvider
) : BaseLocalDataSource(dispatchers) {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    suspend fun getPolicySync(): PolicyEntity? = withIo { readPolicy() }

    suspend fun updatePolicy(policy: PolicyEntity) = withIo { writePolicy(policy) }

    private fun readPolicy(): PolicyEntity? {
        val raw = prefs.getString(KEY_POLICY, null) ?: return null
        return runCatching { gson.fromJson(raw, PolicyEntity::class.java) }.getOrNull()
    }

    private fun writePolicy(policy: PolicyEntity) {
        prefs.edit().putString(KEY_POLICY, gson.toJson(policy)).apply()
    }

    private companion object {
        const val PREFS_NAME = "policy_cache_secure"
        const val KEY_POLICY = "policy_cache"
    }
}
