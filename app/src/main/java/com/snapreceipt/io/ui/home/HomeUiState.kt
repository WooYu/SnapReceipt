package com.snapreceipt.io.ui.home

import com.snapreceipt.io.domain.model.ReceiptEntity

/**
 * 首页列表 UI 状态。
 *
 * 统一描述首页收据列表、分页加载、错误展示以及 OCR 识别中的临时状态，
 * 供 [HomeFragment] 渲染界面。
 */
data class HomeUiState(
    val receipts: List<ReceiptEntity> = emptyList(),
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val empty: Boolean = true,
    val hasLoaded: Boolean = false,
    val hasMore: Boolean = true,
    /** OCR 识别过程中的状态文案资源 ID；仅在上传/扫描期间非空。 */
    val recognitionStatusResId: Int? = null
)
