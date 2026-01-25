package com.snapreceipt.io.ui.common

fun shouldShowEmpty(hasLoaded: Boolean, isEmpty: Boolean): Boolean =
    hasLoaded && isEmpty
