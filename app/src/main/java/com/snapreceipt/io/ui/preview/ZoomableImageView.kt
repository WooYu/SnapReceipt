package com.snapreceipt.io.ui.preview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.widget.AppCompatImageView

/**
 * ImageView that supports pinch-to-zoom, double-tap zoom toggle, and pan when zoomed.
 *
 * Zoom range: 1x (fit-center) to [MAX_ZOOM]x.
 * Double-tap toggles between 1x and [DOUBLE_TAP_ZOOM]x.
 */
class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private companion object {
        const val MAX_ZOOM = 5f
        const val DOUBLE_TAP_ZOOM = 2.5f
        const val ANIM_DURATION_MS = 250L
        const val ZOOM_THRESHOLD = 1.01f
    }

    private val baseMatrix = Matrix()
    private val suppMatrix = Matrix()
    private val drawMatrix = Matrix()
    private val matrixValues = FloatArray(9)

    // Pre-allocated for mappedImageRect() to avoid GC pressure during touch events
    private val tempMatrix = Matrix()
    private val tempRect = RectF()

    private val scaleDetector = ScaleGestureDetector(context, PinchListener())
    private val tapDetector = GestureDetector(context, TapListener())

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var isPinching = false
    private var zoomAnimator: ValueAnimator? = null

    var onSingleTapListener: (() -> Unit)? = null

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        fitToView()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        fitToView()
    }

    override fun onDetachedFromWindow() {
        zoomAnimator?.cancel()
        zoomAnimator = null
        super.onDetachedFromWindow()
    }

    private fun fitToView() {
        val d = drawable ?: return
        if (width == 0 || height == 0) return

        val dw = d.intrinsicWidth.toFloat()
        val dh = d.intrinsicHeight.toFloat()
        if (dw <= 0f || dh <= 0f) return

        val scale = minOf(width / dw, height / dh)

        baseMatrix.reset()
        baseMatrix.postScale(scale, scale)
        baseMatrix.postTranslate((width - dw * scale) / 2f, (height - dh * scale) / 2f)

        suppMatrix.reset()
        applyMatrices()
    }

    private fun applyMatrices() {
        drawMatrix.set(baseMatrix)
        drawMatrix.postConcat(suppMatrix)
        imageMatrix = drawMatrix
    }

    private fun currentZoom(): Float {
        suppMatrix.getValues(matrixValues)
        return matrixValues[Matrix.MSCALE_X]
    }

    /**
     * Returns the image rect mapped through base + supplement matrices.
     * Uses pre-allocated [tempMatrix]/[tempRect] — result is only valid until next call.
     */
    private fun mappedImageRect(): RectF {
        val d = drawable
        if (d == null || d.intrinsicWidth <= 0 || d.intrinsicHeight <= 0) {
            tempRect.setEmpty()
            return tempRect
        }
        tempRect.set(0f, 0f, d.intrinsicWidth.toFloat(), d.intrinsicHeight.toFloat())
        tempMatrix.set(baseMatrix)
        tempMatrix.postConcat(suppMatrix)
        tempMatrix.mapRect(tempRect)
        return tempRect
    }

    /**
     * Keep the image within view bounds: center when smaller, prevent edge gaps when larger.
     */
    private fun clampToBounds() {
        val rect = mappedImageRect()
        if (rect.isEmpty) return

        val vw = width.toFloat()
        val vh = height.toFloat()

        var dx = 0f
        var dy = 0f

        if (rect.width() <= vw) {
            dx = (vw - rect.width()) / 2f - rect.left
        } else {
            if (rect.left > 0f) dx = -rect.left
            else if (rect.right < vw) dx = vw - rect.right
        }

        if (rect.height() <= vh) {
            dy = (vh - rect.height()) / 2f - rect.top
        } else {
            if (rect.top > 0f) dy = -rect.top
            else if (rect.bottom < vh) dy = vh - rect.bottom
        }

        if (dx != 0f || dy != 0f) {
            suppMatrix.postTranslate(dx, dy)
        }
    }

    private fun isZoomedIn(): Boolean = currentZoom() > ZOOM_THRESHOLD

    @Suppress("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        tapDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                zoomAnimator?.cancel()
                activePointerId = event.getPointerId(0)
                lastTouchX = event.x
                lastTouchY = event.y
                if (isZoomedIn()) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                // Second finger → likely pinch; prevent parent from intercepting
                parent?.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isPinching && activePointerId != MotionEvent.INVALID_POINTER_ID) {
                    val idx = event.findPointerIndex(activePointerId)
                    if (idx >= 0 && isZoomedIn()) {
                        val dx = event.getX(idx) - lastTouchX
                        val dy = event.getY(idx) - lastTouchY
                        suppMatrix.postTranslate(dx, dy)
                        clampToBounds()
                        applyMatrices()
                        lastTouchX = event.getX(idx)
                        lastTouchY = event.getY(idx)
                    }
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val leavingIndex = event.actionIndex
                if (event.getPointerId(leavingIndex) == activePointerId) {
                    val stayIndex = if (leavingIndex == 0) 1 else 0
                    if (stayIndex < event.pointerCount) {
                        activePointerId = event.getPointerId(stayIndex)
                        lastTouchX = event.getX(stayIndex)
                        lastTouchY = event.getY(stayIndex)
                    } else {
                        activePointerId = MotionEvent.INVALID_POINTER_ID
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()

    private fun animateZoomTo(targetZoom: Float, focusX: Float, focusY: Float) {
        val startZoom = currentZoom()
        if (startZoom < 0.001f) return

        zoomAnimator?.cancel()
        val snapshot = Matrix(suppMatrix)
        zoomAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ANIM_DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                val current = startZoom + (targetZoom - startZoom) * anim.animatedFraction
                val scaleFactor = current / startZoom
                suppMatrix.set(snapshot)
                suppMatrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
                clampToBounds()
                applyMatrices()
            }
            start()
        }
    }

    private inner class PinchListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            isPinching = true
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val factor = detector.scaleFactor
            val nextZoom = currentZoom() * factor
            if (nextZoom in 0.5f..MAX_ZOOM) {
                suppMatrix.postScale(factor, factor, detector.focusX, detector.focusY)
                clampToBounds()
                applyMatrices()
            }
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            isPinching = false
            if (currentZoom() < 1f) {
                animateZoomTo(1f, width / 2f, height / 2f)
            }
        }
    }

    private inner class TapListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val target = if (currentZoom() < DOUBLE_TAP_ZOOM - 0.1f) DOUBLE_TAP_ZOOM else 1f
            animateZoomTo(target, e.x, e.y)
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            performClick()
            onSingleTapListener?.invoke()
            return true
        }
    }
}
