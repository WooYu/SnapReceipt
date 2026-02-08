package com.snapreceipt.io.ui.widget

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.card.MaterialCardView
import com.snapreceipt.io.R

/**
 * MaterialCardView wrapper with a simple switch to enable/disable shadow.
 *
 * Supported attrs:
 * - `scvShowShadow`: whether card elevation/shadow is visible.
 *
 * XML example:
 * ```xml
 * <com.snapreceipt.io.ui.widget.SnapCardView
 *     android:id="@+id/profile_card"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     app:cardCornerRadius="16dp"
 *     app:scvShowShadow="true" />
 * ```
 *
 * Kotlin example:
 * ```kotlin
 * val card = findViewById<SnapCardView>(R.id.profile_card)
 * card.setShadowEnabled(false)
 * ```
 */
class SnapCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    private var showShadow = true
    private val defaultCardElevation = cardElevation
    private val defaultMaxCardElevation = maxCardElevation

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SnapCardView, defStyleAttr, 0)
        showShadow = typedArray.getBoolean(R.styleable.SnapCardView_scvShowShadow, showShadow)
        typedArray.recycle()
        applyShadow(showShadow)
    }

    fun setShadowEnabled(enabled: Boolean) {
        if (showShadow == enabled) return
        showShadow = enabled
        applyShadow(enabled)
    }

    private fun applyShadow(enabled: Boolean) {
        if (enabled) {
            cardElevation = defaultCardElevation
            maxCardElevation = defaultMaxCardElevation
            return
        }
        // Disable all elevation-related effects for a fully flat card.
        cardElevation = 0f
        maxCardElevation = 0f
        stateListAnimator = null
        translationZ = 0f
    }
}
