package com.snapreceipt.io.ui.widget

import android.animation.TimeInterpolator
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.animation.BounceInterpolator
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.snapreceipt.io.R

/**
 * A composite list widget that encapsulates common list patterns:
 * - SwipeRefreshLayout (pull-to-refresh)
 * - RecyclerView (list content)
 * - Empty state (image + text)
 * - In-list refresh header (top item)
 * - In-list load-state footer (bottom item)
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

    private enum class FooterUiState {
        HIDDEN,
        LOADING,
        NO_MORE
    }

    companion object {
        private const val LOAD_MORE_TRIGGER_THRESHOLD = 3
        private const val REFRESH_TRIGGER_DISTANCE_DP = 96
        private const val RELEASE_CLEAR_DELAY_MS = 180L
        private const val RELEASE_REBOUND_DURATION_MS = 280L
        private const val READY_SETTLE_DURATION_MS = 180L
        private const val MAX_RAW_PULL_PROGRESS = 1.8f
        private const val MAX_PULL_PREVIEW_PROGRESS = 1.28f
        private const val OVERSHOOT_DAMPING_FACTOR = 0.35f
    }

    val recyclerView: RecyclerView
    val swipeRefreshLayout: SwipeRefreshLayout
    private val emptyStateView: View
    private val emptyImageView: ImageView
    private val emptyTextView: TextView
    private val centerLoadingView: View

    private val refreshHeaderAdapter = RefreshHeaderAdapter()
    private val footerStateAdapter = FooterStateAdapter()
    private val concatAdapter = ConcatAdapter(refreshHeaderAdapter, EmptyContentAdapter, footerStateAdapter)
    private var contentAdapter: RecyclerView.Adapter<out RecyclerView.ViewHolder> = EmptyContentAdapter

    private var onLoadMoreListener: (() -> Unit)? = null
    private var isRefreshingState: Boolean = false
    private var isLoadingMoreState: Boolean = false
    private var isNoMoreState: Boolean = false
    private var pullStartY: Float = 0f
    private var pullProgress: Float = 0f
    private val reboundInterpolator: TimeInterpolator = BounceInterpolator()
    private val settleInterpolator: TimeInterpolator = DecelerateInterpolator()
    private val clearPullPreviewRunnable = Runnable {
        if (!isRefreshingState) {
            pullProgress = 0f
            refreshHeaderAdapter.animateReboundToIdle(
                durationMs = RELEASE_REBOUND_DURATION_MS,
                interpolator = reboundInterpolator
            )
        }
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_stateful_list, this, true)

        swipeRefreshLayout = findViewById(R.id.sll_swipe_refresh)
        recyclerView = findViewById(R.id.sll_recycler_view)
        emptyStateView = findViewById(R.id.sll_empty_state)
        emptyImageView = findViewById(R.id.sll_empty_image)
        emptyTextView = findViewById(R.id.sll_empty_text)
        centerLoadingView = findViewById(R.id.sll_center_loading)

        configureSwipeRefreshLayout()

        // Read custom attributes
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

        // Default RecyclerView configuration
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = concatAdapter

        // Built-in load-more scroll listener (triggers when near bottom)
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

    private fun configureSwipeRefreshLayout() {
        swipeRefreshLayout.setColorSchemeColors(
            ContextCompat.getColor(context, R.color.colorPrimary),
            ContextCompat.getColor(context, R.color.colorPrimaryGradientEnd)
        )
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
            ContextCompat.getColor(context, android.R.color.white)
        )
        swipeRefreshLayout.setDistanceToTriggerSync(dp(80))
        swipeRefreshLayout.setSlingshotDistance(dp(132))
        swipeRefreshLayout.setProgressViewOffset(
            true,
            -dp(20),
            dp(56)
        )
        swipeRefreshLayout.setOnTouchListener { _, event ->
            handlePullPreview(event)
            false
        }
        swipeRefreshLayout.setOnChildScrollUpCallback { _, _ ->
            recyclerView.visibility == VISIBLE && recyclerView.canScrollVertically(-1)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }

    private fun updateFooterState() {
        val footerState = when {
            isLoadingMoreState -> FooterUiState.LOADING
            isNoMoreState -> FooterUiState.NO_MORE
            else -> FooterUiState.HIDDEN
        }
        footerStateAdapter.setState(footerState)
    }

    private fun calculateDampedPullProgress(rawProgress: Float): Float {
        val normalized = rawProgress.coerceIn(0f, MAX_RAW_PULL_PROGRESS)
        if (normalized <= 1f) {
            val remaining = 1f - normalized
            return 1f - remaining * remaining
        }
        val overshoot = normalized - 1f
        return (1f + overshoot * OVERSHOOT_DAMPING_FACTOR).coerceAtMost(MAX_PULL_PREVIEW_PROGRESS)
    }

    private fun handlePullPreview(event: MotionEvent) {
        if (recyclerView.visibility != VISIBLE) return
        if (isRefreshingState) return
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                swipeRefreshLayout.removeCallbacks(clearPullPreviewRunnable)
                pullStartY = event.y
                pullProgress = 0f
            }

            MotionEvent.ACTION_MOVE -> {
                if (recyclerView.canScrollVertically(-1)) {
                    if (pullProgress > 0f) {
                        pullProgress = 0f
                        refreshHeaderAdapter.setPullProgress(0f)
                    }
                    return
                }
                val dy = event.y - pullStartY
                if (dy <= 0f) {
                    if (pullProgress > 0f) {
                        pullProgress = 0f
                        refreshHeaderAdapter.setPullProgress(0f)
                    }
                    return
                }
                val rawProgress = dy / dp(REFRESH_TRIGGER_DISTANCE_DP).toFloat()
                val progress = calculateDampedPullProgress(rawProgress)
                if (progress != pullProgress) {
                    pullProgress = progress
                    refreshHeaderAdapter.setPullProgress(progress)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                swipeRefreshLayout.removeCallbacks(clearPullPreviewRunnable)
                val releasedProgress = pullProgress
                when {
                    releasedProgress >= 1f -> {
                        refreshHeaderAdapter.animatePullProgressTo(
                            targetProgress = 1f,
                            durationMs = READY_SETTLE_DURATION_MS,
                            interpolator = settleInterpolator
                        )
                        swipeRefreshLayout.postDelayed(clearPullPreviewRunnable, RELEASE_CLEAR_DELAY_MS)
                    }

                    releasedProgress > 0f -> {
                        refreshHeaderAdapter.animateReboundToIdle(
                            durationMs = RELEASE_REBOUND_DURATION_MS,
                            interpolator = reboundInterpolator
                        )
                    }
                }
                pullProgress = 0f
            }
        }
    }

    private fun syncOverlayRefreshIndicator() {
        val showOverlayIndicator = isRefreshingState && recyclerView.visibility != VISIBLE
        swipeRefreshLayout.isRefreshing = showOverlayIndicator
    }

    // ── Adapter & Listeners ──────────────────────────────────────────

    fun setAdapter(adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>) {
        if (adapter === contentAdapter) return
        concatAdapter.removeAdapter(contentAdapter)
        contentAdapter = adapter
        concatAdapter.addAdapter(1, adapter)
    }

    fun setOnRefreshListener(listener: () -> Unit) {
        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.removeCallbacks(clearPullPreviewRunnable)
            if (isRefreshingState) return@setOnRefreshListener
            setRefreshing(true)
            listener()
        }
    }

    fun setOnLoadMoreListener(listener: () -> Unit) {
        onLoadMoreListener = listener
    }

    // ── State Control ────────────────────────────────────────────────

    fun setRefreshing(refreshing: Boolean) {
        if (isRefreshingState == refreshing) return
        swipeRefreshLayout.removeCallbacks(clearPullPreviewRunnable)
        isRefreshingState = refreshing
        refreshHeaderAdapter.setRefreshing(refreshing)
        swipeRefreshLayout.isEnabled = !refreshing
        if (refreshing) {
            pullProgress = 0f
        } else {
            pullProgress = 0f
        }
        syncOverlayRefreshIndicator()
    }

    fun showContent() {
        recyclerView.visibility = VISIBLE
        emptyStateView.visibility = GONE
        syncOverlayRefreshIndicator()
    }

    fun showEmpty() {
        recyclerView.visibility = GONE
        emptyStateView.visibility = VISIBLE
        syncOverlayRefreshIndicator()
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
        footerStateAdapter.setNoMoreText(text)
    }

    fun setNoMoreText(@StringRes resId: Int) {
        setNoMoreText(context.getText(resId))
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

    private class RefreshHeaderAdapter : RecyclerView.Adapter<RefreshHeaderAdapter.RefreshHeaderViewHolder>() {

        private enum class HeaderPhase {
            PREPARE,
            STRETCH,
            READY
        }

        private var isRefreshing: Boolean = false
        private var pullProgress: Float = 0f
        private var progressAnimator: ValueAnimator? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RefreshHeaderViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_stateful_list_refresh_header, parent, false)
            return RefreshHeaderViewHolder(view)
        }

        override fun onBindViewHolder(holder: RefreshHeaderViewHolder, position: Int) {
            holder.bind(isRefreshing, pullProgress)
        }

        override fun onViewRecycled(holder: RefreshHeaderViewHolder) {
            holder.release()
            super.onViewRecycled(holder)
        }

        override fun getItemCount(): Int = if (isRefreshing || pullProgress > 0f) 1 else 0

        fun setPullProgress(progress: Float) {
            if (isRefreshing) return
            stopProgressAnimation()
            updatePullProgress(progress)
        }

        fun animatePullProgressTo(
            targetProgress: Float,
            durationMs: Long,
            interpolator: TimeInterpolator
        ) {
            if (isRefreshing) return
            val start = pullProgress
            val end = targetProgress.coerceIn(0f, MAX_PULL_PREVIEW_PROGRESS)
            if (kotlin.math.abs(start - end) < 0.001f) {
                updatePullProgress(end)
                return
            }
            stopProgressAnimation()
            progressAnimator = ValueAnimator.ofFloat(start, end).apply {
                duration = durationMs
                this.interpolator = interpolator
                addUpdateListener { animation ->
                    updatePullProgress(animation.animatedValue as Float)
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (progressAnimator === animation) {
                            progressAnimator = null
                        }
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        if (progressAnimator === animation) {
                            progressAnimator = null
                        }
                    }
                })
                start()
            }
        }

        fun animateReboundToIdle(durationMs: Long, interpolator: TimeInterpolator) {
            animatePullProgressTo(
                targetProgress = 0f,
                durationMs = durationMs,
                interpolator = interpolator
            )
        }

        fun setRefreshing(refreshing: Boolean) {
            if (isRefreshing == refreshing) return
            stopProgressAnimation()
            val hadItem = itemCount > 0
            isRefreshing = refreshing
            pullProgress = if (refreshing) 1f else 0f
            val hasItem = itemCount > 0
            when {
                !hadItem && hasItem -> notifyItemInserted(0)
                hadItem && !hasItem -> notifyItemRemoved(0)
                else -> notifyItemChanged(0)
            }
        }

        private fun stopProgressAnimation() {
            progressAnimator?.cancel()
            progressAnimator = null
        }

        private fun updatePullProgress(progress: Float) {
            val clamped = progress.coerceIn(0f, MAX_PULL_PREVIEW_PROGRESS)
            if (kotlin.math.abs(pullProgress - clamped) < 0.001f && !(pullProgress > 0f && clamped == 0f)) {
                return
            }
            val hadItem = itemCount > 0
            pullProgress = clamped
            val hasItem = itemCount > 0
            when {
                !hadItem && hasItem -> notifyItemInserted(0)
                hadItem && !hasItem -> notifyItemRemoved(0)
                else -> notifyItemChanged(0)
            }
        }

        class RefreshHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val container: View = itemView.findViewById(R.id.sll_refresh_container)
            private val arrow: ImageView = itemView.findViewById(R.id.sll_refresh_arrow)
            private val loadingView: View = itemView.findViewById(R.id.sll_refresh_loading)
            private val textView: TextView = itemView.findViewById(R.id.sll_refresh_text)

            private var lastProgress: Float = 0f
            private var phase: HeaderPhase? = null
            private var readyPulseFactor: Float = 0f
            private var readyPulseAnimator: ValueAnimator? = null

            fun bind(isRefreshing: Boolean, pullProgress: Float) {
                if (isRefreshing) {
                    phase = null
                    lastProgress = 1f
                    stopReadyPulse(resetFactor = true)
                    arrow.visibility = GONE
                    loadingView.visibility = VISIBLE
                    container.alpha = 1f
                    container.scaleX = 1f
                    container.scaleY = 1f
                    container.translationY = 0f
                    textView.alpha = 1f
                    textView.setText(R.string.refreshing_records)
                    return
                }

                val progress = pullProgress.coerceIn(0f, MAX_PULL_PREVIEW_PROGRESS)
                lastProgress = progress
                val newPhase = resolvePhase(progress)
                if (newPhase != phase) {
                    phase = newPhase
                    handlePhaseChanged(newPhase)
                }
                renderPull(progress)
            }

            fun release() {
                stopReadyPulse(resetFactor = true)
                container.animate().cancel()
                arrow.animate().cancel()
                textView.animate().cancel()
            }

            private fun resolvePhase(progress: Float): HeaderPhase {
                return when {
                    progress < 0.42f -> HeaderPhase.PREPARE
                    progress < 1f -> HeaderPhase.STRETCH
                    else -> HeaderPhase.READY
                }
            }

            private fun handlePhaseChanged(newPhase: HeaderPhase) {
                when (newPhase) {
                    HeaderPhase.PREPARE -> {
                        stopReadyPulse(resetFactor = true)
                        textView.animate()
                            .alpha(0.92f)
                            .setDuration(120L)
                            .start()
                    }

                    HeaderPhase.STRETCH -> {
                        stopReadyPulse(resetFactor = true)
                        textView.animate()
                            .alpha(1f)
                            .setDuration(120L)
                            .start()
                        // A short accent when entering the stretch phase.
                        arrow.animate().cancel()
                        arrow.animate()
                            .scaleX(1.1f)
                            .scaleY(1.1f)
                            .setDuration(120L)
                            .withEndAction {
                                arrow.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(120L)
                                    .start()
                            }
                            .start()
                    }

                    HeaderPhase.READY -> {
                        startReadyPulse()
                        textView.animate()
                            .alpha(1f)
                            .setDuration(120L)
                            .start()
                    }
                }
            }

            private fun renderPull(progress: Float) {
                val baseProgress = progress.coerceIn(0f, 1f)
                val overPullRange = (MAX_PULL_PREVIEW_PROGRESS - 1f).coerceAtLeast(0.0001f)
                val overPullRatio = ((progress - 1f) / overPullRange).coerceIn(0f, 1f)

                arrow.visibility = VISIBLE
                loadingView.visibility = GONE

                container.alpha = 0.32f + 0.68f * baseProgress
                container.translationY = dp(10f) * (1f - baseProgress) - dp(2f) * overPullRatio

                val baseScale = 0.92f + 0.08f * baseProgress + 0.04f * overPullRatio
                val pulseScale = 0.035f * readyPulseFactor
                container.scaleX = baseScale + pulseScale
                container.scaleY = baseScale + pulseScale

                val stageOneProgress = (baseProgress / 0.42f).coerceIn(0f, 1f)
                val stageTwoProgress = ((baseProgress - 0.42f) / 0.58f).coerceIn(0f, 1f)
                val rotation = when {
                    progress < 0.42f -> 80f * stageOneProgress
                    progress < 1f -> 80f + 100f * stageTwoProgress
                    else -> 180f + 12f * overPullRatio
                }
                arrow.rotation = rotation
                val arrowScale = 0.9f + 0.12f * baseProgress + 0.05f * overPullRatio
                arrow.scaleX = arrowScale
                arrow.scaleY = arrowScale
                arrow.alpha = 0.56f + 0.44f * baseProgress

                textView.setText(if (progress >= 1f) R.string.release_to_refresh else R.string.pull_to_refresh)
            }

            private fun startReadyPulse() {
                if (readyPulseAnimator?.isRunning == true) return
                readyPulseAnimator?.cancel()
                readyPulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 620L
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                    interpolator = AccelerateDecelerateInterpolator()
                    addUpdateListener { animation ->
                        readyPulseFactor = animation.animatedValue as Float
                        renderPull(lastProgress)
                    }
                    start()
                }
            }

            private fun stopReadyPulse(resetFactor: Boolean) {
                readyPulseAnimator?.cancel()
                readyPulseAnimator = null
                if (resetFactor) {
                    readyPulseFactor = 0f
                }
            }

            private fun dp(value: Float): Float {
                return value * itemView.resources.displayMetrics.density
            }
        }
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
