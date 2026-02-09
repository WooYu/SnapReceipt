package com.skybound.space.base.presentation

import android.graphics.Color
import android.os.Bundle
import android.os.Build
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.color.MaterialColors
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
    AppCompatActivity(),
    LoadingOverlayHost {

    /**
     * 默认支持无 ViewModel 的 Activity（如纯展示页）。
     * 需要事件分发的页面直接覆写为非空 VM。
     */
    protected open val viewModel: VM? = null
    protected open val sessionManager: SessionManager? = null
    private var sessionEventHandled = false
    private var loadingDialogController: LoadingDialogController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw into status bar area, but keep navigation bar area reserved by default insets handling.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = MaterialColors.getColor(
            this,
            com.google.android.material.R.attr.colorSurface,
            Color.WHITE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        observeEvents()
        observeSessionEvents()
    }

    override fun onContentChanged() {
        super.onContentChanged()
        applyDefaultNavigationBarInsets()
    }

    private fun observeEvents() {
        val vm = viewModel ?: return
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.events.collect { event ->
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

    /**
     * 全局 loading 由 BaseActivity 统一托管，Fragment 只需要通过 LoadingOverlayHost 调用。
     */
    override fun showGlobalLoading(message: CharSequence?) {
        if (isFinishing || isDestroyed) return
        ensureLoadingController().show(message)
    }

    override fun hideGlobalLoading() {
        loadingDialogController?.hide()
    }

    override fun onDestroy() {
        loadingDialogController?.hide()
        loadingDialogController = null
        super.onDestroy()
    }

    private fun ensureLoadingController(): LoadingDialogController {
        return loadingDialogController ?: LoadingDialogController(supportFragmentManager).also {
            loadingDialogController = it
        }
    }

    private fun applyDefaultNavigationBarInsets() {
        val contentParent = findViewById<ViewGroup>(android.R.id.content) ?: return
        val contentRoot = contentParent.getChildAt(0) ?: return
        val initialLeft = contentRoot.paddingLeft
        val initialTop = contentRoot.paddingTop
        val initialRight = contentRoot.paddingRight
        val initialBottom = contentRoot.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(contentRoot) { view, insets ->
            val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.updatePadding(
                left = initialLeft + navInsets.left,
                top = initialTop,
                right = initialRight + navInsets.right,
                bottom = initialBottom + navInsets.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(contentRoot)
    }
}
