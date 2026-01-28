package com.snapreceipt.io.ui.me.profile

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.snapreceipt.io.R
import com.snapreceipt.io.domain.usecase.auth.AuthFetchUserProfileUseCase
import com.snapreceipt.io.domain.usecase.user.GetUserUseCase
import com.snapreceipt.io.domain.usecase.user.InsertUserUseCase
import com.snapreceipt.io.ui.common.EdgeToEdgeActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PersonalProfileActivity : EdgeToEdgeActivity() {

    @Inject
    lateinit var getUserUseCase: GetUserUseCase

    @Inject
    lateinit var fetchUserProfileUseCase: AuthFetchUserProfileUseCase

    @Inject
    lateinit var insertUserUseCase: InsertUserUseCase

    private lateinit var nameValue: TextView
    private lateinit var emailValue: TextView
    private lateinit var phoneValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_profile)

        nameValue = findViewById(R.id.profile_name)
        emailValue = findViewById(R.id.profile_email)
        phoneValue = findViewById(R.id.profile_phone)

        findViewById<android.view.View>(R.id.btn_back).setOnClickListener { finish() }

        observeUser()
        refreshUserProfile()
    }

    private fun observeUser() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                getUserUseCase().collect { result ->
                    val user = result.getOrNull()
                    nameValue.text = user?.username?.ifBlank { placeholder() } ?: placeholder()
                    emailValue.text = user?.email?.ifBlank { placeholder() } ?: placeholder()
                    phoneValue.text = user?.phone?.ifBlank { placeholder() } ?: placeholder()
                }
            }
        }
    }

    private fun refreshUserProfile() {
        lifecycleScope.launch {
            fetchUserProfileUseCase()
                .onSuccess { user -> insertUserUseCase(user) }
                .onFailure {
                    Toast.makeText(this@PersonalProfileActivity, getString(R.string.unexpected_error), Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun placeholder(): String = getString(R.string.placeholder_dash)
}
