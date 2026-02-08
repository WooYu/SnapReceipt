package com.snapreceipt.io.ui.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.StringRes
import com.snapreceipt.io.R

/**
 * A reusable full-screen loading overlay with a centered card
 * containing a ProgressBar and optional status text.
 *
 * Replaces repeated patterns of semi-transparent overlays with
 * progress indicators found across the app.
 *
 * Supported attrs:
 * - `lovText`: default status text shown below the spinner.
 * - `lovOverlayColor`: background overlay color (default: #80000000).
 *
 * XML example:
 * ```xml
 * <com.snapreceipt.io.ui.widget.LoadingOverlayView
 *     android:id="@+id/loading_overlay"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"
 *     app:lovText="@string/exporting_receipts" />
 * ```
 *
 * Kotlin example:
 * ```kotlin
 * binding.loadingOverlay.show(R.string.uploading_receipt)
 * binding.loadingOverlay.setText(R.string.scanning_receipt)
 * binding.loadingOverlay.hide()
 * ```
 */
class LoadingOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val textView: TextView

    init {
        // Overlay consumes touch events to block interaction below
        isClickable = true
        isFocusable = true
        visibility = GONE

        LayoutInflater.from(context).inflate(R.layout.view_loading_overlay, this, true)
        textView = findViewById(R.id.lov_text)

        // Read custom attributes
        val a = context.obtainStyledAttributes(attrs, R.styleable.LoadingOverlayView, defStyleAttr, 0)
        val overlayColor = a.getColor(
            R.styleable.LoadingOverlayView_lovOverlayColor,
            DEFAULT_OVERLAY_COLOR
        )
        setBackgroundColor(overlayColor)

        a.getText(R.styleable.LoadingOverlayView_lovText)?.let { text ->
            if (text.isNotBlank()) {
                textView.text = text
                textView.visibility = VISIBLE
            }
        }
        a.recycle()
    }

    /**
     * Show the overlay. If [text] is provided, the status text is updated and shown.
     */
    fun show(text: CharSequence? = null) {
        if (text != null) {
            textView.text = text
            textView.visibility = VISIBLE
        }
        visibility = VISIBLE
    }

    /**
     * Show the overlay with a string resource for the status text.
     */
    fun show(@StringRes resId: Int) {
        textView.setText(resId)
        textView.visibility = VISIBLE
        visibility = VISIBLE
    }

    /**
     * Hide the overlay.
     */
    fun hide() {
        visibility = GONE
    }

    /**
     * Update the status text without changing overlay visibility.
     */
    fun setText(text: CharSequence) {
        textView.text = text
        textView.visibility = if (text.isNotBlank()) VISIBLE else GONE
    }

    /**
     * Update the status text with a string resource.
     */
    fun setText(@StringRes resId: Int) {
        textView.setText(resId)
        textView.visibility = VISIBLE
    }

    /**
     * Whether the overlay is currently showing.
     */
    val isShowing: Boolean
        get() = visibility == View.VISIBLE

    companion object {
        private val DEFAULT_OVERLAY_COLOR = Color.parseColor("#80000000")
    }
}
