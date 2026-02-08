package com.snapreceipt.io.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.snapreceipt.io.R

/**
 * A composite list widget that encapsulates common list patterns:
 * - SwipeRefreshLayout (pull-to-refresh)
 * - RecyclerView (list content)
 * - Empty state (image + text)
 * - Load-more indicator (small spinner at bottom)
 * - No-more-data hint (text at bottom)
 * - Center loading indicator (large spinner for initial load)
 *
 * Supported attrs:
 * - `sllEmptyImage`: drawable for empty state (default: `@drawable/img_receipts_empty`)
 * - `sllEmptyText`: text for empty state (default: `@string/no_content`)
 * - `sllNoMoreText`: text for no-more hint (default: `@string/no_more_records`)
 *
 * XML example:
 * ```xml
 * <com.snapreceipt.io.ui.widget.StatefulListLayout
 *     android:id="@+id/stateful_list"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"
 *     app:sllEmptyImage="@drawable/img_receipts_empty"
 *     app:sllEmptyText="@string/no_content" />
 * ```
 *
 * Kotlin example:
 * ```kotlin
 * val list = binding.statefulList
 * list.setAdapter(myAdapter)
 * list.setOnRefreshListener { viewModel.refresh() }
 * list.setOnLoadMoreListener { viewModel.loadMore() }
 * list.applyState(
 *     hasLoaded = state.hasLoaded,
 *     isEmpty = state.empty,
 *     hasMore = state.hasMore,
 *     itemCount = state.items.size,
 *     refreshing = state.refreshing,
 *     loadingMore = state.loadingMore
 * )
 * ```
 */
class StatefulListLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    val recyclerView: RecyclerView
    val swipeRefreshLayout: SwipeRefreshLayout
    private val emptyStateView: View
    private val emptyImageView: ImageView
    private val emptyTextView: TextView
    private val loadMoreIndicator: ProgressBar
    private val noMoreHintView: TextView
    private val centerLoadingView: ProgressBar

    private var onLoadMoreListener: (() -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_stateful_list, this, true)

        swipeRefreshLayout = findViewById(R.id.sll_swipe_refresh)
        recyclerView = findViewById(R.id.sll_recycler_view)
        emptyStateView = findViewById(R.id.sll_empty_state)
        emptyImageView = findViewById(R.id.sll_empty_image)
        emptyTextView = findViewById(R.id.sll_empty_text)
        loadMoreIndicator = findViewById(R.id.sll_load_more)
        noMoreHintView = findViewById(R.id.sll_no_more)
        centerLoadingView = findViewById(R.id.sll_center_loading)

        // Read custom attributes
        val a = context.obtainStyledAttributes(attrs, R.styleable.StatefulListLayout, defStyleAttr, 0)
        a.getDrawable(R.styleable.StatefulListLayout_sllEmptyImage)?.let {
            emptyImageView.setImageDrawable(it)
        }
        a.getText(R.styleable.StatefulListLayout_sllEmptyText)?.let {
            emptyTextView.text = it
        }
        a.getText(R.styleable.StatefulListLayout_sllNoMoreText)?.let {
            noMoreHintView.text = it
        }
        a.recycle()

        // Default RecyclerView configuration
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Built-in load-more scroll listener (triggers when near bottom)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val total = lm.itemCount
                val lastVisible = lm.findLastVisibleItemPosition()
                if (total > 0 && lastVisible >= total - 3) {
                    onLoadMoreListener?.invoke()
                }
            }
        })
    }

    // ── Adapter & Listeners ──────────────────────────────────────────

    fun setAdapter(adapter: RecyclerView.Adapter<*>) {
        recyclerView.adapter = adapter
    }

    fun setOnRefreshListener(listener: () -> Unit) {
        swipeRefreshLayout.setOnRefreshListener { listener() }
    }

    fun setOnLoadMoreListener(listener: () -> Unit) {
        onLoadMoreListener = listener
    }

    // ── State Control ────────────────────────────────────────────────

    fun setRefreshing(refreshing: Boolean) {
        swipeRefreshLayout.isRefreshing = refreshing
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
        loadMoreIndicator.visibility = if (visible) VISIBLE else GONE
    }

    fun showNoMore(visible: Boolean) {
        noMoreHintView.visibility = if (visible) VISIBLE else GONE
    }

    fun showCenterLoading(visible: Boolean) {
        centerLoadingView.visibility = if (visible) VISIBLE else GONE
    }

    // ── Customization ────────────────────────────────────────────────

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
        noMoreHintView.text = text
    }

    fun setNoMoreText(@StringRes resId: Int) {
        noMoreHintView.setText(resId)
    }

    // ── Convenience ──────────────────────────────────────────────────

    /**
     * One-call method to update all list display states at once.
     *
     * Encapsulates the common show-empty / show-no-more / refreshing logic
     * so callers don't need to manage individual view visibility.
     *
     * @param centerLoading whether to show the large center spinner (typically for first load)
     */
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
}
