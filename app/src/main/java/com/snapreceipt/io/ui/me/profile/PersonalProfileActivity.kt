package com.snapreceipt.io.ui.me.profile

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.skybound.space.base.presentation.observeState
import com.snapreceipt.io.R
import com.snapreceipt.io.databinding.ActivityPersonalProfileBinding
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

    private var _binding: ActivityPersonalProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityPersonalProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.pageHeader.setOnLeftIconClickListener { finish() }

        observeState(getUserUseCase()) { result ->
            val user = result.getOrNull()
            binding.profileName.text = user?.username?.ifBlank { placeholder() } ?: placeholder()
            binding.profileEmail.text = user?.email?.ifBlank { placeholder() } ?: placeholder()
            binding.profilePhone.text = user?.phone?.ifBlank { placeholder() } ?: placeholder()
        }
        refreshUserProfile()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
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
