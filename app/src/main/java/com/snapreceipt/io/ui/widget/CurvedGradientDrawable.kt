package com.snapreceipt.io.ui.widget

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.drawable.Drawable

/**
 * Drawable that paints a top-to-bottom gradient with a curved bottom edge.
 *
 * Constructor params:
 * - `startColor`: gradient start color at top.
 * - `endColor`: gradient end color at bottom.
 * - `curveHeight`: depth of curved bottom edge.
 *
 * Kotlin usage example:
 * ```kotlin
 * val drawable = CurvedGradientDrawable(
 *     startColor = ContextCompat.getColor(this, R.color.colorPrimary),
 *     endColor = ContextCompat.getColor(this, R.color.colorPrimaryGradientEnd),
 *     curveHeight = 60f
 * )
 * headerView.background = drawable
 * ```
 */
class CurvedGradientDrawable(
    private val startColor: Int,
    private val endColor: Int,
    private val curveHeight: Float = 60f
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()

        // Build vertical gradient.
        val gradient = LinearGradient(
            0f, 0f,
            0f, height,
            startColor,
            endColor,
            Shader.TileMode.CLAMP
        )

        paint.shader = gradient

        // Build the curved shape path.
        path.reset()
        path.moveTo(0f, 0f)
        path.lineTo(width, 0f)
        path.lineTo(width, height - curveHeight)
        
        // Use a quadratic Bezier to create a smooth bottom arc.
        val controlX = width / 2f
        val controlY = height + curveHeight * 0.8f
        path.quadTo(controlX, controlY, 0f, height - curveHeight)
        
        path.close()

        canvas.drawPath(path, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        return android.graphics.PixelFormat.TRANSLUCENT
    }
}
