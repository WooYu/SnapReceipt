package com.snapreceipt.io.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible

/**
 * Lightweight multi-state container for:
 * - loading
 * - content
 * - empty
 * - error
 */
class MultiStateLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class State {
        LOADING,
        CONTENT,
        EMPTY,
        ERROR
    }

    private var loadingView: View? = null
    private var contentView: View? = null
    private var emptyView: View? = null
    private var errorView: View? = null

    var state: State = State.CONTENT
        private set

    fun bindStateViews(
        loadingView: View,
        contentView: View,
        emptyView: View,
        errorView: View
    ) {
        this.loadingView = loadingView
        this.contentView = contentView
        this.emptyView = emptyView
        this.errorView = errorView
        render()
    }

    fun showLoading() = setState(State.LOADING)

    fun showContent() = setState(State.CONTENT)

    fun showEmpty() = setState(State.EMPTY)

    fun showError() = setState(State.ERROR)

    fun setState(newState: State) {
        if (state == newState) return
        state = newState
        render()
    }

    private fun render() {
//        loadingView?.isVisible = state == State.LOADING
        contentView?.isVisible = state == State.CONTENT
        emptyView?.isVisible = state == State.EMPTY
        errorView?.isVisible = state == State.ERROR
    }
}

