package com.skybound.space.base.presentation.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView

class MultiStateLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    enum class State {
        CONTENT,
        LOADING,
        EMPTY,
        ERROR
    }

    private var contentView: View? = null
    private var currentState: State = State.CONTENT

    private val loadingView: ProgressBar by lazy {
        ProgressBar(context).apply {
            visibility = View.GONE
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        }
    }

    private val emptyView: TextView by lazy {
        TextView(context).apply {
            visibility = View.GONE
            text = "No data"
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
    }

    private val errorView: TextView by lazy {
        TextView(context).apply {
            visibility = View.GONE
            text = "Load failed"
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (childCount > 0) {
            contentView = getChildAt(0)
        }
        ensureStateViews()
    }

    fun showContent() {
        updateState(State.CONTENT)
    }

    fun showLoading() {
        updateState(State.LOADING)
    }

    fun showEmpty(message: CharSequence? = null) {
        message?.let { emptyView.text = it }
        updateState(State.EMPTY)
    }

    fun showError(message: CharSequence? = null) {
        message?.let { errorView.text = it }
        updateState(State.ERROR)
    }

    private fun ensureStateViews() {
        if (loadingView.parent == null) addView(loadingView)
        if (emptyView.parent == null) addView(emptyView)
        if (errorView.parent == null) addView(errorView)
    }

    private fun updateState(state: State) {
        currentState = state
        contentView?.visibility = if (state == State.CONTENT) View.VISIBLE else View.GONE
        loadingView.visibility = if (state == State.LOADING) View.VISIBLE else View.GONE
        emptyView.visibility = if (state == State.EMPTY) View.VISIBLE else View.GONE
        errorView.visibility = if (state == State.ERROR) View.VISIBLE else View.GONE
    }
}
