package com.snapreceipt.io.ui.common

/**
 * 仅当接口已成功返回且列表为空时显示无数据状态。
 * 默认（未请求/加载中/失败）不显示无数据，避免在未拿到接口结果前展示空状态。
 */
fun shouldShowEmpty(hasLoaded: Boolean, isEmpty: Boolean): Boolean =
    hasLoaded && isEmpty

fun shouldShowNoMore(
    hasLoaded: Boolean,
    hasMore: Boolean,
    itemCount: Int,
    loadingMore: Boolean
): Boolean = hasLoaded && !hasMore && itemCount > 0 && !loadingMore
