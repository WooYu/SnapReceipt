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
 * - Empty state (image + text)
 * - In-list load-state footer (bottom item)
 * - Center loading indicator (large spinner for initial load)
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

    private enum class FooterUiState {
        HIDDEN,
        LOADING,
        NO_MORE
    }

    companion object {
        private const val LOAD_MORE_TRIGGER_THRESHOLD = 3
    }

    val recyclerView: RecyclerView
    private val emptyStateView: View
    private val emptyImageView: ImageView
    private val emptyTextView: TextView
    private val centerLoadingView: View

    private val footerStateAdapter = FooterStateAdapter()
    private val concatAdapter = ConcatAdapter(EmptyContentAdapter, footerStateAdapter)
    private var contentAdapter: RecyclerView.Adapter<out RecyclerView.ViewHolder> = EmptyContentAdapter

    private var onLoadMoreListener: (() -> Unit)? = null
    private var isRefreshingState: Boolean = false
    private var isLoadingMoreState: Boolean = false
    private var isNoMoreState: Boolean = false

    init {
        LayoutInflater.from(context).inflate(R.layout.view_stateful_list, this, true)

        recyclerView = findViewById(R.id.sll_recycler_view)
        emptyStateView = findViewById(R.id.sll_empty_state)
        emptyImageView = findViewById(R.id.sll_empty_image)
        emptyTextView = findViewById(R.id.sll_empty_text)
        centerLoadingView = findViewById(R.id.sll_center_loading)

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

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = concatAdapter

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                if (onLoadMoreListener == null) return
                if (isLoadingMoreState || isNoMoreState) return
                if (contentAdapter.itemCount == 0) return
                val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val total = lm.itemCount
                val lastVisible = lm.findLastVisibleItemPosition()
                if (total > 0 && lastVisible >= total - LOAD_MORE_TRIGGER_THRESHOLD) {
                    onLoadMoreListener?.invoke()
                }
            }
        })
    }

    private fun updateFooterState() {
        val footerState = when {
            isLoadingMoreState -> FooterUiState.LOADING
            isNoMoreState -> FooterUiState.NO_MORE
            else -> FooterUiState.HIDDEN
        }
        footerStateAdapter.setState(footerState)
    }

    fun setAdapter(adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>) {
        if (adapter === contentAdapter) return
        concatAdapter.removeAdapter(contentAdapter)
        contentAdapter = adapter
        concatAdapter.addAdapter(0, adapter)
    }

    fun setOnLoadMoreListener(listener: () -> Unit) {
        onLoadMoreListener = listener
    }

    fun setRefreshing(refreshing: Boolean) {
        if (isRefreshingState == refreshing) return
        isRefreshingState = refreshing
    }

    fun showContent() {
        recyclerView.visibility = VISIBLE
        emptyStateView.visibility = GONE
    }

    fun showEmpty() {
        recyclerView.visibility = GONE
        emptyStateView.visibility = VISIBLE
    }

    fun showLoadingMore(visible: Boolean) {
        if (isLoadingMoreState == visible) return
        isLoadingMoreState = visible
        updateFooterState()
    }

    fun showNoMore(visible: Boolean) {
        if (isNoMoreState == visible) return
        isNoMoreState = visible
        updateFooterState()
    }

    fun showCenterLoading(visible: Boolean) {
        centerLoadingView.visibility = if (visible) VISIBLE else GONE
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

    fun applyState(
        hasLoaded: Boolean,
        isEmpty: Boolean,
        hasMore: Boolean,
        itemCount: Int,
        refreshing: Boolean,
        loadingMore: Boolean,
        centerLoading: Boolean = false
    ) {
        setRefreshing(refreshing)

        val showEmpty = hasLoaded && isEmpty
        if (showEmpty) showEmpty() else showContent()

        showLoadingMore(loadingMore)

        val showNoMore = hasLoaded && !hasMore && itemCount > 0 && !loadingMore
        showNoMore(showNoMore)

        showCenterLoading(centerLoading)
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
