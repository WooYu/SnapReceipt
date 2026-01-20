package com.skybound.space.base.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.skybound.space.core.network.auth.SessionEvent
import com.skybound.space.core.network.auth.SessionManager
import kotlinx.coroutines.launch

/**
 * Activity 基类：只负责监听 UiEvent，具体 UI 渲染留给子类。
 */
abstract class BaseActivity<VM : com.skybound.space.base.presentation.viewmodel.BaseViewModel> :
    AppCompatActivity() {

    protected abstract val viewModel: VM
    protected open val sessionManager: SessionManager? = null
    private var sessionEventHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeEvents()
        observeSessionEvents()
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is UiEvent.Toast -> {
                            val text = event.message.ifBlank { event.resId?.let { getString(it) } ?: "" }
                            android.widget.Toast.makeText(
                                this@BaseActivity,
                                text,
                                if (event.long) android.widget.Toast.LENGTH_LONG else android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                        is UiEvent.Snackbar -> {
                            val root = findViewById<android.view.View>(android.R.id.content)
                            Snackbar.make(root, event.message, Snackbar.LENGTH_LONG).apply {
                                if (event.actionLabel != null && event.actionId != null) {
                                    setAction(event.actionLabel) { onSnackbarAction(event.actionId) }
                                }
                            }.show()
                        }
                        is UiEvent.Dialog -> onDialog(event)
                        is UiEvent.Navigation -> onNavigate(event.command)
                        UiEvent.NavigateBack -> onBackPressedDispatcher.onBackPressed()
                        is UiEvent.Custom -> onCustomEvent(event)
                    }
                }
            }
        }
    }

    private fun observeSessionEvents() {
        val manager = sessionManager ?: return
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                manager.events.collect { event ->
                    if (sessionEventHandled) return@collect
                    sessionEventHandled = true
                    onSessionExpired(event)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sessionEventHandled = false
    }

    open fun onNavigate(command: NavigationCommand) {
        // 留给子类根据 NavController/Compose 实现
    }

    open fun onDialog(dialog: UiEvent.Dialog) {
        // 默认不弹出对话框，交给具体页面决定
    }

    open fun onSnackbarAction(actionId: String) {
        // 子类处理具体动作
    }

    open fun onCustomEvent(event: UiEvent.Custom) {
        // 子类处理特定事件
    }

    open fun onSessionExpired(event: SessionEvent) {
        // 子类根据登录态失效处理
    }
}
