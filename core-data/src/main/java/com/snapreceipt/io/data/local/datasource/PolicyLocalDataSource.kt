package com.snapreceipt.io.data.local.datasource

import android.content.Context
import com.google.gson.Gson
import com.snapreceipt.io.data.base.BaseLocalDataSource
import com.snapreceipt.io.domain.model.PolicyEntity
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.skybound.space.core.security.SecureStringPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PolicyLocalDataSource @Inject constructor(
    @ApplicationContext context: Context,
    private val gson: Gson,
    dispatchers: CoroutineDispatchersProvider
) : BaseLocalDataSource(dispatchers) {

    private val prefs = SecureStringPreferences(context, PREFS_NAME)

    suspend fun getPolicySync(): PolicyEntity? = withIo { readPolicy() }

    suspend fun updatePolicy(policy: PolicyEntity) = withIo { writePolicy(policy) }

    private fun readPolicy(): PolicyEntity? {
        val raw = prefs.getString(KEY_POLICY, null) ?: return null
        return runCatching { gson.fromJson(raw, PolicyEntity::class.java) }.getOrNull()
    }

    private fun writePolicy(policy: PolicyEntity) {
        prefs.putString(KEY_POLICY, gson.toJson(policy))
    }

    private companion object {
        const val PREFS_NAME = "policy_cache_secure"
        const val KEY_POLICY = "policy_cache"
    }
}
