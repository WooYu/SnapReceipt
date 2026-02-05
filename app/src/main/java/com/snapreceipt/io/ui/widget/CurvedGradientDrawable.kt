package com.snapreceipt.io.ui.widget

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.drawable.Drawable

/**
 * 自定义 Drawable，用于创建带有弧形底边的渐变背景
 * 
 * @param startColor 渐变起始颜色（左上角）
 * @param endColor 渐变结束颜色（右下角）
 * @param curveHeight 弧形的高度（向下弯曲的深度）
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

        // 创建从左上到右下的线性渐变
        val gradient = LinearGradient(
            0f, 0f,  // 起始点（左上角）
            width, height,  // 结束点（右下角）
            startColor,
            endColor,
            Shader.TileMode.CLAMP
        )

        paint.shader = gradient

        // 创建弧形路径
        path.reset()
        path.moveTo(0f, 0f)  // 左上角
        path.lineTo(width, 0f)  // 右上角
        path.lineTo(width, height - curveHeight)  // 右下角上方
        
        // 创建底部弧形：使用二次贝塞尔曲线
        // 控制点位于底部中间，向下弯曲，形成平滑的弧形
        val controlX = width / 2f
        val controlY = height + curveHeight * 0.8f  // 控制点稍微低一些，使弧形更明显
        path.quadTo(controlX, controlY, 0f, height - curveHeight)  // 弧形到底部左侧
        
        path.close()  // 自动回到起点

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
