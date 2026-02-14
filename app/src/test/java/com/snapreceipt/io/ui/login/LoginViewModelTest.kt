package com.snapreceipt.io.ui.login

import com.google.common.truth.Truth.assertThat
import com.snapreceipt.io.domain.model.AuthTokensEntity
import com.snapreceipt.io.domain.model.PolicyEntity
import com.snapreceipt.io.domain.model.UserEntity
import com.snapreceipt.io.domain.usecase.auth.AuthFetchUserProfileUseCase
import com.snapreceipt.io.domain.usecase.auth.AuthLoginUseCase
import com.snapreceipt.io.domain.usecase.auth.AuthRequestCodeUseCase
import com.snapreceipt.io.domain.usecase.config.FetchPolicyUseCase
import com.snapreceipt.io.domain.usecase.user.InsertUserUseCase
import com.skybound.space.base.presentation.UiEvent
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test

class LoginViewModelTest {

    @Test
    fun submitEmailLogin_profileFetchFailed_doesNotNavigate() = runBlocking {
        val requestCodeUseCase = mockk<AuthRequestCodeUseCase>(relaxed = true)
        val loginUseCase = mockk<AuthLoginUseCase>()
        val fetchUserProfileUseCase = mockk<AuthFetchUserProfileUseCase>()
        val insertUserUseCase = mockk<InsertUserUseCase>(relaxed = true)
        val fetchPolicyUseCase = mockk<FetchPolicyUseCase>()
        val dispatchers = CoroutineDispatchersProvider.SingleThread("login-viewmodel-test-1")

        coEvery { fetchPolicyUseCase.invoke() } returns Result.success(PolicyEntity())
        coEvery { loginUseCase.invoke(any(), any(), any()) } returns Result.success(
            AuthTokensEntity("access-token", "refresh-token")
        )
        coEvery { fetchUserProfileUseCase.invoke() } returns Result.failure(
            IllegalStateException("profile failed")
        )

        val viewModel = LoginViewModel(
            requestCodeUseCase,
            loginUseCase,
            fetchUserProfileUseCase,
            insertUserUseCase,
            fetchPolicyUseCase,
            dispatchers
        )
        viewModel.setAgreementAccepted(true)

        val events = mutableListOf<UiEvent>()
        val collectJob = launch(dispatchers.main) {
            viewModel.events.collect { events += it }
        }

        viewModel.submitEmailLogin("demo@snapreceipt.io", "123456")
        delay(200)

        val hasNavigateMain = events.filterIsInstance<UiEvent.Custom>()
            .any { it.type == LoginEventKeys.NAVIGATE_MAIN }
        assertThat(hasNavigateMain).isFalse()
        assertThat(viewModel.uiState.value.loading).isFalse()

        collectJob.cancel()
        dispatchers.close()
    }

    @Test
    fun submitEmailLogin_everythingSucceeded_navigatesToMain() = runBlocking {
        val requestCodeUseCase = mockk<AuthRequestCodeUseCase>(relaxed = true)
        val loginUseCase = mockk<AuthLoginUseCase>()
        val fetchUserProfileUseCase = mockk<AuthFetchUserProfileUseCase>()
        val insertUserUseCase = mockk<InsertUserUseCase>()
        val fetchPolicyUseCase = mockk<FetchPolicyUseCase>()
        val dispatchers = CoroutineDispatchersProvider.SingleThread("login-viewmodel-test-2")

        coEvery { fetchPolicyUseCase.invoke() } returns Result.success(PolicyEntity())
        coEvery { loginUseCase.invoke(any(), any(), any()) } returns Result.success(
            AuthTokensEntity("access-token", "refresh-token")
        )
        coEvery { fetchUserProfileUseCase.invoke() } returns Result.success(
            UserEntity(id = 1L, username = "demo")
        )
        coEvery { insertUserUseCase.invoke(any()) } returns Result.success(Unit)

        val viewModel = LoginViewModel(
            requestCodeUseCase,
            loginUseCase,
            fetchUserProfileUseCase,
            insertUserUseCase,
            fetchPolicyUseCase,
            dispatchers
        )
        viewModel.setAgreementAccepted(true)

        val events = mutableListOf<UiEvent>()
        val collectJob = launch(dispatchers.main) {
            viewModel.events.collect { events += it }
        }

        viewModel.submitEmailLogin("demo@snapreceipt.io", "123456")
        delay(200)

        val hasNavigateMain = events.filterIsInstance<UiEvent.Custom>()
            .any { it.type == LoginEventKeys.NAVIGATE_MAIN }
        assertThat(hasNavigateMain).isTrue()
        assertThat(viewModel.uiState.value.loading).isFalse()

        collectJob.cancel()
        dispatchers.close()
    }
}
