package com.snapreceipt.io.ui.common

import androidx.annotation.StringRes
import com.snapreceipt.io.R
import com.skybound.space.base.presentation.BaseActivity
import com.skybound.space.base.presentation.viewmodel.BaseViewModel

/**
 * 兼容壳类：历史页面仍可继承该类，实际能力全部来自 BaseActivity。
 * 后续新页面可直接继承 BaseActivity（若需要 ViewModel 事件）或继续沿用该壳类。
 */
open class EdgeToEdgeActivity : BaseActivity<BaseViewModel>() {

    override val viewModel: BaseViewModel? = null

    protected fun showLoadingOverlay(show: Boolean, message: CharSequence? = null) {
        if (show) {
            showGlobalLoading(message)
        } else {
            hideGlobalLoading()
        }
    }

    protected fun showLoadingOverlay(@StringRes messageRes: Int = R.string.loading) {
        showGlobalLoading(getString(messageRes))
    }

    protected fun hideLoadingOverlay() {
        hideGlobalLoading()
    }
}
