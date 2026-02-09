package com.snapreceipt.io.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class CouponDividerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ===== 可配置参数 =====
    var circleRadius = 13f.dp   // 半圆半径
    var circleColor = Color.parseColor("#F3F6FD")  // 半圆颜色（与外部背景一致）
    var dashColor = Color.parseColor("#F3F6FD")  // 虚线颜色
    var dashWidth = 6f.dp    // 每段虚线长度
    var dashGap = 6f.dp    // 虚线段之间的间隔
    var dashStroke = 1.5f.dp  // 虚线粗细（高度）
    var circleToLineGap = 3f.dp  // 半圆和虚线之间的间距

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun updatePaints() {
        circlePaint.apply {
            color = circleColor
            style = Paint.Style.FILL
        }
        dashPaint.apply {
            color = dashColor
            style = Paint.Style.STROKE
            strokeWidth = dashStroke
            pathEffect = DashPathEffect(floatArrayOf(dashWidth, dashGap), 0f)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        updatePaints()

        val centerY = height / 2f

        // 左半圆
        canvas.drawCircle(0f, centerY, circleRadius, circlePaint)
        // 右半圆
        canvas.drawCircle(width.toFloat(), centerY, circleRadius, circlePaint)

        // 虚线
        val startX = circleRadius + circleToLineGap
        val endX = width - circleRadius - circleToLineGap
        canvas.drawLine(startX, centerY, endX, centerY, dashPaint)
    }

    private val Float.dp: Float
        get() = this * resources.displayMetrics.density
}