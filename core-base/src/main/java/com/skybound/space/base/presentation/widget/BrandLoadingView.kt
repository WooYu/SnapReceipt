package com.skybound.space.base.presentation.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.ColorInt
import com.skybound.space.base.R

/**
 * 品牌风格加载动画视图 —— 替代传统 ProgressBar。
 *
 * 采用 Material Design 3 风格的不确定进度动画：
 * - 弧线自动伸展/收缩 + 持续旋转，呈现流畅的追逐效果
 * - 品牌渐变色沿弧线方向流动（尾部透明渐入 → 主色 → 辅色）
 * - 可选浅色轨道环作为背景
 * - 圆角线帽，精致细腻
 *
 * 支持属性：
 * - `blvPrimaryColor` : 渐变起始色，默认 #103CED
 * - `blvSecondaryColor`: 渐变结束色，默认 #02D7FD
 * - `blvStrokeWidth`   : 描边宽度，默认 3.5dp
 * - `blvShowTrack`     : 是否显示底层轨道环，默认 true
 *
 * ```xml
 * <com.skybound.space.base.presentation.widget.BrandLoadingView
 *     android:layout_width="48dp"
 *     android:layout_height="48dp"
 *     app:blvShowTrack="false" />
 * ```
 */
class BrandLoadingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ── 常量 ────────────────────────────────────────────────
    companion object {
        private const val DEFAULT_PRIMARY_COLOR = 0xFF103CED.toInt()
        private const val DEFAULT_SECONDARY_COLOR = 0xFF02D7FD.toInt()
        private const val DEFAULT_STROKE_WIDTH_DP = 3.5f
        private const val DEFAULT_SIZE_DP = 48

        // 动画时长
        private const val ROTATION_DURATION_MS = 1520L  // 整体旋转一周
        private const val SWEEP_CYCLE_MS = 1333L        // 伸缩动画一个周期

        // 弧线角度范围
        private const val MIN_SWEEP_ANGLE = 10f
        private const val MAX_SWEEP_ANGLE = 270f

        // 轨道透明度 (0-255)
        private const val TRACK_ALPHA = 25
    }

    // ── 配置属性 ────────────────────────────────────────────
    @ColorInt private var primaryColor: Int = DEFAULT_PRIMARY_COLOR
    @ColorInt private var secondaryColor: Int = DEFAULT_SECONDARY_COLOR
    private var strokeWidthPx: Float
    private var showTrack: Boolean = true

    // ── 画笔 ──────────────────────────────────────────────
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    // ── 绘制区域 ──────────────────────────────────────────
    private val arcRect = RectF()

    // ── 渐变着色器 ────────────────────────────────────────
    private var gradientShader: SweepGradient? = null
    private val shaderMatrix = Matrix()

    // ── 动画状态 ──────────────────────────────────────────
    private var globalRotation = 0f      // 整体旋转角度
    private var sweepFraction = 0f       // 伸缩周期进度 0..1
    private var startAngleOffset = 0f    // 每个周期后累加的起始角度偏移

    private var rotationAnimator: ValueAnimator? = null
    private var sweepAnimator: ValueAnimator? = null

    // ── 初始化 ────────────────────────────────────────────
    init {
        val density = context.resources.displayMetrics.density
        strokeWidthPx = DEFAULT_STROKE_WIDTH_DP * density

        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.BrandLoadingView, defStyleAttr, 0)
            primaryColor = a.getColor(R.styleable.BrandLoadingView_blvPrimaryColor, DEFAULT_PRIMARY_COLOR)
            secondaryColor = a.getColor(R.styleable.BrandLoadingView_blvSecondaryColor, DEFAULT_SECONDARY_COLOR)
            strokeWidthPx = a.getDimension(R.styleable.BrandLoadingView_blvStrokeWidth, strokeWidthPx)
            showTrack = a.getBoolean(R.styleable.BrandLoadingView_blvShowTrack, true)
            a.recycle()
        }

        arcPaint.strokeWidth = strokeWidthPx
        trackPaint.strokeWidth = strokeWidthPx
        trackPaint.color = withAlpha(primaryColor, TRACK_ALPHA)

        // 硬件加速层开启后 setShadowLayer 不生效，
        // 但弧线渐变本身已足够美观，不额外添加阴影。
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    // ── 测量 ──────────────────────────────────────────────
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val defaultPx = (DEFAULT_SIZE_DP * resources.displayMetrics.density).toInt()
        val w = resolveSize(defaultPx, widthMeasureSpec)
        val h = resolveSize(defaultPx, heightMeasureSpec)
        val size = minOf(w, h) // 保持正方形
        setMeasuredDimension(size, size)
    }

    // ── 布局变化 ──────────────────────────────────────────
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val inset = strokeWidthPx / 2f + 1f
        arcRect.set(
            paddingLeft + inset,
            paddingTop + inset,
            w - paddingRight - inset,
            h - paddingBottom - inset
        )
        createGradientShader()
    }

    /**
     * 创建 SweepGradient 着色器。
     * 渐变从 0° 起: 透明 → primaryColor → secondaryColor
     * 覆盖 MAX_SWEEP_ANGLE / 360 的范围，其余部分不可见。
     */
    private fun createGradientShader() {
        if (arcRect.width() <= 0f || arcRect.height() <= 0f) return

        val maxFraction = MAX_SWEEP_ANGLE / 360f  // ≈0.75

        gradientShader = SweepGradient(
            arcRect.centerX(),
            arcRect.centerY(),
            intArrayOf(
                withAlpha(primaryColor, 10),   // 尾部：几乎透明
                primaryColor,                   // 主色
                secondaryColor,                 // 头部：辅色
                withAlpha(secondaryColor, 10)   // 超出弧线范围后回归透明
            ),
            floatArrayOf(
                0f,
                maxFraction * 0.3f,    // 30% 处主色完全显现
                maxFraction,           // 弧线头部为辅色
                maxFraction + 0.05f    // 略微过渡后透明
            )
        )
    }

    // ── 绘制 ──────────────────────────────────────────────
    override fun onDraw(canvas: Canvas) {
        if (arcRect.width() <= 0f) return

        // 1. 绘制底层轨道
        if (showTrack) {
            canvas.drawOval(arcRect, trackPaint)
        }

        // 2. 计算当前弧线参数
        val startAngle: Float
        val sweepAngle: Float

        if (sweepFraction <= 0.5f) {
            // 阶段一：弧线伸展（头部前进，尾部不动）
            val t = sweepFraction / 0.5f
            val ease = easeInOutCubic(t)
            startAngle = startAngleOffset
            sweepAngle = MIN_SWEEP_ANGLE + (MAX_SWEEP_ANGLE - MIN_SWEEP_ANGLE) * ease
        } else {
            // 阶段二：弧线收缩（尾部追赶头部）
            val t = (sweepFraction - 0.5f) / 0.5f
            val ease = easeInOutCubic(t)
            val delta = (MAX_SWEEP_ANGLE - MIN_SWEEP_ANGLE) * ease
            startAngle = startAngleOffset + delta
            sweepAngle = MAX_SWEEP_ANGLE - delta
        }

        // 3. 旋转渐变着色器对齐弧线起始位置
        val absoluteAngle = globalRotation + startAngle - 90f // -90 使起点在顶部
        shaderMatrix.setRotate(absoluteAngle, arcRect.centerX(), arcRect.centerY())
        gradientShader?.setLocalMatrix(shaderMatrix)
        arcPaint.shader = gradientShader

        // 4. 绘制弧线
        canvas.save()
        canvas.rotate(globalRotation - 90f, arcRect.centerX(), arcRect.centerY())
        canvas.drawArc(arcRect, startAngle, sweepAngle, false, arcPaint)
        canvas.restore()
    }

    // ── 缓动函数 ──────────────────────────────────────────
    /**
     * 三次缓入缓出插值器，使弧线伸缩更自然流畅。
     */
    private fun easeInOutCubic(t: Float): Float {
        return if (t < 0.5f) {
            4f * t * t * t
        } else {
            val p = -2f * t + 2f
            1f - p * p * p / 2f
        }
    }

    // ── 动画生命周期 ──────────────────────────────────────
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimationIfVisible()
    }

    override fun onDetachedFromWindow() {
        stopAnimation()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            startAnimationIfVisible()
        } else {
            stopAnimation()
        }
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) {
            startAnimationIfVisible()
        } else {
            stopAnimation()
        }
    }

    private fun startAnimationIfVisible() {
        if (visibility != VISIBLE || windowVisibility != VISIBLE) return
        if (rotationAnimator?.isRunning == true) return
        startAnimation()
    }

    private fun startAnimation() {
        // 整体旋转动画
        rotationAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = ROTATION_DURATION_MS
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                globalRotation = it.animatedValue as Float
                invalidate()
            }
            start()
        }

        // 弧线伸缩动画
        sweepAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = SWEEP_CYCLE_MS
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                sweepFraction = it.animatedValue as Float
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationRepeat(animation: Animator) {
                    // 每个周期结束后，累加偏移使弧线持续前进
                    startAngleOffset = (startAngleOffset + MAX_SWEEP_ANGLE + MIN_SWEEP_ANGLE) % 360f
                }
            })
            start()
        }
    }

    private fun stopAnimation() {
        rotationAnimator?.cancel()
        rotationAnimator = null
        sweepAnimator?.cancel()
        sweepAnimator = null
    }

    // ── 工具方法 ──────────────────────────────────────────
    /**
     * 设置颜色的透明度（保留 RGB 通道）。
     */
    private fun withAlpha(@ColorInt color: Int, alpha: Int): Int {
        return Color.argb(
            alpha.coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }
}
