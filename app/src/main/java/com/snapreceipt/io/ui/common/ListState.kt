package com.snapreceipt.io.ui.common

fun shouldShowEmpty(hasLoaded: Boolean, isEmpty: Boolean): Boolean =
    hasLoaded && isEmpty

fun shouldShowNoMore(
    hasLoaded: Boolean,
    hasMore: Boolean,
    itemCount: Int,
    loadingMore: Boolean
): Boolean = hasLoaded && !hasMore && itemCount > 0 && !loadingMore
