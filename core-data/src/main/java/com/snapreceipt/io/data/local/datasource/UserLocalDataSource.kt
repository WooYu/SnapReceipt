package com.snapreceipt.io.data.local.datasource

import android.content.Context
import com.google.gson.Gson
import com.snapreceipt.io.data.base.BaseLocalDataSource
import com.snapreceipt.io.domain.model.UserEntity
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import com.skybound.space.core.security.SecureStringPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

class UserLocalDataSource @Inject constructor(
    @ApplicationContext context: Context,
    private val gson: Gson,
    dispatchers: CoroutineDispatchersProvider
) : BaseLocalDataSource(dispatchers) {

    private val prefs = SecureStringPreferences(context, PREFS_NAME)

    private val userState = MutableStateFlow(readUser())

    fun getUser(): Flow<UserEntity?> = userState

    suspend fun getUserSync(): UserEntity? = withIo { readUser() }

    suspend fun updateUser(user: UserEntity) = withIo { writeUser(user) }

    suspend fun insertUser(user: UserEntity) = withIo { writeUser(user) }

    private fun readUser(): UserEntity? {
        val raw = prefs.getString(KEY_USER, null) ?: return null
        return runCatching { gson.fromJson(raw, UserEntity::class.java) }.getOrNull()
    }

    private fun writeUser(user: UserEntity) {
        prefs.putString(KEY_USER, gson.toJson(user))
        userState.value = user
    }

    private companion object {
        const val PREFS_NAME = "user_profile_secure"
        const val KEY_USER = "user_profile"
    }
}
