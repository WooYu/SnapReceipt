package com.snapreceipt.io.ui.me.feedback

import com.skybound.space.base.presentation.UiEvent
import com.skybound.space.base.presentation.viewmodel.BaseViewModel
import com.snapreceipt.io.domain.usecase.feedback.SubmitFeedbackUseCase
import com.snapreceipt.io.domain.usecase.user.GetUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val submitFeedbackUseCase: SubmitFeedbackUseCase,
    private val getUserUseCase: GetUserUseCase
) : BaseViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private var userContact: String = ""

    init {
        fetchUserContact()
    }

    private fun fetchUserContact() {
        viewModelScope.launch {
            runCatching { getUserUseCase() }
                .onSuccess { user ->
                    userContact = user.phone.ifBlank { user.email }
                }
        }
    }

    fun submit(content: String) {
        launchWithLoading(
            updateLoading = { _loading.value = it },
            block = { submitFeedbackUseCase(userContact, content) },
            onSuccess = {
                emitEvent(UiEvent.Toast("Feedback submitted successfully"))
                emitEvent(UiEvent.NavigateBack)
            }
        )
    }
}
