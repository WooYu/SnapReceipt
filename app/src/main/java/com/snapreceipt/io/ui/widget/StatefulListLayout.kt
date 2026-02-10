package com.snapreceipt.io.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.snapreceipt.io.R

/**
 * A composite list widget that encapsulates common list patterns:
 * - RecyclerView (list content)
 * - Multi-state container (loading/content/empty/error)
 * - In-list load-state footer (bottom item)
 *
 * Supported attrs:
 * - `sllEmptyImage`: drawable for empty state (default: `@drawable/img_receipts_empty`)
 * - `sllEmptyText`: text for empty state (default: `@string/no_content`)
 * - `sllNoMoreText`: text for no-more hint (default: `@string/no_more_records`)
 */
class StatefulListLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class ContentState {
        LOADING,
        CONTENT,
        EMPTY,
        ERROR
    }

    data class State(
        val contentState: ContentState = ContentState.CONTENT,
        val loadingMore: Boolean = false,
        val noMore: Boolean = false,
        val errorText: CharSequence? = null
    )

    private enum class FooterUiState {
        HIDDEN,
        LOADING,
        NO_MORE
    }

    companion object {
        private const val LOAD_MORE_TRIGGER_THRESHOLD = 3
    }

    val recyclerView: RecyclerView
    private val multiStateLayout: MultiStateLayout
    private val emptyImageView: ImageView
    private val emptyTextView: TextView
    private val errorTextView: TextView
    private val errorRetryView: View

    private val footerStateAdapter = FooterStateAdapter()
    private val concatAdapter = ConcatAdapter(EmptyContentAdapter, footerStateAdapter)
    private var contentAdapter: RecyclerView.Adapter<out RecyclerView.ViewHolder> = EmptyContentAdapter

    private var onLoadMoreListener: (() -> Unit)? = null
    private var onRetryListener: (() -> Unit)? = null
    private var isLoadingMoreState: Boolean = false
    private var isNoMoreState: Boolean = false
    private var hasPendingLoadMoreRequest: Boolean = false
    private var currentState: State = State()

    init {
        LayoutInflater.from(context).inflate(R.layout.view_stateful_list, this, true)

        multiStateLayout = findViewById(R.id.sll_multi_state)
        recyclerView = findViewById(R.id.sll_recycler_view)
        val loadingStateView: View = findViewById(R.id.sll_loading_state)
        val contentStateView: View = findViewById(R.id.sll_content_state)
        val emptyStateView: View = findViewById(R.id.sll_empty_state)
        val errorStateView: View = findViewById(R.id.sll_error_state)
        emptyImageView = findViewById(R.id.sll_empty_image)
        emptyTextView = findViewById(R.id.sll_empty_text)
        errorTextView = findViewById(R.id.sll_error_text)
        errorRetryView = findViewById(R.id.sll_error_retry)

        multiStateLayout.bindStateViews(
            loadingView = loadingStateView,
            contentView = contentStateView,
            emptyView = emptyStateView,
            errorView = errorStateView
        )

        val a = context.obtainStyledAttributes(attrs, R.styleable.StatefulListLayout, defStyleAttr, 0)
        a.getDrawable(R.styleable.StatefulListLayout_sllEmptyImage)?.let {
            emptyImageView.setImageDrawable(it)
        }
        a.getText(R.styleable.StatefulListLayout_sllEmptyText)?.let {
            emptyTextView.text = it
        }
        val noMoreText = a.getText(R.styleable.StatefulListLayout_sllNoMoreText)
            ?: context.getString(R.string.no_more_records)
        footerStateAdapter.setNoMoreText(noMoreText)
        a.recycle()

        recyclerView.layoutManager = LinearLayoutManager(context).apply {
            reverseLayout = false
            stackFromEnd = false
        }
        recyclerView.adapter = concatAdapter
        recyclerView.overScrollMode = View.OVER_SCROLL_NEVER
        recyclerView.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        recyclerView.itemAnimator = null

        errorRetryView.setOnClickListener {
            onRetryListener?.invoke()
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                if (currentState.contentState != ContentState.CONTENT) return
                if (onLoadMoreListener == null) return
                if (isLoadingMoreState || isNoMoreState || hasPendingLoadMoreRequest) return
                if (contentAdapter.itemCount == 0) return
                val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val total = lm.itemCount
                val lastVisible = lm.findLastVisibleItemPosition()
                if (total > 0 && lastVisible >= total - LOAD_MORE_TRIGGER_THRESHOLD) {
                    hasPendingLoadMoreRequest = true
                    onLoadMoreListener?.invoke()
                }
            }
        })

        render(currentState)
    }

    private fun updateFooterState() {
        val footerState = when {
            isLoadingMoreState -> FooterUiState.LOADING
            isNoMoreState -> FooterUiState.NO_MORE
            else -> FooterUiState.HIDDEN
        }
        footerStateAdapter.setState(footerState)
    }

    private fun renderContainerState(contentState: ContentState) {
        when (contentState) {
            ContentState.LOADING -> multiStateLayout.showLoading()
            ContentState.CONTENT -> multiStateLayout.showContent()
            ContentState.EMPTY -> multiStateLayout.showEmpty()
            ContentState.ERROR -> multiStateLayout.showError()
        }
    }

    fun setAdapter(adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>) {
        if (adapter === contentAdapter) return
        concatAdapter.removeAdapter(contentAdapter)
        contentAdapter = adapter
        concatAdapter.addAdapter(0, adapter)
        hasPendingLoadMoreRequest = false
    }

    fun setOnLoadMoreListener(listener: (() -> Unit)?) {
        onLoadMoreListener = listener
        if (listener == null) {
            hasPendingLoadMoreRequest = false
        }
    }

    fun setOnRetryListener(listener: (() -> Unit)?) {
        onRetryListener = listener
    }

    private fun setLoadingMoreVisible(visible: Boolean) {
        if (isLoadingMoreState == visible) return
        val wasLoadingMore = isLoadingMoreState
        isLoadingMoreState = visible
        if (visible) {
            hasPendingLoadMoreRequest = true
        } else if (wasLoadingMore && !isNoMoreState) {
            hasPendingLoadMoreRequest = false
        }
        updateFooterState()
    }

    private fun setNoMoreVisible(visible: Boolean) {
        if (isNoMoreState == visible) return
        isNoMoreState = visible
        if (visible) {
            hasPendingLoadMoreRequest = true
        } else if (!isLoadingMoreState) {
            hasPendingLoadMoreRequest = false
        }
        updateFooterState()
    }

    fun submit(state: State) {
        currentState = state
        render(state)
    }

    private fun render(state: State) {
        if (!state.errorText.isNullOrBlank()) {
            errorTextView.text = state.errorText
        }
        renderContainerState(state.contentState)

        val shouldShowFooter = state.contentState == ContentState.CONTENT
        val loadingMoreVisible = shouldShowFooter && state.loadingMore
        val noMoreVisible = shouldShowFooter && state.noMore && !state.loadingMore
        setLoadingMoreVisible(loadingMoreVisible)
        setNoMoreVisible(noMoreVisible)
        if (!loadingMoreVisible && !noMoreVisible) {
            hasPendingLoadMoreRequest = false
        }
    }

    fun setEmptyImage(@DrawableRes resId: Int) {
        emptyImageView.setImageResource(resId)
    }

    fun setEmptyText(text: CharSequence) {
        emptyTextView.text = text
    }

    fun setEmptyText(@StringRes resId: Int) {
        emptyTextView.setText(resId)
    }

    fun setNoMoreText(text: CharSequence) {
        footerStateAdapter.setNoMoreText(text)
    }

    fun setNoMoreText(@StringRes resId: Int) {
        setNoMoreText(context.getText(resId))
    }

    private class FooterStateAdapter : RecyclerView.Adapter<FooterStateAdapter.FooterStateViewHolder>() {

        private var state: FooterUiState = FooterUiState.HIDDEN
        private var noMoreText: CharSequence = ""

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FooterStateViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_stateful_list_footer_state, parent, false)
            return FooterStateViewHolder(view)
        }

        override fun onBindViewHolder(holder: FooterStateViewHolder, position: Int) {
            holder.bind(state, noMoreText)
        }

        override fun getItemCount(): Int = if (state == FooterUiState.HIDDEN) 0 else 1

        fun setState(newState: FooterUiState) {
            if (state == newState) return
            val hadItem = state != FooterUiState.HIDDEN
            val hasItem = newState != FooterUiState.HIDDEN
            state = newState
            when {
                !hadItem && hasItem -> notifyItemInserted(0)
                hadItem && !hasItem -> notifyItemRemoved(0)
                else -> notifyItemChanged(0)
            }
        }

        fun setNoMoreText(text: CharSequence) {
            noMoreText = text
            if (state == FooterUiState.NO_MORE) {
                notifyItemChanged(0)
            }
        }

        class FooterStateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val loadingContainer: View = itemView.findViewById(R.id.sll_footer_loading_container)
            private val noMoreTextView: TextView = itemView.findViewById(R.id.sll_footer_no_more_text)

            fun bind(state: FooterUiState, noMoreText: CharSequence) {
                when (state) {
                    FooterUiState.LOADING -> {
                        loadingContainer.visibility = VISIBLE
                        noMoreTextView.visibility = GONE
                    }

                    FooterUiState.NO_MORE -> {
                        loadingContainer.visibility = GONE
                        noMoreTextView.visibility = VISIBLE
                        noMoreTextView.text = noMoreText
                    }

                    FooterUiState.HIDDEN -> {
                        loadingContainer.visibility = GONE
                        noMoreTextView.visibility = GONE
                    }
                }
            }
        }
    }

    private object EmptyContentAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            error("EmptyContentAdapter never creates ViewHolder because itemCount is 0")
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit

        override fun getItemCount(): Int = 0
    }
}
