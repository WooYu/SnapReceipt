package com.skybound.space.base.presentation

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Build
import android.view.View
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
import com.skybound.space.base.R
import com.skybound.space.core.network.auth.SessionEvent
import com.skybound.space.core.network.auth.SessionManager
import kotlinx.coroutines.launch

abstract class BaseActivity<VM : com.skybound.space.base.presentation.viewmodel.BaseViewModel> :
    AppCompatActivity(),
    LoadingOverlayHost,
    UiEventDispatcher.Host {

    protected open val viewModel: VM? = null
    protected open val sessionManager: SessionManager? = null
    private var sessionEventHandled = false
    private var loadingDialogController: LoadingDialogController? = null
    protected open val useDefaultNavigationBarInsets: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        if (useDefaultNavigationBarInsets) {
            applyDefaultNavigationBarInsets()
        }
    }

    private fun observeEvents() {
        val vm = viewModel ?: return
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.events.collect { event ->
                    UiEventDispatcher.dispatch(this@BaseActivity, event)
                }
            }
        }
    }

    private fun observeSessionEvents() {
        val manager = sessionManager ?: return
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                manager.events.collect { event ->
                    when (event) {
                        SessionEvent.AccessTokenRefreshFailed -> onAccessTokenRefreshFailed()
                        SessionEvent.RequireLogin, SessionEvent.LoggedOut -> {
                            if (sessionEventHandled) return@collect
                            sessionEventHandled = true
                            onSessionExpired(event)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sessionEventHandled = false
    }

    // -- UiEventDispatcher.Host --

    override fun hostContext(): Context = this
    override fun hostRootView(): View? = findViewById(android.R.id.content)
    override fun onNavigateBack() { onBackPressedDispatcher.onBackPressed() }
    override fun onNavigate(command: NavigationCommand) {}
    override fun onDialog(dialog: UiEvent.Dialog) {}
    override fun onSnackbarAction(actionId: String) {}
    override fun onCustomEvent(event: UiEvent.Custom) {}

    open fun onSessionExpired(event: SessionEvent) {}

    /**
     * 静默刷新 access token 失败（网络或业务失败，非 refresh 作废）时提示用户。
     * 任意注入 [sessionManager] 的 Activity 在 STARTED 时都会收到事件，默认在此统一 Toast，
     * 避免仅 MainActivity 在前台时子页用户无反馈。
     */
    open fun onAccessTokenRefreshFailed() {
        if (isFinishing || isDestroyed) return
        UiEventDispatcher.dispatch(
            this,
            UiEvent.Toast(message = "", resId = R.string.base_session_refresh_failed)
        )
    }

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